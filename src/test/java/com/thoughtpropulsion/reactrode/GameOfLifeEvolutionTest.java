package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

class GameOfLifeEvolutionTest {

  private static int PRIMORDIAL_GENERATION = -1;
  private static final int GENERATIONS_CACHED = 3;
  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @Test
  void blockPatternTest() throws InterruptedException {

    /*
      "block" is a 2x2 static form: it won't change generation-to-generation

      This pattern is non-square to uncover bugs where row/column sense is inconsistent.
     */

    gameOfLifeSystem = GameOfLifeSystem.create(4, 5, GENERATIONS_CACHED,
        PRIMORDIAL_GENERATION);

    final List<Boolean> pattern = toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);

    paintPattern(cellsFromBits(4, 5,
        pattern, PRIMORDIAL_GENERATION));

    final LongAdder validationOffset = new LongAdder();

    validatePattern(cellsFromBits(4, 5,
        pattern, PRIMORDIAL_GENERATION + 1), 0, validationOffset);

    driveSimulation(()->validationOffset.longValue() >= pattern.size());
  }

  @Test
  void blinkerPatternTest() throws InterruptedException {

    /*
     "blinker" is a form that oscillates with period 2.
     */

    gameOfLifeSystem = GameOfLifeSystem.create(5, 5, GENERATIONS_CACHED,
        PRIMORDIAL_GENERATION);

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

    paintPattern(cellsFromBits(5, 5, a, PRIMORDIAL_GENERATION));

    final int totalSize = a.size() + b.size();

    final LongAdder validationOffset1 = new LongAdder();

    validatePattern(cellsFromBits(5, 5,
        b, PRIMORDIAL_GENERATION + 1), 0, validationOffset1);

    final LongAdder validationOffset2 = new LongAdder();

    validatePattern(cellsFromBits(5, 5,
        a, PRIMORDIAL_GENERATION + 2), 1, validationOffset2);

    driveSimulation(() ->
        validationOffset1.longValue()+validationOffset2.longValue() >= totalSize);
  }

  private List<Boolean> toPattern(final int... bits) {
    return Arrays.stream(bits).boxed().map(b -> b == 1)
        .collect(Collectors.toList());
  }

  private List<Cell> cellsFromBits(final int columns, final int rows,
                                   final List<Boolean> bits, final int generation) {
    final List<Cell> cells = new ArrayList<>(columns * rows);
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        cells.add(Cell.create(
            gameOfLifeSystem.getCoordinateSystem().createCoordinate(x, y, generation),
            bits.get(y * columns + x)));
      }
    }
    return cells;
  }

  private void paintPattern(final Iterable<Cell> pattern) {
    Flux.fromIterable(pattern).delayUntil(cell -> gameOfLifeSystem.getGameState().put(cell));
    gameOfLifeSystem.getGameState().putAll(Flux.fromIterable(pattern));
  }

  private void validatePattern(final Collection<Cell> pattern, final int generation,
                               final LongAdder offset) {

    final Iterator<Cell> patternCells = pattern.iterator();

    gameOfLifeSystem.getGameState().changes(generation)
        .publishOn(Schedulers.parallel())   // this affects the thread used in subscribe()
        .subscribe(new BaseSubscriber<Cell>() {
          @Override
          protected void hookOnNext(final Cell cell) {
            assertThat(patternCells.hasNext()).as("verify that test pattern is not too short")
                .isTrue();
            final Cell expect = patternCells.next();
            assertThat(cell).as("game cell matches expected pattern").isEqualTo(expect);
            offset.increment();
          }

          @Override
          protected void hookOnComplete() {
            System.out.printf("Query complete for generation %d%n",generation);
            assertThat(offset.longValue()).as("game produced as many cells as pattern")
                .isEqualTo(pattern.size());
          }

          @Override
          protected void hookOnError(final Throwable throwable) {
            throw new TestAbortedException("game state changes flux produced error", throwable);
          }
        });
  }

  private void driveSimulation(final BooleanSupplier isComplete)
      throws InterruptedException {

    gameOfLifeSystem.startGame();

    while (! isComplete.getAsBoolean()) {
      Thread.sleep(10);
    }
  }

}