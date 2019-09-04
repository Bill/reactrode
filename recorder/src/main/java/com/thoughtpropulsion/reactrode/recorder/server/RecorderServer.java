package com.thoughtpropulsion.reactrode.recorder.server;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import org.reactivestreams.Publisher;
import org.springframework.data.gemfire.listener.ContinuousQueryDefinition;
import org.springframework.data.gemfire.listener.ContinuousQueryListener;
import org.springframework.data.gemfire.listener.ContinuousQueryListenerContainer;
import org.springframework.data.gemfire.listener.annotation.ContinuousQuery;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import org.apache.geode.cache.query.CqEvent;

@Controller
public class RecorderServer {

  private ContinuousQueryListenerContainer cqlc;

  public RecorderServer(final ContinuousQueryListenerContainer cqlc) {
    this.cqlc = cqlc;
  }

  @MessageMapping("/rsocket/generation")
  public Publisher<Cell> generation(final Coordinates coordinates) {
    return Flux.just(Cell.createAlive(Coordinates.create(0,0,coordinates.generation),true));
  }

  /**
   * The flux of cells starting with a particular generation.
   *
   * @param coordinates contains the generation of interest
   * @return
   */
  @MessageMapping("/rsocket/all-generations-starting-from")
  public Publisher<Cell> allGenerationsStartingFrom(final Coordinates coordinates) {

    final String
        query =
        String.format("select * from /Cells cell where cell.coordinates.generation >= %d",
            coordinates.generation);
    return getCellPublisher(query);

  }

  @MessageMapping("/rsocket/all-generations")
  public Publisher<Cell> allGenerations(final Coordinates _ignored) {
    return getCellPublisher("select * from /Cells");
  }

  private Publisher<Cell> getCellPublisher(final String query) {
  /*
   Push from a single thread.
   TODO: respect backpressure signal from downstream! (can't use push() or create() for that)
   */
    return Flux.push(sink -> {
          cqlc.addListener(
              new ContinuousQueryDefinition(
                  query,
                  cqEvent -> {
                    System.out.println("GOT SHTUFFFFFF!!!! " + cqEvent.getNewValue());
                    sink.next((Cell) cqEvent.getNewValue());
                    },
                  false));
          },
        // TODO: don't buffer!!!!!!
        FluxSink.OverflowStrategy.BUFFER);
//    return Flux.just(Cell.createAlive(Coordinates.create(0,0,coordinates.generation),false));
  }
}
