@echo off

echo Cross-publishing the project ...
call "%~dp0sbt.bat" --no-jrebel %* clean +compile +publish
