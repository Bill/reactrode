package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

class GameOfLifeEvolutionTest {

  private GameStateLocal gameState;
  private GameOfLife gameOfLife;
  private Scheduler parallelScheduler;

  @BeforeEach
  public void beforeEach() {
    parallelScheduler = Schedulers.newParallel("parallel-scheduler",4);
  }

  @AfterEach
  public void afterEach() {
    parallelScheduler.dispose();
  }

  @Test
  public void blockPatternTest() throws InterruptedException {

    /*
      "block" is a 2x2 static form: it won't change generation-to-generation

      This pattern is non-square to uncover bugs where row/column sense is inconsistent.
     */

    configureGame(4, 5);

    final List<Boolean> pattern = toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);

    paintPattern(cellsFromBits( 4, 5,
        pattern, -1));

    final AtomicInteger validationOffset = new AtomicInteger(0);

    final Disposable validation = validatePattern(cellsFromBits(4, 5,
        pattern, 0), 0, validationOffset);

    driveSimulation(validationOffset, pattern.size());

    validation.dispose();
  }

  @Test
  public void blinkerPatternTest() throws InterruptedException {

    /*
     "blinker" is a form that oscillates with period 2.
     */

    configureGame(5, 5);

    final List<Boolean> a = toPattern(0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 1, 1, 1, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0);
    final List<Boolean> b = toPattern(0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 0, 0);

    assertThat(b.size()).as("generation sizes are equal").isEqualTo(a.size());

    paintPattern(cellsFromBits( 5, 5, a, -1));

    final int totalSize = a.size() + b.size();

    final AtomicInteger validationOffset = new AtomicInteger(0);

    final Disposable validation = validatePattern(cellsFromBits(5, 5,
        b, 0), 0, validationOffset);

    final Disposable validation2 = validatePattern(cellsFromBits(5, 5,
        a, 1), 1, validationOffset);

    driveSimulation(validationOffset, totalSize);

    validation.dispose();
    validation2.dispose();
  }

  private List<Boolean> toPattern(final int ... bits) {
    return Arrays.stream(bits).boxed().map(b->Integer.compare(b,1)==0).collect(Collectors.toList());
  }

  private List<Cell> cellsFromBits(final int columns, final int rows,
                                   final List<Boolean> bits, final int generation) {
    final ArrayList<Cell> cells = new ArrayList<>(columns * rows);
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        cells.add(gameOfLife.createCell(x,y,generation,bits.get(y*columns + x)));
      }
    }
    return cells;
  }

  private void paintPattern(final List<Cell> pattern) {
    pattern.forEach(cell -> paintCell(cell));
  }

  private Disposable validatePattern(final List<Cell> pattern, final int generation,
                                        final AtomicInteger offset) {

    final Iterator<Cell> patternCells = pattern.iterator();

    return gameState.changes(Mono.just(generation))
        //.subscribeOn(parallelScheduler) // this does not affect the thread used in subscribe()
        .publishOn(parallelScheduler)   // this affects the thread used in subscribe()
        .subscribe(
            cell -> {
              assertThat(patternCells.hasNext()).as("verify that test pattern is not too short")
                  .isTrue();
              final Cell expect = patternCells.next();
              assertThat(cell).as("game cell maches expected pattern").isEqualTo(expect);
              offset.getAndIncrement();
              System.out.printf("✓ %s", Thread.currentThread().getName());
            }
            ,
            error -> {
              throw new TestAbortedException("game state changes flux produced error", error);
            },
            () -> assertThat(offset.get()).as("game produced as many cells as pattern")
                .isEqualTo(pattern.size()));
  }

  private void driveSimulation(final AtomicInteger offset, final int size)
      throws InterruptedException {
    gameOfLife.getCells().take(size).subscribe(cell -> {
      /*iterate for side effects*/
    });

    while (offset.get() < size)
      Thread.sleep(10);
  }

  private void paintCell(final Cell cell) {
    gameState.put(Mono.just(cell));
  }

  private GameOfLife configureGame(final int columns, final int rows) {
    gameState = new GameStateLocal(columns, rows);
    gameOfLife = new GameOfLife(columns, rows, gameState);
    return gameOfLife;
  }

}