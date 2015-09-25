package com.log.kafka.storm.function;

import backtype.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

/**
 * Created by ahenrick on 7/30/14.
 */
public class ThresholdFilterFunction extends BaseFunction {
  private static final Logger LOG = LoggerFactory.getLogger(ThresholdFilterFunction.class);

  private static enum State {
    BELOW, ABOVE;
  }

  private State last = State.BELOW;
  private double threshold;

  public ThresholdFilterFunction(double threshold) {
    this.threshold = threshold;
  }

  public void execute(TridentTuple tuple, TridentCollector collector) {
    double val = tuple.getDouble(0);
    State newState = val < this.threshold ? State.BELOW : State.ABOVE;
    boolean stateChange = this.last != newState;
    collector.emit(new Values(stateChange, threshold));
    this.last = newState;
    LOG.debug("State change? --> {}", stateChange);
  }
}