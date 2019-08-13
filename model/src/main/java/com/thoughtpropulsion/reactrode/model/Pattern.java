package com.thoughtpropulsion.reactrode.model;

public class Pattern {
  public final CoordinateSystem coordinateSystem;
  public final Iterable<Boolean> cells;

  public Pattern(
      final CoordinateSystem coordinateSystem,
      final Iterable<Boolean> cells) {
    this.coordinateSystem = coordinateSystem;
    this.cells = cells;
  }
}
