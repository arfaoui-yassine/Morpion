@echo off
call jdk8.bat

:: Clean the 'bin' directory before building
rmdir /s /q bin 2>nul
mkdir bin

:: Compile all Java files
javac -d bin -cp . src\shared\*.java src\model\*.java src\server\*.java src\client\*.java

:: Check if compilation succeeded
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

:: Generate RMI stubs for both server classes
rmic -d bin -classpath bin server.MorpionServer server.GameRoom

echo Build complete with JDK 8
dir bin\server\*.class
pause