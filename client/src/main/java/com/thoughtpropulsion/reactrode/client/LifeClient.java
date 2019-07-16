package com.thoughtpropulsion.reactrode.client;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.reactivestreams.Publisher;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

@Component
public class LifeClient {
  private final RSocketRequester rSocketRequester;

  public LifeClient(final RSocketRequester rSocketRequester) {
    this.rSocketRequester = rSocketRequester;
  }

  public Publisher<Cell> allGenerations() {
    return rSocketRequester
        .route("allGenerations")
        .data("ignore me")
        .retrieveFlux(Cell.class);
  }

  public Publisher<String> aMono() {
    return rSocketRequester
        .route("aMono")
        .data("stuff")
        .retrieveMono(String.class);
  }
}
