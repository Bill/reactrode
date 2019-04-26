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


  public static Coordinate create(final int x, final int y, final int generation, final int columns,
                                  final int rows) {
    return new Coordinate(x,y,generation,columns,rows);
  }

  private Coordinate(final int x, final int y, final int generation, final int columns,
                     final int rows) {
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
}
