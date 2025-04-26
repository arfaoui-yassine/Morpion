@echo off
call jdk8.bat
java -cp bin -Djava.rmi.server.codebase=file:bin/ server.MorpionServer
pause