package com.thoughtpropulsion.reactrode.recorder.geodeclient;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.stereotype.Component;

import org.apache.geode.cache.Region;

@Component("CellsTemplate")
@Primary
public class CellGemfireTemplate extends GemfireTemplate {
    public CellGemfireTemplate(@Qualifier("Cells") final Region<Long, Cell> region) {
    super(region);
  }

  @Override
  public <K, V> V put(final K key, final V value) {
    return (V) getRegion().put(key,value);
  }
}
