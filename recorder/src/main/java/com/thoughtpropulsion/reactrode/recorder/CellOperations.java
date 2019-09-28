package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.GameOfLife.enforceGenerationFraming;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import org.reactivestreams.Publisher;
import org.springframework.data.gemfire.GemfireTemplate;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;

public class CellOperations {

  static final int PRIMORDIAL_GENERATION = -1;
  static final CoordinateSystem coordinateSystem = new CoordinateSystem(100, 100);
  static final int LIMIT_REQUEST = 100 * coordinateSystem.size();

  static final Runnable PAUSE_MITIGATION = () -> {
    try {
      Thread.sleep(10_000);
    } catch (InterruptedException _ignored) {
    }
  };

  private CellOperations() {}

  // a higher-order function that takes a consumer op function and wraps it in retrying
  static <ELEMENT> Consumer<ELEMENT> createRetryConsumer(final Consumer<ELEMENT> operation,
                                                         final Runnable mitigation) {
    return (final ELEMENT e) -> {
      int attempt = 1;
      while (true) {
        try {
          operation.accept(e);
          return;
        } catch (final Throwable ex) {
          if (attempt >= 3) {
            throw ex;
          } else {
            System.out.println(String.format(
                "running mitigation after attempt %d, because of exception:",
                attempt, ex));
            ex.printStackTrace(System.out);
            ++attempt;
          }
        }
        mitigation.run();
      }
    };
  }

  static Consumer<Cell> createPutCellFunction(final Region<Integer,Cell> cellsRegion) {
    return (final Cell cell) -> cellsRegion.put(coordinateSystem.toOffset(cell.coordinates), cell);
  }

  private static Publisher<Cell> createSerialPutPublisher(final GemfireTemplate template,
                                                          final CoordinateSystem coordinateSystem,
                                                          final Publisher<Cell> source) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    return Flux.from(source)
        .limitRequest(LIMIT_REQUEST)
        .doOnNext(
            createSingleCellConsumer(template, coordinateSystem, n, firstElementReceived))
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived));
  }

  private static Publisher<Cell> createParallelPutPublisher(
      final GemfireTemplate template, final CoordinateSystem coordinateSystem,
      final Publisher<Cell> source, final int parallelism) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    return Flux.from(source)
        .limitRequest(LIMIT_REQUEST)
        .parallel(parallelism)
        .runOn(Schedulers.elastic())
        .doOnNext(
            createSingleCellConsumer(template, coordinateSystem, n, firstElementReceived))
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
        .sequential();
  }

  static Publisher<List<Cell>> createSerialBulkPutPublisher(
      final GemfireTemplate template, final CoordinateSystem coordinateSystem,
      final Publisher<Cell> source, final int generations) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    /*
    TODO: it would be nice to factor the retying (and mitigation) up out of this method)
          so it could be applied to all the other createXXXPublisher methods
     */
    final Consumer<Collection<Cell>>
        bulkCellConsumer =
        createRetryConsumer(
            createBulkCellConsumer(template, coordinateSystem, n, firstElementReceived),
            PAUSE_MITIGATION
//            createDestroyLRUCellsMitigation(cellGemfireTemplate, coordinateSystem)
            );

    return Flux.from(source)
        .subscribeOn(Schedulers.elastic()) // since we'll do blocking ops downstream
        .limitRequest(generations * coordinateSystem.size())
        .buffer(coordinateSystem.size())
        .doOnNext(
            bulkCellConsumer)
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived));
  }

  private static Publisher<List<Cell>> createParallelBulkPutPublisher(
      final GemfireTemplate template,
      final CoordinateSystem coordinateSystem,
      final Publisher<Cell> source,
      final int parallelism) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    return enforceGenerationFraming(
        Flux.from(source)
            .limitRequest(LIMIT_REQUEST)
            .buffer(coordinateSystem.size()),
        coordinateSystem)

        .parallel(parallelism)
        .runOn(Schedulers.elastic())
        .doOnNext(
            createBulkCellConsumer(template, coordinateSystem, n, firstElementReceived))
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
        .sequential();
  }

  private static Consumer<Cell> createSingleCellConsumer(final GemfireTemplate template,
                                                         final CoordinateSystem coordinateSystem,
                                                         final LongAdder n,
                                                         final AtomicLong firstElementReceived) {
    return cell -> {
      if (firstElementReceived.get() == 0) {
        firstElementReceived.set(System.nanoTime());
      }
      n.increment();
      try {
        template.put(coordinateSystem.toOffset(cell.coordinates), cell);
      } catch (final Exception e) {
        System.out.println("for cell" + cell);
        e.printStackTrace();
        throw e;
      }
    };
  }

  private static Consumer<Collection<Cell>> createBulkCellConsumer(final GemfireTemplate template,
                                                                   final CoordinateSystem coordinateSystem,
                                                                   final LongAdder n,
                                                                   final AtomicLong firstElementReceived) {
    return cells -> {
      if (firstElementReceived.get() == 0)
        firstElementReceived.set(System.nanoTime());
      try {
        final Map<Integer, Cell> entries = cells.stream().map(cell -> {
          final int key = coordinateSystem.toOffset(cell.coordinates);
          return new Pair<>(key, cell);
        }).collect(toLinkedMap(pair -> pair.k, pair -> pair.v));
        n.add(entries.size());
        template.putAll(entries);
      } catch (final Exception e) {
        e.printStackTrace();
        throw e;
      }
    };
  }

  /*
   Collect to a LinkedHashMap so that iteration order will be the same as insertion order.
   */
  public static <T, K, U> Collector<T, ?, Map<K,U>> toLinkedMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper)
  {
    return Collectors.toMap(
        keyMapper,
        valueMapper,
        (u, v) -> {
          throw new IllegalStateException(String.format("Duplicate key %s", u));
        },
        LinkedHashMap::new
    );
  }

  private static Runnable summarizePerformance(final LongAdder n, final long starting,
                                               final AtomicLong firstElementReceived) {
    return () -> {
      final long ending = System.nanoTime();
      final long waitForFirstElement = firstElementReceived.get() - starting;
      final long totalElapsed = ending - starting;
      System.out
          .println(String.format("waited %.2f seconds for first element\naveraged %.0f %s elements per second",
              waitForFirstElement / 1_000_000_000.0,
              n.longValue() * 1.0 / totalElapsed * 1_000_000_000,
              "Cell"));
//      applicationContext.close();
    };
  }

  static class Pair<K,V> {
    public final K k;
    public final V v;

    public Pair(final K k, final V v) {
      this.k = k; this.v = v;
    }
  }

  static Runnable createDestroyLRUCellsMitigation(
      final GemfireTemplate template,
      final CoordinateSystem coordinateSystem) {
    return () -> {

      try {
        final Region<Integer,Cell> region = template.getRegion();
        final int GENERATIONS_TO_DESTROY = 10;
        final SelectResults<Integer> lruKeys = region.query(
            String.format(
                "SELECT key "
                    + "FROM /Cells.entrySet cell, (SELECT MIN(cell.coordinates.generation) FROM /Cells cell) oldestGeneration"
                    + "WHERE cell.value.coordinates.generation >= oldestGeneration "
                    + "AND (cell.value.coordinates.generation < oldestGeneration + %d)",
                GENERATIONS_TO_DESTROY));

        region.removeAll(lruKeys);

        // Now wait a while for GC to recover space we just freed up
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
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
    };
  }

}
