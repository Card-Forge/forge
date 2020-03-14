#!/bin/sh
cd $(dirname "${0}")
java -Xmx1024m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
