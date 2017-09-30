@echo off

set APP=${project.artifactId}-${project.version}.jar
set JAVA_OPTS=-Xms1g -Xmx1g

set ALMA_URL=${alma.url}
set ELASTIC_URL=${elastic.url}
set ELASTIC_USR=${elastic.usr}
set ELASTIC_PWD=${elastic.pwd}
set SYNC_HOST=${sync.host}
set SYNC_PORT=${sync.port}
set SYNC_PATH=${sync.path}

java %JAVA_OPTS% -jar "%APP%" %*
