Installation and Updating to a newer version Instructions:

We have changed the archival format used for the Forge distributions from ".zip" to ".tar.bz2". There are utilities for Windows, Mac OS and the various *nix's that can be used to decompress these ".tar.bz2" archives. We recommend that you decompress the Forge archive into a new and unused folder.

Once the Forge archive has been decompressed you should then be able to launch Forge by using the included launcher. Launching Forge by double clicking on the forge jar file will cause a java heap space error. Forge's memory requirements have increased over time and the launchers increase the java heap space available to Forge.

After downloading and installing a newer version of Forge you may want to move certain files from the older version over to the newer version of Forge. You should maintain your older version of Forge as a back up in case you make a mistake while installing the newer version.

1) The /res/pics/ folder contains the card pictures, token pictures (mtg card tokens an quest pet/plant tokens) and the booster package images. Please note that the /res/pics/icons/ folder was moved out of this folder and placed in the /res/images/ folder.

The /res/images/icons/ folder contains the quest opponent icons, small quest pet/plant icons (non-tokens) and some icons that are used by forge's quest mode. While several of these pictures ship with the forge archive most of them have to be downloaded using the Home screen -> Utilities -> Download Quest Images command.

2) The /res/decks/ folder contains your deck files. You should copy over the files with the extension ".dck".

3) The /res/draft/ and the /res/sealed/ folders contains files for the sealed and draft mode. You should copy over your files inside of these folders that end in the extension ".draft" or ".zsealed".

4) The /res/quest/ folder contains your questData file. This file includes all of the information for your current quest. You will not be able to continue your quest in a newer version of Forge unless you copy over the file named "questData.dat".

5) The Forge root folder contains a preference file named "forge.preferences" and you should also move a copy of this file over to the newer version.


Advanced Updating to a newer version Instructions:

Another option for you to consider is to move some of the files/folders listed above to a different location on your hard drive. Then edit the "main.properties" file in the /res/ folder with any basic text editor. At the "image/base--file=pics" and "image/token--file=pics/tokens". Just change these to absolute paths of your choice eg:

image/base--file=F:/Personal/CardForge/Low Resolution Images

image/token--file=F:/Personal/CardForge/Low Resolution Images/tokens

and do the same for "decks-dir--file=decks", "quest--properties=quest/quest.properties" and whatever else you might usually copy from version to version. Try to avoid folders that are usually updated in the releases. Just remember to use forward slashes for the pathnames. Then just copy the "main.properties" file from version to version. Occasionally compare it to the release version to make sure nothing else has changed, and if it has just replace the adjusted lines instead.


The Mac OS application version:

We have packaged the Forge BETA version as a Mac OS application. You can double click the Forge.app icon to launch the forge application on your Apple computer running Mac OS. This application will automatically increase the java heap space memory for you as it launches. This version does not require the forge.command file and it does not need to start the Terminal application as part of the start up process.

You can move a copy of your pictures and decks over to the "Forge.app" application. Right click or control click on the Forge.app icon. Select "Show Package Contents" from the contextual menu. A Finder window will open and will display a folder named Contents. Navigate to the folder:

/Contents/Resources/Java/res/

Your decks can be placed in the decks folder, your pics can be placed in the pics folder, etc.


Picture location info:

The quest opponent icons jpg picture files go into your /res/pics/icons folder. The quest booster package jpg picture files go into your /res/pics/booster folder. The card token jpg picture files go into your /res/pics/tokens folder.

The quest pets archive contains two subdirectories named "icons" and "tokens". Place the files located inside of the /icons/ folder into the /res/pics/icons/ folder. Place the files located inside of the /tokens/ folder into the /res/pics/tokens/ folder.

Your forge game may not come with one or more of these three folders as part of the forge archive. In this case you should use your computer's OS file system to create the proper folders with the correct names and they must be located inside of the /res/pics/ folder.


Launching Forge and Memory Issues:

In the past, some people noticed java heap space errors and lengthy pauses. The memory requirements for Forge have increased over time. The default setting on your computer for the java heap space may not be enough to prevent the above problems.

The technically proficient can launch the forge jar with an argument from the CLI. The argument "-Xmx512m" may work for computers with 1 Gig of memory. Computers with 2 Gigs or more of memory should be able to use "-Xmx1024m" as an argument.

We have created several scripts that will launch the Forge jar with "-Xmx1024m" as an argument. People using Windows OS should double click the "forge.exe" file. People using one of the other *nix OS should double click the "forge.sh" file. People using Apple's Mac OS X should download the Mac version of Forge and then double click the "forge.app" application.

The script file must be located in the same folder as the Forge jar file and the Forge jar file name can not be changed. Otherwise, the scripts will not work.

If you have a low end machine you may find that the scripts above will prevent java heap space errors but will find that Forge still runs very slowly at times.

In this case you can try the following. Remove the background jpg picture from /res/images/ui/ folder. You can try using low quality pictures rather than the high quality pictures. Or you can try removing all of the jpg pictures from the pics folder.


Java Issues:

Some people that are using an early version of Java 7 under the Windows OS have reported errors that state "Split must have > 2 children". Anyone having this sort of problem should de-install java 7 and install java 6 instead.


Card Picture Issues:

The server which contained the high quality card pictures is now off line and these high quality card pictures are no longer available as a download from within the forge application. We apologize, but the current dev team do not maintain this server and this matter is out of our control.

Some people are choosing to re-download all of the low quality card and card set pictures when they install the next version of forge. This consumes large amounts of bandwidth needlessly. Please be careful!

When you install the new version of Forge, find the forge/res/pics/ folder. Either move it or copy and paste the pics folder over to the recently installed new version of forge. This way you will only have to download the pictures for the new cards.

This should save enough bandwidth that everyone will be able to download the new set pictures from the cardforge server. We do appreciate your efforts to save bandwidth. Thank you.


Reporting Bugs:

To report a bug with an official beta release, please follow the instructions at http://www.slightlymagic.net/wiki/Forge#I_think_I_found_a_bug_in_Forge._What_do_I_do.3F .

To report a bug (1) with an alpha test, (2) with a nightly build, (3) with something compiled from the official Forge software repository, or (4) with the leading edge (formerly "SVN Bug Reports"), please do not submit your bugs to the forum. Instead, please follow the instructions at http://www.slightlymagic.net/wiki/How_to_File_a_Bug_Report_with_Mantis .


Our Lawyers Made Us Do This:

This product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).
