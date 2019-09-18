package com.thoughtpropulsion.reactrode.rxperiment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

public class ErrorRecoveryTest {

  @BeforeAll
  static void beforeAll() { ReactorDebugAgent.init();}

  @Test
  public void retryResubscribes() {

    final Flux<Integer> f = Flux.from(publisherWithTransientError())
        .retry(1);

    StepVerifier.create(f)
        .expectNext(1)
        .expectNext(1)
        .expectNext(2)
        .expectNext(3)
        .verifyComplete();
  }

  @Test
  public void delayElements() {

    StepVerifier
        .withVirtualTime(() -> Flux.just(1, 2).delayElements(Duration.ofSeconds(2)))
        .expectSubscription()
        .then(() -> System.out.println("got subscription"))
        .expectNoEvent(Duration.ofSeconds(2))
        .then(() -> System.out.println("got event"))
        .expectNext(1)
        .then(() -> System.out.println("got 1"))
        .expectNoEvent(Duration.ofSeconds(2))
        .expectNext(2)
        .then(() -> System.out.println("got 2"))
        .verifyComplete();
  }

  private Publisher<Integer> publisherWithTransientError() {
    final AtomicBoolean thrown = new AtomicBoolean(false);
    return Flux.just(1, 2, 3)
        .doOnNext(i -> {
          /*
           Simulate a transient error in processing the flux.
           Throw the first time we see an even number, then never throw again.
           */
          if (i % 2 == 0 && !thrown.getAndSet(true)) {
            throw new IllegalStateException("even numbers are bad");
          }
        });
  }

  private String currentTime() {
    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    final LocalDateTime now = LocalDateTime.now();
    return dtf.format(now);
  }

}
