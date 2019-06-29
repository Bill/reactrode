package com.thoughtpropulsion.reactrode.rxperiment;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

class ReplayTest {

  @Test
  void replays() {
    final ReplayProcessor<String> processor = ReplayProcessor.create(1);

    processor.onNext("apple");
    processor.onNext("orange");
    processor.onComplete();

    StepVerifier.create(processor)
        .expectNext("orange")
        .expectComplete()
        .verify(Duration.ofSeconds(1));
  }

}
