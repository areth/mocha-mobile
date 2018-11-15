#!/bin/bash
set -e

# Use the environment variable to target a specific device
if [ "$DEVICE_ID" = "" ]; then
  echo "Target device: default"
  TARGET=""
else
  echo "Target device: $DEVICE_ID"
  TARGET="-s $DEVICE_ID"
fi

# APP_DIR=$1
#SCRIPT_BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"
#NODEJS_BASE_DIR="$( cd "$( dirname "$0" )" && cd .. && cd .. && cd .. && pwd )"
TEST_APP_BASE_DIR="$( cd "$( dirname "$0" )" && cd mochamobile/ && pwd )"
#TEST_PROXY_TARGETDIR="$( cd "$NODEJS_BASE_DIR" && mkdir -p ./out/android.release/ && cd ./out/android.release/ && pwd )"
# Build the Android test app
# echo $APP_DIR
# echo $APP_IGNORE
# exit 0
# ( cd "$TEST_APP_BASE_DIR" && ./gradlew assembleDebug )
# ( cd "$TEST_APP_BASE_DIR" && ./gradlew assembleDebug -PappSrc=$APP_DIR -PappIgnore=$APP_IGNORE)
( cd "$TEST_APP_BASE_DIR" && ./gradlew assembleDebug)

# Copy the Android proxy to the target directory.
#cp "$SCRIPT_BASE_DIR/node-android-proxy.sh" "$TEST_PROXY_TARGETDIR/node"

# Kill the test app if it's running
adb $TARGET shell 'am force-stop testdomain.test.mochamobile'
# Clean the Android log
adb logcat -c

adb $TARGET install -r "$TEST_APP_BASE_DIR/app/build/outputs/apk/debug/app-debug.apk"

# Start the test app without parameter in order to copy the assets to a writable location
adb $TARGET shell 'am start -n testdomain.test.mochamobile/testdomain.test.mochamobile.MainActivity -e "install" "true"' > /dev/null

# Wait for the installation result to appear in the log
adb $TARGET shell 'logcat -b main -v raw -s MochaMobile:V | (grep -q "^COPYASSETS:" && kill -2 $(ps | grep "logcat" | sed -r "s/[[:graph:]]+[ \t]+([0-9]+).*/\1/g"))' < /dev/null

# Return 0 if the test passed, 1 if it failed
parseLogcat() {
  adb $TARGET shell 'logcat -d -b main -v raw -s MochaMobile:V | grep -m 1 "^COPYASSETS:" | sed -e ''s/COPYASSETS:PASS/0/'' -e ''s/COPYASSETS:FAIL/1/'' '
}
RESULT=$(parseLogcat | sed 's/\r$//')

# Echo the raw stdout and stderr
adb $TARGET shell 'logcat -d -b main -v raw -s MochaMobile:V | sed -E ''s/^COPYASSETS:[A-Z]*//'' '

# if [ $RESULT -eq 1 ]; then
#   exit $RESULT
# fi
if [ $RESULT = "0" ]; then
  exit 0
else
  exit 1
fi