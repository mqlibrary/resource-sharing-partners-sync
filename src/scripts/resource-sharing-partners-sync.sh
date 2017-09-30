#!/bin/bash

APP=${project.artifactId}-${project.version}.jar

ALMA_URL=${alma.url}
ELASTIC_URL=${elastic.url}
ELASTIC_USR=${elastic.usr}
ELASTIC_PWD=${elastic.pwd}
SYNC_HOST=${sync.host}
SYNC_PORT=${sync.port}
SYNC_PATH=${sync.path}

JAVA_OPTS="-Xms1g -Xmx1g"

java $JAVA_OPTS -jar $APP $*
