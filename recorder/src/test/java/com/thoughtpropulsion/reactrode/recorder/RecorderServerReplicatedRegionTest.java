package com.thoughtpropulsion.reactrode.recorder;

import java.io.IOException;

import com.thoughtpropulsion.reactrode.geodeconfig.GeodeServerConfigurationAutoEvictionAndCQ;
import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.recorder.geodeclient.CellGemfireTemplate;
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
    startGemFireServer(GeodeServerConfigurationAutoEvictionAndCQ.class,
        "-Xmx100m", "-Xms100m",
        // While OpenJDK 12 defaults to G1GC now, Geode 1.9 doc says use CMS
        "‑XX:+UseConcMarkSweepGC", "‑XX:CMSInitiatingOccupancyFraction=60");
  }

  @Autowired
  private GemfireTemplate cellsTemplate;

  @Test
  public void recordCellsTest() {
    CellTesting.recordCellsAndVerify(cellsTemplate, 100);
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