#!/bin/sh
cd $(dirname "${0}")
java -Xmx4096m $mandatory.java.args$ -jar $project.build.finalName$
