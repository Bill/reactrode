package com.thoughtpropulsion.reactrode;

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

  static final int MAX_GENERATIONS_CAPACITY = 3;
  static final int DEFAULT_PUTALL_REQUEST_AMOUNT = 100;

  // cells keyed on offset (see toOffset())
  private Map<Integer, Boolean> cells = new ConcurrentHashMap<>();

  // key is generation, value is list of sinks for fluxes subscribed for that generation
  private ConcurrentNavigableMap<Integer, List<FluxSink<Cell>>>
      queries = new ConcurrentSkipListMap<>();

  private final int columns; // x
  private final int rows;    // y

  public GameStateHotChanges(final int columns, final int rows) {
    this.columns = columns;
    this.rows = rows;
  }

  @Override
  public Mono<Boolean> put(final Mono<Cell> cellMono) {
    return
        JavaLang.returning(
            cellMono.map(this::put),
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
  public void putAll(final Flux<Cell> cellFlux) {
    cellFlux
        .map(cell -> put(cell))
        .subscribeOn(Schedulers.parallel())
        .subscribe(createBackpressure());
  }

  @Override
  public Mono<Boolean> get(final Mono<Coordinate> coordinateMono) {
    return coordinateMono
        .map(coordinate -> JavaLang.toBooleanNotNull(cells.get(toOffset(coordinate))));
  }

  /**
   * This implementation of the queries flux is hot. Cells are distributed to these hot fluxes as
   * they arrive (via various Reactive put methods).
   * @see {@link #distributeToQueriesHook(Cell)} and {@link #completeOldQueriesHook(Cell)}
   */
  @Override
  public Flux<Cell> changes(final Mono<Integer> generationMono) {
    return generationMono
        .flatMapMany(generation ->
            Flux.<Cell>create(sink ->
                getQueriesForGeneration(generation).add(sink)));
  }

  private int toOffset(final Coordinate coordinate) {
    return Coordinate.toOffset(coordinate.y, coordinate.x, coordinate.generation, columns, rows);
  }

  private BaseSubscriber<Boolean> createBackpressure() {
    return new BaseSubscriber<Boolean>() {
      @Override
      protected void hookOnSubscribe(final Subscription subscription) {
        request(nextRequest());
      }

      @Override
      protected void hookOnNext(final Boolean oldIsAlive) {
        request(nextRequest());
      }
    };
  }

  /**
   * Imperative logic associated w/ {@link #put(Mono)} and {@link #putAll(Flux)}
   *
   * N.B. that this method must not return a null reference lest it run afoul of Reactor {@code
   * map()} constraints.
   */
  private Boolean put(final Cell cell) {
    return JavaLang.returning(
        JavaLang.toBooleanNotNull(cells.put(toOffset(cell.coordinate), cell.isAlive)),
        oldIsAlive -> {
          // FIXME: we are not reacting to backpressure from query subscribers. Could we?
          distributeToQueriesHook(cell);
          completeOldQueriesHook(cell);
          gcHook(cell);
        });
  }

  private void distributeToQueriesHook(final Cell cell) {
    final List<FluxSink<Cell>>
        queriesForGeneration =
        getQueriesForGeneration(cell.coordinate.generation);
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
    if (cell.coordinate.x == 0 && cell.coordinate.y == 0) {
      final Map<Integer, List<FluxSink<Cell>>>
          expiredGenerations = queries.headMap(cell.coordinate.generation, false);
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
        Coordinate.create(
            cell.coordinate.x,
            cell.coordinate.y,
            cell.coordinate.generation - MAX_GENERATIONS_CAPACITY,
            columns,
            rows)
            .toOffset(columns, rows));
  }

  /**
   * Backpressure calculation for the flux sent to put().
   * @return next request amount
   */
  private int nextRequest() {
    final int generationSize = columns * rows;
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