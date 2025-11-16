# Contributing to Forge

[Official repo](https://github.com/Card-Forge/forge.git).

## Requirements / Tools

- your favourite Java IDE (IntelliJ, Eclipse, VSCodium, Emacs, Vi...)
- Java JDK 17 or later
- Git
- Git client (optional)
- Maven
- GitHub account
- Libgdx (optional: familiarity with this library is helpful for mobile platform development)
- Android SDK (optional: for Android releases)
- RoboVM (optional: for iOS releases) (TBD: Current status of support by libgdx)

## Project Quick Setup

- Login into GitHub with your user account and fork the project
- Clone your forked project to your local machine
- Go to the project location on your machine. Run Maven to download all dependencies and build a snapshot.
  - Example for Windows & Linux: `mvn -U -B clean -P windows-linux install`

## IntelliJ

IntelliJ is the recommended IDE for Forge development. Quick start guide for [setting up the Forge project within IntelliJ](https://github.com/Card-Forge/forge/wiki/IntelliJ-setup).

## Eclipse

Eclipse includes Maven integration so a separate install is not necessary.
Google no longer supports Android SDK releases for Eclipse.

## Windows

TBD

## Linux / Mac OSX

TBD

### Android Platform

In IntelliJ, if the SDK Manager is not already running, go to Tools > Android > Android SDK Manager. Install the following options / versions:

- Android SDK Build-tools 35.0.0
- Android 15 (API 35) SDK Platform

> [!CAUTION]
> Be careful about using unsupported api calls e.g. ``StringBuilder.isEmpty()``. Google's documentation for these is sometimes inaccurate.

### Proguard update

Standalone Proguard 7.6.0 is included with the project (proguard.jar) under forge-gui-android > tools and supports up to Java 23 (latest android uses Java 17).

## Card Scripting

Visit [this page](https://github.com/Card-Forge/forge/wiki/Card-scripting-API) for information on scripting.

Card scripting resources are found in the forge-gui/res/ path.

## General Notes

Art files need to be copyright-free and they should be in the public domain.

### Project Hierarchy

Forge is divided into 4 primary projects with additional projects that target specific platform releases. The primary projects are:

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

The forge-ai project contains the computer opponent logic for gameplay. It includes decision-making algorithms for specific abilities, cards and turn phases.

#### forge-core

The forge-core project contains the core game engine, card mechanics, rules engine, and fundamental game logic. It includes the implementation of Magic: The Gathering rules, card interactions, and the game state management system.

#### forge-game

The forge-game project handles the game session management, player interactions, and game flow control. It includes implementations for multiplayer support, game modes, matchmaking, and game state persistence. This module bridges the core game engine with the user interface and networking components.

#### forge-gui

The forge-gui project contains the user interface components and rendering logic for the game. It includes the main game window, card displays, player interactions, and the scripting resource definitions in the res/ path.

#### forge-gui-android

Libgdx-based backend targeting Android. Requires Android SDK and relies on forge-gui-mobile for GUI logic.

#### forge-gui-desktop

Java Swing based GUI targeting desktop machines.

Screen layout and game logic revolving around the GUI is found here. For example, the overlay arrows (when enabled) that indicate attackers and blockers, or the targets of the stack are defined and drawn by this.

#### forge-gui-ios

Libgdx-based backend targeting iOS. Relies on forge-gui-mobile for GUI logic.

#### forge-gui-mobile

Mobile GUI game logic utilizing [libgdx](https://libgdx.badlogicgames.com/) library. Screen layout and game logic revolving around the GUI for the mobile platforms is found here.

#### forge-gui-mobile-dev

Libgdx backend for desktop development for mobile backends. Utilizes LWJGL. Relies on forge-gui-mobile for GUI logic.

#### forge-installer