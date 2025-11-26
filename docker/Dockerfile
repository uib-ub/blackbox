FROM maven:3-eclipse-temurin-21 AS builder
COPY . .
RUN mvn clean install
FROM tomcat:11-jre21
COPY --from=builder target/blackbox-*.war /usr/local/tomcat/webapps/blackbox.war
RUN useradd -u 10001 tomcat \
    && mkdir -p /var/log/blackbox/ \
    && chown -R 10001 /usr/local/tomcat/webapps /var/log/blackbox/ /usr/local/tomcat/conf/ \
    && sed -i 's/\(<Connector port="8080" protocol="HTTP\/1.1"\)/\1 relaxedQueryChars="\&quot;{}[]"/' /usr/local/tomcat/conf/server.xml
USER 10001