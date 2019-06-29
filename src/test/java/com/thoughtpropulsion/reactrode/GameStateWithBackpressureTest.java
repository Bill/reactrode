package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GameStateWithBackpressureTest {
  private static int PRIMORDIAL_GENERATION = -1;

  @Test
  void mustCacheMoreThanOneGeneration() {
    assertThatThrownBy(() ->
        new GameStateWithBackpressure(
            1,
            new CoordinateSystem(1, 1),
            PRIMORDIAL_GENERATION))
        .hasMessageContaining("generationsCached must be greater than one but got 1");
  }
}