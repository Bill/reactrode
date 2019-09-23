package com.thoughtpropulsion.reactrode.recorder.geodeclient;

import java.util.concurrent.Executors;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer;

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

  @Bean
  @Profile("use-sdg-cq")
  public ContinuousQueryListenerContainerConfigurer getCQContainerConfigurer() {

    // without this setting, the CQLC creates a zillion threads, which causes my carefully-crafted
    // sequence of Cells to be delivered out o' order!
    return (beanName, cqListenerContainer) -> cqListenerContainer.setTaskExecutor(Executors.newSingleThreadExecutor());
  }
}
