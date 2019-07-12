package com.thoughtpropulsion.reactrode.feedback;

import com.thoughtpropulsion.reactrode.Cell;
import com.thoughtpropulsion.reactrode.CoordinateSystem;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

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
   */
  private GameOfLifeSystem(
      final Publisher<Cell> primordialGeneration,
      final CoordinateSystem coordinateSystem,
      final Subscriber<Cell> newLifeSubscriber) {

    this.coordinateSystem = coordinateSystem;

    /*
     We're setting up a feedback loop:

                                               (1)            <-- (5) <--
  --------------------------------------emitterProcessor----------------------
 |                                                                             |
 |     (2)                                                                     |
  -> (Game) -> (game.newLife) ->   (3)             (4)                         |
                                 allCells -> (connectableFlux(2) == newLife) ->
         primordialGeneration ->                                               |
                                                                           (6)  -> newLifeSubscriber

     */


    // (1) we need a processor in the chain to break the loop (try without it if you don't believe)
    final EmitterProcessor<Cell> emitterProcessor = EmitterProcessor.create();

    // (2) emitter processor provides history input to the game
    final GameOfLife gameOfLife = new GameOfLife(this.coordinateSystem, emitterProcessor);

    /*
     (3) EmitterProcessor can Publish to many Subscribers, but it cannot Subscribe
     to many Publishers. Since it can subscribe to only one, we must combine
     the primordial generation and the new life (produced by the game) into a single
     flux and have the EmitterProcessor subscribe to that.
     */
    final Flux<Cell> allCells = Flux.concat(
        primordialGeneration,
        gameOfLife.getNewLife());

    // (4) callers can connect to this flux
    newLife = Flux.from(allCells).publish().refCount(2);

    // (5)
    newLife.subscribe(emitterProcessor);
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