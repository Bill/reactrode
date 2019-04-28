package com.thoughtpropulsion.reactrode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicReference;

class GameOfLifeFramingTest {

  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int COLUMNS = 8;
  private static final int ROWS = 4;
  private static final int GENERATION_SIZE = COLUMNS * ROWS;

  private GameOfLife gameOfLife;

  @BeforeEach
  void beforeEach() {
    configureGame(COLUMNS, ROWS);
  }

  @Test
  void configureWithGameState() {
    assertThatCode(() -> configureGame(COLUMNS, ROWS)).doesNotThrowAnyException();
  }

  @Test
  void producesFirstCell() {
    testFraming(0,
        gameOfLife.createCoordinate(0, 0, 0));
  }

  @Test
  void incrementsRowAndColumn() {
    testFraming(GENERATION_SIZE-1,
        gameOfLife.createCoordinate(COLUMNS-1,ROWS-1,0));
  }

  @Test
  void producesSecondGeneration() {
    testFraming(GENERATION_SIZE,
        gameOfLife.createCoordinate(0,0,1));
  }

  private void testFraming(final int skip, final Coordinate expected) {
    final AtomicReference<Cell> took = new AtomicReference<>();
    gameOfLife.getCells().skip(skip).take(1).subscribe(cell -> took.set(cell));
    assertThat(took.get()).isNotNull();
    assertThat(took.get().coordinate).isEqualTo(expected);
  }

  private GameOfLife configureGame(final int columns, final int rows) {
    gameOfLife = new GameOfLife(columns, rows, new GameStateLocal(columns, rows));
    return gameOfLife;
  }
}