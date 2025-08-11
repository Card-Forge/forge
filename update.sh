#!/bin/sh
git pull
mvn -U -T 4 -DskipTests clean package

