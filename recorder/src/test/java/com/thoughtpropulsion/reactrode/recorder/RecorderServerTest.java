package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomPattern;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.model.GameOfLifeSystem;
import com.thoughtpropulsion.reactrode.model.Patterns;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.integration.config.ClientServerIntegrationTestsConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = RecorderServerTest.GemFireClientConfiguration.class)
@SuppressWarnings("unused")
public class RecorderServerTest extends
    ForkingClientServerIntegrationTestsSupport {

  private static final String GEMFIRE_LOG_LEVEL = "error";
  public static final int PRIMORDIAL_GENERATION = -1;
  public static final CoordinateSystem coordinateSystem = new CoordinateSystem(100, 100);

  @BeforeClass
  public static void startGemFireServer() throws IOException {
    startGemFireServer(GemFireServerConfiguration.class);
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
    assertThat(cells.containsKey(key)).isTrue();
    assertThat(cells.get(key)).isEqualTo(cell);
  }

  @Test
  public void recordMultipleCells() {

    final List<Boolean> pattern = randomPattern(400,400 );

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, PRIMORDIAL_GENERATION, coordinateSystem)),
        coordinateSystem);

    Flux.from(gameOfLifeSystem.getAllGenerations())
        .take(10)
        .doOnNext(cell->cells.put(coordinateSystem.toOffset(cell.coordinates), cell))
        .subscribe();

  }

  @SpringBootApplication
  @EnableLogging(logLevel = GEMFIRE_LOG_LEVEL)
  @Import(ClientServerIntegrationTestsConfiguration.class)
  //@Import(SubscriptionEnabledClientServerIntegrationTestsConfiguration.class)
  public static class GemFireClientConfiguration {

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

  @EnablePdx
  @CacheServerApplication(name = "AutoConfiguredContinuousQueryIntegrationTests", logLevel = GEMFIRE_LOG_LEVEL)
  public static class GemFireServerConfiguration {

    public static void main(String[] args) {

      AnnotationConfigApplicationContext applicationContext =
          new AnnotationConfigApplicationContext(GemFireServerConfiguration.class);

      applicationContext.registerShutdownHook();
    }

    @Bean("Cells")
    public PartitionedRegionFactoryBean<Integer, Cell> cellsRegion(GemFireCache gemfireCache) {

      PartitionedRegionFactoryBean<Integer, Cell> cellsRegion =
          new PartitionedRegionFactoryBean<>();

      cellsRegion.setCache(gemfireCache);
      cellsRegion.setClose(false);
      cellsRegion.setPersistent(false);

      return cellsRegion;
    }

  }
}