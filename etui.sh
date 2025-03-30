set -e

if [ -e setEnv.sh ]; then
  source setEnv.sh
fi

if [ -z "$JAVA_HOME" ]; then
echo The JAVA_HOME environment variable is not defined correctly,
echo this environment variable is needed to run this program.
exit 1
fi

export FROM_SOURCE=${FROM_SOURCE:-false}
export MAVEN_ARGS=${MAVEN_ARGS:-"-q"}
if $FROM_SOURCE; then
  echo Running Etui from source code
  if [ .classpath-src -ot pom.xml ]; then
    echo Resolving classpath
    ./mvnw $MAVEN_ARGS process-classes -Plocal-source -Dclasspath.output=.classpath-src
  fi
  echo Starting Etui
  "$JAVA_HOME/bin/java" --source 22 --enable-preview -cp @.classpath-src src/main/java/Etui.java "$@"
else
  if [ ! -f target/etui.jar ] || [ ! -f .classpath ]; then
    echo Building Etui
    ./mvnw $MAVEN_ARGS clean package -DskipTests -Plocal-package -Dclasspath.output=.classpath
  fi
  echo Starting Etui
  "$JAVA_HOME/bin/java" --enable-preview -cp @.classpath Etui "$@"
fi



