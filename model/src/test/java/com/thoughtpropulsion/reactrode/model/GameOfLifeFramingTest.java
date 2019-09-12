package com.thoughtpropulsion.reactrode.model;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

class GameOfLifeFramingTest {

  private static int PRIMORDIAL_GENERATION = 0;
  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int COLUMNS = 5;
  private static final int ROWS = 4;

  private GameOfLifeSystem gameOfLifeSystem;
  private CoordinateSystem coordinateSystem;

  @BeforeAll
  static void beforeAll() { ReactorDebugAgent.init();}

  @BeforeEach
  void beforeEach() {
    coordinateSystem = new CoordinateSystem(COLUMNS, ROWS);

    final List<Boolean> pattern = Patterns.toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);

    final Iterable<Cell>
        primordialGeneration =
        Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem);

    gameOfLifeSystem = GameOfLifeSystem.create(Flux.fromIterable(primordialGeneration),coordinateSystem);
  }

  @ParameterizedTest
  @ValueSource(ints = {0,1,2,3})
  void producesNthCell(final int i) {
    testFraming(i);
  }

  @Test
  void producesEndOfFirstGeneration() {
    testFraming(gameOfLifeSystem.getCoordinateSystem().size() - 1);
  }

  @Test
  void producesBeginningOfSecondGeneration() {
    testFraming(gameOfLifeSystem.getCoordinateSystem().size());
  }

  private void testFraming(final int skip) {
    final CoordinateSystem cs = gameOfLifeSystem.getCoordinateSystem();
    final Coordinates expected = cs.createCoordinates(skip);

    final Flux<Cell> changes =
        Flux.from(gameOfLifeSystem.getAllGenerations())
            .skip(skip)
            .take(1);

    StepVerifier.create(changes)
        .expectNextMatches(got -> got.coordinates.equals(expected))
        .as("matches " + expected)
        .expectComplete()
        .verify();
  }

}