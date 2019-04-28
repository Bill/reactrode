package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class GameOfLifeEvolutionTest {

  private GameStateLocal gameState;
  private GameOfLife gameOfLife;

  @Test
  public void blockPatternTest() {

    /*
      "block" is a static pattern: it won't change generation-to-generation
      Make the pattern non-rectangular to uncover bugs where row/column sense is inconsistent.
     */
    configureGame(4, 5);
    final List<Boolean> pattern = toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);
    paintPattern(cellsFromBits( 4, 5,
        pattern, -1));
    validatePattern(cellsFromBits( 4, 5,
        pattern, 0));
  }

  @Test
  public void blinkerPatternTest() {
    /*
     "blinker" oscillates with period 2.
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
    paintPattern(cellsFromBits( 5, 5,
        a, -1));
    validatePattern(cellsFromBits( 5, 5,
        b, 0));
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

  private void validatePattern(final List<Cell> pattern) {

    /*
     TODO: feels like I have no great choices here for comparing "sequences". Candidates:
     Java 8 Stream: nothing out of the box
     Flux: I don't want to have to create a Flux for my pattern (List)
     Iterator: just yuck. Again, no good out of the box solution (see below)
     */

    final Iterator<Cell> cells = gameOfLife.getCells().toStream().limit(pattern.size()).iterator();
    final Iterator<Cell> patternCells = pattern.iterator();

    while(cells.hasNext() && patternCells.hasNext()) {
      final Cell cell = cells.next();
      final Cell expect = patternCells.next();
      assertThat(cell).as("game cell maches expected pattern").isEqualTo(expect);
    }
    assertThat(cells.hasNext()).as("game produced as many cells as pattern").isEqualTo(patternCells.hasNext());
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