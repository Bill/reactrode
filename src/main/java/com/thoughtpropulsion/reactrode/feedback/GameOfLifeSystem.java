package com.thoughtpropulsion.reactrode.feedback;

import com.thoughtpropulsion.reactrode.Cell;
import com.thoughtpropulsion.reactrode.CoordinateSystem;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

public class GameOfLifeSystem {
  private final CoordinateSystem coordinateSystem;
  private final Publisher<Cell> newLife;
  
  public CoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }

  public Publisher<Cell> getNewLife() { return newLife;}

  /**
   * Set up a realistic game (with feedback).
   *
   * @param primordialGeneration
   * @param coordinateSystem
   * @param newLifeSubscriber is the downstream subscriber that wants to receive new life (cells)
   */
  private GameOfLifeSystem(
      final Publisher<Cell> primordialGeneration,
      final CoordinateSystem coordinateSystem,
      final Subscriber<Cell> newLifeSubscriber) {

    this.coordinateSystem = coordinateSystem;

    /*
     If we feedback (our output to our input) through an EmitterProcessor,
     and an EmitterProcessor throttles on its slowest consumer then it seems
     we might stall unless we build in a generation-size buffer (demand)
     hence the buffer size in the constructor here.
     */
    final EmitterProcessor<Cell> emitterProcessor =
        EmitterProcessor.create(/*coordinateSystem.size()*/);

    newLife = emitterProcessor;

    /*
     We're setting up a feedback loop:

                           -----------------------------------------------------------------------
                          |                                                                       |
                           ->        (3)       (4)                          (5)        (1)        |
                              combinedGameInput -> (Game) -> (game.newLife) -> emitterProcessor ->
      primordialGeneration ->                                                                     |
                                                                                              (2)  -> newLifeSubscriber

     */


    /*
     We gotta connect e.p. -> newLifeSubscriber before we set up the feedback loop lest
     that subscriber miss the whole show. If we subscriber later, that subscriber is starved.
     TODO: confirm w/ Project Reactor folks that that starvation really happens and isn't a bug
     */
    emitterProcessor.subscribe(newLifeSubscriber);

    /*
     EmitterProcessor can Publish to many Subscribers, but it cannot Subscribe
     to many Publishers. Since it can subscribe to only one, we must combine
     the primordial generation and the new life (produced by the game) into a single
     flux and have the EmitterProcessor subscribe to that.
     */
    final Flux<Cell> combinedGameInput = Flux.concat(
        primordialGeneration,
        emitterProcessor);

    // emitter processor provides history input to the game: e.p. -> game
    final GameOfLife gameOfLife = new GameOfLife(this.coordinateSystem, combinedGameInput);

    gameOfLife.getNewLife().subscribe(emitterProcessor);
  }

  private GameOfLifeSystem(
      final Publisher<Cell> primordialGeneration,
      final CoordinateSystem coordinateSystem) {

    this.coordinateSystem = coordinateSystem;

    final GameOfLife gameOfLife;

    gameOfLife = new GameOfLife(this.coordinateSystem, primordialGeneration);

    newLife = gameOfLife.getNewLife();

 }

  public static GameOfLifeSystem createWithFeedback(
      final Publisher<Cell> primordialGeneration,
      final CoordinateSystem coordinateSystem,
      final Subscriber<Cell> newLifeSubscriber) {
    return new GameOfLifeSystem(primordialGeneration, coordinateSystem, newLifeSubscriber);
  }

  public static GameOfLifeSystem createWithoutFeedback(
      final Publisher<Cell> primordialGeneration,
      final CoordinateSystem coordinateSystem) {
    return new GameOfLifeSystem(primordialGeneration, coordinateSystem);
  }
}