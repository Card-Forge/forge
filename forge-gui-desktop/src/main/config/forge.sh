#!/bin/sh
set -euo pipefail

cd $(dirname "${0}")

if [ ! -e res ]; then
  ln -s ../../forge-gui/res res
fi;

JAVA_EXE="java"
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_EXE="${JAVA_HOME}/bin/java"
fi

# The user can specify an alternate project jar in the first argument
if [ -n "${1:-}" ]; then
  if [ -e "$1" ]; then
    project_jar="$1"
  else
    echo "Error: Specified project jar '$1' does not exist." >&2
    exit 1
  fi
else
  project_jar="$project.build.finalName$"
fi

${JAVA_EXE} $mandatory.java.args$ -jar ${project_jar} "$@"
