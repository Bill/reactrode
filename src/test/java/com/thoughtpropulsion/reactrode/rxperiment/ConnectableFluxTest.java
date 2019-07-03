package com.thoughtpropulsion.reactrode.rxperiment;

import static com.thoughtpropulsion.reactrode.Functional.returning;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

    SplitterSubscriber(
        final String name,
        final Flux<Integer> topic,
        final Scheduler scheduler) {
      this.name = name;
      this.done = new AtomicBoolean();
      this.needMore = new AtomicBoolean(true);
      this.scheduler = scheduler;
      this.subscription = subscribe(name, topic, done, needMore);
      this.seen = new ArrayList<>();
    }

    private AtomicReference<Subscription> subscribe(
        final String name,
        final Flux<Integer> topic,
        final AtomicBoolean done,
        final AtomicBoolean needMore) {

      return returning(new AtomicReference<>(), subscriptionRef ->

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
                  },

                  subscription -> {
                    subscriptionRef.set(subscription);
                  }));
    }

    /*
     This sets up a task that issues upstream demand as needed, sleeping periodically.
     */
    void start(
        final long frequency,
        final Random random) {

      if (done.get())
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
          .doOnNext(_actualDuration -> start(frequency,random))
          .subscribe();
    }

    private static long gaussianLong(final long range, final Random random) {
      return (long) (random.nextGaussian() * range + range);
    }

  }

  @Disabled
  @Test
  void split2WithRealTime() throws InterruptedException {

    final Scheduler scheduler = Schedulers.single();

    final Tuple2<SplitterSubscriber, SplitterSubscriber>
        t2 = doSplittingStuff(scheduler, 100, 1000);

    while (!t2._1.done.get() || !t2._2.done.get()) {
      Thread.sleep(10L);
    }

//    assertThat(t2._1.seen.stream()).isEqualTo(testSequence());
//    assertThat(t2._2.seen.stream()).isEqualTo(testSequence());
    assertThat(t2._1.seen).isEqualTo(testSequence().collect(Collectors.toList()));
    assertThat(t2._2.seen).isEqualTo(testSequence().collect(Collectors.toList()));
  }

  @Test
  void split2WithVirtualTimeShort() {

    final VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();
    final Tuple2<SplitterSubscriber, SplitterSubscriber>
        t2 = doSplittingStuff(scheduler, 100, 1000);

    scheduler.advanceTimeBy(Duration.ofSeconds(60));

//    assertThat(t2._1.seen.stream()).isEqualTo(testSequence());
//    assertThat(t2._2.seen.stream()).isEqualTo(testSequence());
    assertThat(t2._1.seen).isEqualTo(testSequence().collect(Collectors.toList()));
    assertThat(t2._2.seen).isEqualTo(testSequence().collect(Collectors.toList()));
  }

  @Test
  void split2WithVirtualTimeLong() {

    final VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();
    final Tuple2<SplitterSubscriber, SplitterSubscriber>
        t2 = doSplittingStuff(scheduler, 100000, 1000000);

    scheduler.advanceTimeBy(Duration.ofSeconds(60000));

//    assertThat(t2._1.seen.stream()).isEqualTo(testSequence());
//    assertThat(t2._2.seen.stream()).isEqualTo(testSequence());
    assertThat(t2._1.seen).isEqualTo(testSequence().collect(Collectors.toList()));
    assertThat(t2._2.seen).isEqualTo(testSequence().collect(Collectors.toList()));
  }

  private Tuple2<SplitterSubscriber,SplitterSubscriber> doSplittingStuff(
      final Scheduler scheduler,
      final int fastFrequency,
      final int slowFrequency) {
    /*

     !! LOOKIE HERE !! this is the important thing

     publish(2) creates the ConnectableFlux that will start producing to all subscribers
     after the second subscription arrives

     */
    final Flux<Integer> topic =
        Flux.fromStream(testSequence())
            .publish(2)
            .autoConnect(2);

    final SplitterSubscriber fast =
        new SplitterSubscriber("Fast!", topic, scheduler);

    final SplitterSubscriber slow =
        new SplitterSubscriber("slow ", topic, scheduler);

    final Random random = new Random(1);

    scheduler.schedule(
        () -> fast.start(fastFrequency, random));

    scheduler.schedule(
        () -> slow.start(slowFrequency, random));

    return new Tuple2<>(fast,slow);
  }

  private Stream<Integer> testSequence() {
    return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  private static void log(final String name, final String s) {
    System.out.println(
        String.format("%s %s (thread: %s)", name, s, Thread.currentThread().getId()));
  }

}
