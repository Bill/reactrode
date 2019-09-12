package com.thoughtpropulsion.reactrode.recorder.server.config;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnableStatistics;

import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.GemFireCache;

@CacheServerApplication(name = "AutoConfiguredContinuousQueryIntegrationTests", logLevel = "error",
    criticalHeapPercentage = 80f, evictionHeapPercentage = 60f)
@EnablePdx
@EnableLogging(logLevel = "info", logFile="/Users/bburcham/Projects/reactrode/geode.log")
@EnableStatistics(archiveFile="/Users/bburcham/Projects/reactrode/statistics.gfs")
public class GeodeServerConfigurationPartitionedRegion {

  public static void main(String[] args) {

    System.out.println("Geode Server using Java version: " + System.getProperty("java.version") );
    AnnotationConfigApplicationContext applicationContext =
        new AnnotationConfigApplicationContext(GeodeServerConfigurationPartitionedRegion.class);

    applicationContext.registerShutdownHook();
  }

  /*
   This is a single-node Geode cluster. We want a region that will accept an infinite number
   of cells without exhausting the Java heap. We have to use Geode _eviction_, not _expiration_,
   to accomplish that since expiration doesn't make any guarantees about maximum memory utilization.

   With eviction, there are three _algorithm_ (policy) choices. Only one is directly tied to the
   amount of heap memory Java has available and that is EvictionAlgorithm.LRU_HEAP.

   With eviction, there are two choices of _action_ to take when eviction is triggered:
   local destroy, or overflow to disk.

   Because Geode offers no option for "global destroy" as an eviction action (doing so would entail
   adding a distributed notion of "LRU" to the product---similar to the current product's
   distributed notion of "last access time" that supports expiration based on an "idle timeout"
   policy) we have decided to use local destroy for our application.

   Given that we want to use eviction with local destroy, we cannot use a replicated region. If we
   created a replicated region, it would immediately, and silently, be converted to a _preoloaded_
   region when we configured eviction with the local destroy action. That changes the region's
   data policy in to one that is incompatible with continuous queries (which we also need):

   https://geode.apache.org/docs/guide/19/developing/eviction/how_eviction_works.html

   https://geode.apache.org/docs/guide/19/developing/continuous_querying/how_continuous_querying_works.html

   So we will use a partitioned region.

   TODO: when this was a replicated region, eviction worked. Now that it's a partitioned region
   eviction does not appear to be, um, evicting.
   */
  @Bean("Cells")
  public PartitionedRegionFactoryBean<Integer, Cell> cellsRegion(GemFireCache gemfireCache) {

    PartitionedRegionFactoryBean<Integer, Cell> factory =
        new PartitionedRegionFactoryBean<>();

    factory.setCache(gemfireCache);
    factory.setClose(false);
    factory.setPersistent(false);
    factory.setEvictionAttributes(

        /*
         Region allows 1 entry, regardless of criticalHeapPercentage, evictionHeapPercentage
         only cache actions cause eviction
         */
//        EvictionAttributes.createLRUEntryAttributes(1)

        /*
         Region allows 1MB-worth of entries, regardless of criticalHeapPercentage, evictionHeapPercentage
         only cache actions cause eviction
         */
//        EvictionAttributes.createLRUMemoryAttributes(1)  // region allows 1MB-worth of entries

        /*
         Region grows until evictionHeapPercentage is reached, then TBD elements are evicted
         daemon monitors heap memory usage--non-cache actions can result in eviction
         */
        EvictionAttributes.createLRUHeapAttributes()

    );

    return factory;
  }

}
