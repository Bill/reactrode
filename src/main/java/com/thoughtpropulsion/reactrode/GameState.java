package com.thoughtpropulsion.reactrode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GameState {

  /**
   * Sort of like {@link java.util.Map#put(Object, Object)} but {@param cellMono} has both
   * the ({@link Coordinate}) key and the ({@link Boolean}) value.
   * Unlike {@link java.util.Map#put(Object, Object)} this method returns nothing so it's
   * "fire-and-forget" in reactive parlance.
   *
   * @param cellMono
   */
  void put(final Mono<Cell> cellMono);

  /**
   * Assess liveness status at a coordinate. Unlike a {@code Map<K,Boolean>.get()} call,
   * the {@Boolean} produced by this {@link Mono} will never be {@code null}.
   * @param key
   * @return
   */
  Mono<Boolean> get(final Mono<Coordinate> key);

  /**
   *
   * @param generation the generation of interest
   * @return the "hot" sequence of changes (@{link Cell}s) for {@param generation}.
   */
  Flux<Cell> changes(Mono<Integer> generation);
}
