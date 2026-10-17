#!/bin/bash
# Two simulated players test the live link against the Supabase project in online.properties:
# A creates a room, B joins, sees A and sends help and mischief, A receives both and dies,
# B sees the death. Test rows use the friend codes TESTLA/TESTLB, remove them with:
#   delete from live_events where friend_code in ('TESTLA', 'TESTLB');
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HERE="$ROOT/tools/headless-test"
OUT="${TMPDIR:-/tmp}/spd-live-test"
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}
rm -rf "$OUT"; mkdir -p "$OUT/classes"
cd "$ROOT"
./gradlew -q core:jar
CP=$(./gradlew -q -I "$HERE/printcp.gradle" desktop:printCp | grep '^CP=' | sed 's/^CP=//')
"$JAVA_HOME/bin/javac" -nowarn -cp "$CP" -d "$OUT/classes" "$HERE/src/LiveTest.java"
cd "$ROOT/core/src/main/assets"
for R in "A1 pa" "B pb" "A2 pa" "B2 pb"; do
  set -- $R
  "$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$OUT/classes:$CP" LiveTest "$1" "$OUT/$2" | grep -E "^(OK|FAIL|ROLE)"
done
