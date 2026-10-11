@echo off
where java >nul 2>nul || (echo Java JDK 25 fehlt. & exit /b 1)
where gradle >nul 2>nul || (echo Gradle fehlt. & exit /b 1)
gradle clean build
if errorlevel 1 exit /b 1
echo Plugin-JAR in build\libs\ erstellt.
