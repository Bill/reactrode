package com.thoughtpropulsion.reactrode.recorder;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import org.reactivestreams.Publisher;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

@Component
public class RecordingSubscriber {
  private final RSocketRequester rSocketRequester;

  public RecordingSubscriber(final RSocketRequester rSocketRequester) {
    this.rSocketRequester = rSocketRequester;
  }

  public Publisher<Cell> allGenerations() {
    return rSocketRequester
        .route("/rsocket/all-generations")
        /*
         TODO: figure out how to retrieve a flux providing no parameters at all
         TODO: figure out why, if I have to provide a parameter, I can't send a String
         */
        .data(Coordinates.create(0,0,0))
        .retrieveFlux(Cell.class);
  }

}
