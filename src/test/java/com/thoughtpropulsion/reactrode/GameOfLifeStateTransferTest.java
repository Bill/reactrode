package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

class GameOfLifeStateTransferTest {
  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int COLUMNS = 8;
  private static final int ROWS = 4;

  private GameOfLife gameOfLife;
  private GameState gameState;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @BeforeEach
  void beforeEach() {
    configureGame(COLUMNS, ROWS);
  }

  @Test
  void configureWithGameState() {
    assertThatCode(() -> configureGame(COLUMNS, ROWS)).doesNotThrowAnyException();
  }

  @Test
  void accessSingleCell() {
    assertThat(
        gameOfLife.wasAlive(gameOfLife.createCoordinate(0,0,0)).block(Duration.ofMillis(10)))
        .isFalse();
  }

  @Test
  void singleCellEvolution() {
    assertThat(gameOfLife.isAlive(gameOfLife.createCoordinate(0,0,0)).block(Duration.ofMillis(10))).isFalse();
  }

  private GameOfLife configureGame(final int columns, final int rows) {
    gameState = new GameStateColdChanges(columns, rows);
    gameOfLife = new GameOfLife(columns, rows, gameState);
    return gameOfLife;
  }

}