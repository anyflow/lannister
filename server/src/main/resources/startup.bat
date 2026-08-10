@echo off
setlocal
set BASE_DIR=%~dp0
set BASE_DIR=%BASE_DIR:~0,-1%

if not defined JAVA_HOME (
    echo JAVA_HOME is not defined. Please set JAVA_HOME environment variable.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -cp "%BASE_DIR%lib\*" net.anyflow.lannister.Application %*
