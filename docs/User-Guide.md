# Downloads
* **Snapshots**;
  * READ THESE NOTES BEFORE DOWNLOADING SNAPSHOTS:
    * **Please use snapshots for Adventure Mode!**
    * May contain more bugs, bug fixes, **definitely gets newest cards faster** and newer features.
    * These are **NOW** automatically released daily.
    * If the snapshot isn't in the location below, it's because its in the middle of uploading a new snapshot. Come back later to grab it.
  * [_**CLICK HERE FOR DOWNLOAD LINKS - Forge SNAPSHOT Version (DESKTOP/ANDROID)**_](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots)
    * For desktop, grab the installer file that ends in .jar
    * For android, grab the android file that ends in .apk  
* **Android Installation Guide**
  * Quick Guide for installing Android Snapshots: <br />


https://github.com/user-attachments/assets/7a0c7bb8-7cf9-4800-8091-bcc30ff2f4d8



* **Releases**;
  * READ THESE NOTES BEFORE DOWNLOADING RELEASES: 
    - "Releases" are really intended where "99% cards implemented are working and stable."
    - If you are looking for newly spoiled cards as soon as possible, grab the snapshot instead.
    - The current release mechanism is failing unexpectedly for Android. So just stick with snapshots for Android users. 
  * [_**CLICK HERE FOR DOWNLOAD LINKS - RELEASE DESKTOP**_](https://github.com/Card-Forge/forge/releases/latest)
    - Grab the installer file that ends in .jar
 

# Java Requirement

**Forge Requires Java** to run, please make sure you have Java installed on your machine prior to attempting to run.

* **Java 17** is required as minimum version and can be acquired through the Standard Edition Development Kit (JDK) or the OpenJDK. Continued development provides new features in those editions, therefore you need the Java Development Kit to have those newer editions;
  - Download - [https://jdk.java.net/](https://jdk.java.net/)
  - Source Code - [https://github.com/openjdk/jdk/](https://github.com/openjdk/jdk/)

Most people who have problems setting up Forge, do not have Java setup properly. If you are having trouble, open your terminal/command line and run `java --version`. That number should be 17 or higher. 

# Install and Run

Forge requires Java to run. 

_**Download and unpack /install the package to their own new folder!**_

### Install Wizard (jar)
* Run/Double click "**forge-installer**-VERSION.jar" where VERSION is the current release version and click next until the Target Path window appears. If double clicking the .jar file doesn't load the main interface you can run it via terminal/command line ```java -jar FILENAME.jar``` where FILENAME is the name of the installer.

* Browse to your preferred install directory (create a new directory for clean installation) and click next until installation starts.

![image](https://github.com/Card-Forge/forge/assets/9781539/b7575f49-f6b3-4933-a15f-726314547c4f)

* After the installation finishes, close the installer. Run the executable forge|forge-adventure (.exe/.sh/.cmd)

### Manual Extraction (tar.bz2)

* **Desktop Windows**:
  * Unpack "forge...tar.**bz2**" with any unpacking/unzipping app (e.g. 7-zip, winrar, etc) 
    * You'll end up with "forge...**tar**".
  * Unpack that ".tar" file once more into its own folder. 
  * Run Forge app/exe
* **Desktop Linux/Mac**:
  * Unpack "forge...**tar.bz2**" with any unpacking app. (Check your package repository, or app store.)
    * You'll probably end up with just a folder, and fully extracted.
    * If you do end up with a ".tar" file, unpack that file also into it's own folder.
  * Run Forge script; 
    * Linux: Run the ".sh" file in a terminal (double clicking might work.)
    * MacOS/OSX: Run the ".command" file by double clicking in Finder, or run from the terminal.
       * If the command file doesn't appear to do anything, you'll need to [modify the permissions to be executable.](https://support.apple.com/guide/terminal/make-a-file-executable-apdd100908f-06b3-4e63-8a87-32e71241bab4/mac) (This is a temporary bug in the build process.)
       * Additionally OSX needs to have a JRE AND a JDK installed because reasons. 
* **Android**:
  * Sideload/Install "forge...apk"
  * Run Forge

## Play Adventure Mode on Desktop

* Run the Adventure Mode EXE or Script in the Folder you extracted.
* The game will start with an option for Adventure or Classic Mobile UI.
* Android/Mobile builds are built as the Adventure Mode or Mobile UI and nothing special is needed.
  - If adventure mode option does not show up;
    - check you're up to date with your version.
    - check in the settings that the "Selector Mode" is set to `Default`

# System Requirements and Historic Details

Since Forge is written in Java, it is compatible on any Operating System
that can run the Java Runtime Environment. Forge requires
Java 17 (Forge is not backwards compatible with older versions of Java).
If you have difficulties with your System not working with Forge, 
please come to the Discord so we can attempt to help.
This program works best with a screen resolution of **1280 by 720** or
better. Forge can now have it's window minimized to **800 by 600**
pixels but this may make the display area cramped and possibly limit
your ability to play. (This means Forge may not compatible with some
netbook computers.)

The memory requirements for Forge have fluctuated over time. The default
setting on your computer for the java heap space may not be enough to
prevent the above problems. If you launch Forge by double-clicking the
file **run-forge.jar** you will eventually receive a **java heap space
error**. The name of the forge jar file has changed as part of our new
Maven based build and release system. The name format now used is:

**forge-**{version number}**-jar-with-dependencies.jar**

We have created several scripts that will launch Forge with a greater
allotment of system resources. (We do this by passing **-Xmx1024m** as
an argument to the Java VM.) People using Windows OS should double click
the **forge.exe** file. People using Apple's Mac OS X should use the Mac
OS version and double click the **forge.command** file. People using one of the
other \*nix OS should double click the **forge.sh** file.

# What if double-clicking doesn’t work?

Sometimes double-clicking will open the jar file in a different program.
In Windows, you may need to right-click and open the properties to
change the launching program to Java. This might be different in OSX or
Linux systems (file permission related).
