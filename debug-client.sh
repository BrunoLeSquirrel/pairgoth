#!/bin/bash

mvn -DskipTests=true install # needed for pairgoth-common

trap 'kill $CSSWATCH; exit' INT
( cd view-webapp; ./csswatch.sh ) &
CSSWATCH=$!

export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5006"
#mvn --projects view-webapp -Dpairgoth.api.url=http://localhost:8085/api/ package jetty:run
mvn -DskipTests=true --projects view-webapp package jetty:run -Dpairgoth.mode=client
kill $CSSWATCH
