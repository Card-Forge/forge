#!/bin/sh
cd $(dirname "${0}")
java $mandatory.java.args$ -jar $project.build.finalName$
