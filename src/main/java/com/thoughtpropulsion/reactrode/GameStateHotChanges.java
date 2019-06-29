package com.thoughtpropulsion.reactrode;

import static com.thoughtpropulsion.reactrode.Functional.returning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class GameStateHotChanges implements GameState {

  private static final int MAX_GENERATIONS_CAPACITY = 3;
  private static final int DEFAULT_PUTALL_REQUEST_AMOUNT = 100;

  private final CoordinateSystem coordinateSystem;

  // cells keyed on offset (see toOffset())
  // TODO: consider making this a ring buffer!!! just need get(offset), put(offset,isAlive), size(), remove(offset)
  private final Map<Integer, Boolean> cells = new ConcurrentHashMap<>();

  // key is generation, value is list of sinks for fluxes subscribed for that generation
  private final ConcurrentNavigableMap<Integer, List<FluxSink<Cell>>>
      queries = new ConcurrentSkipListMap<>();

  public GameStateHotChanges(final int columns, final int rows) {
    coordinateSystem = new CoordinateSystem(columns, rows);
  }

  @Override
  public Mono<Cell> put(final Mono<Cell> cellMono) {
    return
        returning(
            cellMono.flatMap(cell-> putImperative(cell)),
            mappedCellMono ->
            /*
             As a side-effect (before returning the mapped mono) decouple from calling thread
             and set up a base subscriber to control request amount so as to protect ourselves
             from our cells map growing too large.
             */
                mappedCellMono
                    .subscribeOn(Schedulers.parallel()) // decouple from calling thread
                    .subscribe(createBackpressure()));
  }

  @Override
  public Mono<Cell> put(final Cell cell) {
    return putImperative(cell);
  }

  @Override
  public void putAll(final Flux<Cell> cellFlux) {
    cellFlux
        .flatMap(cell -> putImperative(cell))
        .subscribeOn(Schedulers.parallel())
        .subscribe(createBackpressure());
  }

  @Override
  public Mono<Boolean> get(final Coordinates coordinates) {
    return Mono.justOrEmpty(cells.get(coordinateSystem.toOffset(coordinates)));
  }

  /**
   * This implementation of the queries flux is hot. Cells are distributed to these hot fluxes as
   * they arrive (via various Reactive put methods).
   * @see {@link #distributeToQueriesHook(Cell)} and {@link #completeOldQueriesHook(Cell)}
   * @param generation
   */
  @Override
  public Flux<Cell> changes(final int generation) {
    return Flux.<Cell>create(sink ->
                getQueriesForGeneration(generation).add(sink));
  }

  @Override
  public Flux<Flux<Cell>> generations(final int generation) {
    return null;
  }

  private BaseSubscriber<Cell> createBackpressure() {
    return new BaseSubscriber<Cell>() {
      @Override
      protected void hookOnSubscribe(final Subscription subscription) {
        request(nextRequest());
      }

      @Override
      protected void hookOnNext(final Cell oldIsAlive) {
        request(nextRequest());
      }
    };
  }

  /**
   * Imperative logic associated w/ {@link #put(Mono)} and {@link #putAll(Flux)}
   */
  private Mono<Cell> putImperative(final Cell cellArg) {
    return Mono.justOrEmpty(returning(
        cellArg,
        cell -> {
          cells.put(coordinateSystem.toOffset(cell.coordinates), cell.isAlive);
          // FIXME: we are not reacting to backpressure from query subscribers. Could we?
          distributeToQueriesHook(cell);
          completeOldQueriesHook(cell);
          gcHook(cell);
        }));
  }

  private void distributeToQueriesHook(final Cell cell) {
    final List<FluxSink<Cell>>
        queriesForGeneration =
        getQueriesForGeneration(cell.coordinates.generation);
    queriesForGeneration.forEach(sink -> sink.next(cell));
  }

  /**
   * Implements the policy for completing queries, based on the (new) cell just processed.
   *
   * Current policy is to complete all queries for generations older than this cell's generation.
   *
   * This method should be called after the cell has been delivered to queries, so queries won't
   * miss the last cell of their generation.
   */
  private void completeOldQueriesHook(final Cell cell) {
    if (cell.coordinates.x == 0 && cell.coordinates.y == 0) {
      final Map<Integer, List<FluxSink<Cell>>>
          expiredGenerations = queries.headMap(cell.coordinates.generation, false);
      expiredGenerations.forEach((generation, expiredQueries) ->
          expiredQueries.forEach(sink -> sink.complete()));
    }
  }

  /**
   * Policy hook to retire old cells.
   */
  private void gcHook(final Cell cell) {
    // remove corresponding cell from MAX_GENERATIONS_CAPACITY ago
    cells.remove(
        coordinateSystem.toOffset(
            cell.coordinates.x,
            cell.coordinates.y,
            cell.coordinates.generation - MAX_GENERATIONS_CAPACITY));
  }

  /**
   * Backpressure calculation for the flux sent to put().
   * @return next request amount
   */
  private int nextRequest() {
    final int generationSize = coordinateSystem.size();
    final int capacity = MAX_GENERATIONS_CAPACITY * generationSize;
    if (cells.size() < capacity) {
      return DEFAULT_PUTALL_REQUEST_AMOUNT;
    } else {
      return 0; // close the valve
    }
  }

  /**
   * Get the (possibly empty) list of query {@code List<FluxSink<Cell>>} for the {@param
   * generation}
   * @param generation is the generation of interest
   * @return not null {@code List<FluxSink<Cell>>} for the generation
   */
  private List<FluxSink<Cell>> getQueriesForGeneration(final Integer generation) {
    return queries.computeIfAbsent(generation, (k) -> new ArrayList<>());
  }

}