
@echo off
setlocal
where mvn >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Maven (mvn) not found in PATH. Add "C:\Program Files\Maven\bin" to PATH.
  pause
  exit /b 1
)
mvn -version
mvn clean package
if errorlevel 1 (
  echo Build failed.
  pause
  exit /b 1
)
echo JAR built at: target\MaxInvPlugin-2.3.jar
pause
