@echo off
rem ========================================================================================= 
rem
rem Jar signer wrapper
rem
rem ========================================================================================= 

REM Use the latest JDK if possible, or at least the version of the JRE which execute the jar
set JAVA_HOME=\\plm-clearcase.sel.fr.corp\JDK\jdk1.7.0_51_x86
set KEYSTORE=\\plm-clearcase.sel.fr.corp\TOOLS\Certificates\Faurecia.jks

REM set CURRENTDIR=%~dp0
set PATH=%JAVA_HOME%\bin;%PATH%

set JARPATH=%1

REM Output to a different jar path/name
REM set SIGNEDJAR=%JARPATH:_unsigned=%
REM del %SIGNEDJAR%

set PARAM=
set PARAM=%PARAM% -keystore "%KEYSTORE%"
set PARAM=%PARAM% -storepass faurecia
set PARAM=%PARAM% -storetype JKS
set PARAM=%PARAM% -sigfile faurecia
REM set PARAM=%PARAM% -signedjar "%SIGNEDJAR%"
set PARAM=%PARAM% -verbose
REM set PARAM=%PARAM% -tsa "http://"
set PARAM=%PARAM% %JARPATH%
set PARAM=%PARAM% "eit-it-engineering it-plm (internal ca 2)"

echo %JAVA_HOME%\bin\jarsigner %PARAM%
%JAVA_HOME%\bin\jarsigner %PARAM%
