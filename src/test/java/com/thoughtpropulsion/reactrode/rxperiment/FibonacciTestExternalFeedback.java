package com.thoughtpropulsion.reactrode.rxperiment;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Processor;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.test.StepVerifier;

public class FibonacciTestExternalFeedback {

  @Test
  void fibTest() {

    // Gotta use a processor somewhere in feedback loop
    final Processor<Integer,Integer> feedback =
        UnicastProcessor.create();

    final Flux<Integer> nextFibonacci = Flux.from(feedback)
        .buffer(2, 1)
        .flatMap(pair -> Mono.just(pair.get(0) + pair.get(1)))
        .doOnNext(i -> System.out.println("generated: " + i));

    final AtomicReference<Disposable> stop = new AtomicReference<>();

    final Flux<Integer> allValues = Flux
        .concat(Flux.just(0, 1), nextFibonacci)
        .publish()

        /*
         refCount(2) isn't what we want because it doesn't disconnect when
         count drops below 2---it disconnects when count drops below zero
         */
//        .refCount(2)

        /*
         Without a limitRequest() (or maybe take()), nextFibonacci keeps
         generating values after the StepVerifier finishes.

         It's puzzling because I thought the ConnectableFlux produced by
         publish() would throttle down the upstream demand to that of its
         slowest subscriber, which in this case should be the StepVerifier.
         */

        //.limitRequest(6);

        // _this_ is what we need---capture the Disposable for later
        .autoConnect(2,stop::set);

    allValues.subscribe(feedback);

    StepVerifier.create(allValues.take(6))
        .expectNext(0,1,1,2,3,6)
        .expectComplete()
        .verify();

    // ...and stop the flow after the test is done!
    stop.get().dispose();
  }
}
