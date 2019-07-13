package com.thoughtpropulsion.reactrode.rxperiment;

import static com.thoughtpropulsion.reactrode.Functional.returning;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.scheduler.VirtualTimeScheduler;

public class ConnectableFluxTest {

  static class SplitterSubscriber {
    final String name;
    final AtomicBoolean done;
    final AtomicBoolean needMore;
    final Scheduler scheduler;
    final AtomicReference<Subscription> subscription;
    final List<Integer> seen;
    final Subscriber seenSubscriber;

    SplitterSubscriber(
        final String name,
        final Flux<Integer> topic,
        final Scheduler scheduler,
        final long frequency,
        final TimeUnit timeUnit,
        final Subscriber<List<Integer>> seenSubscriber) {
      this.name = name;
      this.done = new AtomicBoolean(false);
      this.needMore = new AtomicBoolean(false);
      this.scheduler = scheduler;
      this.seenSubscriber = seenSubscriber;
      this.subscription = subscribe(name, topic, done, needMore);
      this.seen = new ArrayList<>();
      scheduler.schedulePeriodically(
          this::step, 0, frequency, timeUnit);
    }

    private AtomicReference<Subscription> subscribe(
        final String name,
        final Flux<Integer> topic,
        final AtomicBoolean done,
        final AtomicBoolean needMore) {

      return returning(
          new AtomicReference<>(),
          subscriptionRef ->
              topic
                  .subscribeOn(scheduler)
                  .subscribe(

                      item -> {
                        log(name, "got item: " + item);
                        needMore.set(true);
                        seen.add(item);
                      },

                      error -> {
                        log(name, "got error: " + error);
                        done.set(true);
                      },

                      () -> {
                        log(name, "complete.");
                        done.set(true);
                        final Mono<List<Integer>> seenMono = Mono.fromSupplier(() -> seen);
                        seenMono.subscribe(seenSubscriber);
                      },

                      subscription -> {
                        subscriptionRef.set(subscription);
                        needMore.set(true);
                      }));
    }

    /*
     Generate upstream demand as needed.

     Call this periodically
     */
    private void step() {

      if (done.get())
        return;

      if (needMore.compareAndSet(true,false)) {
        log(name, "requesting 1");
        subscription.get().request(1);
      }
    }

  }

  @Disabled
  @Test
  void split2WithRealTimeMultiThreaded() throws InterruptedException {

    final Scheduler scheduler = Schedulers.parallel();

    final Tuple2<Publisher<List<Integer>>, Publisher<List<Integer>>>
        publishers = doSplittingStuff(
            scheduler,
        100,
        1000,
        MILLISECONDS);

    Thread.sleep(9_000L);

    validate(publishers);

    final AtomicReference<List<Integer>> fastResult = firstResult(publishers._1);

    final AtomicReference<List<Integer>> slowResult = firstResult(publishers._2);

    while (null == fastResult.get() || null == slowResult.get()) {
      Thread.sleep(10);
    }
  }

  @Disabled
  @Test
  void split2WithRealTimeSingleThreaded() throws InterruptedException {

    final Scheduler scheduler = Schedulers.single();

    final Tuple2<Publisher<List<Integer>>, Publisher<List<Integer>>>
        publishers = doSplittingStuff(scheduler,
        100,
        1000,
        MILLISECONDS);

    Thread.sleep(9_000L);

    validate(publishers);

    final AtomicReference<List<Integer>> fastResult = firstResult(publishers._1);

    final AtomicReference<List<Integer>> slowResult = firstResult(publishers._2);

    while (null == fastResult.get() || null == slowResult.get()) {
      Thread.sleep(10);
    }
  }

  private Random random;

  private static boolean exploreStateSpace = false;

  Random createRandom(final long trySeed) {

    final long actualSeed;

    if (exploreStateSpace)
      actualSeed = nanoTime();
    else
      actualSeed = trySeed;

    System.out.println("using seed " + readableSeed(actualSeed));

    try {
      return returning(
          SecureRandom.getInstance("SHA1PRNG"),
          prng -> prng.setSeed(actualSeed));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }

  }

  @ParameterizedTest
  @ValueSource(longs = {1_961_772_166_836L})
  void split2In9VirtualSeconds(final long seed) {

    random = createRandom(seed);

    final VirtualTimeScheduler scheduler =
        VirtualTimeScheduler.set(
            VirtualTimeSchedulerInaccurate.create(
                random, 20, MILLISECONDS));

    final Tuple2<Publisher<List<Integer>>, Publisher<List<Integer>>>
        publishers = doSplittingStuff(
            scheduler,
        100,
        1000,
        MILLISECONDS);

    scheduler.advanceTimeBy(Duration.ofSeconds(9));

    validate(publishers);
  }

  @ParameterizedTest
  @ValueSource(longs = {142_062_396_813_291L})
  void split2In135VirtualMinutes(final long seed) {

    random = createRandom(seed);

    final VirtualTimeScheduler scheduler =
        VirtualTimeScheduler
            .set(VirtualTimeSchedulerInaccurate.create(
                random, 1, MINUTES));

    final Tuple2<Publisher<List<Integer>>, Publisher<List<Integer>>>
        publishers = doSplittingStuff(
            scheduler,
        2,
        15,
        MINUTES);

    scheduler.advanceTimeBy(Duration.ofMinutes(135));

    validate(publishers);
  }

  @Test
  void split2In10VirtualSecondsIsDeterministic() {
    split2In9VirtualSeconds(1);
    final long afterFirstRun = random.nextLong();
    split2In9VirtualSeconds(1);
    assertThat(random.nextLong())
        .as("random sequences match run to run")
        .isEqualTo(afterFirstRun);
  }

  @Test
  void split2In3VirtualHoursIsDeterministic() {
    split2In135VirtualMinutes(1);
    final long afterFirstRun = random.nextLong();
    split2In135VirtualMinutes(1);
    assertThat(random.nextLong())
        .as("random sequences match run to run")
        .isEqualTo(afterFirstRun);
  }

  private void validate(
      final Tuple2<Publisher<List<Integer>>,Publisher<List<Integer>>> publishers) {

    final AtomicReference<List<Integer>> fastSeen = firstResult(publishers._1);
    final AtomicReference<List<Integer>> slowSeen = firstResult(publishers._2);

    assertThat(fastSeen.get()).as("FAST! consumer saw all the items").isNotNull();
    assertThat(slowSeen.get()).as("slow  consumer saw all the items").isNotNull();

    /*
     Have to turn the stream into an iterable to compare it due to AssertJ bug:
     https://github.com/joel-costigliola/assertj-core/issues/1545
     */
    final Iterable<Integer> asList = testSequence().collect(Collectors.toList());
    assertThat(fastSeen.get()).isEqualTo(asList);
    assertThat(slowSeen.get()).isEqualTo(asList);
  }

  private Tuple2<Publisher<List<Integer>>,Publisher<List<Integer>>> doSplittingStuff(
      final Scheduler scheduler,
      final long fastFrequency,
      final long slowFrequency,
      final TimeUnit timeUnit) {
    /*

     !! LOOKIE HERE !! this is what we're testing

     publish(2) creates the ConnectableFlux that will start producing to all subscribers
     after the second subscription arrives

     */
    final Flux<Integer> topic =
        Flux.fromStream(testSequence())
            .doOnNext(i -> System.out.println("broadcasting item " + i))
            .publish(3)           // bounded demand
            .autoConnect(2); // number of subscribers to wait for

    final ReplayProcessor<List<Integer>> fastSeen = ReplayProcessor.cacheLast();
    final ReplayProcessor<List<Integer>> slowSeen = ReplayProcessor.cacheLast();

    final SplitterSubscriber fast =
        new SplitterSubscriber("Fast!", topic, scheduler, fastFrequency, timeUnit, fastSeen);

    final SplitterSubscriber slow =
        new SplitterSubscriber("slow ", topic, scheduler, slowFrequency, timeUnit, slowSeen);

    return new Tuple2<>(fastSeen,slowSeen);
  }

  private Stream<Integer> testSequence() {
    return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  private static void log(final String name, final String s) {
    System.out.println(
        String.format("%s %s (thread: %s)",
            name, s, Thread.currentThread().getId()));
  }

  String readableSeed(final Long seed) {
    return String.format("%,d",seed).replace(',','_') + "L";
  }

  private AtomicReference<List<Integer>> firstResult(final Publisher<List<Integer>> publisher) {
    return returning(new AtomicReference<>(),
        valueHolder ->
            publisher.subscribe(new Subscriber<List<Integer>>(){

              @Override
              public void onSubscribe(final Subscription s) {
                s.request(1);
              }

              @Override
              public void onNext(final List<Integer> o) {
                valueHolder.set(o);
              }

              @Override
              public void onError(final Throwable t) {
              }

              @Override
              public void onComplete() {
              }
            })
        );
  }

}
