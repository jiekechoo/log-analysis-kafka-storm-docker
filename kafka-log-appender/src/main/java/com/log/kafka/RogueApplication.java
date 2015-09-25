package com.log.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ahenrick on 7/30/14.
 */
public class RogueApplication {

  private static final Logger LOG = LoggerFactory.getLogger(RogueApplication.class);

  public static void main(String[] args) throws Exception {
    int slowCount = 6;
    int fastCount = 15;
    // slow state
    for (int i = 0; i < slowCount; i++) {
      LOG.warn("This is a warning (slow state).");
      Thread.sleep(500);
    }
    // enter rapid state
    for (int i = 0; i < fastCount; i++) {
      LOG.warn("This is a warning (rapid state).");
      Thread.sleep(100);
    }
    // return to slow state
    for (int i = 0; i < slowCount; i++) {
      LOG.warn("This is a warning (slow state).");
      Thread.sleep(500);
    }
  }
}