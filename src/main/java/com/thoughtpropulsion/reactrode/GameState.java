package com.thoughtpropulsion.reactrode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GameState {

  /**
   * Sort of like {@link java.util.Map#put(Object, Object)} but {@param cellsFlux} produces
   * {@link Cell}s that have both the ({@link Coordinate}) key and the ({@link Boolean}) value.
   *
   * @param cellMono
   * @return
   */

  /*
  if I want to backpressure this put, assuming the caller sent Mono.<Cell>just(cell), then
  I need to delay responding (from the previous put())
   */
  // this is a convenience argument because flatmap t -> asynctype<v>:  cell -> mono<boolean>
  Mono<Boolean> put(final Mono<Cell> cellMono);

//  {
//    // can be a Mono once putAll accepts a Publisher
//    return putAll(Mono.just(cell));
//  }
  //Mono<Boolean> put(final Cell cellMono);

  // could return Mono<Void> to signal done, or Flux<Void> to signal progress or Flux<Boolean>
  // to match put() above. Maybe Flux<Tuple2<Cell,Boolean>> to allow out-of-order completion
  void putAll(final Flux<Cell> cellFlux);
  // dig it: be lenient in what we accept (when returning, be specific)
  //void putAll(final Publisher<Cell> cellFlux);

  /**
   * Assess liveness status at a coordinate. Unlike a {@code Map<K,Boolean>.get()} call,
   * the {@Boolean} produced by this {@link Mono} will never be {@code null}.
   * @param key
   * @return
   */
  Mono<Boolean> get(final Mono<Coordinate> key);
  //Mono<Boolean> get(final Coordinate key);

  /**
   *
   * @param generation the generation of interest
   * @return the "hot" sequence of changes (@{link Cell}s) for {@param generation}.
   */
  Flux<Cell> changes(Mono<Integer> generation);
  //Flux<Cell> changes(Integer generation);
}
