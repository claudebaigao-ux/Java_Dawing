@echo off
setlocal
cd /d "%~dp0"

set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA=%JAVA_HOME%\bin\javaw.exe"
if defined JAVA if exist "%JAVA%" goto java_found
where javaw.exe >nul 2>nul
if not errorlevel 1 (
    set "JAVA=javaw.exe"
    goto java_found
)
where java.exe >nul 2>nul
if not errorlevel 1 (
    set "JAVA=java.exe"
    goto java_found
)

echo [ERROR] Java was not found. Please install JDK/JRE 8+ and set JAVA_HOME.
pause
exit /b 1

:java_found
set "AUTOPLAY="
if defined DRAWSTUDIO_AUTOPLAY set "AUTOPLAY=-Ddrawstudio.autoplay=%DRAWSTUDIO_AUTOPLAY%"
if exist "DrawStudio.jar" (
    start "" "%JAVA%" -Dfile.encoding=UTF-8 %AUTOPLAY% -jar "DrawStudio.jar"
    exit /b 0
)

if exist "classes\com\drawstudio\DrawStudioApp.class" (
    if not exist "lib\jlayer-1.0.1.jar" (
        echo [ERROR] Missing lib\jlayer-1.0.1.jar.
        pause
        exit /b 1
    )
    start "" "%JAVA%" -Dfile.encoding=UTF-8 %AUTOPLAY% -cp "classes;lib\jlayer-1.0.1.jar" com.drawstudio.DrawStudioApp
    exit /b 0
)

echo [ERROR] Program is not built yet. Please run build.bat first.
pause
exit /b 1
