@echo off
setlocal
rem Gracefully shut down Lannister MQTT Broker
taskkill /F /IM java.exe
