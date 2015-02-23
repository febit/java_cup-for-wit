
@echo off

cd /d %~dp0

rem set CLPATH=%JAVA_HOME%\lib\classes.zip;%JCUP_HOME%\java-cup-11a.jar

%JAVA_HOME%\bin\java -Dfile.encoding=UTF-8 -jar java_cup_webit-20150227.jar -destdir ../java/java_cup/core -destresdir ../resources/java_cup/core Parser.cup

pause

