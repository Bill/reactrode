package com.thoughtpropulsion.reactrode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

class GameOfLifeFramingTest {

  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int COLUMNS = 8;
  private static final int ROWS = 4;
  private static final int GENERATION_SIZE = COLUMNS * ROWS;

  private GameOfLife gameOfLife;
  private GameState gameState;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @BeforeEach
  void beforeEach() {
    configureGame(COLUMNS, ROWS);
  }

  @Test
  void producesFirstCell() {
    testFraming(0,
        gameOfLife.createCoordinate(0, 0, 0));
  }

  @ParameterizedTest
  @ValueSource(ints = {30})
  void producesNthCell(final int i) {
    final Coordinate coordinate = Coordinate.create(i, COLUMNS, ROWS);
    testFraming(i,
        gameOfLife.createCoordinate(coordinate.x, coordinate.y, coordinate.generation));
  }

  @Test
  void incrementsRowAndColumn() {
    testFraming(GENERATION_SIZE-1,
        gameOfLife.createCoordinate(COLUMNS-1,ROWS-1,0));
  }

  @Test
  void producesSecondGeneration() {
    testFraming(0,
        gameOfLife.createCoordinate(0,0,1));
  }

  private void testFraming(final int skip, final Coordinate expected) {
    // FIXME: this fails a lot because there is a race between the changes flux and the flux feeding the game state
    final AtomicReference<Cell> took = new AtomicReference<>();
    gameState.changes(Mono.just(expected.generation)).skip(skip).take(1).doOnNext(took::set).publishOn(
        Schedulers.parallel()).subscribe();
    gameOfLife.startGame();
    while (null == took.get()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
      }
    }
    assertThat(took.get()).isNotNull();
    assertThat(took.get().coordinate).isEqualTo(expected);
  }

  private GameOfLife configureGame(final int columns, final int rows) {
    gameState = new GameStateHotChanges(columns, rows);
    gameOfLife = new GameOfLife(columns, rows, gameState);
    return gameOfLife;
  }
}