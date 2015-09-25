package com.log.kafka.storm.filter;

import storm.trident.operation.BaseFilter;
import storm.trident.tuple.TridentTuple;

/**
 * Created by ahenrick on 7/30/14.
 */
public class BooleanFilter extends BaseFilter {
   public boolean isKeep(TridentTuple tuple) {
    return tuple.getBoolean(0);
  }
}