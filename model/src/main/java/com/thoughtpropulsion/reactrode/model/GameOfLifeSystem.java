package com.thoughtpropulsion.reactrode.model;

import org.reactivestreams.Publisher;

public class GameOfLifeSystem {
  private final CoordinateSystem coordinateSystem;
  private final Publisher<Cell> allGenerations;

  public CoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }

  public Publisher<Cell> getAllGenerations() { return allGenerations;}

  private GameOfLifeSystem(
      final Publisher<Cell> primordialGenerationPublisher,
      final CoordinateSystem coordinateSystem) {

    this.coordinateSystem = coordinateSystem;

    final GameOfLife gameOfLife;

    gameOfLife = new GameOfLife(this.coordinateSystem, primordialGenerationPublisher);

    allGenerations = gameOfLife.getAllGenerations();
 }

  public static GameOfLifeSystem create(
      final Publisher<Cell> primordialGenerationPublisher,
      final CoordinateSystem coordinateSystem) {
    return new GameOfLifeSystem(primordialGenerationPublisher, coordinateSystem);
  }
}