package com.thoughtpropulsion.reactrode.model;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class GameOfLifePrimordialGenerationCorruptionTest {

  @Test
  void incompletePrimordialGeneration() {
    final CoordinateSystem coordinateSystem = new CoordinateSystem(2, 1);

    // There are two cells per generation: but we've provided only one
    final Flux<Cell> primordialGeneration = Flux.just(
        Cell.createAlive(
            coordinateSystem.createCoordinates(0, 0, 0), true));

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create( primordialGeneration, coordinateSystem);

    StepVerifier.create(gameOfLifeSystem.getAllGenerations())
        .expectNextCount(1)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

    @Test
  void multiGenerationPrimordialGeneration() {

    final CoordinateSystem coordinateSystem = new CoordinateSystem(2, 1);

    // There are two cells per generation: but we've given these two cells different generation #s
    final Flux<Cell> primordialGeneration = Flux.just(
        Cell.createAlive(
            coordinateSystem.createCoordinates(0, 0, 0), true),
        Cell.createAlive(
            coordinateSystem.createCoordinates(0, 0, 1), true));

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create( primordialGeneration, coordinateSystem);

    StepVerifier.create(gameOfLifeSystem.getAllGenerations())
        .expectNextCount(2)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

}
