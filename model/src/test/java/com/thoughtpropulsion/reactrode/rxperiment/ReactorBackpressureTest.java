package com.thoughtpropulsion.reactrode.rxperiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

/**
 * A test that demonstrates how a {@link BaseSubscriber} provided to
 * {@link Flux#subscribe(Subscriber)} can pause and then restart the
 * message flow from the {@link Flux} (a {@link Publisher}).
 */
class ReactorBackpressureTest {

  private static final int CAPACITY = 5;

  private Queue<Integer> storage;
  private Subscription subscriptionToFlux;

  @BeforeAll
  static void beforeAll() { ReactorDebugAgent.init();}

  @BeforeEach
  void beforeEach() {
    storage = new ConcurrentLinkedQueue<Integer>();
  }

  /**
   * Consume {@param intFlux} asynchronously. Delegate to {@link #calculateRequest()} to
   * determine {@link Subscription#request(long)} count (backpressure). This can causes
   * processing to completely stop when {@code storage.size()} grows larger than {#code CAPACITY}.
   * Processing is resumed later by (externally) reducing {@code storage.size()}
   * e.g. via {@code storage.remove()} and then calling {@link Subscription#request(long)}.
   *
   * @param intFlux
   */
  private void f(final Flux<Integer> intFlux, final IntSupplier calculateRequest) {
    intFlux.subscribeOn(Schedulers.parallel()).subscribe(new BaseSubscriber<Integer>() {
      @Override
      protected void hookOnSubscribe(final Subscription subscription) {
        subscriptionToFlux = subscription;
        request(calculateRequest.getAsInt());
      }
      @Override
      protected void hookOnNext(final Integer i) {
        storage.add(i);
        request(calculateRequest.getAsInt());
      }
    });
  }

  /**
   * Notice that the code in the body of g() is identical to the code in the body of f().
   * If Java had union types (the dual to intersection types) we could declare a
   * generic h() with signature:
   *
   * {@code private <T, M extends Mono<T>|Flux<T> > void h(final M intMonoid) {...} }
   *
   * Alas Java does not offer union types so we have to define this redundant fn g().
   *
   * Also, alas: I don't see how to "connect" this fn g() to a flux. I see that e.g. in
   * Spring Reactive Repositories https://spring.io/blog/2016/11/28/going-reactive-with-spring-data
   *
   * "in contrast to the traditional repository interfaces, a reactive repository uses
   * reactive types as return types and can do so for parameter types, too"
   *
   * But when a method takes a mono (not a flux), I don't see how to give that method
   * control over backpressure. {@link Flux#flatMap(Function)} and {@link Flux#concatMap(Function)}
   * seem close to what I need. But those don't expose the publisher (mono) to the function.
   *
   * The only alterative I see is that each mono-taking fn:
   *
   * 1. cannot control backpressure
   * 2. has a "twin function" that takes a flux and can control backpressure
   *
   * That way if a caller has a flux, the caller can use (2).
   *
   * This seems to break symmetry. In a real sense a mono _is_ a flux--just a flux that is limited
   * to carry only one value. If a mono is a flux, then why wouldn't it fully interoperate?
   *
   * I wonder if I could subscribe to the first element of the flux and then
   * {@link Flux#delaySubscription(Publisher)} on the next one in turn. Or maybe
   * use {@link Flux#delayUntil(Function)} somehow.
   *
   * @param intMono
   */
  private void g(final Mono<Integer> intMono) {
    intMono.subscribeOn(Schedulers.parallel()).subscribe(new BaseSubscriber<Integer>() {
      @Override
      protected void hookOnSubscribe(final Subscription subscription) {
        subscriptionToFlux = subscription;
        request(calculateRequest());
      }
      @Override
      protected void hookOnNext(final Integer i) {
        storage.add(i);
        request(calculateRequest());
      }
    });
  }

  private int calculateRequest() {
    if (storage.size() < CAPACITY)
      return 1;
    else
      return 0;
  }

  @Test
  void processFluxWithCustomBackpressure() {

    final AtomicInteger numberProcessed = new AtomicInteger();

    final IntConsumer waitUntilProcessed = (final int n) -> {
      while (numberProcessed.get() < n)
        try {
          Thread.sleep(10);
        } catch (final InterruptedException e) {
          // ignore
        }
    };

    final BiConsumer<Integer,Integer> expect = (expectStored, expectProcessed) -> {
      assertThat(storage.size()).isEqualTo(expectStored);
      assertThat(numberProcessed.get()).isEqualTo(expectProcessed);
    };

//    final IntSupplier calculateRequest = () -> returning(calculateRequest(),
//        request -> System.out.println("requesting: " + request));

    final int MAX = 10;

    f(
        Flux
            .range(0, MAX)
            .doOnNext(i -> numberProcessed.incrementAndGet())
//            .doOnNext(i -> System.out.println("processed one"))
//            .doOnComplete(() -> System.out.println("Flux complete!"))
        ,

        this::calculateRequest /*calculateRequest*/);

    waitUntilProcessed.accept(CAPACITY);

    expect.accept(CAPACITY,CAPACITY);

    storage.clear();

    expect.accept(0,CAPACITY);

    subscriptionToFlux.request(calculateRequest() /*calculateRequest.getAsInt()*/);

    waitUntilProcessed.accept(MAX);

    expect.accept(CAPACITY,MAX);
  }

  @Test
  void processFluxViaMonoFunctionWithCustomBackpressure() {
//    final AtomicInteger numberProcessed = new AtomicInteger();

    /*
     I see no flux operator that takes a fn that consumes a _mono_. e.g. map() and flatMap()
     consume T (Integer in this case), not Mono<T>.

     I need an operator that links the flux to the fn consuming the mono so that the fn can
     gain control of backpressure.
     */

//    f(Flux.range(0,10).doOnNext(i -> numberProcessed.incrementAndGet()));
//
//    while(numberProcessed.get() < CAPACITY)
//      Thread.sleep(10);
//    for(int i = 0; i < CAPACITY; i++)
//      storage.remove();
//    subscriptionToFlux.request(1); // restart processing by making some space
//    while(numberProcessed.get() < 10)
//      Thread.sleep(10);
//    assertThat(numberProcessed.get()).isEqualTo(10);
  }

  /**
   * This is the retrograde case of processing a mono directly (via a fn that consumes a mono,
   * taking control of backpressure). Not all that interesting or useful but it shows that
   * g() kind of works in one sense.
   *
   * @throws InterruptedException
   */
  @Test
  void processMonoViaMonoFunctionWithCustomBackpressure() throws InterruptedException {
    final AtomicInteger numberProcessed = new AtomicInteger();

    g(Mono.just(0).doOnNext(i -> numberProcessed.incrementAndGet()));

    while(numberProcessed.get() < 1)
      Thread.sleep(10);
//    for(int i = 0; i < CAPACITY; i++)
//      storage.remove();
//    subscriptionToFlux.request(1); // restart processing by making some space
//    while(numberProcessed.get() < 10)
//      Thread.sleep(10);
    assertThat(numberProcessed.get()).isEqualTo(1);
  }
}
