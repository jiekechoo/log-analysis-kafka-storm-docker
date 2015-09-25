package com.log.kafka.storm.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.tuple.Fields;
import com.log.kafka.storm.common.EWMA;
import com.log.kafka.storm.common.NotifyMessageMapper;
import com.log.kafka.storm.filter.BooleanFilter;
import com.log.kafka.storm.function.JsonProjectFunction;
import com.log.kafka.storm.function.MovingAverageFunction;
import com.log.kafka.storm.function.ThresholdFilterFunction;
import com.log.kafka.storm.function.XMPPFunction;
import storm.kafka.BrokerHosts;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;
import storm.kafka.trident.OpaqueTridentKafkaSpout;
import storm.kafka.trident.TridentKafkaConfig;
import storm.trident.Stream;
import storm.trident.TridentTopology;

import java.util.Arrays;

public class LogAnalysisTopology {

  private final BrokerHosts brokerHosts;

  public LogAnalysisTopology(String kafkaZookeeper) {
    brokerHosts = new ZkHosts(kafkaZookeeper);
  }

  public StormTopology buildTopology() {

    TridentTopology topology = new TridentTopology();

    TridentKafkaConfig kafkaConfig = new TridentKafkaConfig(brokerHosts, "log-analysis","storm");
    kafkaConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
    kafkaConfig.forceFromStart = true;

    OpaqueTridentKafkaSpout kafkaSpout = new OpaqueTridentKafkaSpout(kafkaConfig);

    Stream spoutStream = topology.newStream("kafka-stream", kafkaSpout);

    Fields jsonFields = new Fields("level", "timestamp", "message", "logger");
    Stream parsedStream = spoutStream.each(new Fields("str"), new JsonProjectFunction(jsonFields), jsonFields);

    // drop the unparsed JSON to reduce tuple size
    parsedStream = parsedStream.project(jsonFields);

    EWMA ewma = new EWMA().sliding(1.0, EWMA.Time.MINUTES).withAlpha(EWMA.ONE_MINUTE_ALPHA);
    Stream averageStream = parsedStream.each(new Fields("timestamp"),
        new MovingAverageFunction(ewma, EWMA.Time.MINUTES), new Fields("average"));

    ThresholdFilterFunction tff = new ThresholdFilterFunction(50D);
    Stream thresholdStream = averageStream.each(new Fields("average"), tff, new Fields("change", "threshold"));

    Stream filteredStream = thresholdStream.each(new Fields("change"), new BooleanFilter());

    filteredStream.each(filteredStream.getOutputFields(), new XMPPFunction(new NotifyMessageMapper()), new Fields());

    return topology.build();
  }

  public static void main(String[] args) throws Exception {

    String dockerIp = args[0];

    Config conf = new Config();
    conf.put(XMPPFunction.XMPP_USER, "storm");
    conf.put(XMPPFunction.XMPP_PASSWORD, "storm");
    conf.put(XMPPFunction.XMPP_SERVER, dockerIp);
    conf.put(XMPPFunction.XMPP_TO, "alarm@sectong.com");
    conf.setMaxSpoutPending(5);
    conf.put(Config.TOPOLOGY_TRIDENT_BATCH_EMIT_INTERVAL_MILLIS, 2000);

    if (args.length > 1) {
      LogAnalysisTopology logAnalysisTopology = new LogAnalysisTopology(args[0]+":2181");
      conf.setNumWorkers(3);
      conf.put(Config.NIMBUS_HOST, dockerIp);
      conf.put(Config.NIMBUS_THRIFT_PORT, 6627);
      conf.put(Config.STORM_ZOOKEEPER_PORT, 2181);
      conf.put(Config.STORM_ZOOKEEPER_SERVERS, Arrays.asList(dockerIp));
      StormSubmitter.submitTopology(args[1], conf, logAnalysisTopology.buildTopology());
    } else {
      LogAnalysisTopology logAnalysisTopology = new LogAnalysisTopology(dockerIp);
      LocalCluster cluster = new LocalCluster();
      cluster.submitTopology("log-analysis", conf, logAnalysisTopology.buildTopology());
    }
  }
}
