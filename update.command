#!/bin/sh
git pull
mvn -U -T 8 -DskipTests clean package

