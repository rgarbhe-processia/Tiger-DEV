@echo off
rem ========================================================================================= 
rem
rem                Génération de l'applet CAD dans FPDM_CAD_Applet and deliver it
rem                %1 argument is (int_as, dev_as, int_es, dev_es, ...)
rem ========================================================================================= 

set ANT_HOME=C:\TEMP\apache-ant-1.9.6-bin\apache-ant-1.9.6
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_65


%ANT_HOME%\bin\ant %1

pause
