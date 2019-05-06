package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoordinateOffsetTest {

  @Test
  public void xAndYStrictlyPositive() {
    for(int offset = -2; offset < 2; offset++) {
      // use non-square grid to catch problems where row/column sense is inconsistent
      final Coordinate coordinate = Coordinate.create(offset, 4, 5);
      assertThat(coordinate.x).isNotNegative();
      assertThat(coordinate.y).isNotNegative();
    }
  }

  @Test
  public void roundTrip() {
    for(int offset = -2; offset < 2; offset++) {
      // use non-square grid to catch problems where row/column sense is inconsistent
      final Coordinate coordinate = Coordinate.create(offset, 4, 5);
      assertThat(coordinate.toOffset(4,5)).isEqualTo(offset);
    }
  }

  @Test
  public void spotCheck() {
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
    final Coordinate actual = Coordinate.create(offset, 3, 2);
    assertThat(actual.generation).isEqualTo(expectGeneration).as("generation");
    assertThat(actual.y).isEqualTo(expectY).as("x");
    assertThat(actual.x).isEqualTo(expectX).as("y");
  }
}