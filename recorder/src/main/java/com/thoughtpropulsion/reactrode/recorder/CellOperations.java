package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.GameOfLife.enforceGenerationFraming;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.recorder.gemfireTemplate.CellGemfireTemplate;
import org.reactivestreams.Publisher;
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
  static final int LIMIT_REQUEST = 3_000_000;

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

  private static Publisher<Cell> createSerialPutPublisher(final CellGemfireTemplate cellGemfireTemplate,
                                                     final CoordinateSystem coordinateSystem,
                                                     final Publisher<Cell> source) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    return Flux.from(source)
        .limitRequest(LIMIT_REQUEST)
        .doOnNext(
            createSingleCellConsumer(cellGemfireTemplate, coordinateSystem, n, firstElementReceived))
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived));
  }

  private static Publisher<Cell> createParallelPutPublisher(
      final CellGemfireTemplate cellGemfireTemplate, final CoordinateSystem coordinateSystem,
      final Publisher<Cell> source, final int parallelism) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    return Flux.from(source)
        .limitRequest(LIMIT_REQUEST)
        .parallel(parallelism)
        .runOn(Schedulers.elastic())
        .doOnNext(
            createSingleCellConsumer(cellGemfireTemplate, coordinateSystem, n, firstElementReceived))
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
        .sequential();
  }

  static Publisher<List<Cell>> createSerialBulkPutPublisher(
      final CellGemfireTemplate cellGemfireTemplate, final CoordinateSystem coordinateSystem,
      final Publisher<Cell> source, final int generations) {

    final LongAdder n = new LongAdder();
    final long starting = System.nanoTime();
    final AtomicLong firstElementReceived = new AtomicLong();

    final Consumer<Collection<Cell>>
        bulkCellConsumer =
        createRetryConsumer(
            createBulkCellConsumer(cellGemfireTemplate, coordinateSystem, n, firstElementReceived),
            PAUSE_MITIGATION
//            createDestroyLRUCellsMitigation(cellGemfireTemplate, coordinateSystem)
            );

    return enforceGenerationFraming(
        Flux.from(source)
            .limitRequest(generations * coordinateSystem.size())
            .buffer(coordinateSystem.size()),
        coordinateSystem)

        .doOnNext(
            bulkCellConsumer)
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived));
  }

  private static Publisher<List<Cell>> createParallelBulkPutPublisher(
      final CellGemfireTemplate cellGemfireTemplate,
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
            createBulkCellConsumer(cellGemfireTemplate, coordinateSystem, n, firstElementReceived))
        .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
        .sequential();
  }

  private static Consumer<Cell> createSingleCellConsumer(final CellGemfireTemplate cellGemfireTemplate,
                                                         final CoordinateSystem coordinateSystem,
                                                         final LongAdder n,
                                                         final AtomicLong firstElementReceived) {
    return cell -> {
      if (firstElementReceived.get() == 0) {
        firstElementReceived.set(System.nanoTime());
      }
      n.increment();
      try {
        cellGemfireTemplate.put(coordinateSystem.toOffset(cell.coordinates), cell);
      } catch (final Exception e) {
        System.out.println("for cell" + cell);
        e.printStackTrace();
        throw e;
      }
    };
  }

  private static Consumer<Collection<Cell>> createBulkCellConsumer(final CellGemfireTemplate cellGemfireTemplate,
                                                                   final CoordinateSystem coordinateSystem,
                                                                   final LongAdder n,
                                                                   final AtomicLong firstElementReceived) {
    return cells -> {
      if (firstElementReceived.get() == 0)
        firstElementReceived.set(System.nanoTime());
      try {
        final Map<Long, Cell> entries = cells.stream().map(cell -> {
          final long key = coordinateSystem.toOffset(cell.coordinates);
          return new Pair<>(key, cell);
        }).collect(Collectors.toMap(pair -> pair.k, pair -> pair.v));
        n.add(entries.size());
        cellGemfireTemplate.putAll(entries);
      } catch (final Exception e) {
        e.printStackTrace();
        throw e;
      }
    };
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
      final CellGemfireTemplate cellGemfireTemplate,
      final CoordinateSystem coordinateSystem) {
    return () -> {

      try {
        final Region<Integer,Cell> region = cellGemfireTemplate.getRegion();
        final int GENERATIONS_TO_DESTROY = 10;
        final SelectResults<Integer> lruKeys = region.query(
            String.format(
                "SELECT key "
                    + "FROM /Cells.entrySet cell, (SELECT MIN(cell.coordinates.generation) FROM /Cells cell) oldestGeneration"
                    + "WHERE cell.value.coordinates.generation >= oldestGeneration "
                    + "AND (cell.value.coordinates.generation < oldestGeneration + %d)",
                GENERATIONS_TO_DESTROY));
        for (final Integer key:lruKeys) {
          region.destroy(key);
        }

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
