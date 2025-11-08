# User Guide

## Downloads

### Snapshots

* Snapshots are automated daily builds of the source code.
* They contain the latest bug fixes, features and cards.
* If the snapshot isn't in the location below, it's because its in the middle of uploading a new snapshot - come back later to grab it.

[***CLICK HERE FOR DOWNLOAD LINKS - Forge SNAPSHOT Version (DESKTOP/ANDROID)***](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots)

* For desktop, grab the installer file that ends in .jar
* For android, grab the android file that ends in .apk
  &dash; Watch the screen recording if one of following steps isn't clear for you

<https://github.com/user-attachments/assets/7a0c7bb8-7cf9-4800-8091-bcc30ff2f4d8>

### Releases

* "Releases" are really intended where "99% cards implemented are working and stable".
* **They are NOT bug-free.** They are not updated after they're built, meaning you need to wait for the next release if you encounter a bug, or use the SNAPSHOT version instead.
* If you are looking for newly spoiled cards as soon as possible, grab the SNAPSHOT version instead.
* The current release mechanism is failing unexpectedly for Android. So just stick with snapshots for Android users.

[***CLICK HERE FOR DOWNLOAD LINKS - RELEASE DESKTOP***](https://github.com/Card-Forge/forge/releases/latest)

* Grab the installer file that ends in .jar

## System Requirements

**Forge Requires Java** to run, please make sure you have Java installed on your machine prior to attempting to run.

* **Java 17** is required as minimum version and can be acquired through the Standard Edition Development Kit (JDK) or the OpenJDK. Continued development provides new features in those editions, therefore you need the Java Development Kit to have those newer editions:
  * Download - [https://jdk.java.net/](https://jdk.java.net/)
  * Source Code - [https://github.com/openjdk/jdk/](https://github.com/openjdk/jdk/)

Most people who have problems setting up Forge, do not have Java setup properly. If you are having trouble, open your terminal/command line and run `java --version`. That number should be 17 or higher.

The memory requirements for Forge have fluctuated over time. The default
setting on your computer for the Java heap space may not be enough to
prevent the above problems. If you launch Forge by double-clicking the
jar files directly you could eventually receive a **java heap space
error**.

We have created several scripts that will launch Forge with a greater
allotment of system resources. (We do this by passing `-Xmx1024m` as
an argument to the Java VM.)

## Install and Run

> Warning: Do **NOT** owerwrite an existing installation. Always unpack/install the package in a new folder to avoid problems!

### Install Wizard (jar)

* Run/Double click "**forge-installer**-VERSION.jar" where VERSION is the current release version and click next until the Target Path window appears. If double clicking the .jar file doesn't load the main interface you can run it via terminal/command line ```java -jar FILENAME.jar``` where FILENAME is the name of the installer.

* Browse to your preferred install directory and click next until installation starts.

![image](https://github.com/Card-Forge/forge/assets/9781539/b7575f49-f6b3-4933-a15f-726314547c4f)

* After the installation finishes, close the installer. Run the executable forge|forge-adventure (.exe/.sh/.cmd)

### What if double-clicking doesn’t work?

Sometimes double-clicking will open the jar file in a different program.
In Windows, you may need to right-click and open the properties to change the launching program to Java.
This might be different in OSX or Linux systems (file permission related).

### Manual Extraction (tar.bz2)

#### Desktop Windows

* Unpack "forge...*tar.bz2*" with any unpacking/unzipping app (e.g. 7-zip, winrar, etc)
  * You'll end up with "forge...*tar*".
* Unpack that ".tar" file once more into its own folder.
* Run Forge app/exe

#### Desktop Linux/Mac

* Unpack "forge...*tar.bz2*" with any unpacking app. (Check your package repository, or app store.)
  * You'll probably end up with just a folder, and fully extracted.
  * If you do end up with a ".tar" file, unpack that file also into its own folder.
* Run Forge script:
  * Linux: Run the ".sh" file in a terminal (double clicking might work.)
  * MacOS/OSX: Run the ".command" file by double clicking in Finder, or run from the terminal.
    * If the command file doesn't appear to do anything, you'll need to [modify the permissions to be executable.](https://support.apple.com/guide/terminal/make-a-file-executable-apdd100908f-06b3-4e63-8a87-32e71241bab4/mac) (This is a temporary bug in the build process.)
    * Additionally OSX needs to have a JRE AND a JDK installed because reasons.

#### Android

* Sideload/Install "forge...apk"
* Run Forge

### Play Adventure Mode on Desktop

* Run the Adventure Mode EXE or Script in the Folder you extracted.
* The game will start with an option for Adventure or Classic Mobile UI.
* Android/Mobile builds are built as the Adventure Mode or Mobile UI and nothing special is needed.
  * If adventure mode option does not show up;
    * check you're up to date with your version.
    * check in the settings that the "Selector Mode" is set to `Default`

## Gameplay

### Full Control

Right click/long tap on your player avatar:

This feature lets you skip different helpers that streamline gameplay by avoiding somewhat annoying GUI interactions and instead use AI logic to make reasonable decisions for beginners.\nUseful for certain corner cases or if you want to challenge yourself with the Comprehensive Rules:\ne.g. the opposite cost order is needed if activating an animated "Halo Fountain" that's also tapped.
