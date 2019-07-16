package com.thoughtpropulsion.reactrode.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Patterns {

  public static Iterable<Cell> cellsFromBits(final List<Boolean> bits, final int generation,
                                      final CoordinateSystem coordinateSystem) {
    final int columns = coordinateSystem.columns;
    final int rows = coordinateSystem.rows;

    final Collection<Cell> cells = new ArrayList<>(columns * rows);
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        cells.add(Cell.create(
            coordinateSystem.createCoordinates(x, y, generation),
            bits.get(y * columns + x)));
      }
    }
    return cells;
  }

  public static List<Boolean> toPattern(final int... bits) {
    return Arrays.stream(bits).boxed().map(b -> b == 1)
        .collect(Collectors.toList());
  }
}
