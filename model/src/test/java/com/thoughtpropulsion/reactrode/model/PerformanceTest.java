package com.thoughtpropulsion.reactrode.model;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomPattern;
import static com.thoughtpropulsion.reactrode.model.Timing.elapsed;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

public class PerformanceTest {

  public static final int PRIMORDIAL_GENERATION = -1;

  @ParameterizedTest
  @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
  public void timeToGenerate(final int exponent) {
    final long n = Math.round(Math.pow(10, exponent));

    final CoordinateSystem coordinateSystem = new CoordinateSystem(100, 100);

    final List<Boolean> pattern = randomPattern(400,400 );

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    final long elapsed = elapsed(() ->
        Flux.from(gameOfLifeSystem.getAllGenerations()).take(n).subscribe());

    System.out.println(String.format("1 x 10^%d cells generated in %d nanoseconds",exponent,elapsed));
  }
}
