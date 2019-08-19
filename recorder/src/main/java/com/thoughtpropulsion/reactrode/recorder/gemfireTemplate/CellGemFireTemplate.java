package com.thoughtpropulsion.reactrode.recorder.gemfireTemplate;

import com.thoughtpropulsion.reactrode.model.Cell;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.stereotype.Component;

import org.apache.geode.cache.Region;

@Component
public class CellGemFireTemplate extends GemfireTemplate {
    public CellGemFireTemplate(@Qualifier("Cells") final Region<Long, Cell> region) {
    super(region);
  }

  @Override
  public <K, V> V put(final K key, final V value) {
    return (V) getRegion().put(key,value);
  }
}
