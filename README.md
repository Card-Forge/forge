# Forge

Gitlab repo is found [here](https://git.cardforge.org/core-developers/forge).

Dev instructions here: [Getting Started](https://www.slightlymagic.net/wiki/Forge:How_to_Get_Started_Developing_Forge) (Somewhat outdated)

Discord channel [here](https://discordapp.com/channels/267367946135928833/267742313390931968)

# Requirements / Tools

- Java IDE such as IntelliJ or Eclipse
- Java JDK 8 or later
- Git
- Git client (optional)
- Maven
- Gitlab account
- Libgdx (optional: familiarity with this library is helpful for mobile platform development)
- Android SDK (optional: for Android releases)
- RoboVM (optional: for iOS releases) (TBD: Current status of support by libgdx)

# Project Quick Setup

- Log in to gitlab with your user account and fork the project.

- Clone your forked project to your local machine

- Go to the project location on your machine.  Run Maven to download all dependencies and build a snapshot.  Example for Windows & Linux: `mvn -U -B clean -P windows-linux install`

# Eclipse

So, you are glutton for punishment?  Eclipse support has dodgy and, in the case of Android, may require loading tools that are no longer supported.

## Project Setup

- Follow the instructions for cloning from Gitlab.  You'll need a Gitlab account setup and an SSH key defined.  

  If you are on a Windows machine you can use Putty with TortoiseGit.  Run puttygen.exe to generate the key -- save the private key and export
  the OpenSSH public key.  If you just leave the dialog open, you can copy and paste the key from it to your Gitlab profile under
  "SSH keys".  Run pageant.exe and add the private key generated earlier.  TortoiseGit will use this for accessing Gitlab.
   
- Fork the Forge git repo to your Gitlab account.

- Clone your forked repo to your local machine.

- Make sure the Java SDK is installed -- not just the JRE.  Java 8 or newer required.  At the time of this writing, JDK 11 works as expected.

- Install Eclipse 2018-12 or later for Java.  Launch it.

- Create a workspace.  Go to the workbench.  Right-click inside of Package Explorer > Import... > Maven > Existing Maven Projects > Navigate to root path of the local forge repo and
  import everything.
  
- Let Eclipse run through building the project.  You may be prompted for resolving any missing Maven plugins -- accept the ones offered.

## Project Launch

### Desktop

- Right-click on forge-gui-desktop > Run As... > Java Application > "Main - forge.view" > Proceed

### Mobile (Desktop dev)

- Right-click on forge-gui-mobile-dev > Run As... > Java Application > "Main - forge.app" > Proceed

## Eclipse / Android SDK Integration

Google no longer supports Android SDK releases for Eclipse.  That said it is still possible to build and debug Android platforms.  Beware ye who enter here!

### Android SDK

Reference SO for obtaining a specific release: https://stackoverflow.com/questions/27043522/where-can-i-download-an-older-version-of-the-android-sdk

#### Windows 

Download the following archived version of the Android SDK: http://dl-ssl.google.com/android/repository/tools_r25.2.3-windows.zip. Install it somewhere on your machine.

#### Linux / Mac OSX

TBD

### Android Plugin for Eclipse

Google's last plugin release does not work completely with target's running Android 7.0 or later.  Download the ADT-24.2.0-20160729 plugin 
from: https://github.com/khaledev/ADT/releases  

In Eclipse go to: Help > Install New Software... > Add > Name: ADT Update, Click on the "Archive:" button and navigate to the downloaded .zip file.  Install everything.

### Android Platform

In Eclipse, go to Window > Android SDK Manager.  Install the following options / versions:

- Android SDK Build-tools 26.0.1
- Android 7.1.1 (API 25) SDK Platform
- Android Support Library 23.2.1
- Google USB Driver 11

### Android Build

The Eclipse plug-ins do NOT support building things for Android.  They do however allow you to use the debugger so you can still set breakpoints and trace
things out.

Right-click on the forge-gui-android project.  Run as.. > Maven build.

On the Main tab, set Goals: install, Profiles: android-debug
On the Environment tab, you may need to define the variable ANDROID_HOME with the value containing the path to your Android SDK installation.  For example,
Variable: ANDROID_HOME, Value: D:\projects\sdk\android-sdk-windows.

You should now be able to "run" the forge-gui-android Maven build.  This may take a few minutes.  If everything worked, you should see "BUILD SUCCESS" in the Console View.

Assuming you got this far, you should have an Android forge-android-<version>.apk in the forge-gui-android/target path.

### Android Debugging

Deploy the .apk file created above to your target Android device and launch it.  In Eclipse, launch the DDMS.  Window > Perspective > Open Perspective > DDMS.  You should see the 
forge app in the list.  Click on the debug button and a green debug button should appear next to the app's name.  You can now set breakpoints and step through the source code.

# IntelliJ

TBD  

# Card Scripting

Visit [this page](https://www.slightlymagic.net/wiki/Forge_API) for information on scripting.

Card scripting resources are found in the forge-gui/res/ path.

# General Notes

## Project Hierarchy

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

### forge-ai

### forge-core

### forge-game

### forge-gui

The forge-gui project includes the scripting resource definitions in the res/ path.

### forge-gui-android

Libgdx-based backend targeting Android.  Requires Android SDK and relies on forge-gui-mobile for GUI logic.

### forge-gui-desktop

Java Swing based GUI targeting desktop machines.  

Screen layout and game logic revolving around the GUI is found here.  For example, the overlay arrows (when enabled) that indicate attackers and blockers, or the targets of the stack are defined and drawn by this.

### forge-gui-ios

Libgdx-based backend targeting iOS.  Relies on forge-gui-mobile for GUI logic.

### forge-gui-mobile

Mobile GUI game logic utilizing [libgdx](https://libgdx.badlogicgames.com/) library.  Screen layout and game logic revolving around the GUI for the mobile platforms is found here.

### forge-gui-mobile-dev

Libgdx backend for desktop development for mobile backends.  Utilizes LWJGL.  Relies on forge-gui-mobile for GUI logic.
 
