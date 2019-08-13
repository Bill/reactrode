package com.thoughtpropulsion.reactrode.model;

import static com.thoughtpropulsion.reactrode.model.Functional.returning;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Patterns {

  public static Iterable<Cell> cellsFromBits(final List<Boolean> bits, final int generation,
                                      final CoordinateSystem coordinateSystem) {
    final int columns = coordinateSystem.columns;
    final int rows = coordinateSystem.rows;

    final Collection<Cell> cells = new ArrayList<>(columns * rows);
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        final Boolean isAlive = bits.get(y * columns + x);
        final Cell newCell;
        if (isAlive)
          newCell = Cell.createAlive(
            coordinateSystem.createCoordinates(x, y, generation), true);
        else
          newCell = Cell.createDead(
              coordinateSystem.createCoordinates(x, y, generation));
        cells.add(newCell);
      }
    }
    return cells;
  }

  public static List<Boolean> toPattern(final int... bits) {
    return Arrays.stream(bits).boxed().map(b -> b == 1)
        .collect(Collectors.toList());
  }

  public static List<Boolean> randomPattern(final int columns, final int rows) {
    final Random random = createRandom(1L);
    return Stream.generate(random::nextBoolean).limit(columns * rows).collect(Collectors.toList());
  }

  public static Random createRandom(final long seed) {
    try {
      return returning(SecureRandom.getInstance("SHA1PRNG"), random -> random.setSeed(seed));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
