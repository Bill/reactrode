package com.thoughtpropulsion.reactrode.recorder.server;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class RecorderServer {

  @MessageMapping("/rsocket/generation")
  public Flux<Cell> generation(final Coordinates coordinates) {
    return Flux.just(Cell.createAlive(Coordinates.create(0,0,coordinates.generation),true));
  }

  @MessageMapping("/rsocket/all-generations-starting-from")
  public Flux<Cell> allGenerationsStartingFrom(final Coordinates coordinates) {
    return Flux.just(Cell.createAlive(Coordinates.create(0,0,coordinates.generation),false));
  }

}
