package com.thoughtpropulsion.reactrode.recorder.server;

import static org.apache.geode.cache.Operation.CREATE;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
import org.apache.geode.cache.query.Struct;
import org.apache.geode.cache.query.TypeMismatchException;
import org.apache.geode.cache.util.CqListenerAdapter;

@Controller
public class RecorderServer {

  private final GemFireCache gemFireCache;
  private final Region<Long, Cell> cellsRegion;
  private final GemfireTemplate cellsGemfireTemplate;

  /*
   Elide the oldest generation in hopes of avoiding an incomplete generation, which would
   throw off the framing of our subscribers.
   */
  private static final String CellsStartingWithOldestCompleteGenerationQuery = String.format(
      "SELECT cell "
          + "FROM /Cells cell, (SELECT (MIN(cell.coordinates.generation) + 1) FROM /Cells cell) oldestGeneration"
          + "WHERE cell.value.coordinates.generation >= oldestGeneration");

  public RecorderServer(final GemFireCache gemFireCache,
                        @Qualifier("Cells") final Region<Long, Cell> cellsRegion,
                        @Qualifier("CellsTemplate") final GemfireTemplate cellsGemfireTemplate) {
    this.cellsRegion = cellsRegion;
    this.gemFireCache = gemFireCache;
    this.cellsGemfireTemplate = cellsGemfireTemplate;
  }

  @MessageMapping("/rsocket/all-generations")
  public Publisher<Cell> allGenerations(final Coordinates _ignored) {
    return getCellPublisherNewCellsOnly();
  }

  @MessageMapping("/rsocket/generation")
  public Publisher<Cell> generation(final Coordinates coordinates) {
    throw new NotImplementedException();
  }

  /**
   * The flux of cells starting with a particular generation.
   *
   * @param coordinates contains the generation of interest
   * @return
   */
  @MessageMapping("/rsocket/all-generations-starting-from")
  public Publisher<Cell> allGenerationsStartingFrom(final Coordinates coordinates) {
    throw new NotImplementedException();
  }

  /*
  This publisher is a "hot" flux of Cells. It produces only newly-arrived Cells.
  If no cells are arriving, none will be published.

  TODO: make this publisher respect backpressure signal from downstream!
  */
  private Publisher<Cell> getCellPublisherNewCellsOnly() {

    return Flux.push(sink ->
        {
          try {
            gemFireCache.getQueryService().newCq("SELECT * FROM /Cells",
                createCqAttributes(sink)).execute();
          } catch (final CqException | RegionNotFoundException e) {
            sink.error(e);
          }
        },
        // BUFFERING == DANGER
        FluxSink.OverflowStrategy.BUFFER);
  }

  /*
   This publisher approximates a cold-ish flux of Cells.

   This implementation has a number of shortcomings:
   1.
   2.
   */
  private Publisher<Cell> getCellPublisherInitialAndNewCells() {

  return Flux.push(sink ->
        {
          try {

            /*
              I toyed with using CQ executeWithInitialResults(). But I needed (wanted)
              the results in key-order. To order by key, key must appear in projection.

              But per: java.lang.UnsupportedOperationException:
              "CQ queries must have a region path only as the first iterator in the FROM clause"

              This means we can't ORDER BY key in a CQ query!

              There is another issue: the initial result set from
              executeWithInitialResults() is not distinct from the results delivered to the
              CQ event handler. That means that it's up to the application (outside Geode)
              to take care of any necessary de-duplication.

              So I need two separate queries: one for the existing entries (with an ORDER BY clause)
              and a second (without an ORDER BY clause) for the CQ. Because I need two different
              queries, I'm not using executeWithInitialResults(). Instead I explicitly query
              for the initial results, and then set up a CQ for the rest.

              And regardless of whether I use executeWithInitialResults() on the CQ or
              query() on the region/template---neither of those _streams_ their results.
              The results are moved as a block, so memory consumption is bounded only by the
              volume of the whole result set. If the region was 100MB in size, we might need
              an additional 100MB on the server to queue this result and 100MB on the client
              (all at once) to see the results.
             */

            final SelectResults<Struct> results =
                cellsGemfireTemplate.query("SELECT key,value from /Cells.entries ORDER BY key");

            System.out.println(String.format("got %d cells in initial query", results.size()));

            for (final Struct s : results) {
              sink.next((Cell)s.get("value"));
            }

            /*
             TODO: we miss some events because the CQ is set up after the query. A better approach
             would be to set up the CQ, blocked on e.g. a latch, before the query. After the query
             completes we release the latch. In the CQ event listener, we'd have to elide any events
             that had already been sent.
             */

            /*
             Spring Data GemFire, via the ContinuousQueryListenerContainer, doesn't support the
             initial result set yet:

             https://jira.spring.io/browse/SGF-65

             Also, C.Q.L.C. has an Executor we don't really need for this prototype, so we're using
             the plain old query service directly from Geode.
             */

            gemFireCache.getQueryService().newCq("SELECT * FROM /Cells", createCqAttributes(sink)).execute();

          } catch (final CqException  | NameResolutionException e) {
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
