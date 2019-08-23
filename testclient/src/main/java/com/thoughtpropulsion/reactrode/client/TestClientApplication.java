package com.thoughtpropulsion.reactrode.client;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Empty;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
public class TestClientApplication {

  private static ConfigurableApplicationContext applicationContext;
  final LifeClient lifeClient;

  public TestClientApplication(final LifeClient lifeClient) {
    this.lifeClient = lifeClient;
  }

  public static void main(String[] args) {
    applicationContext = new SpringApplicationBuilder()
        .main(TestClientApplication.class)
        .sources(TestClientApplication.class)
        .run(args);
  }

//  @Bean
  public ApplicationRunner getRunnerXXX() throws Exception {
    final Publisher<Cell> source = lifeClient.allGenerations();
    final String elementType = "Cell";
    return createRunner(source, elementType);
  }

  @Bean
  public ApplicationRunner getRunner() throws Exception {
    final Publisher<Empty> source = lifeClient.empties();
    final String elementType = "Element";
    return createRunner(source, elementType);
  }

  private <E> ApplicationRunner createRunner(final Publisher<E> source, final String elementType) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();
      final int demand = 1_000_000;
      Flux.from(source)
          .subscribeOn(Schedulers.elastic())
          .limitRequest(demand)
          .doOnNext(
              _ignored -> {
                if (firstElementReceived.get() == 0) {
                  firstElementReceived.set(System.nanoTime());
                }
                n.increment();
              })
          .doOnComplete(() -> {
            final long ending = System.nanoTime();
            final long waitForFirstElement = firstElementReceived.get() - starting;
            final long totalElapsed = ending - starting;
            System.out
                .println(String.format("waited %.2f seconds for first element\naveraged %.0f %s elements per second",
                    waitForFirstElement / 1_000_000_000.0,
                    n.longValue() * 1.0 / totalElapsed * 1_000_000_000,
                    elementType));
          })
          .doOnCancel(() -> System.out.println("canceled"))
          .doOnError((err)-> System.out.println("error: " + err))
          .doFinally((_ignored) ->applicationContext.close())
          .subscribe();
    };
  }

}