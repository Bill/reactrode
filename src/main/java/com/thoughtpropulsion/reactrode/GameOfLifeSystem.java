package com.thoughtpropulsion.reactrode;

/**
 * Handle assembly of the system, with dependency injection, and provide access to
 * system components.
 */
public class GameOfLifeSystem {
  private final CoordinateSystem coordinateSystem;
  private final GameOfLife gameOfLife;
  private final GameState gameState;

  public CoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }
  public GameState getGameState() {
    return gameState;
  }
  public GameOfLife getGameOfLife() {
    return gameOfLife;
  }

  private GameOfLifeSystem(final CoordinateSystem coordinateSystem,
                           final GameState gameState, final GameOfLife gameOfLife) {
    this.coordinateSystem = coordinateSystem;
    this.gameState = gameState;
    this.gameOfLife = gameOfLife;
  }

  public static GameOfLifeSystem create(
      final int columns,
      final int rows,
      final int generationsCached,
      final int primordialGeneration) {
    final CoordinateSystem coordinateSystem = new CoordinateSystem(columns, rows);
    final GameState gameState =
        new GameStateWithBackpressure(generationsCached, coordinateSystem, primordialGeneration);
    return new GameOfLifeSystem(coordinateSystem, gameState, new GameOfLife(coordinateSystem,
        gameState));
  }

  void startGame() {
    // TODO: change this to subscribe here-- don't rely on putAll() to do it
    // because that will eventually be in another VM!
    gameState.putAll(gameOfLife.createCellsFlux());
  }

  /*
   This shows how we could provide cells to the game state in parallel
   */
  private void startGame2() {
    // TODO: change this to subscribe here-- don't rely on putAll() to do it
    // because that will eventually be in another VM!
    gameOfLife.createCellsFlux()
        .log()
        .flatMap(cell -> gameState.put(cell), 5)
        .subscribe();
  }


}