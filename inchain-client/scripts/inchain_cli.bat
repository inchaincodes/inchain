@echo off
if "%OS%" == "Windows_NT" setlocal

rem --------------------------------------------------------------------------
rem Start script for the Reaper Server
rem
rem $Id: inchain.bat,v 1.0 2016/11/17 ln$
rem ---------------------------------------------------------------------------


rem 设置java运行环境
rem Make sure prerequisite environment variables are set
if not "%JAVA_HOME%" == "" goto gotJavaHome
echo The JAVA_HOME environment variable is not defined
echo This environment variable is needed to run this program
goto end
:gotJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\javaw.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\jdb.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\javac.exe" goto noJavaHome
goto okJavaHome
:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okJavaHome

rem 打印JAVA_HOME变量
rem echo JAVA_HOME=%JAVA_HOME%

rem 设置SERVER_HOME变量
rem Guess SERVER_HOME if not defined
if not "%SERVER_HOME%" == "" goto goReaperHome
set SERVER_HOME=.

if exist "%SERVER_HOME%\bin\inchain.bat" goto okReaperHome
set SERVER_HOME=..

if exist "%SERVER_HOME%\bin\inchain.bat" goto okReaperHome
set SERVER_HOME=%~dp0
cd %SERVER_HOME%
cd ..
set SERVER_HOME=%cd%

:goReaperHome
echo %SERVER_HOME%
if exist "%SERVER_HOME%\bin\inchain.bat" goto okReaperHome
echo The SERVER_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okReaperHome

rem 打印SERVER_HOME变量
rem echo SERVER_HOME=%SERVER_HOME%


set _EXECJAVA="%JAVA_HOME%\bin\java"
set _MAINCLASS=org.inchain.rpc.RPCClient

set CLASSPATH=%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%CLASSPATH%;%SERVER_HOME%\conf
set CLASSPATH=%CLASSPATH%;%SERVER_HOME%\lib\inchain-core-0.1.jar;%SERVER_HOME%\lib\jackson-core-asl-1.9.4.jar;%SERVER_HOME%\lib\jackson-mapper-asl-1.9.4.jar;%SERVER_HOME%\lib\jettison-1.3.7.jar

if ""%1"" == """" goto help

goto doExec


:doExec
rem %_EXECJAVA%  %_JAVA_OPTS%  -classpath  "%CLASSPATH%"  %_MAINCLASS% %1
%_EXECJAVA%  -classpath  "%CLASSPATH%"  %_MAINCLASS% %1

:end