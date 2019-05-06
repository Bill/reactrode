package com.thoughtpropulsion.reactrode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

class BackpressureAndGCTest {

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
  void foo() {}

  private GameOfLife configureGame(final int columns, final int rows) {
    gameState = new GameStateColdChanges(columns, rows);
    gameOfLife = new GameOfLife(columns, rows, gameState);
    return gameOfLife;
  }

}