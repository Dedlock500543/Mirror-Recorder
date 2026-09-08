@echo off
chcp 65001 >nul
rem Проверяет, что сборка пойдёт на Java 8 (Temurin 1.8.0_502 и совместимые).
setlocal enabledelayedexpansion

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    )
)
if not defined JAVA_EXE set "JAVA_EXE=java"

"%JAVA_EXE%" -version 2>"%TEMP%\mirror_java_ver.txt"
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена. Установите JDK 8 или задайте JAVA_HOME.
    exit /b 1
)

set "JAVA_LINE="
for /f "usebackq delims=" %%L in ("%TEMP%\mirror_java_ver.txt") do (
    if not defined JAVA_LINE set "JAVA_LINE=%%L"
)

echo  Java: !JAVA_LINE!
echo !JAVA_LINE! | find "1.8" >nul
if errorlevel 1 (
    echo [ОШИБКА] Нужна Java 8. ForgeGradle 3 и Gradle 5.6.4 не работают на новых JDK.
    echo          Задайте JAVA_HOME на JDK 8, например:
    echo          set "JAVA_HOME=<path-to-jdk8>"
    exit /b 1
)

exit /b 0
