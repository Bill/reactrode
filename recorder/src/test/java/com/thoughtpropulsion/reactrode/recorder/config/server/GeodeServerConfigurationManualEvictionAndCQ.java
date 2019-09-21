package com.thoughtpropulsion.reactrode.recorder.config.server;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.IndexFactoryBean;
import org.springframework.data.gemfire.IndexType;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnableStatistics;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;

/*
 This config was used in an attempt to implement eviction manually. When LowMemoryException
 is encountered during put()/putAll(), the code in createDestroyLRUCellsMitigation() will query
 for the least-recently-used cells and will destroy() them.

 Status: the mitigation found LRU cells and destroyed the first batch. But even with e.g.
 a 5-second or more delay after that, no memory actually seems to have gotten reclaimed.
 As a result, we quickly hit the critical memory threshold and failed.
 */

@CacheServerApplication(name = "AutoConfiguredContinuousQueryIntegrationTests", logLevel = "error",
    criticalHeapPercentage = 90f, evictionHeapPercentage = 70f)
@EnablePdx
@EnableLogging(logLevel = "info", logFile = "/Users/bburcham/Projects/reactrode/recorder/src/test/logs/geode.log")
@EnableStatistics(archiveFile = "/Users/bburcham/Projects/reactrode/recorder/src/test/logs/statistics.gfs")
public class GeodeServerConfigurationManualEvictionAndCQ {

  public static void main(String[] args) {
    System.out.println("Geode Server using Java version: " + System.getProperty("java.version") );

    // so that createDestroyLRUCellsMitigation can query when we are past evictionHeapPercentage
    System.setProperty("gemfire.Cache.DISABLE_QUERY_MONITOR_FOR_LOW_MEMORY", "true");

    AnnotationConfigApplicationContext applicationContext =
        new AnnotationConfigApplicationContext(
            GeodeServerConfigurationManualEvictionAndCQ.class);

    applicationContext.registerShutdownHook();
  }

  @Bean("Cells")
  public ReplicatedRegionFactoryBean<Integer, Cell> cellsRegion(GemFireCache gemfireCache) {
    System.out.println("Geode Server using Java version: " + System.getProperty("java.version") );

    ReplicatedRegionFactoryBean<Integer, Cell> factory =
        new ReplicatedRegionFactoryBean<>();

    factory.setCache(gemfireCache);
    factory.setClose(false);
    factory.setPersistent(false);

    // No automatic eviction is configured. We'll do it in application code!

    return factory;
  }

  @Bean("CellsGenerationIndex")
  IndexFactoryBean cellsGenerationIndex(final Cache cache) {

    final IndexFactoryBean cellsGenerationIndex = new IndexFactoryBean();

    cellsGenerationIndex.setCache(cache);
    cellsGenerationIndex.setName("CellsGenerationIndex");
    cellsGenerationIndex.setExpression("coordinates.generation");
    cellsGenerationIndex.setFrom("/Cells");
    cellsGenerationIndex.setType(IndexType.FUNCTIONAL);

    return cellsGenerationIndex;
  }

}
