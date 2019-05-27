package com.thoughtpropulsion.reactrode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

import io.vavr.control.Option;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class GameStateColdChanges implements GameState {

  static final int MAX_GENERATIONS_CAPACITY = 3;
  private final CoordinateSystem coordinateSystem;
  private int cellsDemanded;

  // cells keyed on offset (see toOffset())
  // TODO: consider making this a ring buffer!!! just need get(offset), put(offset,isAlive), size(), remove(offset)
  private Map<Integer,Boolean> cells = new ConcurrentHashMap<>();

  // key is generation, value is list of sinks for fluxes subscribed for that generation
  private ConcurrentNavigableMap<Integer, List<FluxSink<Cell>>> queries = new ConcurrentSkipListMap<>();

  private final AtomicReference<Option<Coordinate>> highestOffsetCoordinate = new AtomicReference<>(Option.none());

  public GameStateColdChanges(final int columns, final int rows) {
    coordinateSystem = new CoordinateSystem(columns, rows);
    this.cellsDemanded = 0;
  }

  @Override
  public Mono<Boolean> put(final Mono<Cell> cellMono) {

    // could do something like this to show that the blocking code isn't going to block
    //return Mono.fromCallable(() -> put(cellMono.block())).subscribeOn(Schedulers.parallel());
    // or we could have a blocking put call that did the backpressure calculation
    //return Mono.fromCallable(() -> putBlocking(cellMono.block())).subscribeOn(Schedulers.parallel());

    /*
    to backpressure here either chain to putAll() (reusing it's backpressure mech)
    or delay (in time) responding e.g. proportional to space remaining
     */
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
              .subscribe(createBackpressuringSubscriber()));
  }

  @Override
  public void putAll(final Flux<Cell> cellFlux) {
    cellFlux
        .map(cell->put(cell))
        .subscribeOn(Schedulers.parallel())
        .subscribe(createBackpressuringSubscriber());
  }

  @Override
  public Mono<Boolean> get(final Mono<Coordinate> coordinateMono) {
    return coordinateMono.map(
        coordinate -> JavaLang.toBooleanNotNull(cells.get(coordinateSystem.toOffset(coordinate))));
  }

  /**
   * This implementation of the queries flux is cold. Cells are delivered (from cells map) on-demand
   * (not as they arrive).
   *
   * The value of this approach is it lets us experiment with backpressure:
   * if cells arrive and we have outstanding queries for old generations we can't just complete
   * those old queries (fluxes) because they might still have cells to deliver.
   *
   * Contrast with {@link GameStateHotChanges#changes(Mono)} where we can complete the old queries
   * (fluxes) whenever we are sure we aren't going to see any new cells matching the query's
   * generation criterion.
   *
   * @param generationMono
   * @return
   *
   * @see {@link #distributeToQueriesHook(Cell)}
   */
  @Override
  public Flux<Cell> changes(final Mono<Integer> generationMono) {
    return generationMono
        .flatMapMany(generation ->
          highestCompleteGeneration()
              .flatMap(highestComplete -> highestComplete < generation ? Option.none() : Option.some(generation))
              .map(highestComplete ->
                  // a flux that can start producing items right now
                  createColdQueryFlux(generation))
              .getOrElse(
                  /*
                   A flux for which we have captured the sink. Some thread will produce to that sink
                   after {@code generation} is ready.
                   */
                  () -> Flux.create(sink -> getQueriesForGeneration(generation).add(sink)))); // flatMapMany
  }

  private Flux<Cell> createColdQueryFlux(final Integer generation) {
    return Flux.<Cell, Coordinate>generate(
        ()->coordinateSystem.createCoordinate(0,0,generation),
        (coordinate,sink)-> {
          if (coordinate.generation > generation) {
            sink.complete();
          }
          sink.next(Cell.create(coordinate, JavaLang.toBooleanNotNull(cells.get(coordinate))));
          return inc(coordinate);
        })
        .doOnComplete(() -> {
          /*
           TODO: if this is the last query for this generation and this is the lowest generation
           we can free up cells for this generation (and older).
           */

        });
  }

  private Coordinate inc(final Coordinate coordinate) {
    return coordinateSystem.createCoordinate(
        coordinateSystem.toOffset(coordinate) + 1);
  }

  private BaseSubscriber<Boolean> createBackpressuringSubscriber() {
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
   * N.B. that this method must not return a null reference lest it run afoul of Reactor
   * {@code map()} constraints.
   *
   * @param cell
   * @return
   */
  private Boolean put(final Cell cell) {
    return JavaLang.returning(
        JavaLang.toBooleanNotNull(cells.put(coordinateSystem.toOffset(cell.coordinate), cell.isAlive)),
        oldIsAlive -> {
          System.out.printf("put(%s)%n",cell);
          recordHighestOffsetHook(cell);
          distributeToQueriesHook(cell);
        });
  }

  private void recordHighestOffsetHook(final Cell cell) {
    highestOffsetCoordinate.updateAndGet(
        oldHighest -> Option.some(
          Coordinate.max(
              oldHighest.getOrElse(cell.coordinate),
              cell.coordinate)));
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
