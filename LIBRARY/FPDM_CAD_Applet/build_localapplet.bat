@echo off
rem ========================================================================================= 
rem
rem                Génération de l'applet CAD dans FPDM_CAD_Applet
rem
rem ========================================================================================= 

set ANT_HOME=C:\TEMP\apache-ant-1.9.6-bin\apache-ant-1.9.6
set JAVA_HOME=C:\Program Files\Java\jdk1.7.0_79

%ANT_HOME%\bin\ant compile

pause
