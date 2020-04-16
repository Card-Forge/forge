#!/bin/sh
cd $(dirname "${0}")
java -Xmx4096m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
