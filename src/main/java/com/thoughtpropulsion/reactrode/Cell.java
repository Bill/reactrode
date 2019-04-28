package com.thoughtpropulsion.reactrode;

import java.util.Objects;

public class Cell {
  public final Coordinate coordinate;
  public final boolean isAlive;

  public static Cell create(final Coordinate coordinate, final boolean isAlive) {
    return new Cell(coordinate,isAlive);
  }

  private Cell(final Coordinate coordinate, final boolean isAlive) {
    this.coordinate = coordinate; this.isAlive = isAlive;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Cell cell = (Cell) o;
    return isAlive == cell.isAlive &&
        coordinate.equals(cell.coordinate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinate, isAlive);
  }
}
