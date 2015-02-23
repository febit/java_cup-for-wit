@echo off

cd /d %~dp0

%JAVA_HOME%\bin\java -Dfile.encoding=UTF-8 -jar %JFLEX_HOME%\lib\jflex.jar -d ../java/java_cup/core Lexer.jflex

pause
