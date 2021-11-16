# Forge

[Official GitLab repo](https://git.cardforge.org/core-developers/forge).

Dev instructions here: [Getting Started](https://git.cardforge.org/core-developers/forge/-/wikis/(SM-autoconverted)-how-to-get-started-developing-forge) (Somewhat outdated)

Discord channel [here](https://discord.gg/fWfNgCUNRq)

## Requirements / Tools

- you favourite Java IDE (IntelliJ, Eclipse, VSCodium, Emacs, Vi...)
- Java JDK 8 or later (some IDEs such as Eclipse require JDK11+, whereas the Android build currently only works with JDK8)
- Git
- Git client (optional)
- Maven
- Gitlab account
- Libgdx (optional: familiarity with this library is helpful for mobile platform development)
- Android SDK (optional: for Android releases)
- RoboVM (optional: for iOS releases) (TBD: Current status of support by libgdx)

## Project Quick Setup

- Log in to gitlab with your user account and fork the project.

- Clone your forked project to your local machine

- Go to the project location on your machine.  Run Maven to download all dependencies and build a snapshot.  Example for Windows & Linux: `mvn -U -B clean -P windows-linux install`

## Eclipse

Eclipse includes Maven integration so a separate install is not necessary.  For other IDEs, your mileage may vary.

### Project Setup

- Follow the instructions for cloning from Gitlab.  You'll need a Gitlab account setup and an SSH key defined.

  If you are on a Windows machine you can use Putty with TortoiseGit for SSH keys.  Run puttygen.exe to generate the key -- save the private key and export
  the OpenSSH public key.  If you just leave the dialog open, you can copy and paste the key from it to your Gitlab profile under
  "SSH keys".  Run pageant.exe and add the private key generated earlier.  TortoiseGit will use this for accessing Gitlab.

- Fork the Forge git repo to your Gitlab account.

- Clone your forked repo to your local machine.

- Make sure the Java SDK is installed -- not just the JRE.  Java 8 or newer required.  If you execute `java -version` at the shell or command prompt, it should report version 1.8 or later.

- Install Eclipse 2018-12 or later for Java.  Launch it.

- Create a workspace.  Go to the workbench.  Right-click inside of Package Explorer > Import... > Maven > Existing Maven Projects > Navigate to root path of the local forge repo and
  ensure everything is checked > Finish.

- Let Eclipse run through building the project.  You may be prompted for resolving any missing Maven plugins -- accept the ones offered.  You may see errors appear in the "Problems" tab.  These should
  be automatically resolved as plug-ins are installed and Eclipse continues the build process.  If this is the first time for some plug-in installs, Eclipse may prompt you to restart.  Do so.  Be patient
  for this first time through.

- Once everything builds, all errors should disappear.  You can now advance to Project launch.

### Project Launch

#### Desktop

This is the standard configuration used for releasing to Windows / Linux / MacOS.

- Right-click on forge-gui-desktop > Run As... > Java Application > "Main - forge.view" > Ok

- The familiar Forge splash screen, etc. should appear.  Enjoy!

#### Mobile (Desktop dev)

This is the configuration used for doing mobile development using the Windows / Linux / MacOS front-end.  Knowledge of libgdx is helpful here.

- Right-click on forge-gui-mobile-dev > Run As... > Java Application > "Main - forge.app" > Ok.

- A view similar to a mobile phone should appear.  Enjoy!

### Eclipse / Android SDK Integration

Google no longer supports Android SDK releases for Eclipse.  That said, it is still possible to build and debug Android platforms.

#### Android SDK

Reference SO for obtaining a specific release: https://stackoverflow.com/questions/27043522/where-can-i-download-an-older-version-of-the-android-sdk

##### Windows

Download the following archived version of the Android SDK: http://dl-ssl.google.com/android/repository/tools_r25.2.3-windows.zip. Install it somewhere on your machine.  This is referenced
in the following instructions as your 'Android SDK Install' path.

##### Linux / Mac OSX

TBD

#### Android Plugin for Eclipse

Google's last plugin release does not work completely with target's running Android 7.0 or later.  Download the ADT-24.2.0-20160729.zip plugin
from: https://github.com/khaledev/ADT/releases

In Eclipse go to: Help > Install New Software... > Add > Name: ADT Update, Click on the "Archive:" button and navigate to the downloaded ADT-24.2.0-20160729.zip file > Add.  Install all "Developer Tools".  Eclipse
should restart and prompt you to run the SDK Manager.  Launch it and continue to the next steps below.

#### Android Platform

In Eclipse, if the SDK Manager is not already running, go to Window > Android SDK Manager.  Install the following options / versions:

- Android SDK Build-tools 26.0.1
- Android 8.0.0 (API 26) SDK Platform
- Google USB Driver (in case your phone is not detected by ADB)

Note that this will populate additional tools in the Android SDK install path extracted above.

#### Proguard update

The Proguard included with the Android SDK Build-tools is outdated and does not work with Java 1.8.  Download Proguard 6.0.3 or later (last tested with 7.0.1) from https://github.com/Guardsquare/proguard
- Go to the Android SDK install path.  Rename the tools/proguard/ path to tools/proguard-4.7/.

- Extract your Proguard version to the Android SDK install path under tools/.  You will need to either rename the dir proguard-<your-version> to proguard/ or, if your filesystem supports it, use a symbolic link (the later is highly recommended), such as `ln -s proguard proguard-<your-version>`.

#### Android Build

The Eclipse plug-ins do NOT support building things for Android.  They do however allow you to use the debugger so you can still set breakpoints and trace
things out.  The steps below show how to generate a debug Android build.

1) Create a Maven build for the forge top-level project.  Right-click on the forge project.  Run as.. > Maven build...
   - On the Main tab, set Goals: clean install

2) Run forge Maven build.  If everything built, you should see "BUILD SUCCESS" in the Console View.

3) Right-click on the forge-gui-android project.  Run as.. > Maven build...

   - On the Main tab, set Goals: install, Profiles: android-debug
   - On the Environment tab, you may need to define the variable ANDROID_HOME with the value containing the path to your Android SDK installation.  For example, Variable: ANDROID_HOME, Value: Your Android SDK install path here.

4) Run the forge-gui-android Maven build.  This may take a few minutes.  If everything worked, you should see "BUILD SUCCESS" in the Console View.

Assuming you got this far, you should have an Android forge-android-[version].apk in the forge-gui-android/target path.

#### Android Deploy

You'll need to have the Android SDK install path platform-tools/ path in your command search path to easily deploy builds.

- Open a command prompt.  Navigate to the forge-gui-android/target/ path.

- Connect your Android device to your dev machine.

- Ensure the device is visible using `adb devices`

- Remove the old Forge install if present: `adb uninstall forge.app`

- Install the new apk: `adb install forge-android-[version].apk`

#### Android Debugging

Assuming the apk is installed, launch it from the device.

In Eclipse, launch the DDMS.  Window > Perspective > Open Perspective > Other... > DDMS.  You should see the forge app in the list.  Highlight the app, click on the green debug button and a
green debug button should appear next to the app's name.  You can now set breakpoints and step through the source code.

### Windows / Linux SNAPSHOT build

SNAPSHOT builds can be built via the Maven integration in Eclipse.

1) Create a Maven build for the forge top-level project.  Right-click on the forge project.  Run as.. > Maven build...
   - On the Main tab, set Goals: clean install, set Profiles: windows-linux

2) Run forge Maven build.  If everything built, you should see "BUILD SUCCESS" in the Console View.

The resulting snapshot will be found at: forge-gui-desktop/target/forge-gui-desktop-[version]-SNAPSHOT

## IntelliJ

Quick start guide for [setting up the Forge project within IntelliJ](https://git.cardforge.org/core-developers/forge/-/wikis/Development/intellij-setup).

## Card Scripting

Visit [this page](https://git.cardforge.org/core-developers/forge/-/wikis/Card-scripting-API/Card-scripting-API) for information on scripting.

Card scripting resources are found in the forge-gui/res/ path.

## General Notes

### Project Hierarchy

Forge is divided into 4 primary projects with additional projects that target specific platform releases.  The primary projects are:

- forge-ai
- forge-core
- forge-game
- forge-gui

The platform-specific projects are:

- forge-gui-android
- forge-gui-desktop
- forge-gui-ios
- forge-gui-mobile
- forge-gui-mobile-dev

#### forge-ai

#### forge-core

#### forge-game

#### forge-gui

The forge-gui project includes the scripting resource definitions in the res/ path.

#### forge-gui-android

Libgdx-based backend targeting Android.  Requires Android SDK and relies on forge-gui-mobile for GUI logic.

#### forge-gui-desktop

Java Swing based GUI targeting desktop machines.

Screen layout and game logic revolving around the GUI is found here.  For example, the overlay arrows (when enabled) that indicate attackers and blockers, or the targets of the stack are defined and drawn by this.

#### forge-gui-ios

Libgdx-based backend targeting iOS.  Relies on forge-gui-mobile for GUI logic.

#### forge-gui-mobile

Mobile GUI game logic utilizing [libgdx](https://libgdx.badlogicgames.com/) library.  Screen layout and game logic revolving around the GUI for the mobile platforms is found here.

#### forge-gui-mobile-dev

Libgdx backend for desktop development for mobile backends.  Utilizes LWJGL.  Relies on forge-gui-mobile for GUI logic.
