package com.log.kafka.appender;

/**
 * Created by ahenrick on 8/3/14.
 */

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.log.kafka.formatter.Formatter;
import com.log.kafka.formatter.MessageFormatter;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.util.Properties;

public class KafkaAppender extends AppenderBase<ILoggingEvent> {

  private String zookeeperHost;
  private Producer<String, String> producer;
  private Formatter formatter;
  private String brokerList;
  private String topic;

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public void setBrokerList(String s) {
    this.brokerList = s;
  }

  public String getBrokerList() {
    return this.brokerList;
  }

  public String getZookeeperHost() {
    return zookeeperHost;
  }

  public void setZookeeperHost(String zookeeperHost) {
    this.zookeeperHost = zookeeperHost;
  }

  public Formatter getFormatter() {
    return formatter;
  }

  public void setFormatter(Formatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public void start() {
    if (this.formatter == null) {
      this.formatter = new MessageFormatter();
    }
    super.start();
    Properties props = new Properties();
    props.put("zk.connect", this.zookeeperHost);
    props.put("metadata.broker.list", this.brokerList);
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    ProducerConfig config = new ProducerConfig(props);
    this.producer = new Producer<String, String>(config);
  }

  @Override
  public void stop() {
    super.stop();
    this.producer.close();
  }

  @Override
  protected void append(ILoggingEvent event) {
    String payload = this.formatter.format(event);
    this.producer.send(new KeyedMessage<String, String>(getTopic(), payload));
  }

}