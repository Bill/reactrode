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

  Mono<Boolean> get(final Mono<Coordinate> key);

  /**
   *
   * @param generation the starting generation
   * @return the sequence of changes (@{link Cell}s) starting with {@param generation}. If you keep
   * consuming you'll see changes for successive generations.
   */
  Flux<Cell> changes(Mono<Integer> generation);
}
