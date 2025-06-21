FROM maven:3-eclipse-temurin-17 AS builder
COPY . .
RUN mvn clean install && \ 
ls -alh && ls -alh target && ls src
FROM tomcat:11-jre17
COPY --from=builder target/blackbox-*.war /usr/local/tomcat/webapps/blackbox.war
RUN chown -R 10001 /usr/local/tomcat/webapps
USER 10001
 
