package com.log.kafka.formatter;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Created by ahenrick on 8/3/14.
 */
public class MessageFormatter implements Formatter {

  public String format(ILoggingEvent event) {
    return event.getFormattedMessage();
  }

}
