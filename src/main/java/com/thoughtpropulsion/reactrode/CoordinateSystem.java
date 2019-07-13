package com.thoughtpropulsion.reactrode;

/**
 * Maps cartesian coordinates into the range (0 to {@code columns}, 0 to {@code rows}. Coordinates
 * outside that range are are wrapped via a toroidal mapping.
 *
 * The toroidal mapping solves problems for liveness calculations. Since liveness of a cell is
 * based on its (8) neighboring cells having edges would require special case processing.
 *
 * The coordinates system is used to construct {@link Coordinates} objects and also to calculate
 * integer offsets for them.
 *
 * This abstraction is imperfect. {@link Coordinates}s do not remember their {@link
 * CoordinateSystem}. It is possible to create a {@link Coordinates} via one {@link CoordinateSystem}
 * and then e.g. calculate its offset via a different {@link CoordinateSystem}. Don't do that.
 */
public class CoordinateSystem {
  public final int columns; // x
  public final int rows;    // y

  public CoordinateSystem(final int columns, final int rows) {
    this.columns = columns;
    this.rows = rows;
  }

  public Coordinates createCoordinates(final int x, final int y, final int generation) {
    // modular arithmetic maps the coordinates parameters into the torus
    return Coordinates.create(Math.floorMod(x,columns), Math.floorMod(y,rows), generation);
  }

  public Coordinates createCoordinates(final int offset) {
    final int generationSize = columns * rows;

    final int generation;
    if (offset < 0) {
      generation = (offset - generationSize + 1) / generationSize;
    } else {
      generation = offset / generationSize;
    }

    final int generationStart = generation * generationSize;
    final int y = (offset - generationStart) / columns;
    final int rowStart = generationStart + y * columns;
    final int x = offset - rowStart;
    return Coordinates.create(x, y, generation);
  }

  public int toOffset(final Coordinates coordinates) {
    return toOffset(coordinates.x, coordinates.y, coordinates.generation);
  }

  public int toOffset(final int x, final int y, final int generation) {
    return columns * (generation * rows + y) + x;
  }

  public int size() {
    return columns * rows;
  }

  /*
   Create coordinates at compass points relative to receiver
   */

  public Coordinates n(final Coordinates c) {
    return createCoordinates(c.x,c.y+1,c.generation);
  }

  public Coordinates s(final Coordinates c) {
    return createCoordinates(c.x,c.y-1,c.generation);
  }

  public Coordinates e(final Coordinates c) {
    return createCoordinates(c.x+1,c.y,c.generation);
  }

  public Coordinates w(final Coordinates c) {
    return createCoordinates(c.x-1,c.y,c.generation);
  }

  public Coordinates ne(final Coordinates c) {
    return createCoordinates(c.x+1,c.y+1,c.generation);
  }

  public Coordinates se(final Coordinates c) {
    return createCoordinates(c.x+1,c.y-1,c.generation);
  }

  public Coordinates sw(final Coordinates c) {
    return createCoordinates(c.x-1,c.y-1,c.generation);
  }

  public Coordinates nw(final Coordinates c) {
    return createCoordinates(c.x-1,c.y+1,c.generation);
  }

  /*
   Create coordinates at the same x/y position but time-shifted
   */

  public Coordinates timeShifted(final Coordinates c, final int generation) {
    return createCoordinates(c.x,c.y,generation);
  }


}
