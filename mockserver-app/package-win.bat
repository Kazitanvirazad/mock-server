@echo OFF
TITLE "MockServer Packaging"

echo "Loading signing properties..."
call "%~dp0load-win-signing.bat"

setlocal EnableDelayedExpansion

echo "Packaging Started..."

echo "Deleting existing build directory..."
IF EXIST "target\" rmdir /s /q "target\"

call cd ..
IF EXIST "build\" rmdir /s /q "build\"

echo "Building Maven project..."
call mvn clean
if defined MOCKSERVER_JARSIGN_KEYSTORE (
  echo "JAR signing enabled (MOCKSERVER_JARSIGN_KEYSTORE is set); running mvn verify -Pjar-sign ..."
  call mvn verify -Pjar-sign
) else (
  call mvn package
)
IF ERRORLEVEL 1 EXIT /B 1

call cd mockserver-app

echo "Copying jar..."
IF NOT EXIST target\libs mkdir target\libs
call copy target\MockServer.jar target\libs\MockServer.jar

IF NOT EXIST target\installer mkdir target\installer
IF NOT EXIST target\installer-work mkdir target\installer-work

echo "Packaging Windows MSI..."

call "%JAVA_HOME%\bin\jpackage" ^
 --name MockServer ^
 --vendor "Kazi Tanvir Azad" ^
 --verbose ^
 --app-version 1.2 ^
 --main-jar MockServer.jar ^
 --main-class com.server.app.Launcher ^
 --icon appicon.ico ^
 --input target\libs\ ^
 --temp target\installer-work\ ^
 --dest target\installer\ ^
 --type msi ^
 --win-menu ^
 --win-shortcut ^
 --win-per-user-install ^
 --win-dir-chooser ^
 --win-shortcut-prompt ^
 --win-menu-group 'MockServer' ^
 --runtime-image "%JAVA_HOME%"

echo "Packaging script execution complete!"

echo "Copying msi installer to build directory!"
IF NOT EXIST ..\build mkdir ..\build
call copy target\installer\MockServer-*.msi ..\build\

if defined MOCKSERVER_CODESIGN_PFX (
  echo "Signing MSI (Authenticode)..."
  call "%~dp0sign-win-msi.bat" "%~dp0..\build"
  IF ERRORLEVEL 1 EXIT /B 1
)
