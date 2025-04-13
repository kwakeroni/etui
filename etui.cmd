@echo off
setlocal

if exist setEnv.cmd (
    call setEnv.cmd
) else if exist %~dp0setEnv.cmd (
    call %~dp0setEnv.cmd
)

if "%JAVA_HOME%" == "" (
    echo The JAVA_HOME environment variable is not defined correctly,
    echo this environment variable is needed to run this program.
    exit /B 1
)

if "%FROM_SOURCE%" == "" (set FROM_SOURCE=false)
if "%MAVEN_ARGS%" == "" (set MAVEN_ARGS=-q)
if /I "%SHOW_CONSOLE%" == "true" (
  set JAVA_BIN=%JAVA_HOME%\bin\java
) else (
  set JAVA_BIN=%JAVA_HOME%\bin\javaw
)

if not exist %~dp0target\etui.jar (set FORCE_BUILD=true)
if not exist %~dp0.classpath (set FORCE_BUILD=true)
if not exist %~dp0.classpath-src (set NO_SRC_CLASSPATH=true)

if /I not "%FROM_SOURCE%" == "false" (
    echo Running Etui from source code
    if not "%NO_SRC_CLASSPATH%" == "" (
        echo Resolving classpath
        call %~dp0mvnw.cmd %MAVEN_ARGS% -f %~dp0pom.xml process-classes -Plocal-source -Dclasspath.output=%~dp0.classpath-src
    )
    echo Starting Etui
    start "" "%JAVA_BIN%" --source 22 -splash:%~dp0src\main\resources\splash.png -cp @%~dp0.classpath-src %~dp0src/main/java/Etui.java %*
) else (
    if not "%FORCE_BUILD%" == "" (
        echo Building Etui
        call %~dp0mvnw.cmd %MAVEN_ARGS% -f %~dp0pom.xml clean package -DskipTests -Plocal-package -Dclasspath.output=%~dp0.classpath
    )
  echo Starting Etui
  start "" "%JAVA_BIN%" -splash:%~dp0src\main\resources\splash.png -cp @%~dp0.classpath Etui %*
)
endlocal
