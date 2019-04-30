package com.thoughtpropulsion.reactrode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GameOfLife {

  public final int columns; // x
  public final int rows;    // y
  private final GameState gameState;

  public GameOfLife(final int columns, final int rows,
                    final GameState gameState) {
    this.columns = columns; this.rows = rows; this.gameState = gameState;
  }

  public Cell createLiveCell(final int x, final int y, final int generation) {
    return createCell(x, y, generation, true);
  }

  public Cell createCell(final int x, final int y, final int generation,
                         final boolean isAlive) {
    return Cell.create(
        createCoordinate(x, y, generation),
        isAlive);
  }

  public Coordinate createCoordinate(final int x, final int y, final int generation) {
    return Coordinate.create(x, y, generation, columns, rows);
  }

  /**
   * Produce cells starting at generation 0.
   * Caller places initial pattern in GameState in generation -1.
   * Caller has responsibility of adding the cells produced by this flux to the GameState,
   * or not.
   * @return the cold flux of cells ordered by generation, then by row, then by column
   */
  Flux<Cell> getCells() {
    return Flux.range(0, Integer.MAX_VALUE) // generations
        .flatMap(generation -> Flux.range(0, rows)
            .flatMap(y -> Flux.range(0, columns)
                .flatMap( x -> isAlive(x, y, generation)
                    .map(isAlive ->
                        JavaLang.returning(
                            createCell(x, y, generation, isAlive),
                            // store cell in game state as a side-effect
                            cell -> {
                              gameState.put(Mono.just(cell));
                            })))));
  }

  /**
   * Calculate liveness for a new cell (state).
   *
   * @param x
   * @param y
   * @param generation
   * @return true iff cell should be alive in {@param generation}
   * the {@Boolean} produced by this {@link Mono} will never be {@code null}
   */
  private Mono<Boolean> isAlive(final int x, final int y, final int generation) {
    final int previousGen = generation - 1;

    return wasAliveCount(x - 1, y + 1, previousGen)
        .concatWith(wasAliveCount(x, y + 1, previousGen))
        .concatWith(wasAliveCount(x + 1, y + 1, previousGen))

        .concatWith(wasAliveCount(x - 1, y, previousGen))
        .concatWith(wasAliveCount(x + 1, y, previousGen))

        .concatWith(wasAliveCount(x - 1, y - 1, previousGen))
        .concatWith(wasAliveCount(x, y - 1, previousGen))
        .concatWith(wasAliveCount(x + 1, y - 1, previousGen))
        .reduce(0, (a, b) -> a + b)
        .zipWith(wasAlive(x,y,previousGen))
        .map((t2) -> {
          final int liveNeighbors = t2.getT1();
          final boolean wasAlive = t2.getT2();

          if (wasAlive) {
            if (liveNeighbors < 2)
              return false; // underpopulation
            else if (liveNeighbors > 3)
              return false; // overpopulation
            else
              return true;  // survival
          } else {
            if (liveNeighbors == 3)
              return true;  // reproduction
            else
              return false; // status quo
          }
        });
  }

  /**
   * Convert boolean wasAlive() to integer liveness contribution.
   *
   * @param x
   * @param y
   * @param generation
   * @return 1 if cell was alive, otherwise return 0.
   * the {@Integer} produced by this {@link Mono} will never be {@code null}
   */
  private Mono<Integer> wasAliveCount(final int x, final int y, final int generation) {
    return wasAlive(x,y,generation).map(wasAlive -> wasAlive ? 1 : 0);
  }

  /**
   * Assess historical life status of a cell, from the GameState.
   *
   * @param x
   * @param y
   * @param generation
   * @return the {@Boolean} produced by this {@link Mono} will never be {@code null}
   */
  private Mono<Boolean> wasAlive(final int x, final int y, final int generation) {
    return gameState.get(Mono.just(Coordinate.create(x, y, generation, columns, rows)));
  }

}
