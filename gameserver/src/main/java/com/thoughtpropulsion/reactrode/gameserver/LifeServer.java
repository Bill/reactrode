package com.thoughtpropulsion.reactrode.gameserver;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import com.thoughtpropulsion.reactrode.model.Empty;
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

  @MessageMapping("/rsocket/all-generations")
  public Publisher<Cell> allGenerations(final Coordinates _ignored) {
    return Flux.from(allGenerations);
  }

  @MessageMapping("/rsocket/empties")
  public Publisher<Empty> empties(final Empty _ignored) {
    return Flux.generate(sink->{
      sink.next(Empty.create());
    });
  }

}