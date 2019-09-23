package com.thoughtpropulsion.reactrode.recorder.server;

import static org.apache.geode.cache.Operation.CREATE;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.CqAttributes;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqException;
import org.apache.geode.cache.query.CqListener;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.RegionNotFoundException;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;
import org.apache.geode.cache.util.CqListenerAdapter;

@Controller
public class RecorderServer {

  private final GemFireCache gemFireCache;
  private final Region<Long, Cell> cellsRegion;

  /*
   Elide the oldest generation in hopes of avoiding an incomplete generation, which would
   throw off the framing of our subscribers.
   */
  private static final String CellsStartingWithOldestCompleteGenerationQuery = String.format(
      "SELECT cell "
          + "FROM /Cells cell, (SELECT (MIN(cell.coordinates.generation) + 1) FROM /Cells cell) oldestGeneration"
          + "WHERE cell.value.coordinates.generation >= oldestGeneration");

  public RecorderServer(final GemFireCache gemFireCache,
                        @Qualifier("Cells") final Region<Long, Cell> cellsRegion) {
    this.cellsRegion = cellsRegion;
    this.gemFireCache = gemFireCache;
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
    return getCellPublisher("SELECT * from /Cells");
  }

  @MessageMapping("/rsocket/all-generationsXXXX")
  public Publisher<Cell> allCurrentGenerationsStartingFrom(final Coordinates _ignored) {
    // TODO: respect backpressure from downstream
    return Flux.push(sink -> {

          try {
            final SelectResults<Cell>
                results =
                cellsRegion.query(CellsStartingWithOldestCompleteGenerationQuery);
            for(Cell cell:results) {
              sink.next(cell);
            }
          } catch (FunctionDomainException e) {
            e.printStackTrace();
          } catch (TypeMismatchException e) {
            e.printStackTrace();
          } catch (NameResolutionException e) {
            e.printStackTrace();
          } catch (QueryInvocationTargetException e) {
            e.printStackTrace();
          }

        },
        // BUFFERING == DANGER
        FluxSink.OverflowStrategy.BUFFER);
  }

    private Publisher<Cell> getCellPublisher(final String query) {
  /*
   Push from a single thread.
   TODO: respect backpressure signal from downstream! (can't use push() or create() for that)

   Also would be interesting to process initial result set here but apparently, Spring Data GemFire
   doesn't support the initial result set yet:

   https://jira.spring.io/browse/SGF-65
   */
      return Flux.push(sink ->
          {
            try {
              gemFireCache.getQueryService().newCq(query, createCqAttributes(sink)).execute();
            } catch (final CqException | RegionNotFoundException e) {
              sink.error(e);
            }
          },
          // BUFFERING == DANGER
          FluxSink.OverflowStrategy.BUFFER);
    }

  private static CqAttributes createCqAttributes(final FluxSink<Cell> sink) {
    return new CqAttributes() {

      private final CqListener[] cqListeners = new CqListener[]{createCqListenerAdapter(sink)};

      @Override
      public CqListener[] getCqListeners() {
        return cqListeners;
      }

      @Override
      public CqListener getCqListener() {
        return getCqListeners()[0];
      }
    };

  }

  private static CqListenerAdapter createCqListenerAdapter(final FluxSink<Cell> sink) {

    return new CqListenerAdapter() {
      @Override
      public void onEvent(final CqEvent cqEvent) {
        if (cqEvent.getBaseOperation() == CREATE) {
          sink.next((Cell) cqEvent.getNewValue());
        }
      }
    };

  }

}
