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