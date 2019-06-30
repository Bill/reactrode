package com.thoughtpropulsion.reactrode;

import static com.thoughtpropulsion.reactrode.Functional.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

import io.vavr.control.Option;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * An early (failed) attempt to provide a cold {@link Flux} of states via
 * {@link GameState#changes(int)}.
 *
 * Storage is limited. Only states in the most recent generations are retained.
 *
 * When more space is needed, old states are discarded. Old states are discarded a generation
 * at a time. No partial generations are maintained.
 *
 * A generation will not be disposed if outstanding queries are active for it. So we can have
 * a situation where more space is needed but we cannot free it up (yet). In that case, inbound
 * data flow (via the {@link Publisher} provided to {@link #putAll(Flux)} or {@link #put(Mono)}
 * will be paused (via backpressure).
 *
 * Later, after one or more queries have completed (via cancellation, normal completion, or error),
 * it may be possible to release a generation, thereby freeing up space. In that case backpressure
 * will be relieved and messages will again be permitted to flow in.
 *
 */
public class GameStateColdChanges implements GameState {

  static final int MAX_GENERATIONS_CAPACITY = 3;
  private final CoordinateSystem coordinateSystem;
  private int cellsDemanded;

  // cells keyed on offset (see toOffset())
  // TODO: consider making this a ring buffer!!! just need get(offset), put(offset,isAlive), size(), remove(offset)
  private Map<Integer,Boolean> cells = new ConcurrentHashMap<>();

  // key is generation, value is list of sinks for fluxes subscribed for that generation
  private ConcurrentNavigableMap<Integer, List<FluxSink<Cell>>> queries = new ConcurrentSkipListMap<>();

  private final AtomicReference<Option<Coordinates>> highestOffsetCoordinate = new AtomicReference<>(Option.none());

  public GameStateColdChanges(final int columns, final int rows) {
    coordinateSystem = new CoordinateSystem(columns, rows);
    this.cellsDemanded = 0;
  }

  @Override
  public Mono<Cell> put(final Mono<Cell> cellMono) {

    // could do something like this to show that the blocking code isn't going to block
    //return Mono.fromCallable(() -> put(cellMono.block())).subscribeOn(Schedulers.parallel());
    // or we could have a blocking put call that did the backpressure calculation
    //return Mono.fromCallable(() -> putBlocking(cellMono.block())).subscribeOn(Schedulers.parallel());

    /*
    to backpressure here either chain to putAll() (reusing it's backpressure mech)
    or delay (in time) responding e.g. proportional to space remaining
     */
    return
        returning(
            cellMono.flatMap(cell -> put(cell)),
            mappedCellMono ->
            /*
             As a side-effect (before returning the mapped mono) decouple from calling thread
             and set up a base subscriber to control request amount so as to protect ourselves
             from our cells map growing too large.
             */
            mappedCellMono
              .subscribeOn(Schedulers.parallel()) // decouple from calling thread
              .subscribe(createBackpressuringSubscriber()));
  }

  @Override
  public Mono<Cell> put(final Cell cellArg) {
    return Mono.justOrEmpty(returning(
        cellArg,
        cell -> {
          cells.put(coordinateSystem.toOffset(cell.coordinates), cell.isAlive);
          System.out.printf("put(%s)%n", cell);
          recordHighestOffsetHook(cell);
          distributeToQueriesHook(cell);
        }));
  }

  @Override
  public void putAll(final Flux<Cell> cellFlux) {
    cellFlux
        .flatMap(cell -> put(cell))
        .subscribeOn(Schedulers.parallel())
        .subscribe(createBackpressuringSubscriber());
  }

  @Override
  public Mono<Boolean> get(final Coordinates coordinates) {
    return Mono.justOrEmpty(cells.get(coordinateSystem.toOffset(coordinates)));
  }

  /**
   * This implementation of the queries flux is cold. Cells are delivered (from cells map) on-demand
   * (not as they arrive).
   *
   * The value of this approach is it lets us experiment with backpressure:
   * if cells arrive and we have outstanding queries for old generations we can't just complete
   * those old queries (fluxes) because they might still have cells to deliver.
   *
   * Contrast with {@link GameState#changes(int)} where we can complete the old queries
   * (fluxes) whenever we are sure we aren't going to see any new cells matching the query's
   * generation criterion.
   *
   * @param generation
   * @return
   *
   * @see {@link #distributeToQueriesHook(Cell)}
   */
  @Override
  public Flux<Cell> changes(final int generation) {
    return highestCompleteGeneration()
        .flatMap(highestComplete -> highestComplete < generation ? Option.none()
            : Option.some(generation))
        .map(highestComplete ->
            // a flux that can start producing items right now
            createColdQueryFlux(generation))
        .getOrElse(
                  /*
                   A flux for which we have captured the sink. Some thread will produce to that sink
                   after {@code generation} is ready.
                   */
            () -> Flux.create(sink -> getQueriesForGeneration(generation).add(sink))); // flatMapMany
  }

  @Override
  public Flux<Generation> generations() {
    return null;
  }

  private Flux<Cell> createColdQueryFlux(final Integer generation) {
    return Flux.<Cell, Coordinates>generate(
        ()->coordinateSystem.createCoordinate(0,0,generation),
        (coordinate,sink)-> {
          if (coordinate.generation > generation) {
            sink.complete();
          } else {
            /*
             Danger! While this class is designed so that in this case, cells.get() will not
             return null, nevertheless, per the Java type system, cells.get() can return null.
             If a null is returned then sink.next() will barf on it.
             */
            sink.next(Cell.create(coordinate, cells.get(coordinate)));
          }
          return inc(coordinate);
        })
        .doOnComplete(() -> {
          /*
           TODO: if this is the last query for this generation and this is the lowest generation
           we can free up cells for this generation (and older).
           */

        });
  }

  private Coordinates inc(final Coordinates coordinates) {
    return coordinateSystem.createCoordinate(
        coordinateSystem.toOffset(coordinates) + 1);
  }

  private BaseSubscriber<Cell> createBackpressuringSubscriber() {
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

  private void recordHighestOffsetHook(final Cell cell) {
    highestOffsetCoordinate.updateAndGet(
        oldHighest -> Option.some(
          Coordinates.max(
              oldHighest.getOrElse(cell.coordinates),
              cell.coordinates)));
  }

  private Option<Integer> highestCompleteGeneration() {
    return highestOffsetCoordinate.get().flatMap(highest ->
        Option.some( highest.x < coordinateSystem.columns - 1 &&
            highest.y < coordinateSystem.rows - 1 &&
            highest.generation > Integer.MIN_VALUE ?
            highest.generation - 1:
            highest.generation));
  }

  private void distributeToQueriesHook(final Cell cell) {
    highestCompleteGeneration().peek(generation -> {

      /*
       Because this method is run from multiple threads concurrently, we first get the
       snapshot and then iterate over it. If we can actually win the removal race for
       a particular generation (key) then we process the queries for that key.
       */

      final ConcurrentNavigableMap<Integer, List<FluxSink<Cell>>> readyQueriesSnapshot =
          queries.headMap(generation, true);

      for(final Integer readyGeneration:readyQueriesSnapshot.navigableKeySet()) {
        final List<FluxSink<Cell>> readyQueries = readyQueriesSnapshot.remove(readyGeneration);
        if (null != readyQueries) {
          for(final FluxSink<Cell> sink:readyQueries) {
            feedQuery(sink, generation);
          }
        }
      }
    });
  }

  private void feedQuery(final FluxSink<Cell> sink, final int generation) {
    createColdQueryFlux(generation)
        // TODO: don't use the global scheduler here--_configure_ with a (parallel) scheduler
        .subscribeOn(Schedulers.parallel())
        // TODO: confirm it's ok to unconditionally push to this sink here
        .subscribe(cell -> sink.next(cell));
  }

  /**
   * Backpressure calculation for the flux sent to put().
   *
   * @return next request amount
   */
  private int nextRequest() {
    if (cellsDemanded > 0)
      return 0;
    final int generationSize = coordinateSystem.size();
    final int capacity = MAX_GENERATIONS_CAPACITY * generationSize;
    cellsDemanded = Math.max(0, capacity - cells.size());

    cellsDemanded = Math.min(1,cellsDemanded);

    System.out.println("demanding: " + cellsDemanded);
    return cellsDemanded;
  }

  /**
   * Get the (possibly empty) list of query {@code List<FluxSink<Cell>>} for the {@param generation}
   *
   * @param generation is the generation of interest
   * @return not null {@code List<FluxSink<Cell>>} for the generation
   */
  private List<FluxSink<Cell>> getQueriesForGeneration(final Integer generation) {
    return queries.computeIfAbsent(generation, (k) -> new ArrayList<>());
  }

}
