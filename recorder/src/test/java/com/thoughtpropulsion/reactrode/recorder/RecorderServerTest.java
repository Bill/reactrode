package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.annotation.Resource;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.model.GameOfLifeSystem;
import com.thoughtpropulsion.reactrode.model.Patterns;
import com.thoughtpropulsion.reactrode.recorder.server.config.GeodeServerConfigurationPartitionedRegion;
import com.thoughtpropulsion.reactrode.recorder.server.config.GeodeServerConfigurationReplicatedRegion;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;

@RunWith(SpringRunner.class)
//@ContextConfiguration(classes =RecorderServerTest.GeodeClientConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = RecorderServerTest.GeodeClientConfiguration.class)
@SuppressWarnings("unused")
public class RecorderServerTest extends
    ForkingClientServerIntegrationTestsSupport {

  static final int PRIMORDIAL_GENERATION = -1;
  static final CoordinateSystem coordinateSystem = new CoordinateSystem(100, 100);

  Consumer<Cell>
      putCell = (final Cell cell) -> this.cells.put(coordinateSystem.toOffset(cell.coordinates), cell);

  // a higher-order function that takes a cell op function and wraps it in retrying
  UnaryOperator<Consumer<Cell>>
      withRetry = (final Consumer<Cell> operation) -> (final Cell cell) -> {
    int attempt = 1;
    while (true) {
      try {
        operation.accept(cell);
        return;
      } catch (final Throwable e) {
        if (attempt >= 3)
          throw e;
        else {
          System.out.println(String.format(
              "sleeping after attempt %d to put cell %s, due to exception:",
              attempt, cell, e));
          e.printStackTrace(System.out);
          ++attempt;
        }
      }
      try {
        Thread.sleep(5_000);
      } catch (InterruptedException _ignored) {
      }
    }
  };

  @BeforeClass
  public static void startGeodeServer() throws IOException {
    startGemFireServer(GeodeServerConfigurationReplicatedRegion.class,
        "-Xmx100m", "-Xms100m",
        // While OpenJDK 12 defaults to G1GC now, Geode 1.9 doc says use CMS
        "‑XX:+UseConcMarkSweepGC", "‑XX:CMSInitiatingOccupancyFraction=60");
  }

  @Autowired
  private GemfireTemplate cellsTemplate;

  @SuppressWarnings("all")
  @Resource(name = "Cells")
  private Region<Integer, Cell> cells;

  @Test
  public void recordAFewCellsTest() {
    recordCells(withRetry.apply(putCell), 10);
  }

  @Test
  public void recordLotsOfCellsTest() {
    recordCells(putCell, 1_000_000);
  }

  @Test
  public void recordLotsOfCellsWithRetryOnExceptionTest() {
    recordCells(withRetry.apply(putCell), 1_000_000);
  }

  void recordCells(final Consumer<Cell> recorder, final int n) {

    final List<Boolean> pattern = randomList(coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    final Flux<Cell> cells = Flux.from(gameOfLifeSystem.getAllGenerations())
        .limitRequest(n)
        .doOnNext(recorder);
    StepVerifier.create(cells)
        .expectNextCount(n)
        .verifyComplete();
  }

  @ClientCacheApplication
//  @EnableLogging(logLevel = "warn")
  @EnablePdx
  static class GeodeClientConfiguration {

    @Bean("Cells")
    public ClientRegionFactoryBean<Integer, Cell> cellsRegion(
        GemFireCache gemfireCache) {

      ClientRegionFactoryBean<Integer, Cell> cellsRegion =
          new ClientRegionFactoryBean<>();

      cellsRegion.setCache(gemfireCache);
      cellsRegion.setClose(false);
      cellsRegion.setShortcut(ClientRegionShortcut.PROXY);

      return cellsRegion;
    }

    @Bean
    @DependsOn("Cells")
    GemfireTemplate cellsTemplate(GemFireCache gemfireCache) {
      return new GemfireTemplate(gemfireCache.getRegion("/Cells"));
    }
  }


}