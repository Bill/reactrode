package com.thoughtpropulsion.reactrode;

public class Cell {
  public final Coordinate coordinate;
  public final boolean isAlive;

  public static Cell create(final Coordinate coordinate, final boolean isAlive) {
    return new Cell(coordinate,isAlive);
  }

  private Cell(final Coordinate coordinate, final boolean isAlive) {
    this.coordinate = coordinate; this.isAlive = isAlive;
  }
}
