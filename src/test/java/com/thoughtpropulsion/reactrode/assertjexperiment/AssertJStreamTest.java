package com.thoughtpropulsion.reactrode.assertjexperiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AssertJStreamTest {

  private static Supplier<Stream<Integer>> ss = () -> Stream.of(1, 2);

  @Disabled
  @Test
  void compareWithSelf() {
    /*
     This fails:

      org.opentest4j.AssertionFailedError:
      Expecting:
       <[1, 2]>
      to be equal to:
       <java.util.stream.ReferencePipeline$Head@7e0e6aa2>
      but was not.
      Expected :java.util.stream.ReferencePipeline$Head@7e0e6aa2
      Actual   :[1, 2]

     */
    assertThat(ss.get()).isEqualTo(ss.get());
  }

  @Test
  void compareWithSelfThroughIterable() {
    // to compare a stream on RHS, it must be converted to something iterable
    final Iterable<Integer> c = ss.get().collect(Collectors.toList());
    assertThat(ss.get()).isEqualTo(c);
  }

  @Test
  void containsExactly() {
    // use containsExactly() to compare stream on LHS w/ varargs on RHS
    assertThat(ss.get()).containsExactly(1,2);
  }

}
