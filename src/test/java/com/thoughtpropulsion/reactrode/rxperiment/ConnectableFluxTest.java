package com.thoughtpropulsion.reactrode.rxperiment;

import static com.thoughtpropulsion.reactrode.Functional.returning;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.scheduler.VirtualTimeScheduler;

public class ConnectableFluxTest {

  @Disabled
  @Test
  void split2() {

    final VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();

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
        subscribe("Fast!", topic, fastDone, fastNeedMore, scheduler);

    final AtomicBoolean slowDone = new AtomicBoolean();
    final AtomicBoolean slowNeedMore = new AtomicBoolean(true);
    final AtomicReference<Subscription> slowSubscription =
        subscribe("slow ", topic, slowDone, slowNeedMore, scheduler);

    final Random random = new Random(1);

    scheduler.schedule(() ->
      startConsumerProcess("Fast!", fastSubscription,10000, random,
          fastNeedMore, fastDone, scheduler));

    scheduler.schedule(() ->
        startConsumerProcess("slow ", slowSubscription,100000, random,
          slowNeedMore, slowDone, scheduler));

    scheduler.advanceTimeBy(Duration.ofSeconds(60000));

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
      final AtomicBoolean needMore, final Scheduler scheduler) {

    return returning(new AtomicReference<>(), subscriptionRef ->

        topic
            .subscribeOn(scheduler)
            .subscribe(

                item -> {
                  log(name, "got item: " + item);
                  needMore.set(true);
                },

                error -> {
                  log(name, "got error: " + error);
                  done.set(true);
                },

                () -> {
                  log(name, "complete.");
                  done.set(true);
                },

                subscription -> {
                  subscriptionRef.set(subscription);
                }));
  }

  /*
   This sets up a task that issues upstream demand as needed, sleeping periodically.
   */
  static void startConsumerProcess(
      final String name,
      final AtomicReference<Subscription> subscription,
      final long frequency,
      final Random random,
      final AtomicBoolean needMore,
      final AtomicBoolean complete, final Scheduler scheduler) {

    if (complete.get())
      return;

    if (needMore.get()) {
      needMore.set(false);
      log(name, "requesting 1");
      subscription.get().request(1);
    }

    final long delay = gaussianLong(frequency, random);

    log(name, "delaying " + delay);
    Mono
        .delay(Duration.ofMillis(delay),scheduler)
        .doOnNext(_actualDuration -> startConsumerProcess(
            name, subscription,frequency,random, needMore, complete, scheduler))
        .subscribe();
  }

  private static long gaussianLong(final long range, final Random random) {
    return (long) (random.nextGaussian() * range + range);
  }

  private static void log(final String name, final String s) {
    System.out.println(
        String.format("%s %s (thread: %s)", name, s, Thread.currentThread().getId()));
  }

}
