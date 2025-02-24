# Contributing to Forge

[Official repo](https://github.com/Card-Forge/forge.git).

Dev instructions here: [Getting Started](https://github.com/Card-Forge/forge/wiki) (Somewhat outdated)

## Requirements / Tools

- you favourite Java IDE (IntelliJ, Eclipse, VSCodium, Emacs, Vi...)
- Java JDK 17 or later
- Git
- Git client (optional)
- Maven
- GitHub account
- Libgdx (optional: familiarity with this library is helpful for mobile platform development)
- Android SDK (optional: for Android releases)
- RoboVM (optional: for iOS releases) (TBD: Current status of support by libgdx)

## Project Quick Setup

- Login into GitHub with your user account and fork the project.

- Clone your forked project to your local machine

- Go to the project location on your machine.  Run Maven to download all dependencies and build a snapshot.  Example for Windows & Linux: `mvn -U -B clean -P windows-linux install`

## IntelliJ

IntelliJ is the recommended IDE for Forge development. Quick start guide for [setting up the Forge project within IntelliJ](https://github.com/Card-Forge/forge/wiki/IntelliJ-setup).


## Eclipse

Eclipse includes Maven integration so a separate install is not necessary.  For other IDEs, your mileage may vary.
At this time, Eclipse is not the recommended IDE for Forge development.

### Project Setup

- Follow the instructions for cloning from GitHub.  You'll need to setup an account and your SSH key.

  If you are on a Windows machine you can use Putty with TortoiseGit for SSH keys.  Run puttygen.exe to generate the key -- save the private key and export
  the OpenSSH public key.  If you just leave the dialog open, you can copy and paste the key from it to your GitHub profile under
  "SSH keys".  Run pageant.exe and add the private key generated earlier.  TortoiseGit will use this for accessing GitHub.

- Fork the Forge git repo to your GitHub account.

- Clone your forked repo to your local machine.

- Make sure the Java SDK is installed -- not just the JRE.  Java 17 or newer required.  If you execute `java -version` at the shell or command prompt, it should report version 17 or later.

- Install Eclipse 2021-12 or later for Java.  Launch it.

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

Google no longer supports Android SDK releases for Eclipse. use IntelliJ.

#### Android SDK

TBD

##### Windows

TBD

##### Linux / Mac OSX

TBD

#### Android Plugin for Eclipse

TBD

#### Android Platform

In Intellij, if the SDK Manager is not already running, go to Tools > Android > Android SDK Manager.  Install the following options / versions:

- Android SDK Build-tools 35.0.0
- Android 15 (API 35) SDK Platform

#### Proguard update

Standalone Proguard 7.6.0 is included with the project (proguard.jar) under forge-gui-android > tools and supports up to Java 23 (latest android uses Java 17).

#### Android Build

TBD

#### Android Deploy

TBD

#### Android Debugging

TBD

### Windows / Linux SNAPSHOT build

SNAPSHOT builds can be built via the Maven integration in Eclipse.

1) Create a Maven build for the forge top-level project.  Right-click on the forge project.  Run as.. > Maven build...
   - On the Main tab, set Goals: clean install, set Profiles: windows-linux

2) Run forge Maven build.  If everything built, you should see "BUILD SUCCESS" in the Console View.

The resulting snapshot will be found at: forge-gui-desktop/target/forge-gui-desktop-[version]-SNAPSHOT

## Card Scripting

Visit [this page](https://github.com/Card-Forge/forge/wiki/Card-scripting-API) for information on scripting.

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
