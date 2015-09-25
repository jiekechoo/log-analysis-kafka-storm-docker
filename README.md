> 本文主要目的在于将Storm运行环境集成到docker容器中，在一个快速开发模式下运行测试。代码内容来自 [Storm Blueprints: Patterns for Distributed Real-time Computation](http://www.amazon.com/Storm-Blueprints-Distributed-Real-time-Computation/dp/178216829X) 第四章。
```
本代码仅供学习。这个样例程序主要是分析日志文件，一旦符合某个threshold，将通过XMPP告警。
 - 将日志信息写到Apache Kafka； 
 - Storm流处理streaming功能负责分析日志； 
 - 实现移动平均线式的分析；
 - Storm将告警信息使用XMPP协议通知给管理员。
```


##下载源码
git clone https://github.com/jiekechoo/log-analysis-kafka-storm-docker.git

 - kafka-log-appender：将日志内容写到kafka程序
 - log-kafka-storm：docker-compose脚本和storm程序
##准备docker环境
###启动docker
docker-compose环境搭建过程请查看我的[另一篇博客:CentOS 6 install docker and docker-compose](http://blog.csdn.net/jiekechoo/article/details/48690841)

进入log-kafka-storm目录，查看docker-compose.yml文件，假设我们的宿主主机是：**192.168.1.231**，如有需要改成你自己的主机地址，代码里所有的地方都要把地址改一下。

```
[root@docker2 log-kafka-storm]# more docker-compose.yml 
zookeeper:
  image: jplock/zookeeper
  ports: 
    - "2181:2181"
nimbus:
  image: wurstmeister/storm-nimbus:0.9.2
  ports:
    - "3773:3773"
    - "3772:3772"
    - "6627:6627"
  links: 
    - zookeeper:zk
    - kafka:kafka
supervisor:
  image: wurstmeister/storm-supervisor:0.9.2
  ports:
    - "8000:8000"
  links: 
    - nimbus:nimbus
    - zookeeper:zk
    - kafka:kafka
ui:
  image: wurstmeister/storm-ui:0.9.2
  ports:
    - "8080:8080"
  links: 
    - nimbus:nimbus
    - zookeeper:zk
kafka:
  image: wurstmeister/kafka:0.8.1
  ports:
    - "9092:9092"
  links:
    - zookeeper:zk
  environment:
    BROKER_ID: 1
    HOST_IP: 192.168.1.231
    PORT: 9092
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
openfire:
  image: mdouglas/openfire
  ports:
  - "5222:5222"
  - "5223:5223"
  - "9091:9091"
  - "9090:9090"
```
启动docker-compose
```
[root@docker2 log-kafka-storm]# docker-compose up -d
Creating logkafkastorm_zookeeper_1...
Creating logkafkastorm_openfire_1...
Creating logkafkastorm_kafka_1...
Creating logkafkastorm_nimbus_1...
Creating logkafkastorm_ui_1...
Creating logkafkastorm_supervisor_1...
```
启动完成后应该都是  Up 状态
```
[root@docker2 log-kafka-storm]# docker-compose ps
            Name                           Command                          State                           Ports             
-----------------------------------------------------------------------------------------------------------------------------
logkafkastorm_kafka_1           /bin/sh -c start-kafka.sh       Up                              0.0.0.0:9092->9092/tcp        
logkafkastorm_nimbus_1          /bin/sh -c /usr/bin/start-      Up                              0.0.0.0:3772->3772/tcp,       
                                ...                                                             0.0.0.0:3773->3773/tcp,       
                                                                                                0.0.0.0:6627->6627/tcp        
logkafkastorm_openfire_1        /start.sh                       Up                              0.0.0.0:5222->5222/tcp,       
                                                                                                0.0.0.0:5223->5223/tcp,       
                                                                                                0.0.0.0:9090->9090/tcp,       
                                                                                                0.0.0.0:9091->9091/tcp        
logkafkastorm_supervisor_1      /bin/sh -c /usr/bin/start-      Up                              6700/tcp, 6701/tcp, 6702/tcp, 
                                ...                                                             6703/tcp,                     
                                                                                                0.0.0.0:8000->8000/tcp        
logkafkastorm_ui_1              /bin/sh -c /usr/bin/start-      Up                              0.0.0.0:8080->8080/tcp        
                                ...                                                                                           
logkafkastorm_zookeeper_1       /opt/zookeeper-3.4.5/bin/z      Up                              0.0.0.0:2181->2181/tcp,       
                                ...                                                             2888/tcp, 3888/tcp            
```
打开浏览器查询Storm UI状态，查看 http://192.168.1.231:8080/ 应该看到下面的画面，没有报错就可以了。
![这里写图片描述](http://img.blog.csdn.net/20150925232509387)

至此，docker算是启动完毕。

###配置kafka
运行 start-kafka-shell.sh 脚本，主要是两个工作：

 - 1.进入kafka容器；
 - 2.创建topic名为 log-analysis

```
[root@docker2 log-kafka-storm]# ./start-kafka-shell.sh 
root@4c7a5d233991:/# $KAFKA_HOME/bin/kafka-topics.sh --create --topic log-analysis --partitions 1 --zookeeper zk --replication-factor 1
Created topic "log-analysis".
```
查看kafka的topic是否创建成功
```
root@4c7a5d233991:/# $KAFKA_HOME/bin/kafka-topics.sh --describe --zookeeper zk
Topic:log-analysis      PartitionCount:1        ReplicationFactor:1     Configs:
        Topic: log-analysis     Partition: 0    Leader: 9092    Replicas: 9092  Isr: 9092
```
###配置Openfire
浏览器打开 http://192.168.1.231:9090/ ，首先是初始化，域使用你自己的，我们这里是 sectong.com 。删除 服务器证书，否则android和ios客户端登录不上。
![这里写图片描述](http://img.blog.csdn.net/20150925233633140)
创建两个用户，邮箱用 @sectong.com 结尾：

 - 用户名 *storm* 密码 *storm* ，用于发送消息； 
 - 用户名 *alarm* 密码 *alarm* ，用户接收消息；

下载一个windows版本的IM客户端，[Spark](http://www.igniterealtime.org/projects/spark/) windows版本安装后如下图所示：

![这里写图片描述](http://img.blog.csdn.net/20150925234256419)

#编译程序
##Storm运行程序
编译Storm程序
```
[root@docker2 log-kafka-storm]# mvn clean package
...此处省略过程
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 16.359 s
[INFO] Finished at: 2015-09-25T23:53:29+08:00
[INFO] Final Memory: 31M/373M
[INFO] ------------------------------------------------------------------------

[root@docker2 log-kafka-storm]# ll target/
total 20M
drwxr-xr-x+ 1 ppl None   0 Sep 25 23:53 archive-tmp/
drwxr-xr-x+ 1 ppl None   0 Sep 25 23:53 classes/
drwxr-xr-x+ 1 ppl None   0 Sep 25 23:53 generated-sources/
-rwxr-xr-x  1 ppl None 20M Sep 25 23:53 log-kafka-storm-1.0-SNAPSHOT-jar-with-dependencies.jar
-rwxr-xr-x  1 ppl None 17K Sep 25 23:53 log-kafka-storm-1.0-SNAPSHOT.jar
drwxr-xr-x+ 1 ppl None   0 Sep 25 23:53 maven-archiver/

```
上传至Storm集群
```
[root@docker2 log-kafka-storm]# storm jar ./target/log-kafka-storm-1.0-SNAPSHOT-jar-with-dependencies.jar com.log.kafka.storm.topology.LogAnalysisTopology 192.168.1.231 log-analysis-topology

...省略
36971 [main] INFO  backtype.storm.StormSubmitter - Submitting topology log-analysis-topology in distributed mode with conf {"storm.xmpp.server":"192.168.1.231","nimbus.host":"192.168.1.231","storm.xmpp.password":"storm","topology.workers":3,"storm.zookeeper.port":2181,"storm.xmpp.user":"storm","storm.xmpp.to":"ahenrick@sectong.com","nimbus.thrift.port":6627,"storm.zookeeper.servers":["192.168.1.231"],"topology.max.spout.pending":5,"topology.trident.batch.emit.interval.millis":2000}
37663 [main] INFO  backtype.storm.StormSubmitter - Finished submitting topology: log-analysis-topology
```
jar包上传完毕，Storm UI 应该增加一个 Topology

![这里写图片描述](http://img.blog.csdn.net/20150926000526845)
点击，进入
![这里写图片描述](http://img.blog.csdn.net/20150926001212441)

到这里，Storm程序已经上传完毕。

##日志写入kafka程序

进入 kafka-log-appender 目录，编译程序
```
[root@docker2 kafka-log-appender]# mvn clean package

...省略
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 3.287 s
[INFO] Finished at: 2015-09-26T00:13:35+08:00
[INFO] Final Memory: 20M/180M
[INFO] ------------------------------------------------------------------------

[root@docker2 kafka-log-appender]# ll target/
total 8.0K
drwxr-xr-x+ 1 ppl None    0 Sep 26 00:13 classes/
-rwxr-xr-x  1 ppl None 7.0K Sep 26 00:13 kafka-log-appender-1.0-SNAPSHOT.jar
drwxr-xr-x+ 1 ppl None    0 Sep 26 00:13 maven-archiver/
drwxr-xr-x+ 1 ppl None    0 Sep 26 00:13 maven-status/
```

运行jar包程序，向kafka发送日志内容。这里需要注意：运行此jar包的主机需要得到kafka容器的主机名和ip，写入hosts文件。

```
[root@docker2 kafka-log-appender]# java -cp target/kafka-log-appender-1.0-SNAPSHOT.jar com.log.kafka.RogueApplication

913  [main] INFO  kafka.client.ClientUtils$ - Fetching metadata from broker id:0,host:192.168.1.231,port:9092 with correlation id 0 for 1 topic(s) Set(log-analysis) 
931  [main] INFO  kafka.producer.SyncProducer - Connected to 192.168.1.231:9092 for producing 
989  [main] INFO  kafka.producer.SyncProducer - Disconnecting from 192.168.1.231:9092 
1053 [main] INFO  kafka.producer.SyncProducer - Connected to c470c1bea851:9092 for producing 
796  [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
1566 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
2069 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
2572 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
3074 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
3578 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
4080 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4183 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4286 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4389 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4494 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4598 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4702 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4806 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
4912 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5018 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5137 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5240 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5343 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5446 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5550 [main] WARN  com.log.kafka.RogueApplication - This is a warning (rapid state). 
5653 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
6157 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
6660 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
7162 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
7664 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
8166 [main] WARN  com.log.kafka.RogueApplication - This is a warning (slow state). 
```
发送成功后，Storm UI 相应数字增加，证明已经运行成功

![这里写图片描述](http://img.blog.csdn.net/20150926002322699)

Spark IM 客户端也会收到相应的告警信息

![这里写图片描述](http://img.blog.csdn.net/20150926002418280)

Android手机下载 [Xabber](http://www.xabber.com/)客户端，iOS下载[Monal IM](http://monal.im)，登录IM，可以实时收到推送告警消息

![这里写图片描述](http://img.blog.csdn.net/20150926005107617)

谢谢观赏！
