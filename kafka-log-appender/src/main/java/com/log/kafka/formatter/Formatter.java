package com.log.kafka.formatter;

/**
 * Created by ahenrick on 8/3/14.
 */
import ch.qos.logback.classic.spi.ILoggingEvent;

public interface Formatter {
  String format(ILoggingEvent event);
}