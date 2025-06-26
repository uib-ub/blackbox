FROM maven:3-eclipse-temurin-17 AS builder
COPY . .
RUN mvn clean install && \
ls -alh && ls -alh target && ls src
FROM tomcat:11-jre17
COPY --from=builder target/blackbox-*.war /usr/local/tomcat/webapps/blackbox.war
RUN mkdir -p /var/log/blackbox/ \
    && chown -R 10001 /usr/local/tomcat/webapps /var/log/blackbox/ /usr/local/tomcat/conf/ \
    && sed -i 's/\(<Connector port="8080" protocol="HTTP\/1.1"\)/\1 relaxedQueryChars="\&quot;{}[]"/' /usr/local/tomcat/conf/server.xml
USER 10001
 
