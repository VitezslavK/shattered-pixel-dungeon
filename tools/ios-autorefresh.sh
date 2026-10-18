#!/bin/bash
# Keeps the free-Apple-ID iOS install alive: apps signed this way stop launching after 7 days.
# Run daily by a LaunchAgent (see tools/ios-autorefresh-setup.sh). When the signing profile has less
# than REFRESH_DAYS left, it fetches a fresh one, re-signs the last installed game and installs it
# again, then emails the result. A failed attempt emails how long the game still has.
#
# Everything lives in ~/.spd-refresh (outside ~/Documents, which background jobs can't read):
#   config    BUNDLE=..., TEAM=..., EMAIL=... (also the sender, so it must be an account in Mail)
#   base.ipa  the game as last installed by tools/ios-install.sh
#   ent.plist its signing entitlements
#   expires   unix time the installed profile expires
#   log.txt   what happened
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
DIR="$HOME/.spd-refresh"
source "$DIR/config"
REFRESH_DAYS=${REFRESH_DAYS:-3}
PROFILES="$HOME/Library/Developer/Xcode/UserData/Provisioning Profiles"
LOG="$DIR/log.txt"
exec >> "$LOG" 2>&1
echo "=== $(date '+%Y-%m-%d %H:%M') ==="

now=$(date +%s)
expires=$(cat "$DIR/expires" 2>/dev/null || echo 0)
left=$(( expires - now ))
if [ "$1" != "--force" ] && [ $left -gt $(( REFRESH_DAYS * 86400 )) ]; then
	echo "still valid until $(date -r $expires '+%d.%m. %H:%M'), nothing to do"
	exit 0
fi

fmt_date(){ date -r "$1" '+%d.%m.%Y v %H:%M'; }

send_mail(){
	local subject="$1" body="$2"
	osascript - "$EMAIL" "$subject" "$body" <<'OSA'
on run argv
	tell application "Mail"
		set m to make new outgoing message with properties {sender:item 1 of argv, subject:item 2 of argv, content:item 3 of argv, visible:false}
		tell m to make new to recipient at end of to recipients with properties {address:item 1 of argv}
		send m
	end tell
end run
OSA
}

fail(){
	echo "FAILED: $1"
	local when
	if [ $expires -gt 0 ]; then
		if [ $left -gt 0 ]; then
			when="Hra na iPhonu přestane jít spustit $(fmt_date $expires) (zbývá asi $(( left / 3600 )) hodin). Zkusím to znovu zítra."
		else
			when="Platnost hry na iPhonu už vypršela ($(fmt_date $expires)), hra teď nejde spustit. Uložené hry zůstávají."
		fi
	else
		when="Nevím, dokdy hra na iPhonu platí."
	fi
	send_mail "SPD Mod: obnova na iPhonu se NEPOVEDLA" "Automatická obnova hry SPD Mod na iPhonu se nepovedla.

Důvod: $1

$when

Co udělat: nech Mac zapnutý, iPhone odemkni a připoj ho kabelem nebo na stejnou Wi-Fi jako Mac. Obnova proběhne sama při dalším pokusu."
	exit 1
}

[ -f "$DIR/base.ipa" ] || fail "chybí uložená hra (spusť jednou tools/ios-install.sh)"

DEVICE=$(xcrun devicectl list devices 2>/dev/null | awk '/physical/ && (/connected/ || /available/) {for(i=1;i<=NF;i++) if ($i ~ /^[0-9A-F]{8}-[0-9A-F]{16}$/) print $i}' | head -1)
[ -n "$DEVICE" ] || fail "iPhone není připojený k Macu (ani kabelem, ani přes Wi-Fi)"

# a fresh 7-day profile: drop the old one so Xcode has to create a new one
STUB="$DIR/stub"
if [ ! -d "$STUB/SPDModStub.xcodeproj" ]; then
	mkdir -p "$STUB/Sources"
	echo 'import SwiftUI
@main struct StubApp: App { var body: some Scene { WindowGroup { Text("SPD Mod") } } }' > "$STUB/Sources/App.swift"
	cat > "$STUB/project.yml" <<YML
name: SPDModStub
targets:
  SPDModStub:
    type: application
    platform: iOS
    deploymentTarget: "15.0"
    sources: [Sources]
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: $BUNDLE
        DEVELOPMENT_TEAM: $TEAM
        CODE_SIGN_STYLE: Automatic
        GENERATE_INFOPLIST_FILE: YES
YML
	(cd "$STUB" && xcodegen generate -q) || fail "nepodařilo se připravit podpisový projekt"
fi
mkdir -p "$DIR/old-profiles"
for p in "$PROFILES"/*.mobileprovision; do
	[ -f "$p" ] || continue
	if security cms -D -i "$p" 2>/dev/null | grep -q "<string>iOS Team Provisioning Profile: $BUNDLE</string>"; then
		mv "$p" "$DIR/old-profiles/"
	fi
done
(cd "$STUB" && xcodebuild -quiet -project SPDModStub.xcodeproj -scheme SPDModStub -destination "id=$DEVICE" \
	-allowProvisioningUpdates -allowProvisioningDeviceRegistration -derivedDataPath "$STUB/dd" build) \
	|| fail "Apple nevydal nové podepsání (zkontroluj, že jsi v Xcode > Settings > Accounts přihlášený)"
PROFILE=$(ls -t "$PROFILES"/*.mobileprovision 2>/dev/null | head -1)
[ -n "$PROFILE" ] || fail "Apple nevydal nové podepsání"
exp_iso=$(security cms -D -i "$PROFILE" 2>/dev/null | plutil -extract ExpirationDate raw -o - - 2>/dev/null)
new_exp=$(date -j -u -f "%Y-%m-%dT%H:%M:%SZ" "$exp_iso" +%s 2>/dev/null)
[ -n "$new_exp" ] || fail "nepodařilo se přečíst platnost nového podepsání"

# re-sign the saved game with the new profile
IDENTITY=$(security find-identity -v -p codesigning | grep -o '"Apple Development: [^"]*"' | head -1 | tr -d '"')
WORK="$DIR/work"
rm -rf "$WORK"; mkdir -p "$WORK"
(cd "$WORK" && unzip -q "$DIR/base.ipa") || fail "uložená hra je poškozená"
APP=$(ls -d "$WORK"/Payload/*.app)
cp "$PROFILE" "$APP/embedded.mobileprovision"
codesign -f -s "$IDENTITY" --entitlements "$DIR/ent.plist" --generate-entitlement-der "$APP" \
	|| fail "nepodařilo se hru podepsat (je Mac odemčený?)"
(cd "$WORK" && zip -qr game.ipa Payload)
xcrun devicectl device install app --device "$DEVICE" "$WORK/game.ipa" \
	|| fail "instalace do iPhonu se nepovedla (iPhone musí být odemčený a na stejné Wi-Fi nebo na kabelu)"
rm -rf "$WORK"

echo "$new_exp" > "$DIR/expires"
echo "OK, valid until $(fmt_date $new_exp)"
send_mail "SPD Mod: hra na iPhonu obnovena" "Hra SPD Mod na tvém iPhonu byla automaticky obnovena.

Nově platí do $(fmt_date $new_exp). Další obnova proběhne sama asi $REFRESH_DAYS dny předtím."
