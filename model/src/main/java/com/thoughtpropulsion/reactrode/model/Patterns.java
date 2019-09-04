package com.thoughtpropulsion.reactrode.model;

import static com.thoughtpropulsion.reactrode.model.Functional.returning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Patterns {

  // pattern form factor may differ from coordinateSystem but it must not be larger
  public static Iterable<Cell> cellsFromBits(final Pattern pattern, final int generation,
                                             final CoordinateSystem coordinateSystem) {
    final int columns = coordinateSystem.columns;
    final int rows = coordinateSystem.rows;
    final int colOffset = Math.round((columns - pattern.coordinateSystem.columns)/2);
    final int rowOffset = Math.round((rows - pattern.coordinateSystem.rows)/2);
    assert(colOffset >= 0);
    assert(rowOffset >= 0);
    final Iterator<Boolean> patternIterator = pattern.cells.iterator();
    final Collection<Cell> cells = new ArrayList<>(columns * rows);
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < columns; col++) {
        final Cell newCell;
        if (row < rowOffset || row >= rowOffset + pattern.coordinateSystem.rows ||
            col < colOffset || col >= colOffset + pattern.coordinateSystem.columns) {
          newCell = Cell.createDead(
              coordinateSystem.createCoordinates(col, row, generation));
        } else {
          final Boolean isAlive = patternIterator.next();
          if (isAlive)
            newCell = Cell.createAlive(
                coordinateSystem.createCoordinates(col, row, generation), true);
          else
            newCell = Cell.createDead(
                coordinateSystem.createCoordinates(col, row, generation));
        }
        cells.add(newCell);
      }
    }
    return cells;

  }

  // Call this if bits has same form factor as coordinateSystem
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

  public static Pattern randomPattern(final int columns, final int rows) {
    return new Pattern(new CoordinateSystem(columns,rows), randomList(columns,rows));
  }

  public static List<Boolean> randomList(final int columns, final int rows) {
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

  public static List<Boolean> stableBlockPattern() {
    return toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);
  }

  public static Pattern pufferfishSpaceshipPattern() {
    final InputStream
        bits =
        Patterns.class.getClassLoader().getResourceAsStream("pufferfishSpaceship.bits");
    assert(bits != null);
    final Reader chars = new BufferedReader(new InputStreamReader(bits));
    final ArrayList<Boolean> cells = new ArrayList<>();
    int c;
    int columns = 0;
    int rows = 0;
    try {
      while ((c = chars.read()) != -1) {
        switch(c) {
          case '1':
            cells.add(true);
            break;
          case '0':
            cells.add(false);
            break;
          case '\n':
            if (columns == 0) {
              columns = cells.size();
            }
        }
      }
      rows = cells.size() / columns;
      assert (columns <= 100);
      assert (rows <= 100);
      return new Pattern(new CoordinateSystem(columns,rows), cells);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
