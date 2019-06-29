package com.thoughtpropulsion.reactrode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;

class GameOfLifeFramingTest {

  private static int PRIMORDIAL_GENERATION = -1;
  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int COLUMNS = 8;
  private static final int ROWS = 4;
  private static final int GENERATIONS_CACHED = 3;

  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @BeforeEach
  void beforeEach() {
    gameOfLifeSystem = GameOfLifeSystem.create(COLUMNS, ROWS, GENERATIONS_CACHED,
        PRIMORDIAL_GENERATION);
    SystemTestSupport.initializePrimordialGeneration(gameOfLifeSystem);
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
    final Coordinates expected = cs.createCoordinate(skip);
    final int offset = expected.x + expected.y * cs.columns;

    final Flux<Cell> changes = gameOfLifeSystem.getGameState().changes(expected.generation).skip(offset).take(1);

    gameOfLifeSystem.startGame();

    StepVerifier.create(changes)
        .expectNextMatches(got -> got.coordinates.equals(expected))
        .as("matches " + expected)
        .expectComplete()
        .verify();
  }

}