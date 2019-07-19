package com.thoughtpropulsion.reactrode.server;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import org.reactivestreams.Publisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class LifeServer {

  private final Publisher<Cell> allGenerations;

  public LifeServer(final Publisher<Cell> allGenerations) {
    this.allGenerations = allGenerations;
  }

  @MessageMapping("allGenerations")
  // TODO: see if I can return Publisher<Cell> instead
  public Flux<Cell> allGenerations(final Coordinates _ignored) {
    return Flux.from(allGenerations);
  }

}