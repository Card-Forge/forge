@echo off
SETLOCAL
cd %~dp0
java -Xmx1024M -cp "%~dp0res";"%~dp0${project.build.finalName}-jar-with-dependencies.jar" forge.view.Main