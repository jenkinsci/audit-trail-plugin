#!/bin/bash

sudo systemctl stop jenkins && mvn clean package -DskipTests && sudo cp target/audit-trail.hpi /var/lib/jenkins/plugins/audit-trail.jpi && sudo chown jenkins:jenkins /var/lib/jenkins/plugins/audit-trail.jpi && sudo systemctl start jenkins && sleep 5 && sudo systemctl status jenkins
