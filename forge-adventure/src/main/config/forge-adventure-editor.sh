#!/bin/sh
cd $(dirname "${0}")
java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
$java -Xmx4096m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
