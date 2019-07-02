package com.thoughtpropulsion.reactrode.rxperiment;

import static com.thoughtpropulsion.reactrode.Functional.returning;
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

    /*

     !! LOOKIE HERE !! this is the important thing

     publish(2) creates the ConnectableFlux that will start producing to all subscribers
     after the second subscription arrives

     */
    final Flux<Integer> topic = head.publish(2).autoConnect(2);

    final AtomicBoolean fastDone = new AtomicBoolean();
    final AtomicBoolean fastNeedMore = new AtomicBoolean(true);
    final AtomicReference<Subscription> fastSubscription =
        subscribe("Fast!", topic, fastDone, fastNeedMore);

    final AtomicBoolean slowDone = new AtomicBoolean();
    final AtomicBoolean slowNeedMore = new AtomicBoolean(true);
    final AtomicReference<Subscription> slowSubscription =
        subscribe("slow ", topic, slowDone, slowNeedMore);

    final Random random = new Random(1);

    generateRequestsUntilComplete("Fast!", fastSubscription,100, random,
        fastNeedMore, fastDone);

    generateRequestsUntilComplete("slow ", slowSubscription,500, random,
        slowNeedMore, slowDone);

    waitUntilAll(fastDone,slowDone);

    assertThat(true).isTrue();
  }

  /*
   Return ref to Subscription to (ConnectableFlux). This method takes a Flux but that Flux
   was returned from ConnectableFlux.autoConnect()
   */
  private AtomicReference<Subscription> subscribe(
      final String name,
      final Flux<Integer> topic,
      final AtomicBoolean done,
      final AtomicBoolean needMore) {

    final AtomicReference<Subscription> subscriptionRefs = new AtomicReference<>();

    return returning(subscriptionRefs, subscriptionRef ->

        topic.subscribe(

            item -> {
              System.out.println(name + " got item: " + item);
              needMore.set(true);},

            error -> {
              System.out.println(name + " got error: " + error);
              done.set(true);},

            () -> {
              System.out.println(name + " complete.");
              done.set(true);},

            subscription -> {
              subscriptionRef.set(subscription);}));
  }

  /*
   This sets up a task that issues upstream demand as needed, sleeping periodically.
   */
  static void generateRequestsUntilComplete(
      final String name,
      final AtomicReference<Subscription> subscription,
      final long frequency,
      final Random random,
      final AtomicBoolean needMore,
      final AtomicBoolean complete) {

    if (needMore.get() && !complete.get()) {
      needMore.set(false);
      System.out.println(name + " requesting 1");
      subscription.get().request(1);
    }

    final long delay = gaussianLong(frequency, random);

    System.out.println(name + " delaying " + delay);
    Mono
        .delay(Duration.ofMillis(delay))
        .doOnNext(_actualDuration -> generateRequestsUntilComplete(
            name, subscription,frequency,random, needMore, complete))
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

  private static long gaussianLong(final long range, final Random random) {
    return (long) (random.nextGaussian() * range + range);
  }

}
