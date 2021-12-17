# Script to deploy Blackbox to Tomcat container. The script must be run as a root.
# Hemed, 2018-09-17
#!/bin/bash
set -e

BLACKBOX_INSTALL_DIR="/blackbox/sources"
WEBAPPS_DIR="/var/lib/tomcat/webapps"
LOG_DIR="/var/log/blackbox"

cd ${BLACKBOX_INSTALL_DIR}

echo "Compiling Blackbox ..."
mvn clean install #-X
service tomcat stop

echo "Deploying blackbox in $WEBAPPS_DIR"
if [ -d "$WEBAPPS_DIR" ];
then
  rm -rf ${WEBAPPS_DIR}/blackbox ${WEBAPPS_DIR}/blackbox.war
fi
cp -rp ${BLACKBOX_INSTALL_DIR}/target/*.war ${WEBAPPS_DIR}/blackbox.war

echo "Checking log directory in $LOG_DIR"
if [ ! -d "$LOG_DIR" ];
then
echo "$LOG_DIR does not exist. Creating ... "
  mkdir -p $LOG_DIR
fi

chown -R tomcat:tomcat ${WEBAPPS_DIR}
chown -R tomcat:tomcat ${LOG_DIR}

service tomcat start
echo "Blackbox successfully deployed"
