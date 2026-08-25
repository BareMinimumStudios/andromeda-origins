@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat --version
if errorlevel 1 goto :error
call gradlew.bat build
if errorlevel 1 goto :error
echo.
echo Build complete. Check build\libs\
pause
exit /b 0
:error
echo.
echo Build failed. Make sure IntelliJ/Gradle is using Java 21.
pause
exit /b 1
