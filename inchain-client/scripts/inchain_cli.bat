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

rem 设置CLASSPATH
set CLASSPATH=%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%CLASSPATH%;%SERVER_HOME%\conf
set CLASSPATH=%CLASSPATH%;%SERVER_HOME%\inchain-1.0.jar
for /R %SERVER_HOME%\lib %%f in (*.jar) do echo set CLASSPATH=%%CLASSPATH%%;%%f >>lib.bat
call lib.bat
del lib.bat

if ""%1"" == """" goto help

goto doExec


:doExec
rem %_EXECJAVA%  %_JAVA_OPTS%  -classpath  "%CLASSPATH%"  %_MAINCLASS% %*
%_EXECJAVA%  -classpath  "%CLASSPATH%"  %_MAINCLASS% %*

:end