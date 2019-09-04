package com.thoughtpropulsion.reactrode.recorder;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.annotation.Resource;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import com.thoughtpropulsion.reactrode.recorder.server.RecorderServer;
import com.thoughtpropulsion.reactrode.recorder.server.config.GemFireServerConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableContinuousQueries;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.listener.annotation.ContinuousQuery;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.query.CqEvent;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RecorderServerCQTest.GemFireClientConfiguration.class)
@SuppressWarnings("unused")
public class RecorderServerCQTest extends
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

  @Autowired
  private RecorderServer recorderServer;

  @SuppressWarnings("all")
  @Resource(name = "Cells")
  private Region<Integer, Cell> cells;

  @Test
  public void simpleCQ() {
    final Publisher<Cell>
        gens =
        recorderServer.allGenerationsStartingFrom(Coordinates.create(0, 0, PRIMORDIAL_GENERATION));

    StepVerifier.create(gens)
        .then(()-> putACell())
        .thenRequest(1)
        .expectNextCount(1)
        .thenCancel()
        .verify();
  }

  private void putACell() {
    final Cell
        cell =
        Cell.createAlive(coordinateSystem.createCoordinates(0, 0, PRIMORDIAL_GENERATION), true);
    final int key = coordinateSystem.toOffset(cell.coordinates);
    cells.put(key, cell);
  }

  @ClientCacheApplication(subscriptionEnabled = true, retryAttempts = 1, servers = @ClientCacheApplication.Server,
      readyForEvents = true, durableClientId = "22", durableClientTimeout = 5)
  @EnableLogging(logLevel = GEMFIRE_LOG_LEVEL)
  @EnablePdx
  @Import(value = RecorderServer.class)
  @EnableContinuousQueries
  static class GemFireClientConfiguration {

    @ContinuousQuery(name = "OurFirstCQ", query = "select * from /Cells")
    public void someCQ(CqEvent cqEvent) {
      System.out.println("GemFireClientConfiguration.someCQ " + cqEvent.getNewValue());
    }

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