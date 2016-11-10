#!/usr/bin/env bash
# JVM_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:+AggressiveOpts -server"
# java $JVM_OPTS -jar target/uxbox-backend-0.1.0-SNAPSHOT-standalone.jar -m uxbox.main
export PATH=/opt/img/bin:$PATH
lein trampoline run -m uxbox.main
