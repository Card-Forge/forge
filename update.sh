#!/bin/sh
git pull
mvn -U -T ${1:-8} -DskipTests clean package

