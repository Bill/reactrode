package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RingBufferSequentialTest {

  private RingBufferSequential<String> buffer;

  @BeforeEach
  void before() {
    buffer = new RingBufferSequential<String>(1);
  }

  @Test
  void canPut() {
    assertThat(buffer.offer("hi")).isTrue();
  }

  @Test
  void canGetWhatWasPut() {

    buffer.offer("hi");

    assertGet("hi");
  }

  @Test
  void canFill() {
    buffer.offer("apple");
    assertGet("apple");
  }

  @Test
  void capacityLimit() {
    buffer.offer("apple");
    assertThat(buffer.offer("orange")).isFalse();
  }

  @Test
  void windowMoves() {
    buffer.offer("apple");
    buffer.poll();
    buffer.offer("orange");
    assertGet("orange");
  }

  private void assertGet(final String value) {
    final Tuple2<Integer, Option<String>> t2 = buffer.poll();
    final Option<String> strings = t2._2;
    strings.peek(string -> assertThat(string).isEqualTo(value))
        .orElse(() -> {throw new AssertionError("expected to get something but got nothing");});
  }

}