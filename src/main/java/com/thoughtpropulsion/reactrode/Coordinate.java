package com.thoughtpropulsion.reactrode;

import java.util.Objects;

/**
 * This is a coordinate for a cell in Conway's Game of Life. Since we're modeling the game in an
 * immutable "event sourcing" style, the coordinate includes the generation, a time coordinate.
 *
 * The game board/world is modeled as a torus so that there are no edges. This solves problems
 * for liveness calculations. Since liveness of a cell is based on its (8) neighboring cells
 * having edges would require special case processing.
 */
public class Coordinate {
  public final int generation;
  public final int y;
  public final int x;

  public static Coordinate create(final int x, final int y, final int generation,
                                  final int columns, final int rows) {
    return new Coordinate(x,y,generation,columns,rows);
  }

  public static Coordinate create(final int offset,
                                  final int columns, final int rows) {
    final int generationSize = columns * rows;

    final int generation;
    if (offset < 0) {
      generation = (offset - generationSize + 1) / generationSize;
    } else {
      generation = offset / generationSize;
    }

    final int generationStart = generation * generationSize;
    final int y = (offset - generationStart)/columns;
    final int rowStart = generationStart + y * columns;
    final int x = offset - rowStart;
    return new Coordinate(x,y, generation, columns, rows);
  }

  public static int toOffset(final int x, final int y, final int generation,
                             final int columns, final int rows) {
    return columns * (generation * rows + y) + x;
  }

  public int toOffset(final int columns, final int rows) {
    return toOffset(x,y,generation,columns,rows);
  }

  private Coordinate(final int x, final int y, final int generation,
                     final int columns, final int rows) {
    // modulo arithmetic maps the coordinate parameters into canonical locations on the torus
    this.x = x%columns; this.y = y%rows; this.generation = generation;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Coordinate that = (Coordinate) o;
    return generation == that.generation &&
        y == that.y &&
        x == that.x;
  }

  @Override
  public int hashCode() {
    return Objects.hash(generation, y, x);
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("Coordinate{");
    sb.append("generation=").append(generation);
    sb.append(", y=").append(y);
    sb.append(", x=").append(x);
    sb.append('}');
    return sb.toString();
  }
}
