FROM openjdk:8-slim

ENV APP target/${project.artifactId}-${project.version}-jar-with-dependencies.jar
ENV DIR /opt/resource-sharing-partners-sync

COPY ${APP} ${DIR}/${APP}
WORKDIR ${DIR}
EXPOSE 8080
CMD java $JAVA_OPTS -jar ${APP}
