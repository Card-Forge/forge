cd ../forge-core
call mvn clean install
cd ../forge-net
call mvn clean install
cd ../forge-ai
call mvn clean install
cd ../forge-game
call mvn clean install
cd ../forge-gui
call mvn clean install
cd ../forge-gui-mobile
call mvn clean install
cd ../forge-gui-android
call mvn -U -B clean -P android-release-build,android-release-sign,android-release-upload install -Dsign.keystore=forge.keystore -Dsign.alias=Forge -Dsign.storepass=forge72 -Dsign.keypass=forge72 -Dcardforge.user=drdev@cardforge.org -Dcardforge.pass=W%GdM]_o7@wEUEJIvs
pause