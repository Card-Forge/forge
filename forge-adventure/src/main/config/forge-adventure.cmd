@echo off

pushd %~dp0

java -version 1>nul 2>nul || (
   echo no java installed
   popd
   exit /b 2
)
for /f tokens^=2^ delims^=.-_^+^" %%j in ('java -fullversion 2^>^&1') do set "jver=%%j"

if %jver% GEQ 17 (
  java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -Xmx4096m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
  popd
  exit /b 0
)

if %jver% GEQ 11 (
  java --illegal-access=permit -Xmx4096m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
  popd
  exit /b 0
)

java -Xmx4096m -Dfile.encoding=UTF-8 -jar $project.build.finalName$
popd