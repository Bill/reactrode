package com.thoughtpropulsion.reactrode.recorder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.recorder.config.RecordingConfiguration;
import com.thoughtpropulsion.reactrode.recorder.gemfireTemplate.CellGemFireTemplate;
import com.thoughtpropulsion.reactrode.recorder.subscriber.RecordingSubscriber;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.apache.geode.cache.GemFireCache;

@SpringBootApplication(scanBasePackageClasses = RecordingConfiguration.class)
public class RecorderApplication {

  private static volatile ConfigurableApplicationContext applicationContext;

  final RecordingSubscriber recordingSubscriber;

  public RecorderApplication(final RecordingSubscriber recordingSubscriber) {
    this.recordingSubscriber = recordingSubscriber;
  }

  public static void main(String[] args) {
    applicationContext = new SpringApplicationBuilder()
        .sources(RecorderApplication.class)
        .web(WebApplicationType.NONE)
        .run(args);
  }

  @Bean
  public CoordinateSystem getCoordinateSystem() {
    return new CoordinateSystem(100, 100);
  }

  private static class Pair<K,V> {
    public final K k;
    public final V v;

    public Pair(final K k, final V v) {
      this.k = k; this.v = v;
    }
  }

  @Bean
  public ApplicationRunner getRunner(
      final CellGemFireTemplate cellGemFireTemplate,
      final CoordinateSystem coordinateSystem,
      GemFireCache cache) throws Exception {

    final Publisher<Cell> source = recordingSubscriber.allGenerations();

    return createParallelPutRunner(cellGemFireTemplate, coordinateSystem, source);
  }

  private ApplicationRunner createSerialPutRunner(final CellGemFireTemplate cellGemFireTemplate,
                                                  final CoordinateSystem coordinateSystem,
                                                  final Publisher<Cell> source) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();
      final int demand = 200000;
      Flux.from(source)
          .limitRequest(demand)
          .doOnNext(
              getSingleCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived))
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .blockLast();
    };
  }

  private ApplicationRunner createParallelPutRunner(final CellGemFireTemplate cellGemFireTemplate,
                                                    final CoordinateSystem coordinateSystem,
                                                    final Publisher<Cell> source) {
    return args -> {
//      System.out.println("cache: "+cache.isClosed());
//      final Cell cell = Cell.createAlive(coordinateSystem.createCoordinates(0,0,-1), true);
//      final int key = coordinateSystem.toOffset(cell.coordinates);
//      cellGemFireTemplate.put(key, cell);
//      final Object got = cellGemFireTemplate.get(key);
//      System.out.println("got: " + got);

      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();
      final int demand = 2_000_000;
      Flux.from(source)
          .limitRequest(demand)
          .parallel(4)
          .runOn(Schedulers.elastic())
          .doOnNext(
              getSingleCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived))
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .subscribe();

      Thread.sleep(120_000); // TODO: find a better way to keep app alive until flux is done
    };
  }

  private ApplicationRunner createSerialBulkPutRunner(final CellGemFireTemplate cellGemFireTemplate,
                                                  final CoordinateSystem coordinateSystem,
                                                  final Publisher<Cell> source) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();
      final int demand = 200000;
      Flux.from(source)
          .limitRequest(demand)
          .buffer(8192)
          .doOnNext(
              getBulkCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived)
          )
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .blockLast();
    };
  }

  private Consumer<Cell> getSingleCellConsumer(final CellGemFireTemplate cellGemFireTemplate,
                                               final CoordinateSystem coordinateSystem,
                                               final LongAdder n,
                                               final AtomicLong firstElementReceived) {
    return cell -> {
      if (firstElementReceived.get() == 0) {
        firstElementReceived.set(System.nanoTime());
      }
      n.increment();
      try {
        cellGemFireTemplate.put(coordinateSystem.toOffset(cell.coordinates), cell);
      } catch (final Exception e) {
        System.out.println("for cell" + cell);
        e.printStackTrace();
        throw e;
      }
    };
  }

  private Runnable summarizePerformance(final LongAdder n, final long starting,
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

  private Consumer<Collection<Cell>> getBulkCellConsumer(final CellGemFireTemplate cellGemFireTemplate,
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
        cellGemFireTemplate.putAll(entries);
      } catch (final Exception e) {
        e.printStackTrace();
        throw e;
      }
    };
  }

}
