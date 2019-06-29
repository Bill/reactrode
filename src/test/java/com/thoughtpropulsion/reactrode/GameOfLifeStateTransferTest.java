package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

class GameOfLifeStateTransferTest {
  private static int PRIMORDIAL_GENERATION = -1;
  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int COLUMNS = 8;
  private static final int ROWS = 4;
  private static final int GENERATIONS_CACHED = 2;

  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @BeforeEach
  void beforeEach() {
    gameOfLifeSystem = GameOfLifeSystem.create(COLUMNS, ROWS, GENERATIONS_CACHED,
        PRIMORDIAL_GENERATION);
    SystemTestSupport.initializePrimordialGeneration(gameOfLifeSystem);
  }

  @Test
  void accessSingleCell() {
    assertThat(
        gameOfLifeSystem.getGameOfLife().wasAlive(gameOfLifeSystem.getCoordinateSystem().createCoordinate(-1)).block(Duration.ofMillis(10)))
        .isFalse();
  }

  @Test
  void singleCellEvolution() {
    assertThat(gameOfLifeSystem.getGameOfLife().isAlive(gameOfLifeSystem.getCoordinateSystem().createCoordinate(0)).block(Duration.ofMillis(10))).isFalse();
  }

}