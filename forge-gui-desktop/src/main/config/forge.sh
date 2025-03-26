#!/bin/sh
cd $(dirname "${0}")
java $mandatory.java.args$ $addopen.java.args$ -jar $project.build.finalName$
