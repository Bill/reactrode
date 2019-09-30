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

    startGemFireServer(

        // FRAGILE WITHOUT RETRY, ROBUST WITH RETRY
//        GeodeTestServerConfigurationAutoEvictionNoCQ.class,
//        "-Xmx70m", "-Xms70m",

        // ROBUST (NO RETRY NEEDED)
        GeodeServerConfigurationAutoEvictionAndCQ.class,
        "-Xmx200m", "-Xms200m",

        // While OpenJDK 12 defaults to G1GC now, Geode 1.9 doc says use CMS
        "‑XX:+UseConcMarkSweepGC", "‑XX:CMSInitiatingOccupancyFraction=60");

//    BlockHound.install();
  }

  @Autowired
  private GemfireTemplate cellsTemplate;

  @Test
  public void recordCellsAndVerifySerialPutTest() {
    CellTesting.recordCellsAndVerifySerialPut(cellsTemplate, 100_000);
  }

  @Test
  public void recordCellsAndVerifySerialBulkPutTest() {
    CellTesting.recordCellsAndVerifySerialBulkPut(cellsTemplate, 10);
  }

  @Test
  public void recordCellsAndVerifyParallelPutTest() {
    CellTesting.recordCellsAndVerifyParallelPut(cellsTemplate, 100_000, 8);
  }

  @Test
  public void recordCellsAndVerifyParallelBulkPutTest() {
    CellTesting.recordCellsAndVerifyParallelBulkPut(cellsTemplate, 10, 8);
  }

//  @Test
//  @Disabled
//  public void failsBlockHound() {
//    Mono.delay(Duration.ofMillis(1))
//        .doOnNext(it -> {
//          try {
//            Thread.sleep(10);
//          }
//          catch (InterruptedException e) {
//            throw new RuntimeException(e);
//          }
//        })
//        .block(); // should throw an exception about Thread.sleep
//  }

//  @Test
//  @Disabled
//  public void queryTest() {
//    CellTesting.recordCellsAndVerifySerialBulkPut(cellsTemplate, 1);
//    final String q = "SELECT * from /Cells";
//    final SelectResults<Cell> results = cellsTemplate.query(q);
//    for(final Cell cell : results) {
//      System.out.println(cell);
//    }
//  }

//  @Test
//  @Disabled
//  public void queryOrderByTest() {
//    CellTesting.recordCellsAndVerifySerialBulkPut(cellsTemplate, 1);
//    // to order by key, key must appear in projection
//    final String q = "SELECT key,value from /Cells.entries ORDER BY key";
//    final SelectResults<Struct> results = cellsTemplate.query(q);
//    for(final Struct s : results) {
//      System.out.println((Cell)s.get("value"));
//    }
//  }

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