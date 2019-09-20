package com.thoughtpropulsion.reactrode.recorder;

import java.io.IOException;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.recorder.gemfireTemplate.CellGemfireTemplate;
import com.thoughtpropulsion.reactrode.recorder.server.config.GeodeServerConfigurationPartitionedRegion;
import com.thoughtpropulsion.reactrode.recorder.server.config.GeodeServerConfigurationReplicatedIndexedRegion;
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

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

@RunWith(SpringRunner.class)
//@ContextConfiguration(classes =RecorderServerTest.GeodeClientConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RecorderServerReplicatedRegionTest.GeodeClientConfiguration.class,CellGemfireTemplate.class})
@SuppressWarnings("unused")
public class RecorderServerReplicatedRegionTest extends
    ForkingClientServerIntegrationTestsSupport {

  @BeforeClass
  public static void startGeodeServer() throws IOException {
    startGemFireServer(GeodeServerConfigurationPartitionedRegion.class,
        "-Xmx200m", "-Xms200m",
        // While OpenJDK 12 defaults to G1GC now, Geode 1.9 doc says use CMS
        "‑XX:+UseConcMarkSweepGC", "‑XX:CMSInitiatingOccupancyFraction=60"/*,
        "-Dgemfire.Cache.DISABLE_QUERY_MONITOR_FOR_LOW_MEMORY=true"*/);
  }

  @Autowired
  private CellGemfireTemplate cellsTemplate;

  @Test
  public void recordCellsTest() {
    CellTesting.recordCellsAndVerify(cellsTemplate, 400);
  }

  @Test
  public void recordLotsOfCellsWithRetryOnExceptionTest() {
//    recordCells(CellOperations.withRetry.apply(CellOperations.createPutCellFunction(cells)), 1_000_000);
  }

  @Test
  public void recordAndPlayBackTest() {

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