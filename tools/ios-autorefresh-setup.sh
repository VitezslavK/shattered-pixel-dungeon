#!/bin/bash
# Installs the daily iOS refresh job (tools/ios-autorefresh.sh) as a LaunchAgent.
#   tools/ios-autorefresh-setup.sh <bundle id> <team id> <email for reports>
# Run tools/ios-install.sh once before, it saves the game that gets re-signed.
set -e
BUNDLE=${1:?bundle id}
TEAM=${2:?team id}
EMAIL=${3:?email for the reports}
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$HOME/.spd-refresh"
LABEL="com.spdmod.ios-refresh"
PLIST="$HOME/Library/LaunchAgents/$LABEL.plist"

mkdir -p "$DIR" "$HOME/Library/LaunchAgents"
cp "$ROOT/tools/ios-autorefresh.sh" "$DIR/refresh.sh"
chmod +x "$DIR/refresh.sh"
printf 'BUNDLE=%s\nTEAM=%s\nEMAIL=%s\n' "$BUNDLE" "$TEAM" "$EMAIL" > "$DIR/config"

#every day at 10:00 (or right after the Mac wakes up if it slept through it) and at login
cat > "$PLIST" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>Label</key><string>$LABEL</string>
	<key>ProgramArguments</key><array><string>/bin/bash</string><string>$DIR/refresh.sh</string></array>
	<key>StartCalendarInterval</key><dict><key>Hour</key><integer>10</integer><key>Minute</key><integer>0</integer></dict>
	<key>RunAtLoad</key><true/>
</dict>
</plist>
PL
launchctl bootout "gui/$(id -u)/$LABEL" 2>/dev/null || true
launchctl bootstrap "gui/$(id -u)" "$PLIST"
echo "Daily refresh installed. Log: $DIR/log.txt"
