@echo off
setlocal

if exist setEnv.cmd call setEnv.cmd

if "%JAVA_HOME%" == "" (
    echo The JAVA_HOME environment variable is not defined correctly,
    echo this environment variable is needed to run this program.
    exit /B 1
)

if "%FROM_SOURCE%" == "" (set FROM_SOURCE=false)
if "%MAVEN_ARGS%" == "" (set MAVEN_ARGS=-q)

if not exist target\etui.jar (set FORCE_BUILD=true)
if not exist .classpath (set FORCE_BUILD=true)
if not exist .classpath-src (set NO_SRC_CLASSPATH=true)

if not "%FROM_SOURCE%" == "false" (
    echo Running Etui from source code
    if not "%NO_SRC_CLASSPATH%" == "" (
        echo Resolving classpath
        call mvnw.cmd %MAVEN_ARGS% process-classes -Plocal-source -Dclasspath.output=.classpath-src
    )
    echo Starting Etui
    "%JAVA_HOME%\bin\java" --source 22 --enable-preview -cp @.classpath-src src/main/java/Etui.java %*
) else (
    if not "%FORCE_BUILD%" == "" (
        echo Building Etui
        call mvnw.cmd %MAVEN_ARGS% clean package -DskipTests -Plocal-package -Dclasspath.output=.classpath
    )
  echo Starting Etui
  "%JAVA_HOME%\bin\java" --enable-preview -cp @.classpath Etui %*
)

endlocal
