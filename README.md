# Forge

Dev instructions here: [Getting Started](https://www.slightlymagic.net/wiki/Forge:How_to_Get_Started_Developing_Forge) (Somewhat outdated)

Discord channel [here](https://discordapp.com/channels/267367946135928833/267742313390931968)

# Requirements / Tools

- Java IDE such as IntelliJ or Eclipse
- Git
- Maven
- Android dev kit (optional: for Android releases)
- Xamarin (optional: for iOS development)
- Gitlab account

# Project Quick Setup

- Log in to gitlab with your user account and fork the project.

- Clone your forked project to your local machine

- Go to the project location on your machine.  Run Maven to download all dependencies and build a snapshot.  Example for Windows & Linux: `mvn -U -B clean -P windows-linux install`

- 

# Eclipse

## Project Setup

- Follow the instructions for cloning from Gitlab.  You'll need a Gitlab account setup and an SSH key defined.  I'm on a
  windows machine and use Putty with TortoiseGit.  Run puttygen.exe to generate the key -- save the private key and export
  the OpenSSH public key.  If you just leave the dialog open, you can copy and paste the key from it to your Gitlab profile under
  "SSH keys".
   
  Run pageant.exe and add the private key generated earlier.  TortoiseGit will use this for accessing Gitlab.
   
- Fork the Forge git repo to your Gitlab account.

- Clone your forked repo to your local machine.

- You need maven to load in dependencies and build.  Install that. `mvn -U -B clean -P windows-linux install` from the root repo dir will create everything needed.

- Make sure the Java SDK is installed -- not just the JRE.  Java 8 or newer required.  I'm personally running 11.

- Install Eclipse for Java.  Launch it.  I'm using Eclipse 2018-12.

- Create a workspace.  Go to the workbench.  Right-click inside of Package Explorer > Import... > General > Existing Projects into Workspace > Navigate to local forge repo >
  Check "Search for nested projects" > Uncheck 'forge', check the rest > Finish.
  
- Let Eclipse run through building the project.

## Project Launch

### Desktop

- Right-click on forge-gui-desktop > Run As... > Java Application > "Main - forge.view" > Proceed

### Mobile (Desktop dev)

- Right-click on forge-gui-mobile-dev > Run As... > Java Application > "Main - forge.app" > Proceed
  
# General Notes

## Desktop

- The desktop GUI is based off of Java Swing.

- Use the following build command to create and populate `forge-gui-desktop/target/forge-gui-desktop-<release-name>` for Linux and Windows:

    `mvn -U -B clean -P windows-linux install`

## Mobile

The mobile GUI is based off of [libgdx](https://libgdx.badlogicgames.com/).  As with many other libgdx projects, there is a mobile-dev version "forge-gui-mobile-dev" that runs on the desktop but is separate and distinct from the 'desktop GUI' mentioned above.
 
