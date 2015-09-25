package com.log.kafka.storm.function;

import storm.trident.tuple.TridentTuple;

import java.io.Serializable;

/**
 * Created by ahenrick on 7/30/14.
 */
public interface MessageMapper extends Serializable {
  public String toMessageBody(TridentTuple tuple);
}