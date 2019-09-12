package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

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
import org.apache.geode.cache.client.ServerOperationException;

@RunWith(SpringRunner.class)
//@ContextConfiguration(classes =RecorderServerTest.GeodeClientConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = RecorderServerTest.GeodeClientConfiguration.class)
@SuppressWarnings("unused")
public class RecorderServerTest extends
    ForkingClientServerIntegrationTestsSupport {

  public static final int PRIMORDIAL_GENERATION = -1;
  public static final CoordinateSystem coordinateSystem = new CoordinateSystem(100, 100);

  @BeforeClass
  public static void startGeodeServer() throws IOException {
    startGemFireServer(GeodeServerConfigurationPartitionedRegion.class,
        "-Xmx100m", "-Xms100m", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseShenandoahGC");
  }

  @Autowired
  private GemfireTemplate cellsTemplate;

  @SuppressWarnings("all")
  @Resource(name = "Cells")
  private Region<Integer, Cell> cells;

  @Test
  public void recordCell() {
    final Cell cell = Cell.createAlive(coordinateSystem.createCoordinates(0, 0, PRIMORDIAL_GENERATION),true);
    final int key = coordinateSystem.toOffset(cell.coordinates);
    cells.put(key, cell);
    assertThat(cells.keySetOnServer().contains(key)).isTrue();
    assertThat(cells.get(key)).isEqualTo(cell);
  }

  @Test
  public void recordAFewCells() {

    final List<Boolean> pattern = randomList(400,400 );

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    Flux.from(gameOfLifeSystem.getAllGenerations())
        .limitRequest(10)
        .doOnNext(cell->cells.put(coordinateSystem.toOffset(cell.coordinates), cell))
        .subscribe();
  }

  @Test
  public void recordLotsOfCells() {

    final List<Boolean> pattern = randomList(400,400 );

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    final int N = 10_000_000;

    final Flux<Cell> cells = Flux.from(gameOfLifeSystem.getAllGenerations())
        .limitRequest(N)
        .doOnNext(cell -> this.cells.put(coordinateSystem.toOffset(cell.coordinates), cell))
//        .retryBackoff(10, Duration.ofSeconds(5))
//        .retry(e->{
//          System.out.println("retrying because of:\n" + e);
//          if (e instanceof ServerOperationException) {
//            final ServerOperationException soe = (ServerOperationException)e;
//            System.out.println("\n  cause:\n" +soe.getCause());
//          }
//          return true;})
        ;

    StepVerifier.create(cells)
        .expectNextCount(N)
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