package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;

class GameOfLifeEvolutionTest {

  private static final int PRIMORDIAL_GENERATION = -1;
  private static final int GENERATIONS_CACHED = 3;
  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @Test
  void blockPatternTest() {

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

    validatePattern(cellsFromBits(4, 5,
        pattern, PRIMORDIAL_GENERATION + 1), 0);
  }

  @Test
  void blinkerPatternTest() {

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

    validatePattern(cellsFromBits(5, 5,
        b, PRIMORDIAL_GENERATION + 1), 0);

    validatePattern(cellsFromBits(5, 5,
        a, PRIMORDIAL_GENERATION + 2), 1);

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

  private void validatePattern(final Iterable<Cell> pattern, final int generationNumber) {

    final Flux<Cell> generation = gameOfLifeSystem.getGameState().changes(generationNumber);

    gameOfLifeSystem.startGame();

    StepVerifier.create(generation)
        .expectNextSequence(pattern)
        .expectComplete()
        .verify();
  }

}