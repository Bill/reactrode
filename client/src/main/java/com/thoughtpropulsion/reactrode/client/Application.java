package com.thoughtpropulsion.reactrode.client;

import java.util.concurrent.atomic.LongAdder;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
public class Application {

  private static ConfigurableApplicationContext applicationContext;
  final LifeClient lifeClient;

  public Application(final LifeClient lifeClient) {
    this.lifeClient = lifeClient;
  }

  public static void main(String[] args) {

    applicationContext = new SpringApplicationBuilder()
        .main(Application.class)
        .sources(Application.class)
        .profiles("client")
        .run(args);
  }

  @Bean
  public ApplicationRunner getRunnerXXX() throws Exception {
    return args -> {
      // TODO: figure out why this flux never terminates (app hangs)
      final LongAdder liveCount = new LongAdder();
      final long starting = System.nanoTime();
      final int demand = 2000000;
      Flux.from(lifeClient.allGenerations())
          .subscribeOn(Schedulers.parallel())
          .publishOn(Schedulers.elastic())
//          .take(demand)
          .limitRequest(demand)
          .doOnNext(
              cell -> {
                if (cell.isAlive) {
                  liveCount.increment();
                } else {
                  liveCount.decrement();
                }
              })
          .doOnComplete(() -> {
            final long ending = System.nanoTime();
            final long elapsed = ending - starting;
            System.out
                .println(String.format("got %.0f cells per second", demand * 1.0 / elapsed * 1_000_000_000));
          })
          .doOnCancel(() -> System.out.println("canceled"))
          .doOnError((err)-> System.out.println("error: " + err))
          .doFinally((_ignored) -> applicationContext.close())
          .subscribe();
    };
  }

  @Bean
  public ApplicationRunner getRunner() throws Exception {
    return args -> {
      // TODO: figure out why this flux never terminates (app hangs)
      final LongAdder liveCount = new LongAdder();
      final long starting = System.nanoTime();
      final int demand = 2000000;
      Flux.from(lifeClient.empties())
          .subscribeOn(Schedulers.parallel())
          .publishOn(Schedulers.elastic())
//          .take(demand)
          .limitRequest(demand)
          .doOnNext(empty->liveCount.increment())
          .doOnComplete(() -> {
            final long ending = System.nanoTime();
            final long elapsed = ending - starting;
            System.out
                .println(String.format("got %.0f empties per second", demand * 1.0 / elapsed * 1_000_000_000));
          })
          .doOnCancel(() -> System.out.println("canceled"))
          .doOnError((err)-> System.out.println("error: " + err))
          .doFinally((_ignored) -> applicationContext.close())
          .subscribe();
    };
  }

}