package com.thoughtpropulsion.reactrode.rxperiment;

import static com.thoughtpropulsion.reactrode.Functional.returning;

import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class FibonacciTestInternalFeedback {
  @Test
  void fibTest() {

    final Flux<Integer> initialSequence = Flux.just(0, 1);

    final Flux<Integer> fibonacci = initialSequence
        .buffer(2)
        .flatMap(initialList ->
            Flux.generate(
                () -> new Tuple2<>(initialList.get(0), initialList.get(1)),
                (oldPair, sink) ->
                    returning(new Tuple2<>(oldPair._2, oldPair._1 + oldPair._2),
                        newPair -> {
                          sink.next(newPair._2);
                          System.out.println("generating: " + newPair._2);
                        })));

    final Flux<Integer> fibonacciFromZero = Flux.concat(initialSequence, fibonacci);

    StepVerifier.create(fibonacciFromZero.take(6))
        .expectNext(0, 1, 1, 2, 3, 5)
        .expectComplete()
        .verify();
  }
}
