#!/bin/bash
# Runs SmithTest: the Smith class, without a screen.
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HERE="$ROOT/tools/headless-test"
OUT="${TMPDIR:-/tmp}/spd-smithtest"
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}
mkdir -p "$OUT/prefs" "$OUT/classes"
cd "$ROOT"
./gradlew -q core:jar
CP=$(./gradlew -q -I "$HERE/printcp.gradle" desktop:printCp | grep '^CP=' | sed 's/^CP=//')
"$JAVA_HOME/bin/javac" -nowarn -cp "$CP" -d "$OUT/classes" "$HERE/src/SmithTest.java"
cd "$ROOT/core/src/main/assets"
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$OUT/classes:$CP" SmithTest "$OUT/prefs"
