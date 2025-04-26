

@echo off
call jdk8.bat

:: Compile
javac -d bin src\shared\*.java src\model\*.java src\server\*.java src\client\*.java

:: Generate RMI stubs
rmic -d bin -classpath bin server.MorpionServer

echo Build complete with JDK 8
dir bin\server\MorpionServer_*.class
pause