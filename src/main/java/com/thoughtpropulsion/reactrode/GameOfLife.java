package com.thoughtpropulsion.reactrode;

import reactor.core.publisher.Flux;

public class GameOfLife {

  public final int columns;
  public final int rows;
  private final GameState gameState;

  public GameOfLife(final int columns, final int rows,
                    final GameState gameState) {
    this.columns = columns; this.rows = rows; this.gameState = gameState;
  }

  public Cell createLiveCell(final int column, final int row, final int generation) {
    return createCell(column, row, generation, true);
  }

  public Coordinate createCoordinate(final int column, final int row, final int generation) {
    return Coordinate.create(column, row, generation, columns, rows);
  }

  Flux<Cell> getCells() {
    return Flux.range(0, Integer.MAX_VALUE) // generations
        .flatMap(generation -> Flux.range(0, rows)
            .flatMap(row -> Flux.range(0, columns)
                .map(column -> createCell(column, row, generation, isNextGenerationAlive(column,row,generation)))));
  }

  private Cell createCell(final int column, final int row, final int generation,
                          final boolean isAlive) {
    return Cell.create(
        createCoordinate(column, row, generation),
        isAlive);
  }

  private boolean isNextGenerationAlive(final int column, final int row, final int generation) {
    // TODO: compute liveness
    return true;
  }

}
