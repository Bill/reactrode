package com.thoughtpropulsion.reactrode.model;

import java.util.Comparator;
import java.util.Objects;

import org.springframework.data.annotation.PersistenceConstructor;

/**
 * These are coordinates for a cell in Conway's Game of Life. Since we're modeling the game in an
 * immutable "event sourcing" style, the coordinates includes the generation, a time coordinates.
 */
public class Coordinates implements Comparable<Coordinates> {

  public final int generation;
  public final int y;
  public final int x;

  private static final Comparator<Coordinates> COMPARATOR =
      Comparator
          .comparing((final Coordinates c) -> c.generation)
          .thenComparing(c -> c.y)
          .thenComparing(c -> c.x);

  // for use by CoordinateSystem only
  public static Coordinates create(final int x, final int y, final int generation) {
    return new Coordinates(x,y,generation);
  }

  // don't call this. It's here to make RSocket serialization via Jackson work
  // But it's incompatible w/ spring-data-geode deserialization so it's commented out?!?
  private Coordinates() {
    this(0,0,0);
  }

  // this annotation is here to make spring-data-geode deserialization work
  @PersistenceConstructor
  public Coordinates(final int x, final int y, final int generation) {
    this.x = x; this.y = y; this.generation = generation;
  }

  public static Coordinates max(final Coordinates a, final Coordinates b) {
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
    final Coordinates that = (Coordinates) o;
    return compareTo(that) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(generation, y, x);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Coordinates{");
    sb.append("generation=").append(generation);
    sb.append(", y=").append(y);
    sb.append(", x=").append(x);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public int compareTo(final Coordinates o) {
    return COMPARATOR.compare(this,o);
  }
}
