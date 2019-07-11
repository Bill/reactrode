package com.thoughtpropulsion.reactrode;

public class SystemTestSupport {
  static void initializePrimordialGeneration(final GameOfLifeSystem gameOfLifeSystem) {
    final CoordinateSystem coordinateSystem = gameOfLifeSystem.getCoordinateSystem();
    for (int i = -coordinateSystem.size(); i < 0; ++i) {
      gameOfLifeSystem
          .getGameState().put(Cell.create(coordinateSystem.createCoordinates(i),false));
    }
  }
}
