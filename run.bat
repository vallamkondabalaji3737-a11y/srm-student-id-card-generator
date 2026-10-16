@echo off
cd /d "%~dp0"
javac Server.java
if errorlevel 1 (
  echo Install a Java Development Kit 8 or newer and make sure javac is on PATH.
  pause
  exit /b 1
)
start "" http://localhost:8080
java Server
pause
