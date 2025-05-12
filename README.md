# 前言
代码是基于 https://github.com/apache/rocketmq-dashboard 增加了相关场景的解决方案实现。

在好几年前rocketmq-dashboard还叫做rocketmq-console，那时候rocketmq还是4.x版本，5.0还没有。

基于当时的rocketmq-console进行了一些个性化场景的二开。
今天翻老电脑的时候发现这个版本实现，想起来提交到github了。

最新的5.x是否支持不确定，应该是不支持，没试，4.x版本肯定是支持的。

相对于4.9版本的rocketmq-dashboard，额外的能力如下：
* 平台化的流程管理
* 消息轨迹节点不参与集群业务的查询解决方案
* 历史备份消息的可视化查询

## RocketMQ-Console-NG [![Coverage Status](https://coveralls.io/repos/github/rocketmq/rocketmq-console-ng/badge.svg?branch=master)](https://coveralls.io/github/rocketmq/rocketmq-console-ng?branch=master)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
## How To Install

表结构在: ./db/mysql_rmq.sql

执行sql语句初始化表结构.

### With Docker

* get docker image

```
mvn clean package -Dmaven.test.skip=true docker:build
```

or

```
docker pull apacherocketmq/rocketmq-console
```

> currently the newest available docker image is apacherocketmq/rocketmq-console:2.0.0


* run it (change namesvrAddr and port yourself)

```
docker run -e "JAVA_OPTS=-Drocketmq.namesrv.addr=127.0.0.1:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8080:8080 -t apacherocketmq/rocketmq-console-ng
```

or 

```
docker run -e "JAVA_OPTS=-Drocketmq.namesrv.addr=127.0.0.1:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8080:8080 -t apacherocketmq/rocketmq-console-ng:2.0.0
```

### Without Docker
require java 1.8+
1.package
```
mvn clean package -Dmaven.test.skip -Dcheckstyle.skip=true
```
2. upload the *.tar.gz file to the server
3. unpack and run
```shell script
tar -zxvf rocketmq-console-ng-2.0.0-release.tar
cd rocketmq-console-ng-2.0.0
chmod +x rocketmq-console
./rocketmq-console -start
```

4.stop
```shell script
./rocketmq-console -stop
```
#### Tips
```shell script
./rocketmq-console -help see args describe
```
  
* if you use the rocketmq < 3.5.8,please add -Dcom.rocketmq.sendMessageWithVIPChannel=false when you start rocketmq-console-ng(or you can change it in ops page)
* change the rocketmq.config.namesrvAddr in resource/application.properties.(or you can change it in ops page)