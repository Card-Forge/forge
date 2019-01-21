# Forge

Gitlab repo is found [here](https://git.cardforge.org/core-developers/forge).

Dev instructions here: [Getting Started](https://www.slightlymagic.net/wiki/Forge:How_to_Get_Started_Developing_Forge) (Somewhat outdated)

Discord channel [here](https://discordapp.com/channels/267367946135928833/267742313390931968)

# Requirements / Tools

- Java IDE such as IntelliJ or Eclipse
- Git
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

## Project Setup

- Follow the instructions for cloning from Gitlab.  You'll need a Gitlab account setup and an SSH key defined.  If you are on a
  Windows machine you can use Putty with TortoiseGit.  Run puttygen.exe to generate the key -- save the private key and export
  the OpenSSH public key.  If you just leave the dialog open, you can copy and paste the key from it to your Gitlab profile under
  "SSH keys".
   
  Run pageant.exe and add the private key generated earlier.  TortoiseGit will use this for accessing Gitlab.
   
- Fork the Forge git repo to your Gitlab account.

- Clone your forked repo to your local machine.

- Make sure the Java SDK is installed -- not just the JRE.  Java 8 or newer required.  At the time of this writing, JDK 11 works as expected.

- You need maven to load in dependencies and build.  Obtain that [from here](https://maven.apache.org/download.cgi). Execute the following from the root repo dir to download dependencies, etc:

    `mvn -U -B clean -P windows-linux install`
    
  For the desktop, this will create a populated directory at `forge-gui-desktop/target/forge-gui-desktop-<release-name>` containing typical release files such as the jar, Windows executable, resource files, etc.

- Install Eclipse for Java.  Launch it.  At the time of this writing, Eclipse 2018-12 works as expected.  YMMV for other versions.

- Create a workspace.  Go to the workbench.  Right-click inside of Package Explorer > Import... > General > Existing Projects into Workspace > Navigate to local forge repo >
  Check "Search for nested projects" > Uncheck 'forge', check the rest > Finish.
  
- Let Eclipse run through building the project.

## Project Launch

### Desktop

- Right-click on forge-gui-desktop > Run As... > Java Application > "Main - forge.view" > Proceed

### Mobile (Desktop dev)

- Right-click on forge-gui-mobile-dev > Run As... > Java Application > "Main - forge.app" > Proceed
  
# IntelliJ

TBD  

# Card Scripting

Visit [this page]()https://www.slightlymagic.net/wiki/Forge_API) for information on scripting.

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
 
