package com.thoughtpropulsion.reactrode.recorder.geodeclient;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

@Configuration
public class GeodeClientConfiguration {
  @Bean("Cells")
  public ClientRegionFactoryBean getCellsRegion(GemFireCache gemfireCache) {
    ClientRegionFactoryBean<Long, Cell> cellRegionFactoryBean = new ClientRegionFactoryBean<>();
    cellRegionFactoryBean.setCache(gemfireCache);
    cellRegionFactoryBean.setName("Cells");
//    cellRegionFactoryBean.setShortcut(ClientRegionShortcut.LOCAL);
    cellRegionFactoryBean.setShortcut(ClientRegionShortcut.PROXY);
    return cellRegionFactoryBean;
  }
}
