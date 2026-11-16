@echo off
setlocal
cd /d "%~dp0"

set "JAVAC=javac.exe"
set "JARTOOL=jar.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JARTOOL=%JAVA_HOME%\bin\jar.exe"

where "%JAVAC%" >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac was not found. Please install JDK 8+ and set JAVA_HOME.
    pause
    exit /b 1
)

if not exist "lib\jlayer-1.0.1.jar" (
    echo [ERROR] Missing lib\jlayer-1.0.1.jar.
    pause
    exit /b 1
)

echo [1/3] Cleaning old build output...
if exist "classes" rmdir /s /q "classes"
mkdir "classes"

echo [2/3] Compiling Java sources...
set "SOURCES=sources.tmp"
(
    echo src\com\drawstudio\DrawStudioApp.java
    echo src\com\drawstudio\DrawingCanvas.java
    echo src\com\drawstudio\DrawShape.java
    echo src\com\drawstudio\Icons.java
    echo src\com\drawstudio\MainFrame.java
    echo src\com\drawstudio\MusicPlayer.java
    echo src\com\drawstudio\TextDialog.java
    echo src\com\drawstudio\Theme.java
) > "%SOURCES%"
"%JAVAC%" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -Xlint:-deprecation -cp "lib\jlayer-1.0.1.jar" -d "classes" "@%SOURCES%"
set "RESULT=%ERRORLEVEL%"
del "%SOURCES%" >nul 2>nul
if not "%RESULT%"=="0" (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)
if exist "src\com\drawstudio\mascot.png" (
    copy /Y "src\com\drawstudio\mascot.png" "classes\com\drawstudio\mascot.png" >nul
)

echo [3/3] Packaging DrawStudio.jar...
(
    echo Manifest-Version: 1.0
    echo Main-Class: com.drawstudio.DrawStudioApp
    echo Class-Path: lib/jlayer-1.0.1.jar
    echo.
) > "manifest.tmp"
if exist "DrawStudio.jar" del "DrawStudio.jar"
"%JARTOOL%" cfm "DrawStudio.jar" "manifest.tmp" -C "classes" .
set "RESULT=%ERRORLEVEL%"
del "manifest.tmp" >nul 2>nul
if not "%RESULT%"=="0" (
    echo [ERROR] Packaging failed.
    pause
    exit /b 1
)

echo.
echo Build complete. Double-click run.bat to start.
pause
exit /b 0
