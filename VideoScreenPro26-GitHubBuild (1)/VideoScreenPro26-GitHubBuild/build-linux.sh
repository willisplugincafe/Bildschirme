#!/usr/bin/env bash
set -euo pipefail
command -v java >/dev/null || { echo 'Java fehlt (JDK 25 erforderlich).'; exit 1; }
command -v gradle >/dev/null || { echo 'Gradle fehlt. Installiere Gradle 9 oder verwende einen kompatiblen Gradle Wrapper.'; exit 1; }
JAVA_VERSION=$(java -version 2>&1 | head -1)
echo "Java: $JAVA_VERSION"
java -version 2>&1 | grep -q 'version "25\.' || { echo 'Bitte JDK 25 aktivieren.'; exit 1; }
gradle clean build
printf '\nPlugin-JAR unter build/libs/ erstellt.\n'
