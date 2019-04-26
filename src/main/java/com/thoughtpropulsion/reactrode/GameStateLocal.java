package com.thoughtpropulsion.reactrode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GameStateLocal implements GameState {

  @Override
  public void put(final Mono<Cell> cellMono) {
  }

  @Override
  public Mono<Boolean> get(final Mono<Coordinate> key) {
    return null;
  }

  @Override
  public Flux<Cell> changes(final Mono<Integer> generation) {
    return null;
  }
}
