package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;

class GameOfLifeEvolutionTest {

  private static final int PRIMORDIAL_GENERATION = -1;
  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @Test
  void blockPatternTest() {

    /*
      "block" is a 2x2 static form: it won't change generation-to-generation

      This pattern is non-square to uncover bugs where row/column sense is inconsistent.
     */

    final List<Boolean> pattern = SystemTestSupport.toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);

    final CoordinateSystem coordinateSystem = new CoordinateSystem(4, 5);

    gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            SystemTestSupport.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        SystemTestSupport.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem),
        Flux.from(gameOfLifeSystem.getAllGenerations()).take(coordinateSystem.size()));

    gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            SystemTestSupport.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        SystemTestSupport.cellsFromBits(pattern, PRIMORDIAL_GENERATION + 1, coordinateSystem),
        Flux.from(gameOfLifeSystem.getAllGenerations()).skip(coordinateSystem.size()).take(coordinateSystem.size()));
  }

  @Test
  void blinkerPatternTest() {

    /*
     "blinker" is a form that oscillates with period 2.
     */

    final List<Boolean> a = SystemTestSupport.toPattern(0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 1, 1, 1, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0);
    final List<Boolean> b = SystemTestSupport.toPattern(0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 0, 0);

    assertThat(b.size()).as("generation sizes are equal").isEqualTo(a.size());

    final CoordinateSystem coordinateSystem = new CoordinateSystem(5, 5);

    gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(SystemTestSupport.cellsFromBits(a, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        SystemTestSupport.cellsFromBits(b, PRIMORDIAL_GENERATION + 1, coordinateSystem),
        Flux.from(gameOfLifeSystem.getAllGenerations()).skip(coordinateSystem.size()).take(coordinateSystem.size())
    );

    gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            SystemTestSupport.cellsFromBits(b, PRIMORDIAL_GENERATION + 1, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        SystemTestSupport.cellsFromBits(a, PRIMORDIAL_GENERATION + 2, coordinateSystem),
        Flux.from(gameOfLifeSystem.getAllGenerations()).skip(coordinateSystem.size()).take(coordinateSystem.size())
    );
  }

  private void validatePattern(final Iterable<Cell> pattern, final Publisher<Cell> allHistory) {
    StepVerifier.create(allHistory)
        .expectNextSequence(pattern)
        .expectComplete()
        .verify();
  }

}