#!/bin/sh
git pull
mvn -U -T $1 -DskipTests clean package

