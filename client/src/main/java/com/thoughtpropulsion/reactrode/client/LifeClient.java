package com.thoughtpropulsion.reactrode.client;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.reactivestreams.Publisher;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

@Component
public class LifeClient {
  private final RSocketRequester rSocketRequester;

  public LifeClient(final RSocketRequester rSocketRequester) {
    this.rSocketRequester = rSocketRequester;
  }

  public Publisher<Cell> allGenerations() {
    return rSocketRequester
        .route("allGenerations")
        .data(new GreetingsRequest("ignore me"))
        .retrieveFlux(Cell.class);
  }

  public Publisher<String> aStringMono() {
    return rSocketRequester
        .route("a-string-mono")
        .data("stuff")
        .retrieveMono(String.class);
  }

  public Publisher<GreetingsResponse> greet(final String name) {
    return rSocketRequester
        .route("greet")
        .data(new GreetingsRequest(name))
        .retrieveMono(GreetingsResponse.class);
  }

  public Publisher<GreetingsResponse> greetStream(@PathVariable String name) {
    return rSocketRequester
        .route("greet-stream")
        .data(new GreetingsRequest(name))
        .retrieveFlux(GreetingsResponse.class);
  }

}
