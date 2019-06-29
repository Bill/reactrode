package com.thoughtpropulsion.reactrode;

import java.util.Objects;

/**
 * A single cell in Conway's Game of Life.
 */
public class Cell {
  public final Coordinates coordinates;
  public final boolean isAlive;

  public static Cell create(final Coordinates coordinates, final boolean isAlive) {
    return new Cell(coordinates,isAlive);
  }

  private Cell(final Coordinates coordinates, final boolean isAlive) {
    this.coordinates = coordinates; this.isAlive = isAlive;
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
        coordinates.equals(cell.coordinates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinates, isAlive);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Cell{");
    sb.append("coordinates=").append(coordinates);
    sb.append(", isAlive=").append(isAlive);
    sb.append('}');
    return sb.toString();
  }
}
