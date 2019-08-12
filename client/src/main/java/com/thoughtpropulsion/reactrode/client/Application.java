package com.thoughtpropulsion.reactrode.client;

import java.util.concurrent.atomic.LongAdder;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class Application {

  final LifeClient lifeClient;

  public Application(final LifeClient lifeClient) {
    this.lifeClient = lifeClient;
  }

  public static void main(String[] args) {

    new SpringApplicationBuilder()
        .main(Application.class)
        .sources(Application.class)
        .profiles("client")
        .run(args);
  }

  @Bean
  public ApplicationRunner getRunner() throws Exception {
    return args -> {
      // TODO: figure out why this flux never terminates (app hangs)
      final LongAdder liveCount = new LongAdder();
      final long starting = System.nanoTime();
      final int demand = 20 * 400 * 400;
      Flux.from(lifeClient.allGenerations())
          .take(demand)
          .doOnNext(
              cell -> {
                if (cell.isAlive) {
                  liveCount.increment();
                } else {
                  liveCount.decrement();
                }
              })
          .doFinally(_ignored -> {
            final long ending = System.nanoTime();
            final long elapsed = ending - starting;
            System.out
                .println(String.format("%d cells with net live count: %s took %d nanoseconds", demand, liveCount, elapsed));
          })
          .subscribe();
    };
  }

}