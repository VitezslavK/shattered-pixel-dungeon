#!/bin/bash
# Builds the iOS version and installs it on a USB-connected iPhone, signed with a free Apple ID.
# Free provisioning expires after 7 days, rerun this script to refresh it.
#   tools/ios-install.sh <bundle id> <team id>
# The signing profile for the bundle id is created on first use by building a tiny stub app
# with xcodebuild -allowProvisioningUpdates (needs the Apple ID signed in under Xcode > Settings).
set -e
BUNDLE=${1:?bundle id, e.g. com.example.spdmod}
TEAM=${2:?team id from Xcode, e.g. ABCDE12345}
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}
DEVICE=$(xcrun devicectl list devices 2>/dev/null | awk '/physical/ && /connected/ {for(i=1;i<=NF;i++) if ($i ~ /^[0-9A-F]{8}-[0-9A-F]{16}$/) print $i}' | head -1)
[ -n "$DEVICE" ] || { echo "No connected iPhone found (plug it in, unlock it, trust this Mac)"; exit 1; }

# refresh the provisioning profile through a stub Xcode project
STUB="${TMPDIR:-/tmp}/spd-ios-stub"
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
command -v xcodegen >/dev/null || brew install xcodegen
(cd "$STUB" && xcodegen generate -q && xcodebuild -quiet -project SPDModStub.xcodeproj -scheme SPDModStub \
  -destination "id=$DEVICE" -allowProvisioningUpdates -allowProvisioningDeviceRegistration -derivedDataPath "$STUB/dd" build)
IDENTITY=$(security find-identity -v -p codesigning | grep -o '"Apple Development: [^"]*"' | head -1 | tr -d '"')

cd "$ROOT"
./gradlew ios:createIPA -PiosBundleId="$BUNDLE" -Probovm.iosSignIdentity="$IDENTITY" \
  -Probovm.iosProvisioningProfile="iOS Team Provisioning Profile: $BUNDLE" -Probovm.archs=arm64 --console=plain -q
IPA=$(ls "$HOME"/.spd-build/ios/robovm/*.ipa 2>/dev/null || ls ios/build/robovm/*.ipa)

# iOS 27 terminates apps built against its SDK unless they adopt the UIScene lifecycle, which the
# libGDX RoboVM backend doesn't do yet. Stamping the binary with an older SDK version keeps the
# legacy app lifecycle working, the app is then re-signed with the same identity and entitlements.
WORK="${TMPDIR:-/tmp}/spd-ios-ipa"
rm -rf "$WORK"; mkdir -p "$WORK"; (cd "$WORK" && unzip -q "$IPA")
APP=$(ls -d "$WORK"/Payload/*.app)
EXE=$(/usr/libexec/PlistBuddy -c "Print :CFBundleExecutable" "$APP/Info.plist")
codesign -d --entitlements - --xml "$APP" > "$WORK/ent.plist" 2>/dev/null
vtool -set-build-version ios 12.0 18.0 -replace -output "$APP/$EXE.new" "$APP/$EXE" 2>/dev/null
mv "$APP/$EXE.new" "$APP/$EXE"; chmod +x "$APP/$EXE"
/usr/libexec/PlistBuddy -c "Set :CFBundleDisplayName SPD Mod" -c "Set :CFBundleName SPD Mod" "$APP/Info.plist"
codesign -f -s "$IDENTITY" --entitlements "$WORK/ent.plist" --generate-entitlement-der "$APP"
codesign --verify --deep --strict "$APP"
(cd "$WORK" && zip -qr fixed.ipa Payload)
xcrun devicectl device install app --device "$DEVICE" "$WORK/fixed.ipa"
echo "Installed. First time only: iPhone Settings > General > VPN & Device Management > trust your Apple ID."
