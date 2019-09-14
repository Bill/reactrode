package com.thoughtpropulsion.reactrode.model;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomList;
import static com.thoughtpropulsion.reactrode.model.Timing.elapsed;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

public class PerformanceTest {

  public static final int PRIMORDIAL_GENERATION = -1;

  @ParameterizedTest
  @ValueSource(ints = {0,1,2,3,4,5,6,7})
  public void timeToGenerate(final int exponent) {
    final long n = Math.round(Math.pow(10, exponent));

    final CoordinateSystem coordinateSystem = new CoordinateSystem(100, 100);

    final List<Boolean> pattern = randomList(coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    final LongAdder generated = new LongAdder();

    final long elapsed = elapsed(() ->
        Flux.from(gameOfLifeSystem.getAllGenerations()).take(n).doOnNext(c -> generated.increment()).subscribe());

    assertThat(generated.sum()).isEqualTo(n);

    System.out.println(String.format("1 x 10^%d cells generated in %d nanoseconds",exponent,elapsed));
  }
}
