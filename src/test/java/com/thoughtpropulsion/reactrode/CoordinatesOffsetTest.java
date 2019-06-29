package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoordinatesOffsetTest {

  @Test
  void xAndYStrictlyPositive() {
    final CoordinateSystem coordinateSystem = new CoordinateSystem(4, 5);
    for(int offset = -10; offset < 10; offset++) {
      // use non-square grid to catch problems where row/column sense is inconsistent
      final Coordinates coordinates = coordinateSystem.createCoordinate(offset);
      assertThat(coordinates.x).isNotNegative();
      assertThat(coordinates.y).isNotNegative();
    }
  }

  @Test
  void xAndYStrictlyPositive2() {
    final CoordinateSystem coordinateSystem = new CoordinateSystem(4, 5);
    for(int x = -2; x < 2; x++) {
      for(int y = -2; y < 3; y++) {
        // use non-square grid to catch problems where row/column sense is inconsistent
        final Coordinates coordinates = coordinateSystem.createCoordinate(x,y,0);
        assertThat(coordinates.x).isNotNegative();
        assertThat(coordinates.y).isNotNegative();
      }
    }
  }

  @Test
  void roundTrip() {
    final CoordinateSystem coordinateSystem = new CoordinateSystem(4, 5);
    for(int offset = -2; offset < 2; offset++) {
      // use non-square grid to catch problems where row/column sense is inconsistent
      final Coordinates coordinates = coordinateSystem.createCoordinate(offset);
      assertThat(coordinateSystem.toOffset(coordinates)).isEqualTo(offset);
    }
  }

  @Test
  void spotCheck() {
    checkOffset(-7, -2, 1, 2);
    checkOffset(-6, -1, 0, 0);
    checkOffset(-5, -1, 0, 1);
    checkOffset(-4, -1, 0, 2);
    checkOffset(-3, -1, 1, 0);
    checkOffset(-2, -1, 1, 1);
    checkOffset(-1, -1, 1, 2);
    checkOffset(0, 0, 0, 0);
    checkOffset(1, 0, 0, 1);
    checkOffset(2, 0, 0, 2);
    checkOffset(3, 0, 1, 0);
    checkOffset(4, 0, 1, 1);
    checkOffset(5, 0, 1, 2);
    checkOffset(6, 1, 0, 0);
  }

  private void checkOffset(final int offset, final int expectGeneration, final int expectY, final int expectX) {
    final CoordinateSystem coordinateSystem = new CoordinateSystem(3, 2);

    final Coordinates actual = coordinateSystem.createCoordinate(offset);
    assertThat(actual.generation).as("generation").isEqualTo(expectGeneration);
    assertThat(actual.y).as("y").isEqualTo(expectY);
    assertThat(actual.x).as("x").isEqualTo(expectX);
  }
}