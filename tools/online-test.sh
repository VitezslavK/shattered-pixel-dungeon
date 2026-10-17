#!/bin/bash
# End-to-end test of the online features against the Supabase project in online.properties.
# It uploads a test ghost, bones and a daily score (player name SPDTEST, daily on 2000-01-01),
# downloads them again and builds ghosts from them. Remove the test rows afterwards with:
#   delete from ghosts where player_name = 'SPDTEST';
#   delete from daily_scores where day = '2000-01-01';
#   delete from bones where depth = 31 and created_at > now() - interval '1 hour';
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HERE="$ROOT/tools/headless-test"
OUT="${TMPDIR:-/tmp}/spd-online-test"
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}
mkdir -p "$OUT/classes" "$OUT/prefs"
cd "$ROOT"
./gradlew -q core:jar
CP=$(./gradlew -q -I "$HERE/printcp.gradle" desktop:printCp | grep '^CP=' | sed 's/^CP=//')
"$JAVA_HOME/bin/javac" -nowarn -cp "$CP" -d "$OUT/classes" "$HERE/src/OnlineTest.java"
cd "$ROOT/core/src/main/assets"
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$OUT/classes:$CP" OnlineTest "$OUT/prefs" | grep -v "^\[log\]"
