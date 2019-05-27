package com.thoughtpropulsion.reactrode;

import java.util.Comparator;
import java.util.Objects;

/**
 * This is a coordinate for a cell in Conway's Game of Life. Since we're modeling the game in an
 * immutable "event sourcing" style, the coordinate includes the generation, a time coordinate.
 */
public class Coordinate implements Comparable<Coordinate> {

  public final int generation;
  public final int y;
  public final int x;

  private static final Comparator<Coordinate> COMPARATOR =
      Comparator
          .comparing((final Coordinate c) -> c.generation)
          .thenComparing(c -> c.y)
          .thenComparing(c -> c.x);

  // for use by CoordinateSystem only
  static Coordinate create(final int x, final int y, final int generation) {
    return new Coordinate(x,y,generation);
  }

  private Coordinate(final int x, final int y, final int generation) {
    this.x = x; this.y = y; this.generation = generation;
  }

  public static Coordinate max(final Coordinate a, final Coordinate b) {
    if (a.compareTo(b) < 0)
      return b;
    else
      return a;
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

  @Override
  public int compareTo(final Coordinate o) {
    return COMPARATOR.compare(this,o);
  }
}
