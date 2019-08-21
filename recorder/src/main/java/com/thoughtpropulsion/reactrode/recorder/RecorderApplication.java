package com.thoughtpropulsion.reactrode.recorder;

import java.io.Console;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.recorder.config.RecordingConfiguration;
import com.thoughtpropulsion.reactrode.recorder.gemfireTemplate.CellGemFireTemplate;
import com.thoughtpropulsion.reactrode.recorder.subscriber.RecordingSubscriber;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.GemfireTemplate;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import org.apache.geode.cache.GemFireCache;

@SpringBootApplication(scanBasePackageClasses = RecordingConfiguration.class)
public class RecorderApplication {

  final RecordingSubscriber recordingSubscriber;

  public RecorderApplication(final RecordingSubscriber recordingSubscriber) {
    this.recordingSubscriber = recordingSubscriber;
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder()
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
    return args -> {
//      System.out.println("cache: "+cache.isClosed());
//      final Cell cell = Cell.createAlive(coordinateSystem.createCoordinates(0,0,-1), true);
//      final int key = coordinateSystem.toOffset(cell.coordinates);
//      cellGemFireTemplate.put(key, cell);
//      final Object got = cellGemFireTemplate.get(key);
//      System.out.println("got: " + got);

      // TODO: figure out why this flux never terminates (app hangs)
      final LongAdder liveCount = new LongAdder();
      final long starting = System.nanoTime();
      final int demand = 20000;
      Flux.from(recordingSubscriber.allGenerations())
          .subscribeOn(Schedulers.elastic())
          .limitRequest(demand)
          .parallel(2,1024)
          .doOnNext(
              cell -> {
                if (cell.isAlive) {
                  liveCount.increment();
                } else {
                  liveCount.decrement();
                }
                try {
                  cellGemFireTemplate.put(coordinateSystem.toOffset(cell.coordinates), cell);
                } catch (final Exception e) {
                  System.out.println("for cell" + cell);
                  e.printStackTrace();
                  throw e;
                }
              })
//          .buffer(1000)
//          .doOnNext(
//              cells -> {
//                try {
//                  final Map<Long, Cell> entries = cells.stream().map(cell -> {
//                    final long key = coordinateSystem.toOffset(cell.coordinates);
//                    return new Pair<>(key, cell);
//                  }).collect(Collectors.toMap(pair -> pair.k, pair -> pair.v));
//
//                  cellGemFireTemplate.putAll(entries);
//                } catch (final Exception e) {
//                  e.printStackTrace();
//                  throw e;
//                }
//              }
//          )
          .doOnTerminate(() -> {
            final long ending = System.nanoTime();
            final long elapsed = ending - starting;
            System.out
                .println(String.format("%d cells with net live count: %s took %d nanoseconds", demand, liveCount, elapsed));
          })
//          .blockLast();
          .subscribe();

      Thread.sleep(10000);
    };

  }

}
