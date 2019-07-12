package com.thoughtpropulsion.reactrode.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.thoughtpropulsion.reactrode.Cell;
import com.thoughtpropulsion.reactrode.CoordinateSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.UnicastProcessor;
import reactor.test.StepVerifier;

class GameOfLifeEvolutionTest {

  private static final int PRIMORDIAL_GENERATION = -1;
  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @Test
  void blockPatternTest() {

    /*
      "block" is a 2x2 static form: it won't change generation-to-generation

      This pattern is non-square to uncover bugs where row/column sense is inconsistent.
     */

    final List<Boolean> pattern = toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);

    final CoordinateSystem coordinateSystem = new CoordinateSystem(4, 5);

    gameOfLifeSystem = GameOfLifeSystem.createWithoutFeedback(
        Flux.fromIterable(cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem),
        Flux.from(gameOfLifeSystem.getNewLife()).take(coordinateSystem.size()));

    gameOfLifeSystem = GameOfLifeSystem.createWithoutFeedback(
        Flux.fromIterable(cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        cellsFromBits(pattern, PRIMORDIAL_GENERATION + 1, coordinateSystem),
        Flux.from(gameOfLifeSystem.getNewLife()).skip(coordinateSystem.size()).take(coordinateSystem.size()));
  }

  @Test
  void blinkerPatternTest() {

    /*
     "blinker" is a form that oscillates with period 2.
     */

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

    final CoordinateSystem coordinateSystem = new CoordinateSystem(5, 5);

    gameOfLifeSystem = GameOfLifeSystem.createWithoutFeedback(
        Flux.fromIterable(cellsFromBits(a, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        cellsFromBits(b, PRIMORDIAL_GENERATION + 1, coordinateSystem),
        Flux.from(gameOfLifeSystem.getNewLife()).skip(coordinateSystem.size()).take(coordinateSystem.size())
    );

    gameOfLifeSystem = GameOfLifeSystem.createWithoutFeedback(
        Flux.fromIterable(cellsFromBits(b, PRIMORDIAL_GENERATION + 1, coordinateSystem)),
        coordinateSystem);

    validatePattern(
        cellsFromBits(a, PRIMORDIAL_GENERATION + 2, coordinateSystem),
        Flux.from(gameOfLifeSystem.getNewLife()).skip(coordinateSystem.size()).take(coordinateSystem.size())
    );
  }

  private Iterable<Cell> cellsFromBits(final List<Boolean> bits, final int generation,
                                       final CoordinateSystem coordinateSystem) {
    final int columns = coordinateSystem.columns;
    final int rows = coordinateSystem.rows;

    final Collection<Cell> cells = new ArrayList<>(columns * rows);
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        cells.add(Cell.create(
            coordinateSystem.createCoordinates(x, y, generation),
            bits.get(y * columns + x)));
      }
    }
    return cells;
  }

  private List<Boolean> toPattern(final int... bits) {
    return Arrays.stream(bits).boxed().map(b -> b == 1)
        .collect(Collectors.toList());
  }

  private void validatePattern(final Iterable<Cell> pattern, final Publisher<Cell> allHistory) {
    StepVerifier.create(allHistory)
        .expectNextSequence(pattern)
        .expectComplete()
        .verify();
  }

}