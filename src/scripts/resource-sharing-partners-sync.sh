#!/bin/bash

APP=${project.artifactId}-${project.version}.jar

export ALMA_URL=${alma.url}
export ELASTIC_URL=${elastic.url}
export ELASTIC_USR=${elastic.usr}
export ELASTIC_PWD=${elastic.pwd}
export SYNC_HOST=${sync.host}
export SYNC_PORT=${sync.port}
export SYNC_PATH=${sync.path}

JAVA_OPTS="-Xms1g -Xmx1g"

java $JAVA_OPTS -jar $APP $*
