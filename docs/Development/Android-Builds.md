In order to build and sign the android release, you will need the `forge.keystore` file (which is not present in the repository).  This file will need to be placed in the `forge-gui-android` folder.  This file should **never** be committed to the repository.

In preparation for the android release, update the version recorded in the following files:

```
forge-gui-android/pom.xml
forge-gui-ios/pom.xml
forge-gui-mobile/src/forge/Forge.java
```

In the first two, you're looking for `alpha-version` (~line 10 in both files) and setting the string accordingly.
In the last one, you're looking for the declaration of `CURRENT_VERSION` (~line 37) and setting it to the same value as the previous two files.

Commit the changes to these three files to the repository with an appropriately descriptive commit message.

A script such as the following will compile and sign the android build:

```
export ANDROID_HOME=/opt/android-sdk/
export _JAVA_OPTIONS="-Xmx2g"
mvn -U -B clean \\
    -P android-release-build,android-release-sign \\
    install \\
    -Dsign.keystore=forge.keystore \\
    -Dsign.alias=Forge \\
    -Dsign.storepass=${FORGE_STOREPASS} \\
    -Dsign.keypass=${FORGE_KEYPASS}
```

Once the above build has successfully completed and passed any desired testing, the following will build, sign, and publish the build:

```
export ANDROID_HOME=/opt/android-sdk/
export _JAVA_OPTIONS="-Xmx2g"
mvn -U -B clean \\
    -P android-release-build,android-release-sign,android-release-upload \\
    install \\
    -Dsign.keystore=forge.keystore \\
    -Dsign.alias=Forge \\
    -Dsign.storepass=${FORGE_STOREPASS} \\
    -Dsign.keypass=${FORGE_KEYPASS} \\
    -Dcardforge.user=${FORGE_FTP_USER} \\
    -Dcardforge.pass=${FORGE_FTP_PASS}
```
