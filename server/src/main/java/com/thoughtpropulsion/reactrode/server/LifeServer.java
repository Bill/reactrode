package com.thoughtpropulsion.reactrode.server;

import java.time.Instant;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.reactivestreams.Publisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class LifeServer {

  private final Publisher<Cell> allGenerations;

  public LifeServer(final Publisher<Cell> allGenerations) {
    this.allGenerations = allGenerations;
  }

  @MessageMapping("allGenerations")
  // TODO: see if I can return Publisher<Cell> instead
  public Flux<Cell> allGenerations(final String _ignored) {
    return Flux.from(allGenerations);
  }

  @MessageMapping("a-string-mono")
  public Mono<String> aStringMono(final String ignored) {
    return Mono.just("howdy!");
  }

  @MessageMapping("greet")
  Mono<GreetingsResponse> greet(final GreetingsRequest request) {
    return Mono.just(
        new GreetingsResponse("Hello " + request.getName() + " @ " + Instant.now()));
  }

}