#!/bin/bash
docker run --link logkafkastorm_zookeeper_1:zk -i -t wurstmeister/kafka:0.8.1 /bin/bash
