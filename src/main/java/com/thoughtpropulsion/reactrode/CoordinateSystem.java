package com.thoughtpropulsion.reactrode;

public class CoordinateSystem {
  public final int columns; // x
  public final int rows;    // y

  CoordinateSystem(final int columns, final int rows) {
    this.columns = columns; this.rows = rows;
  }

  public Coordinate createCoordinate(final int x, final int y, final int generation) {
    // modular arithmetic maps the coordinate parameters into the torus
    return Coordinate.create(x%columns, y%rows, generation);
  }

  public Coordinate createCoordinate(final int offset) {
    final int generationSize = columns * rows;

    final int generation;
    if (offset < 0) {
      generation = (offset - generationSize + 1) / generationSize;
    } else {
      generation = offset / generationSize;
    }

    final int generationStart = generation * generationSize;
    final int y = (offset - generationStart)/ columns;
    final int rowStart = generationStart + y * columns;
    final int x = offset - rowStart;
    return Coordinate.create(x,y,generation);
  }

  public int toOffset(final Coordinate coordinate) {
    return toOffset(coordinate.x, coordinate.y, coordinate.generation);
  }

  public int toOffset(final int x, final int y, final int generation) {
    return columns * (generation * rows + y) + x;
  }

  public int size() {
    return columns * rows;
  }

}
