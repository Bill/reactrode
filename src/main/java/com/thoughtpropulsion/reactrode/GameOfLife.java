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

  public Coordinate createCoordinate(final int x, final int y, final int generation) {
    return Coordinate.create(x, y, generation, columns, rows);
  }

  void startGame() {
    gameState.putAll(
        Flux.range(0, Integer.MAX_VALUE)
            .map(offset -> Coordinate.create(offset, columns, rows))
            /*
             TODO: if/when we parallelize game state generation, we might want to allow
             some interleaving here via flatMap()
             */
            .flatMapSequential(this::nextGenerationFor));
  }

  private Mono<Cell> nextGenerationFor(final Coordinate coordinate) {
    return isAlive(coordinate).map(isAlive -> Cell.create(
        coordinate,
        isAlive));
  }

  /**
   * Calculate liveness for a new cell (state).
   *
   * @param coordinate
   * @return true iff cell should be alive in {@param generation}
   * the {@Boolean} produced by this {@link Mono} will never be {@code null}
   */
   Mono<Boolean> isAlive(final Coordinate coordinate) {

    // TODO: take Mono<Coordinate>!

    final int x = coordinate.x;
    final int y = coordinate.y;
    final int generation = coordinate.generation;

    final int previousGen = generation - 1;

    return wasAliveCount(Coordinate.create(x - 1, y + 1, previousGen, columns, rows))
        .mergeWith(wasAliveCount(Coordinate.create(x, y + 1, previousGen, columns, rows)))
        .mergeWith(wasAliveCount(Coordinate.create(x + 1, y + 1, previousGen, columns, rows)))

        .mergeWith(wasAliveCount(Coordinate.create(x - 1, y, previousGen, columns, rows)))
        .mergeWith(wasAliveCount(Coordinate.create(x + 1, y, previousGen, columns, rows)))

        .mergeWith(wasAliveCount(Coordinate.create(x - 1, y - 1, previousGen, columns, rows)))
        .mergeWith(wasAliveCount(Coordinate.create(x, y - 1, previousGen, columns, rows)))
        .mergeWith(wasAliveCount(Coordinate.create(x + 1, y - 1, previousGen, columns, rows)))
        .reduce(0, (a, b) -> a + b)
        .zipWith(wasAlive(Coordinate.create(x, y, previousGen, columns, rows)))
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
   * @return 1 if cell was alive, otherwise return 0.
   * the {@Integer} produced by this {@link Mono} will never be {@code null}
   * @param coordinate
   */
   Mono<Integer> wasAliveCount(final Coordinate coordinate) {
    return wasAlive(coordinate).map(wasAlive -> wasAlive ? 1 : 0);
  }

  /**
   * Assess historical life status of a cell, from the GameState.
   *
   * @return the {@Boolean} produced by this {@link Mono} will never be {@code null}
   * @param coordinate
   */
  Mono<Boolean> wasAlive(final Coordinate coordinate) {
    return gameState.get(Mono.just(coordinate));
  }

}
