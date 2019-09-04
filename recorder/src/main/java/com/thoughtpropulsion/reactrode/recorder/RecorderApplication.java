package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.GameOfLife.enforceGenerationFraming;

import java.util.Collection;
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
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableContinuousQueries;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.apache.geode.cache.GemFireCache;

@SpringBootApplication(scanBasePackageClasses = RecordingConfiguration.class)
@ClientCacheApplication(subscriptionEnabled = true)
@EnableContinuousQueries
public class RecorderApplication {

  private static final int PARALLELISM = 4;
  private static final int LIMIT_REQUEST = 3_000_000;
  private static volatile ConfigurableApplicationContext applicationContext;

  final RecordingSubscriber recordingSubscriber;

  public RecorderApplication(final RecordingSubscriber recordingSubscriber) {
    this.recordingSubscriber = recordingSubscriber;
  }

  public static void main(String[] args) {
    applicationContext = new SpringApplicationBuilder()
        .sources(RecorderApplication.class)
//        .web(WebApplicationType.NONE)
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

    return createParallelBulkPutRunner(cellGemFireTemplate, coordinateSystem, source,
        PARALLELISM);
  }

  private ApplicationRunner createSerialPutRunner(final CellGemFireTemplate cellGemFireTemplate,
                                                  final CoordinateSystem coordinateSystem,
                                                  final Publisher<Cell> source) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();
      Flux.from(source)
          .limitRequest(LIMIT_REQUEST)
          .doOnNext(
              getSingleCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived))
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .blockLast();
    };
  }

  private ApplicationRunner createParallelPutRunner(final CellGemFireTemplate cellGemFireTemplate,
                                                    final CoordinateSystem coordinateSystem,
                                                    final Publisher<Cell> source,
                                                    final int parallelism) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();
      Flux.from(source)
          .limitRequest(LIMIT_REQUEST)
          .parallel(parallelism)
          .runOn(Schedulers.elastic())
          .doOnNext(
              getSingleCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived))
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .sequential()
          .blockLast();
    };
  }

  private ApplicationRunner createSerialBulkPutRunner(final CellGemFireTemplate cellGemFireTemplate,
                                                      final CoordinateSystem coordinateSystem,
                                                      final Publisher<Cell> source,
                                                      final int batchSize) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();

      enforceGenerationFraming(
          Flux.from(source)
              .limitRequest(LIMIT_REQUEST)
              .buffer(batchSize),
          coordinateSystem)

          .doOnNext(
              getBulkCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived))
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .blockLast();
    };
  }

  private ApplicationRunner createParallelBulkPutRunner(
      final CellGemFireTemplate cellGemFireTemplate,
      final CoordinateSystem coordinateSystem,
      final Publisher<Cell> source,
      final int parallelism) {
    return args -> {
      final LongAdder n = new LongAdder();
      final long starting = System.nanoTime();
      final AtomicLong firstElementReceived = new AtomicLong();

      enforceGenerationFraming(
          Flux.from(source)
              .limitRequest(LIMIT_REQUEST)
              .buffer(coordinateSystem.size()),
          coordinateSystem)

          .parallel(parallelism)
          .runOn(Schedulers.elastic())
          .doOnNext(
              getBulkCellConsumer(cellGemFireTemplate, coordinateSystem, n, firstElementReceived))
          .doOnTerminate(summarizePerformance(n, starting, firstElementReceived))
          .sequential()
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

}
