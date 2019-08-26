package com.thoughtpropulsion.reactrode.geodeserver.config;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.EnableClusterConfiguration;
import org.springframework.data.gemfire.config.annotation.EnableLocator;
import org.springframework.data.gemfire.config.annotation.EnableManager;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;

import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.GemFireCache;

@Configuration
@CacheServerApplication
@EnableLocator
@EnableGemfireRepositories
@EnableClusterConfiguration(useHttp = true, requireHttps = false)
@EnableManager
public class GeodeConfiguration {

  @Bean("Cells")
  ReplicatedRegionFactoryBean<Long, Cell> getCellRegion(
      final GemFireCache gemFireCache) {
    final ReplicatedRegionFactoryBean<Long, Cell>
        regionFactoryBean =
        new ReplicatedRegionFactoryBean<>();
    regionFactoryBean.setName("Cells");
    regionFactoryBean.setCache(gemFireCache);
    regionFactoryBean.setEvictionAttributes(
        EvictionAttributes.createLRUMemoryAttributes(50)
    );
    return regionFactoryBean;
  }
}
