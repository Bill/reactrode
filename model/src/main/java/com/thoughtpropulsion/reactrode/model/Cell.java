package com.thoughtpropulsion.reactrode.model;

import java.util.Objects;

import org.springframework.data.annotation.PersistenceConstructor;

/**
 * A single cell in Conway's Game of Life.
 */
public class Cell {
  public final Coordinates coordinates;
  public final boolean isAlive;
  public final boolean isNewborn; // if isAlive, is this cell newborn?


  public static Cell createDead(final Coordinates coordinates) {
    return new Cell(coordinates, false, false);
  }

  public static Cell createAlive(final Coordinates coordinates,
                                 final boolean isNewborn) {
    return new Cell(coordinates, true, isNewborn);
  }

  // don't call this. It's here to make RSocket serialization via Jackson work
  private Cell() {
    this(null, false, false);
  }

  // this annotation is here to make spring-data-geode deserialization work
  @PersistenceConstructor
  public Cell(final Coordinates coordinates, final boolean isAlive, final boolean isNewborn) {
    this.coordinates = coordinates;
    this.isAlive = isAlive;
    this.isNewborn = isNewborn;
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
