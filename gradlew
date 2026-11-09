#!/bin/sh
#
# Copyright © 2015-2024 the original authors.
#
# Licensed under the Apache License, Version 2.0.
# See https://www.apache.org/licenses/LICENSE-2.0
#

APP_HOME=$(cd "$(dirname "$0")" && pwd -P) || exit
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi
exec "$JAVACMD" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
