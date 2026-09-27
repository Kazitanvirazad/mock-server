#!/bin/bash

echo "MockServer Packaging"

echo "Building Maven project..."
mvn clean
mvn package -DskipTests || exit 1

echo "Copying jar..."

mkdir -p target/libs
cp target/MockServer.jar target/libs/MockServer.jar

mkdir -p target/installer
mkdir -p target/installer-work

echo "Packaging ARM MacOS DMG..."

jpackage \
 --name MockServer \
 --vendor "Kazi Tanvir Azad" \
 --verbose \
 --app-version 1.2 \
 --main-jar MockServer.jar \
 --main-class com.server.app.Launcher \
 --icon appicon.icns \
 --input target/libs/ \
 --temp target/installer-work/ \
 --dest target/installer/ \
 --type dmg \
 --mac-package-name MockServer

echo "Packaging script execution complete!"
