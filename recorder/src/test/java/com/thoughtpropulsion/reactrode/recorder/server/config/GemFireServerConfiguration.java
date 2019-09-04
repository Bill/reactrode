package com.thoughtpropulsion.reactrode.recorder.server.config;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnablePdx;

import org.apache.geode.cache.GemFireCache;

@EnablePdx
@CacheServerApplication(name = "AutoConfiguredContinuousQueryIntegrationTests", logLevel = "error")
public class GemFireServerConfiguration {

  public static void main(String[] args) {

    AnnotationConfigApplicationContext applicationContext =
        new AnnotationConfigApplicationContext(GemFireServerConfiguration.class);

    applicationContext.registerShutdownHook();
  }

  /*
   Using a paritioned region so CQ will work. From the Geode docs:

   "you cannot create a CQ on a replicated region with eviction setting of local-destroy
   since this eviction setting changes the region’s data policy"
   */
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
