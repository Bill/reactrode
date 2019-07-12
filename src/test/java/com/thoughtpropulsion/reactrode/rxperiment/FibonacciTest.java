package com.thoughtpropulsion.reactrode.rxperiment;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Processor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.test.StepVerifier;

public class FibonacciTest {

  @Test
  void fibTest() {

    // Gotta use a processor somewhere in feedback loop
    final Processor<Integer,Integer> feedback =
        UnicastProcessor.create();

    final Flux<Integer> nextFibonacci = Flux.from(feedback)
        .buffer(2, 1)
        .flatMap(pair -> Mono.just(pair.get(0) + pair.get(1)))
        .doOnNext(i -> System.out.println("generated: " + i));

    final Flux<Integer> allValues = Flux
        .concat(Flux.just(1, 2), nextFibonacci)
        .publish()
        .refCount(2);

        /*
         Without a limitRequest() (or maybe take()), nextFibonacci keeps
         generating values ahead of the StepVerifier's demand, and the
         StepVerifier starves---I don't think it ever sees any values.

         It's puzzling because I thought the ConnectableFlux produced by
         publish() would throttle down the upstream demand to that of its
         slowest subscriber, which in this case should be the StepVerifier.
         */

        //.limitRequest(6);

    allValues.subscribe(feedback);

    StepVerifier.create(allValues)
        .expectNext(1,2,3,5,8,13)
        .expectComplete()
        .verify();
  }
}
