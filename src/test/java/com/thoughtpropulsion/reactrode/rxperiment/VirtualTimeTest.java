package com.thoughtpropulsion.reactrode.rxperiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.scheduler.VirtualTimeScheduler;

public class VirtualTimeTest {

  private VirtualTimeScheduler scheduler;

  @BeforeEach
  void beforeEach() {
    scheduler = VirtualTimeScheduler.create();
  }

  @Test
  void delayTest() throws InterruptedException {

    final AtomicBoolean triggered = new AtomicBoolean();
    final AtomicLong duration = new AtomicLong();

    Mono
        .delay(Duration.ofMillis(100L),scheduler)
        .doOnNext(actualDuration -> {
          triggered.set(true);
          duration.set(actualDuration);
        })
        .subscribe();

    scheduler.advanceTimeBy(Duration.ofSeconds(1));

    assertThat(triggered.get()).isTrue();
    assertThat(duration.get()).isEqualTo(0L);
  }
}
