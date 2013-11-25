@echo off
REM Please adjust the paths JFLEX_HOME and JAVA_HOME to suit your needs
REM (please do not add a trailing backslash)

REM set JFLEX_HOME=C:\JFLEX

REM only needed for JDK 1.1.x:
REM set JAVA_HOME=C:\JAVA


REM ------------------------------------------------------------------- 

cd /d %~dp0

set CLPATH=%JAVA_HOME%\lib\classes.zip;%JFLEX_HOME%\lib\JFlex.jar

REM for JDK 1.1.x
%JAVA_HOME%\bin\java -classpath %CLPATH% JFlex.Main ./Lexer.jflex

REM for JDK 1.2
rem java -Xmx128m -jar %JFLEX_HOME%\lib\JFlex.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
