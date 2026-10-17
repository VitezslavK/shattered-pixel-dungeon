#!/bin/bash
# Headless smoke test: generates every floor for every class, simulates combat and item use,
# saves and reloads, and reports exceptions that come from game logic.
#   tools/headless-test/run.sh [seeds] [extra java -D options, e.g. -Denonly=true]
set -e
SEEDS=${1:-8}; shift || true
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HERE="$ROOT/tools/headless-test"
OUT="${TMPDIR:-/tmp}/spd-headless"
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}
mkdir -p "$OUT/prefs" "$OUT/classes"
cd "$ROOT"
./gradlew -q core:jar
CP=$(./gradlew -q -I "$HERE/printcp.gradle" desktop:printCp | grep '^CP=' | sed 's/^CP=//')
CORE_JAR=$(ls "$HOME"/.spd-build/core/libs/core-*.jar 2>/dev/null | head -1)
[ -f "$CORE_JAR" ] || CORE_JAR=$(ls "$ROOT"/core/build/libs/core-*.jar | head -1)
"$JAVA_HOME/bin/javac" -nowarn -cp "$CP" -d "$OUT/classes" "$HERE/src/Harness.java" "$HERE/src/com/shatteredpixel/shatteredpixeldungeon/actors/Sim.java"
git diff master --name-only --diff-filter=A -- core/src/main/java | sed 's#.*/##; s#\.java##' > "$OUT/newclasses.txt"
cd "$ROOT/core/src/main/assets"
"$JAVA_HOME/bin/java" -XX:-OmitStackTraceInFastThrow -Dsim=true -Djava.awt.headless=true "$@" \
  -cp "$OUT/classes:$CP" Harness "$OUT/prefs" "$SEEDS" "$CORE_JAR" > "$OUT/run.log" 2>&1
grep "crash at\|HANG" "$OUT/run.log" || echo "no crashes or hangs"
grep "SIM" "$OUT/run.log" | grep -v "turns=150" || true
grep -E "^[0-9]+x (BUILDHERO|GENEXTRA|GENITEM|RUNETABLET|SETUP)" -A4 "$OUT/run.log" || true
grep "NOTEXT" "$OUT/run.log" | grep -E "$(paste -sd'|' "$OUT/newclasses.txt")" || echo "all new classes have texts"
python3 "$HERE/classify.py" "$OUT" "$OUT/run.log"
echo "full log: $OUT/run.log"
