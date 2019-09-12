package com.thoughtpropulsion.reactrode.geodeserver.config;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableClusterConfiguration;
import org.springframework.data.gemfire.config.annotation.EnableLocator;
import org.springframework.data.gemfire.config.annotation.EnableManager;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;

import org.apache.geode.cache.AttributesFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.PartitionAttributes;
import org.apache.geode.cache.PartitionAttributesFactory;
import org.apache.geode.cache.RegionAttributes;

@Configuration
@Profile("partitioned-region")
@CacheServerApplication
@EnableLocator
@EnableGemfireRepositories
@EnableClusterConfiguration(useHttp = true, requireHttps = false)
@EnableManager
public class GeodeConfiguration {

  /*
  TODO: make LRU work again
  When this changed from a ReplicatedRegionFactoryBean<Long,Cell> in order to make CQ work,
  the LRU stopped working. See javadoc comment for createLRUMemoryAttributes():

   * For a region with {@link DataPolicy#PARTITION}, even if maximumMegabytes are supplied, the
   * EvictionAttribute <code>maximum</code>, is always set to
   * {@link PartitionAttributesFactory#setLocalMaxMemory(int) " local max memory "} specified for
   * the {@link PartitionAttributes}.

  */
  @Bean("Cells")
  PartitionedRegionFactoryBean<Long, Cell> getCellRegion(
      final GemFireCache gemFireCache) {

    final PartitionedRegionFactoryBean<Long, Cell>
        regionFactoryBean =
        new PartitionedRegionFactoryBean<>();

    regionFactoryBean.setName("Cells");
    regionFactoryBean.setCache(gemFireCache);

    // this works only for Replicated regions--not Partitioned regions
//    regionFactoryBean.setEvictionAttributes(
//        EvictionAttributes.createLRUMemoryAttributes(50)
//    );

    // so let's create PartitionAttributes and set the local max memory

    final PartitionAttributesFactory<Long, Cell> paf = new PartitionAttributesFactory<>();
    paf.setLocalMaxMemory(50);

    final PartitionAttributes<Long, Cell> pa = paf.create();

    // woops: can't set PartitionAttributes on a PartitionedRegionFactoryBean<>
//    regionFactoryBean.setAttributes(pa);

    // I found this deprecated class tho!
    AttributesFactory<Long,Cell> af = new AttributesFactory<>();
    af.setPartitionAttributes(pa);
    RegionAttributes ra = af.create();

    regionFactoryBean.setAttributes(ra);

    return regionFactoryBean;
  }
}
