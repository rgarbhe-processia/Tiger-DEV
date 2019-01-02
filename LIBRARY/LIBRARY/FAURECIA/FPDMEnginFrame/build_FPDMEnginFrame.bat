@echo off
set JAVA_HOME=\\plm-clearcase.sel.fr.corp\JDK\jdk1.7.0_51_x64
set ANT_HOME=\\plm-clearcase.sel.fr.corp\Ant\apache-ant-1.9.4
set GENLOGFILENAME=build_FPDMEnginFrame.log

call %ANT_HOME%\bin\ant -verbose -buildfile build.xml -l %GENLOGFILENAME%

echo Don't forget to put the new compiled jar to the relevant lib directory

pause
