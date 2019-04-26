package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class GameOfLifeEvolutionTest {

  private GameStateLocal gameState;
  private GameOfLife gameOfLife;

  @Test
  public void blockPatternTest() {
    configureGame(4, 4);
    final int[][] block = {{1,1},{2,1},{2,1},{1,2}};
    paintPattern(block);

  }

  private void paintPattern(final int[][] pattern) {
    for(int[] coord:pattern) {
      paintCell(coord[0], coord[1]);
    }
  }

  private void paintCell(final int x, final int y) {
    gameState.put(Mono.just(gameOfLife.createLiveCell(x, y, -1)));
  }

  private GameOfLife configureGame(final int columns, final int rows) {
    gameState = new GameStateLocal();
    gameOfLife = new GameOfLife(columns, rows, gameState);
    return gameOfLife;
  }

}