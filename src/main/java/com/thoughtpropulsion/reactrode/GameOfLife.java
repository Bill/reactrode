package com.thoughtpropulsion.reactrode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Generate states for Conway's Game of Life.
 */
public class GameOfLife {

  private final CoordinateSystem coordinateSystem;
  private final GameState gameState;

  public GameOfLife(final CoordinateSystem coordinateSystem, final GameState gameState) {
    this.coordinateSystem = coordinateSystem;
    this.gameState = gameState;
  }

  public Flux<Cell> createCellsFlux() {
    return Flux.range(0, Integer.MAX_VALUE)
        .map(offset -> coordinateSystem.createCoordinate(offset))
        /*
         TODO: if/when we parallelize game state generation, we might want to allow
         some interleaving here via flatMap()
         */
        .flatMapSequential(this::nextGenerationFor);
  }

  private Mono<Cell> nextGenerationFor(final Coordinates coordinates) {
    return isAlive(coordinates).map(isAlive -> Cell.create(coordinates, isAlive));
  }

  /**
   * Calculate liveness for a new cell (state).
   *
   * @param coordinates is the cell's coordinates
   * @return true iff cell should be alive in {@param generation}
   * the {@Boolean} produced by this {@link Mono} will never be {@code null}
   */
  Mono<Boolean> isAlive(final Coordinates coordinates) {

    // TODO: take Mono<Coordinates>?

    final int x = coordinates.x;
    final int y = coordinates.y;
    final int generation = coordinates.generation;

    final int previousGen = generation - 1;

    return wasAliveCount(coordinateSystem.createCoordinate(x - 1, y + 1, previousGen))
        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x, y + 1, previousGen)))
        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x + 1, y + 1, previousGen)))

        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x - 1, y, previousGen)))
        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x + 1, y, previousGen)))

        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x - 1, y - 1, previousGen)))
        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x, y - 1, previousGen)))
        .mergeWith(wasAliveCount(coordinateSystem.createCoordinate(x + 1, y - 1, previousGen)))

        .reduce(0, (a, b) -> a + b)

        .zipWith(wasAlive(coordinateSystem.createCoordinate(x, y, previousGen)))

        .map((t2) -> {
          final int liveNeighbors = t2.getT1();
          final boolean wasAlive = t2.getT2();

          if (wasAlive) {
            if (liveNeighbors < 2) {
              return false; // underpopulation
            } else if (liveNeighbors > 3) {
              return false; // overpopulation
            } else {
              return true;  // survival
            }
          } else {
            if (liveNeighbors == 3) {
              return true;  // reproduction
            } else {
              return false; // status quo
            }
          }
        });
  }

  /**
   * Convert boolean wasAlive() to integer liveness contribution.
   *
   * @return 1 if cell was alive, otherwise return 0.
   * the {@Integer} produced by this {@link Mono} will never be {@code null}
   * @param coordinates
   */
   private Mono<Integer> wasAliveCount(final Coordinates coordinates) {
    return wasAlive(coordinates).map(wasAlive -> wasAlive ? 1 : 0);
  }

  /**
   * Assess historical life status of a cell, from the GameState.
   *
   * @return the {@Boolean} produced by this {@link Mono} will never be {@code null}
   * @param coordinates
   */
  Mono<Boolean> wasAlive(final Coordinates coordinates) {
    return gameState.get(coordinates);
  }

}
