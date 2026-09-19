@echo off
rem CLI build entry: clears broken JAVA_HOME so Gradle uses JDK 21 from PATH.
set "JAVA_HOME="
call "%~dp0gradlew.bat" %*
