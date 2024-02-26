#!/bin/sh

# debug version
# mvn package && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006 -jar application/target/pairgoth-engine.jar

mvn -DskipTests=true package && java -Dpairgoth.mode=standalone -jar application/target/pairgoth-engine.jar
