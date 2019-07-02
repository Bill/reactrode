package com.thoughtpropulsion.reactrode.rxperiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ConnectableFluxTest {

  @Disabled
  @Test
  void foo() {

    final Flux<Integer> head = Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    final Flux<Integer> topic = head.publish(2).autoConnect(2);

    final AtomicReference<Subscription> s1 = new AtomicReference<>();
    final AtomicReference<Subscription> s2 = new AtomicReference<>();

    final AtomicBoolean s1done = new AtomicBoolean();
    final AtomicBoolean s2done = new AtomicBoolean();

    topic.subscribe(
        item -> { System.out.println("S1 got item: " + item);},
        error -> { System.out.println("S1 got error: " + error); s1done.set(true);},
        () -> { System.out.println("S1 complete."); s1done.set(true);},
        subscription -> { s1.set(subscription);});

    topic.subscribe(
        item -> { System.out.println("S2 got item: " + item);},
        error -> { System.out.println("S2 got error: " + error); s2done.set(true);},
        () -> { System.out.println("S2 complete."); s2done.set(true);},
        subscription -> { s2.set(subscription);});

    final Random random = new Random(1);


    generateRequestsForever(s1,1000,random);
    generateRequestsForever(s2,5000,random);

    waitUntilAll(s1done,s2done);

    assertThat(true).isTrue();
  }

  static void generateRequestsForever(
      final AtomicReference<Subscription> subscription,
      final long frequency,
      final Random random) {

    System.out.println("requesting 1 on " + subscription);

    subscription.get().request(1);

    final long delay = (long) ((random.nextGaussian() * frequency) + frequency);

    System.out.println("delaying " + delay);
    Mono
        .delay(Duration.ofMillis(delay))
        .doOnNext(_actualDuration -> generateRequestsForever(subscription,frequency,random))
        .subscribe();
  }

  static void waitUntilAll(final AtomicBoolean ... terms) {
    while (!and(terms)) {
      try {
        System.out.println("sleeping...");
        Thread.sleep(500L);
      } catch (InterruptedException e) {
        // no problemo
      }
    }
  }

  static boolean and(final AtomicBoolean ... terms) {
    return Stream
        .of(terms)
        .map(AtomicBoolean::get)
        .reduce(true, Boolean::logicalAnd);
  }

}
