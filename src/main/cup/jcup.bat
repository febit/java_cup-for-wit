
@echo off

cd /d %~dp0


%JAVA_HOME%\bin\java -jar java_cup-0.12zqq.jar -interface -nopositions parser.cup

pause

