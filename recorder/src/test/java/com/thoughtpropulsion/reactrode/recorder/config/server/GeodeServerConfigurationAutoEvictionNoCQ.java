package com.thoughtpropulsion.reactrode.recorder.config.server;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnableStatistics;

import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.GemFireCache;

/*
 This configuration's eviction is able to keep up with the rate of put()/putAll() just fine.
 Unfortunately, since configuring eviction w/ action "local destroy" causes the region's data
 policy to switch to "pre-loaded", the region is subsequently unable to support continuous
 queries!
 */

@CacheServerApplication(name = "AutoConfiguredContinuousQueryIntegrationTests", logLevel = "error",
    criticalHeapPercentage = 90f, evictionHeapPercentage = 70f)
@EnablePdx
@EnableLogging(logLevel = "info", logFile = "/Users/bburcham/Projects/reactrode/recorder/src/test/logs/geode.log")
@EnableStatistics(archiveFile = "/Users/bburcham/Projects/reactrode/recorder/src/test/logs/statistics.gfs")
public class GeodeServerConfigurationAutoEvictionNoCQ {

  public static void main(String[] args) {
    System.out.println("Geode Server using Java version: " + System.getProperty("java.version") );

    AnnotationConfigApplicationContext applicationContext =
        new AnnotationConfigApplicationContext(
            GeodeServerConfigurationAutoEvictionNoCQ.class);

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
    factory.setEvictionAttributes(
        /*
         Region grows until evictionHeapPercentage is reached, then TBD elements are evicted
         daemon monitors heap memory usage--non-cache actions can result in eviction
         */
        EvictionAttributes.createLRUHeapAttributes()
    );

    return factory;
  }
}
