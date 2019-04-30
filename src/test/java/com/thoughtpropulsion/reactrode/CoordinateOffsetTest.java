package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoordinateOffsetTest {

  @Test
  public void roundTrip1() {
    for(int offset = -100; offset < 100; offset++) {
      final Coordinate coordinate = Coordinate.create(offset, 4, 4);
      assertThat(coordinate.toOffset(4,4)).isEqualTo(offset);
    }
  }
}