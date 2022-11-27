#!/bin/sh
cd $(dirname "${0}")
java -XstartOnFirstThread -Xmx4096m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
