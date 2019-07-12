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
     We're setting up a feedback loop:

    ---------------------------------------------------------------------------
   |                                                                           |
   | (3)                                                                       |
    -> (Game) -> (game.newLife) ->   (4)    (5)      (1)                       |
                                   allCells -> (emitterProcessor == newLife) ->
           primordialGeneration ->                                             |
                                                                           (2)  -> newLifeSubscriber
     */


    final EmitterProcessor<Cell> emitterProcessor = EmitterProcessor.create();

    newLife = emitterProcessor;

    /*
     (2) We gotta connect e.p. -> newLifeSubscriber before we set up the feedback loop lest
     that subscriber miss the whole show. If we subscriber later, that subscriber is starved.
     TODO: confirm w/ Project Reactor folks that that starvation really happens and isn't a bug
     */
    emitterProcessor.subscribe(newLifeSubscriber);

    // (3) emitter processor provides history input to the game: e.p. -> game
    final GameOfLife gameOfLife = new GameOfLife(this.coordinateSystem, emitterProcessor);

    /*
     (4) EmitterProcessor can Publish to many Subscribers, but it cannot Subscribe
     to many Publishers. Since it can subscribe to only one, we must combine
     the primordial generation and the new life (produced by the game) into a single
     flux and have the EmitterProcessor subscribe to that.
     */
    final Flux<Cell> allCells = Flux.concat(
        primordialGeneration,
        gameOfLife.getNewLife());


    // (5)
    allCells.subscribe(emitterProcessor);
  }

  private GameOfLifeSystem(
      final Publisher<Cell> primordialGeneration,
      final CoordinateSystem coordinateSystem) {

    this.coordinateSystem = coordinateSystem;

    /*
     We're setting up a simple (not all that useful) linear flow for testing

     primordialGeneration -> (Game) -> (game.newLife == newLife)
     */

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