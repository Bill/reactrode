package com.thoughtpropulsion.reactrode;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

/**
 * Game state providing a cold flux of changes. As new cells arrive, old generations
 * are released. But a generation will not be released until all its subscribers have
 * seen completion.
 */
public class GameStateWithBackpressure implements GameState {
  /*
   Usage pattern will be:

   (step (1) in any order)

     1. (observer(s)) will subscribe for changes for generation(s)
        [-1, -1 + GENERATIONS_CACHED] via changes(int generation)

     1. (experimenter) will fill PRIMORDIAL_GENERATION (-1) via putAll(flux1)

   2. (experimenter) will run the simulation (GameOfLifeSystem.startGame()) which causes (game) to
      putAll(flux2). that flux produces cells starting with generation 0

   3. (game) needs to get() from generation N to generate (put()) generation N+1 so cache
      has to hold at least _two_ generations

   4. to allow (observer(s)) to be slower than (game) we the cellsCache needn't hold more than
      2 generations--but we do have to allow more subscription (EmitterProcessors) to hang around.
      each of those caches a generation's worth of cells


  thinking of subscription (in putAll(Flux)) independent of cells cache:

  Subscription (to game flux) and distribution (to observers):

   GameState initially subscribes to cellsFlux with a request for N (= coordinateSystem.size())

   after those have all arrived (mechanism? [1] via Flux.window(int size)), feed any
   subscriptions (sinks) for that generation (synchronously? [2] via EmitterProcessor), then
   close that generation (set youngestGenerationBuffered, don't allow any new subscriptions
   to that gen) and request another N

  Cells Cache GC:

   when cell arrives, cache it

   when closing a generation (when a putAll(Flux) completes a generation G
   (mechanism? [1] via Flux.window(int size)), remove cells from generations < (G - 1)
   from cache e.g. if G is 0 remove generations -2 and earlier

   or

   when adding a cell -- remove the cell at that same position two (more? [3] no--exactly two)
   generations earlier


   Questions:

   [1] could count cells as they arrive; better: Flux.window(int size) will turn my Flux<Cell>
       into a Flux<Flux<Cell>> where each (inner) flux has N cells

   [2] could block the thread servicing putAll() to iteratively send cells to subscribers via
       FluxSink.next(T t) but that wouldn't respect backpressure (from the subscriber(s));
       could make cells for generation G "available" to subscribers (and disallow new subscribers)
       but how? Those subscribers need to be subscribed to _processors_. could use an Emitter
       per generation (multiple subscribers per Emitter) since replay won't be needed in this case,
       i.e. these fluxes will be hot. EmitterProcessor.create(int bufferSize) with bufferSize
       set to coordinateSystem.size(). Eliminating/reducing that buffering could be a refinement.

   [3] why would we want to keep older generations? they can't be of any use since, in general
       generation N must be complete before generation N+1 can be started. Then again we could
       break the board into rectangles and treat each one independently. But the rule still holds for
       each rectangle: generation N must be complete before generation N+1 can start.

   */

  private final CoordinateSystem coordinateSystem;

  /*
   These are the cold fluxes returned by changes(int generation)
   */
  private final RingBufferSequential<ReplayProcessor<Cell>> changes;

  private final NavigableMap<Integer, Boolean> cellsCache = new ConcurrentSkipListMap<>();

  public GameStateWithBackpressure(final int generationsCached,
                                   final CoordinateSystem coordinateSystem,
                                   final int primordialGeneration) {

    if (coordinateSystem.columns < 1)
      throw new IllegalArgumentException(
          "columns must be greater than zero but got " + coordinateSystem.columns);

    if (coordinateSystem.rows < 1)
      throw new IllegalArgumentException(
          "rows must be greater than zero but got " + coordinateSystem.rows);

    if (generationsCached < 2)
      /*
       The game needs at least two generations cached because it reads generation N to create
       generation N+1
       */
      throw new IllegalArgumentException(
          "generationsCached must be greater than one but got " + generationsCached);

    this.coordinateSystem = coordinateSystem;

    changes = new RingBufferSequential<>(generationsCached, primordialGeneration);

    for (int i = 0; i < generationsCached; ++i) {
      changes.offer(createReplayProcessor());
    }
  }

  @Override
  public Mono<Cell> put(final Mono<Cell> cellMono) {
    return cellMono.flatMap(this::put);
  }

  /**
   * Put a cell into the game state. Returns a mono with acknowledgment status: the cell
   * is on success, otherwise error. This method supports adding cells in any order,
   * so long as the cells are in range of the buffer.
   *
   * When the last cell for the youngest frame arrives, it triggers a window shift,
   * i.e. destruction of the oldest frame, and creation of a new youngest frame.
   * That is, unless subscriptions are outstanding to the oldest frame. In that case
   * the oldest frame is kept alive until its subscribers are all complete.
   * After they complete, the window will shift and processing will continue.
   *
   * @param cell
   * @return
   */
  @Override
  public Mono<Cell> put(final Cell cell) {
    /*
     If the cell generation is in range (of the changes ring buffer) then:
     1. add the cell to the cellsCache
     2. notify the replay processor
     otherwise (cell generation is not in range) so return an error mono
     */
    final int generation = cell.coordinates.generation;
    return changes.peek(generation)
        .peek(replayProcessor -> {
          cellsCache.put(coordinateSystem.toOffset(cell.coordinates), cell.isAlive);
          replayProcessor.onNext(cell);
          if (isGenerationComplete(generation, cell)) {
            replayProcessor.onComplete();
          }
        })
        .map(_ignoredReplayProcessor -> Mono.just(cell))
        .getOrElse(() -> Mono.error(new IllegalArgumentException(
            String.format("can't put() cell (%s) outside buffer range: (%s)",
                cell, changes))));
  }

  @Override
  public void putAll(final Flux<Cell> cellFlux) {
    cellFlux
        .flatMap(this::put)
        .subscribeOn(Schedulers.parallel())
        .subscribe(createSubscriber());
  }

  @Override
  public Mono<Boolean> get(final Coordinates key) {
    return Mono.justOrEmpty(
        cellsCache.get(coordinateSystem.toOffset(key)));
  }

  @Override
  public Flux<Cell> changes(final int generation) {
    return changes.peek(generation).flatMap(Option::<Flux<Cell>>of)
        .getOrElse(Flux.error(new SubscriptionException(
            String.format("Can't subscribe to generation %d since it's outside buffer range: %s)",
                generation, changes))));
  }

  @Override
  public Flux<Flux<Cell>> generations(final int startingGeneration) {
    return Flux.generate(()->startingGeneration, (generation,sink) -> {
      final Option<ReplayProcessor<Cell>> processors = changes.peek(generation);

      return generation + 1;
    });
  }

  private boolean isGenerationComplete(final int _generation, final Cell cell) {
    final Coordinates c = cell.coordinates;

    /*
     This onComplete() determination depends on the supplier providing cells in order
     TODO: to allow more parallelism, keep the count of (distinct) cells added per generation
           and use that to determine completion status
     */
    return c.x == coordinateSystem.columns - 1 && c.y == coordinateSystem.rows - 1;
  }

  private ReplayProcessor<Cell> createReplayProcessor() {
    return ReplayProcessor.create(coordinateSystem.size());
  }

  private BaseSubscriber<Cell> createSubscriber() {
    return new FrameAtATimeSubscriber();
  }

  private class FrameAtATimeSubscriber extends BaseSubscriber<Cell> {

    private final LongAdder upstreamDemand;

    FrameAtATimeSubscriber() {
      this.upstreamDemand = new LongAdder();
    }

    @Override
    protected void hookOnSubscribe(final Subscription subscription) {
      requestOneFrame();
    }

    @Override
    protected void hookOnNext(final Cell cell) {
      System.out.println("hookOnNext() got cell " + cell);
      doRequest(cell);
    }

    private void doRequest(final Cell cell) {

      upstreamDemand.decrement();
      System.out.println("doRequest() decremented upstreamDemand now: " + upstreamDemand);

      if (upstreamDemand.longValue() == 0) {

        // We've received a complete frame and need to request the next one...

        final int nextGeneration = cell.coordinates.generation + 1;

        final Option<ReplayProcessor<Cell>> processorsForGeneration =
            changes.peek(nextGeneration);

        System.out.println("doRequest() upstreamDemand is 0, processor for generation " +
            nextGeneration + " is " + processorsForGeneration);

        processorsForGeneration.onEmpty(() -> {
          System.out.println("doRequest() NEED ADVANCE");
          /*
           There is no room in our ring buffer so we must advance. Look at the oldest
           ReplayProcessor. If it has no subscribers then we can free up its location
           in the ring buffer and offer a new ReplayProcessor.
           */
          final Tuple2<Integer, Option<ReplayProcessor<Cell>>> t2 = changes.peek();
          final Integer oldestGeneration = t2._1;
          final Option<ReplayProcessor<Cell>> oldestProcessors = t2._2;

          assert isGenerationComplete(oldestGeneration,cell);

          oldestProcessors
              .peek(oldestProcessor -> {
                if (oldestProcessor.hasDownstreams()) {
                  System.out.println("doRequest(): waiting for subscribers to finish");
                  /*
                   Since there are still subscribers to this replayProcessor we can't free it up
                   yet. Start a background process to try later. Recurse!
                   TODO: this retry should be replaced with an event-driven hook. How can we hook
                         into the subscribers on the replay processors to free it up when the last
                         subscriber's flux finishes?
                   */
                  Mono.delay(Duration.ofSeconds(1)).doOnNext(_elapsed -> doRequest(cell));
                } else {
                  advanceWindow(oldestGeneration);
                }
              });
        });

        requestOneFrame();

        System.out.println(
            String.format("doRequest(): requested a frame, upstreamDemand now: %s, cells cached: %d",
                upstreamDemand, cellsCache.size()));
      }
    }

    private void advanceWindow(final int oldestGeneration) {
      System.out.println("advanceWindow() oldestGeneration is " + oldestGeneration);

      final Tuple2<Integer, Option<ReplayProcessor<Cell>>>
          t2 =
          changes.poll();// dispose of oldest processor
      final Integer oldestGeneration2 = t2._1;
      final Option<ReplayProcessor<Cell>> oldestReplayProcessors = t2._2;
      assert oldestGeneration2 == oldestGeneration;
      System.out.println("advanceWindow() disposed of generation " + oldestGeneration);

      final SortedMap<Integer, Boolean> garbage =
          cellsCache.headMap(
              coordinateSystem.toOffset(
                  coordinateSystem.columns - 1,
                  coordinateSystem.rows - 1,
                  oldestGeneration),
              true);

      System.out.println("advanceWindow() purging " + garbage.size() + " cells from buffer");

      garbage.clear(); // dispose of cell cache entries

      changes.offer(createReplayProcessor());

      System.out.println("advanceWindow() ring buffer is now " + changes);
    }

    private void requestOneFrame() {
      assert upstreamDemand.longValue() == 0;
      upstreamDemand.add(coordinateSystem.size());
      request(upstreamDemand.intValue());
    }

  }

}
