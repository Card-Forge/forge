#!/bin/sh

cd `dirname "$0"`

if ! zip --version | grep Info-ZIP >/dev/null; then
  echo >&2 `basename "$0"`: error: this only works with Info-ZIP.
  exit -1
fi

if [ -r cardsfolder.zip ]; then
    echo Updating cardsfolder.zip...
else
    echo Creating cardsfolder.zip...
fi

find . -name '*.txt' -print | zip -1@ouX cardsfolder.zip
