#!/bin/sh
cd $(dirname "${0}")
java -XstartOnFirstThread -Xmx4096m $mandatory.java.args$ -jar $project.build.finalName$
