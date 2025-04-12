@echo off
mkdir bin 2> nul
javac -d bin src\shared\*.java src\model\*.java src\server\*.java src\client\*.java
echo Compiled successfully!
pause