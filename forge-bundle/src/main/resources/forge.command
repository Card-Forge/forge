#!/bin/sh
cd "`dirname \"$0\"`"
java -Xmx1024m -cp res;${project.build.finalName}-jar-with-dependencies.jar forge.view.Main