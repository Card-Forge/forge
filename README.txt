Installation and Updating to a newer version Instructions:

We have changed the archival format used for the Forge distributions from ".zip" to ".tar.bz2". There are utilities for Windows, Mac OS and the various *nix's that can be used to decompress these ".tar.bz2" archives. We recommend that you decompress the Forge archive into a new and unused folder.

Once the Forge archive has been decompressed you should then be able to launch Forge by using the included launcher. Launching Forge by double clicking on the forge jar file will cause a java heap space error. Forge's memory requirements have increased over time and the launchers increase the java heap space available to Forge.

After downloading and installing a newer version of Forge you may want to move certain files from the older version over to the newer version of Forge. You should maintain your older version of Forge as a back up in case you make a mistake while installing the newer version.

1) The /res/pics/ folder contains the card pictures, icons and token pictures (mtg card tokens and quest pet/plant tokens). Please note that the /res/images/icons/ folder was moved back to the /res/pics/ folder. The forge /res/images/ folder no longer is used as of version 1.2.8.

The /res/pics/icons/ folder contains the quest opponent icons, small quest pet/plant icons (non-tokens) and some icons that are used by forge's quest mode. While several of these pictures ship with the forge archive most of them have to be downloaded using the Home screen -> Utilities -> Download Quest Images command.

2) The /res/pics_product/ folder contains four folders which in turn contain pictures for the booster, fatpacks, precons and tournamentpacks products.

3) The /res/decks/ folder contains your deck files. You should copy over the files with the extension ".dck". In version 1.2.4 and later the /decks/ folder contains 4 subdirectories named constructed, cube, draft and sealed.

You can place your deck files from an earlier version of Forge into the /res/decks/ folder. When you next launch Forge these decks will be converted to a newer format and will be moved into the proper subdirectory.

4) The /res/draft/ and the /res/sealed/ folders contains files for the sealed and draft mode. You should copy over your files inside of these folders that end in the extension ".draft" or ".zsealed".

5) The /res/quest/data folder contains your questData files. These files include all of the information for your current quests. You will not be able to continue your quests in a newer version of Forge unless you copy over the your quest data files. These files have a ".dat" extension. You can now have multiple ongoing quests at the same time.

6) The Forge /res/preferences/ folder contains the preference files named "forge.preferences" and "editor.preferences". You should also move a copy of these files over to the newer version. The /res/preferences/ folder was added to version 1.2.8. The main.properties file was also moved to the /res/preferences/ folder.

7) The /res/layouts/ folder contains a file named "match_preferred.xml". This file contains information that forge uses when setting the layout for the battlefield display. The match_default.xml file contains the default layout for the battlefield.

The editor_preferred.xml file contains information that forge uses when setting the layout for the deck editor display. The editor_default.xml file contains the default layout for the deck editor display.


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

If you update your OS to Apple OSX 10.8 Mountain Lion and try to launch a new version of forge that you will likely get a dialog which states "File is damaged and cannot be opened. Please move to trash." 

Mountain Lion comes with a new Gatekeeper feature and this is probably blocking your ability to launch this newer version of forge. Visit the link below and follow the instructions. They are fairly long and detailed.

http://support.apple.com/kb/HT5290?viewlocale=en_US&locale=en_US

Please note that the issue is most likely caused by Mountain Lion's Gatekeeper feature and it is extremely unlikely that the forge dev team will attempt to get a unique Developer ID from Apple and use it to digitally sign our forge app.


Picture location info:

The quest opponent icons jpg picture files go into your /res/images/icons folder. Please note that the /res/pics/icons/ folder was moved out of this folder and placed in the /res/images/ folder.

The quest booster package jpg picture files go into your /res/pics/booster folder. The card token jpg picture files go into your /res/pics/tokens folder.

The quest pets archive contains two subdirectories named "icons" and "tokens". Place the files located inside of the /icons/ folder into the /res/images/icons/ folder. Place the files located inside of the /tokens/ folder into the /res/pics/tokens/ folder.

Your forge game may not come with one or more of these three folders as part of the forge archive. In this case you should use your computer's OS file system to create the proper folders with the correct names and they must be located inside of the /res/pics/ folder.

The in-game downloaders will grab an image for face-down (Morph) cards on the battlefield.  To use a custom image, place a "morph.jpg" file in your res/pics/tokens directory.


Launching Forge and Memory Issues:

In the past, some people noticed java heap space errors and lengthy pauses. The memory requirements for Forge have increased over time. The default setting on your computer for the java heap space may not be enough to prevent the above problems.

The technically proficient can launch the forge jar with an argument from the CLI. The argument "-Xmx512m" may work for computers with 1 Gig of memory. Computers with 2 Gigs or more of memory should be able to use "-Xmx1024m" as an argument.

We have created several scripts that will launch the Forge jar with "-Xmx1024m" as an argument. People using Windows OS should double click the "forge.exe" file. People using one of the other *nix OS should double click the "forge.sh" file. People using Apple's Mac OS X should download the Mac version of Forge and then double click the "forge.app" application.

The script file must be located in the same folder as the Forge jar file and the Forge jar file name can not be changed. Otherwise, the scripts will not work.

If you have a low end machine you may find that the scripts above will prevent java heap space errors but will find that Forge still runs very slowly at times.

In this case you can try the following. Remove the background jpg picture from /res/images/ui/ folder. You can try using low quality pictures rather than the high quality pictures. Or you can try removing all of the jpg pictures from the pics folder.


Forge failed to launch:

If you're trying to run Forge for the first time, but it doesn't open up, you can try the following to get some output and help yourself/us solve the problem.

1) Open up a terminal
- Under Windows, press Windows+R, type "cmd", hit enter
- Under Linux, you probably know that yourself. Use your distribution's application menu, and search for "terminal" in a group like "utilities".
- Launch the program named "Console.app" which can be found in your /Applications/Utilities/ folder. Highlight the "All Messages" option and click on the "Clear Display" button before launching Forge.

2) Go to the folder where you unpacked Forge
- Windows: Let's say your forge is in D:\Programs\Forge.
- Type "D:", Enter to change to the D: drive.
- Type "cd \Programs\Forge", Enter to change to the directory.
- NOTE: On nonenglish systems, you might have problems due to the poor localization of Windows. Go to the innermost directory you find (worst case is "\"), then "dir", Enter to show all folders in that folder. Search for the one you're probably wanting. For Example the German "Programme" could really be "Program Files" or something like that.
- NOTE: You might have to "quote" directory names with Spaces in them
- Linux: Let's say your forge is in /home/user/Forge
- Type "cd /home/user/Forge", Enter
- NOTE: You might have to "quote" or 'quote' directory names with Spaces in them
- Current versions of Forge no longer include a launcher script for Mac OS, proceed to step three.

3) Run Forge
- On Windows, just type "forge.exe", Enter
- On Linux, just type "forge.sh", Enter
- Launch the Forge application bundle by double clicking on the program named "Forge.app".

Now you will probably see some sort of Error in the console. the first few lines contain a message that might help you. If you can't fix the problem yourself, please take the complete output and report your problem on the Forum.


The Card Pictures disappear when you restart Forge:

if you're running Windows 7, make sure you're running the program as an admin, otherwise no changes will be made to your system (nothing is saved). In Windows 7, Forge may be happier when run from somewhere in the My Documents structure, (they call them Libraries now???) or from another partition other than C:. The user has little permission to do much on the system drive.


Java Issues:

Some people that are using an early version of Java 7 under the Windows OS have reported errors that state "Split must have > 2 children". Anyone having this sort of problem should de-install java 7 and install java 6 instead.

Forge requires java 6 and will not run if you have an earlier version of java. You will need to update to java 6.


Card Picture Issues:

The server which contained the high quality card pictures is now off line and these high quality card pictures are no longer available as a download from within the forge application. We apologize, but the current dev team do not maintain this server and this matter is out of our control.

Some people are choosing to re-download all of the low quality card and card set pictures when they install the next version of forge. This consumes large amounts of bandwidth needlessly. Please be careful!

When you install the new version of Forge, find the forge/res/pics/ folder. Either move it or copy and paste the pics folder over to the recently installed new version of forge. This way you will only have to download the pictures for the new cards.

This should save enough bandwidth that everyone will be able to download the new set pictures from the cardforge server. We do appreciate your efforts to save bandwidth. Thank you.


Reporting Bugs:

To report a bug with an official beta release, please follow the instructions at http://www.slightlymagic.net/wiki/Forge#I_think_I_found_a_bug_in_Forge._What_do_I_do.3F .

To report a bug (1) with an alpha test, (2) with a nightly build, (3) with something compiled from the official Forge software repository, or (4) with the leading edge (formerly "SVN Bug Reports"), please do not submit your bugs to the forum. Instead, please follow the instructions at http://www.slightlymagic.net/wiki/How_to_File_a_Bug_Report_with_Mantis .


*************************** start adding material from the old betas starting here ***************************


Multiple quest files:
 
Multiple quest files are now supported. This allows you to start a new quest and give it a unique name, and it will not overwrite your previous quest game file.


The new UI now uses tabbed panes:

We now have a tab system for sub-menus in the home screen. Quest mode refactored to fit this tab system. It's now considerably easier to use - less cramped, less clicks, more functionality.


The quest mode card shop:

You can now buy PreCon decks, Starter packs, Tournament packs and Fat packs from the quest mode card shop.


Player Avatar pictures:

The UI has a few new features including the option to pick an avatar from a collection of pictures. This can be accessed from the Settings -> Avatars tab.


The organizational structure of the /res/decks/ folder:

The organizational structure of the /res/decks/ folder has been improved and we now have these six subdirectories:

/decks/constructed/
/decks/cube/
/decks/draft/
/decks/plane/
/decks/scheme/
/decks/sealed/

You can place your deck files from an earlier version of Forge into the /res/decks/ folder. When you next launch Forge these decks will be converted to a newer format and will be moved into the proper subdirectory.


User-created themes for Forge's background, fonts, colors and icons:

When you select a new skin in the Preferences view Forge should save the change to the preference file, quit and then automatically re-launch with the new skin displayed. During testing some people have noticed that Forge is not restarting on their computer and they have to re-launch Forge themselves.

If anyone is interested in creating additional themes for inclusion in the Forge project then you should visit this topic at CCGH:

http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=8449


The Battlefield UI:

The Battlefield UI has a new feature implemented which allows us to move and resize the panels to new places on the battlefield. This allows us to personalize the battlefield display to our own liking. You should try moving panels by clicking and dragging their tabs.

If you do not like your efforts to personalize the battlefield display you can revert the display to the default layout by clicking on the Dock button labeled "Revert layout".


The pets in quest mode:

Some adjustments to the pets in quest mode were made. The quest mode plant wall's Deathtouch ability was deemed to be too strong against the AI's attack code and this ability was changed to Wither in this version. This includes a new pet.


The dock tab has a new button labeled "Open Layout":

The dock now has a new button labeled "Open Layout" along with old button with original function "Revert Layout". Modifying the battlefield layout will result in your changes being saved to a file named  "match_preferred.xml". You can copy and rename that file to share your layouts with other people.


The res/images/icons/ folder:

The res/images/icons/ folder has been moved to res/pics/icons/. This folder holds the quest opponent icons, bazaar pets, and bazaar plants. If you have downloaded this content, it will still work, but the folder must be moved on your computer.


The new Deck Editors:

The work on the new UI is now finished and this version adds the new UI to the deck editors. We all would like to thank Doublestrike for his contributions to the new UI.

The new deck editors include:

* a better text search (can search for multiple terms, and "not" terms
* interval filters for P/T and CMC
* add/remove 4
* better statistics and draw probabilities
* Toggle-able, sort-able, resize-able, move-able columns
* and of course uses the drag cell layout.


Performance issues on low end machines:

Several people have noticed forge slowing down after playing a number of matches without quitting forge in between the matches that are played. The new UI may be involved somehow. We also hope to have this figured out and fixed in the near future. Please be patient in the meanwhile. A recent fix was implemented that should improve the slowdown problem somewhat.

A lot of time and effort have gone into fixing the memory leak problems that were recently noticed and reported to the dev team. Doublestrike and Slapshot deserve our applause and we are very thankful. People should be able to now play long multi match sessions without noticing slow downs and pauses.

Some performance changes were made to Forge and it should now operate more quickly on low end machines. Mid to high level machines are unlikely to notice as much of a performance increase. We tried to hunt down all of the bugs that resulted from these changes but there may still be a bug or two in this beta release.


A note about winning booster packs in quest mode:

If you win a quest mode match, you get a booster pack for every 1 or 2 (default) Wins, depending on the difficulty level. If you lose and you are playing on easy mode, you get a booster pack every 1 (default) Loss.


The new UI:

The work on the new UI is now for the most part finished. We should not expect major changes or major additions to the UI. Future betas may include a few minor bug fixes to the UI. And we may also include a few minor tweaks.


The new Alpha Strike button:

A new Alpha Strike button was added to the dock. The Dock is one of the tabs availble in the battlefield view.


Using Forge with the new Mac OS Mountain Lion:

If you update your OS to Apple OSX 10.8 Mountain Lion and try to launch a new version of forge that you will likely get a dialog which states "File is damaged and cannot be opened. Please move to trash." 

Mountain Lion comes with a new Gatekeeper feature and this is probably blocking your ability to launch this newer version of forge. Visit the link below and follow the instructions. They are fairly long and detailed.

http://support.apple.com/kb/HT5290?viewlocale=en_US&locale=en_US

Please note that the issue is most likely caused by Mountain Lion's Gatekeeper feature and it is extremely unlikely that the forge dev team will attempt to get a unique Developer ID from Apple and use it to digitally sign our forge app.


The Forge sealed deck mode:

The Forge sealed deck mode has undergone significant changes. You can find these in the 1.2.14 beta and later versions. Instead of a single sealed deck match, you can now choose a 1-5 round gauntlet-style tournament where you will face increasingly difficult (probably) opponent decks. You can also choose to use starter packs instead of boosters in the block mode, choose to use 3-12 boosters instead of the default 6 in the full cardpool and custom (cube) modes, and so on.

Perhaps the most notable changes to the sealed deck mode are related to "fantasy blocks" and the greatly increased flexibility you have when you are building your own blocks.


The new Gauntlet mode:

A new Gauntlet mode has been added. This mode gives you four options: Quick Gauntlet, Build A Gauntlet, Load Gauntlet and Gauntlet Contests. You can create a group of computer decks to play against by choosing either Custom user decks, Quest Decks, Fully random color decks or Semi-random theme decks.


The new Variant mode (was named Multiplayer):

A new multiplayer mode has also been added. You should be able to play against multiple AI opponents at this time. You should note that the current Archenemy mode does not use Schemes at this time.

A lot of things are planned for this new multiplayer mode and it will take time to finish. Please enjoy what we have at this time and be patient. :)

Since Multiplayer is so new, not all cards will be 100% compatible right away as we expand scripting to handle multiple players.

The older match layout files are incompatible with the new multiplayer mode. The original match_default.xml, match_preferred.xml and the match_preferred.xml saved to a different name files have to go and can no longer be used. You can keep your editor_preferred.xml file. But you will have to setup your match view panels using the new match_default.xml file.


The new damage dialog:

The new damage dialog now uses the new UI.


When choosing cards, sources, etc. using a list box:

When choosing cards, sources, etc. using a list box, the currently selected card will now be visually highlighted on the playfield (to better distinguish between e.g. three different cards with the same name on the playfield). Now the visual highlighting of a card will also work when declaring the order of blockers.


Return to Ravnica Guild Sealed Deck mode:

Added Return to Ravnica Guild Sealed Deck mode. Start a new sealed deck game, choose "Block / Set" and then scroll down until you find "Return to Ravnica Guild Sealed (block)". Select that. From the "Choose Set Combination" menu, select the first option. You will be prompted twice to pick your guild (once for the promo cards, once for the actual booster - you should choose the same guild both times). After that you're ready to go.


Targeting arrows are now available in the battlefield display:

The Targeting Overlay has been fixed and re-enabled. It now correctly shows the targeting arcs in cases when it previously showed them in the wrong direction. The match UI is properly refreshed when the targeting arcs are switched on/off. The defunct "mouseover-only" mode is currently disabled (it crashes Forge, difficult to fix).

The visuals for targeting arrows has been improved and looks better, with an adaptation of the arrow drawing code from MAGE. Thanks to the MAGE team for permission for the adaptation.


The new sound system:

Forge now has a sound effect system in place. Several basic sounds are linked to the code now and will be enabled when "Enable Sounds" option is checked in the preferences. It supports WAV and AU file formats.

Currently supported sound effects are:

AddCounter - add_counter.wav - triggered when a counter is added to a permanent.
Artifact - artifact.wav - triggered when an artifact is played.
ArtifactCreature - artifact_creature.wav - triggered when an artifact creature is played.
BlackLand - black_land.wav - triggered when a land with the "B" mana ability is played.
BlueLand[*] - blue_land.wav - triggered when a land with the "U" mana ability is played.
Creature - creature.wav - triggered when a creature is played.
Damage - damage.wav - triggered when a creature is damaged.
Destroy - destroy.wav - triggered when a permanent is destroyed.
Discard - discard.wav - triggered when a player discards a card.
Draw - draw.wav - triggered when a player draws a card.
Enchantment - enchant.wav - triggered when an enchantment is played.
EndOfTurn - end_of_turn.wav - triggered at the end of turn.
Equip - equip.wav - triggered when an equipment is equipped.
FlipCoin - flip_coin.wav - triggered when a coin is flipped.
GreenLand - green_land.wav - triggered when a land with the "G" mana ability is played.
Instant - instant.wav - triggered when an instant is played.
LifeLoss - life_loss.wav - triggered when a player loses life.
LoseDuel[*] - lose_duel.wav - triggered when a player loses a duel.
ManaBurn - mana_burn.wav - triggered during a mana burn if the appropriate rule is enabled.
OtherLand - other_land.wav - triggered when a land with non-color mana abilities or any other land is played.
Planeswalker - planeswalker.wav - triggered when a planeswalker is played.
Poison - poison.wav - triggered when a player receives a poison counter.
RedLand - red_land.wav - triggered when a land with the "R" mana ability is played.
Regen - regeneration.wav - triggered when a creature is regenerated.
RemoveCounter - remove_counter.wav - triggered when a counter is removed from a permanent.
Sacrifice - sacrifice.wav - triggered when a permanent is sacrificed.
Sorcery - sorcery.wav - triggered when a sorcery is played.
Shuffle[*] - shuffle.wav - triggered when a player shuffles his deck.
Tap[*] - tap.wav - triggered when a permanent is tapped.
Untap - untap.wav - triggered when a permanent is untapped.
WhiteLand - white_land.wav - triggered when a land with the "W" mana ability is played.
WinDuel - win_duel.wav - triggered when a player wins the duel.

All sounds use the event bus model now and are not called directly. Two new sounds to the list of sounds supported by Forge, Blocker Assigned and Token Created.


The new Vanguard mode:

We now have a Vanguard mode implemented. This is a work in progress. The older match layout files are incompatible with the new Vanguard mode. The original match_default.xml, match_preferred.xml and the match_preferred.xml saved to a different name files need to be deleted and can no longer be used. You can keep your editor_preferred.xml file. But you will have to setup your match view panels using the new match_default.xml file.


The new Archenemy mode:

Schemes have been added to the Archenemy mode. This is a work in progress and there may be a bug or two for us to find.


Quest Worlds information, stage 1:

This version simply allows you to travel between the regular quest world and the newly-added Shandalar world to get different duel opponents and challenges. You will have to complete your current challenges before travelling or you will lose them. 

World-specific format enforcing and starting world selection will be added in later stages. Also, something has to be done about locked (non-repeatabled) challenges so they do not end up locking other challenges in different worlds.


Forge now has sideboards for the human player:

Sideboards have been implemented for Human players. We currently have:

* Sideboard creation support in relevant deck editor modes.
* In-game sideboarding with persistence between rounds in a match.
* Sideboard supported as a zone, with some relevant cards already in.
* Correct validation of decks, both before the game starts and between the rounds (Limited min 40, Constructed min 60, free-form sideboard/main in Draft and Sealed, 1:1 sideboarding with 0 or 15 cards allowed in sideboard in Constructed (all variants) and Quest; OK to have less than minimum between rounds in a match in all modes if lost cards on ante).
* Correct (fingers crossed) interaction of sideboarding with other relevant aspects of Forge rule enforcement (mulligan and ante interactions were corrected, initial hand and library between rounds were both corrected, everything else looks so far so good).

We don't yet have:

* Sorting in the in-game sideboard editor.
* AI sideboarding.


The deck conformance/legality limitaion:

The deck conformance/legality is now a user-togglable preference and is enabled by default. You no longer need to turn on dev mode to play an illegal deck.


Using Forge on displays that are only 600 pixels tall or slightly larger:

The "Sanctioned Format: Constructed" view should now be compatible with displays that are only 600 pixels tall. The deck list at 600 pixels tall should now display three lines of text rather than less than a single line of text.


We are looking for help in finding additional sound files for the new sound feature:

This version of forge includes a few sound files for the new sound effect system. While we have several sounds assigned to a few of the available events there are a number of events that do not yet have a assigned sound file. This should be considered a work in progress and we could use some help in finding interesting sounds that we can add to forge.

The sound files need to be in wav or au format, wav appears to be more widespread but the code can handle either format. The sound files need to be copyright-free and they should be in the public domain.

You can either record your own sounds if you have the necessary equipment or you may be able to find an appropriate sound on a website such as http://www.freesound.org/browse/

You should note that sound files can be large and we would like to avoid this if possible. A good size to shoot for would be 50 K or less. There is a freeware sound editor that may have versions for all operating systems. This app is named Audacity.

We have a forge forum topic at the Collectible Card Games Headquarters web site that is devoted to finding sounds for this new sound system. Please visit this topic and contribute a sound or two. We can use your help and assistance. :)

http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=8570


Notes about the second Quest World, Jamuraa:

A second Quest World, Jamuraa, has been added to Forge. When playing Quest mode, it is now possible to 'Travel' between the regular Quest environment and the two Worlds, Shandalar and Jamuraa, both of which include special formats, opponents and challenges. Or you can start a new Quest in any of them.

Like Shandalar, Jamuraa is a fantasy world. Its peaceful existence has recently been wrecked by a planar conjunction that shattered the barriers between Jamuraa and the infernal planes known as Jahim, Saqar, and Jahannam. The demon planeswalkers who rule those planes, and their hellish sister, Lilith, are now extending their influence over Jamuraa and gradually destroying the whole continent. Your task is to fight their minions and ultimately challenge the four demons - but beware, their destructive might is unfathomable!

From a technical perspective, the following sets are available to the player in Jamuraa: 
5th Edition, Arabian Nights, Mirage, Visions, Weatherlight.

Jamuraa contains:
- 81 opponent decks, broken down as follows: 13 'easy' decks, 17 'medium' decks, 31 'hard' decks, and 20 'very hard' decks.
- 9 challenges, including the 4 demon planeswalkers (the 3 demon rulers and Lilith) and 5 other special scenarios set in Jamuraa. All challenges are repeatable. All are fairly hard, and the 4 demon challenges are especially fiendish.
For the most part, the opponent duel and challenge decks are built with the same format restrictions as your own cardpool, and some of the easiest opponent decks were in fact based on a limited cardpool. But there will be exceptions, especially in the hard/very hard decks and challenges, which can be more like Vintage/T1 decks than pure Mirage + 5th Edition decks. There will be older cards here and there, and maybe even a random Tempest card or two (although these are extremely scarce!). 
Hint: if you find the later 'Vintage' opponent decks unfair or near-impossible to beat with your 5th Edition/Mirage block cards, you can Travel to Shandalar and collect some old power cards there, and then return to Jamuraa. Just remember to complete your challenges before traveling.

Information on the quest worlds format can be found in this topic:

http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=9258


New Deck Editor features with improved Filters:

Some work is being done on the various deck editors -- including the quest spell shop -- and we hope to add in additional features while improving the UI. Here is a quick tutorial on the new features:

FILTERS
The Filters tab has been removed and filters are now controlled from the Card Catalog tab. Pretty much everything that used to be a checkbox is now represented by a toggle button. The statistics labels at the top for colors and card types can now be clicked to toggle display of the related cards. Clicking the Total Cards label in the upper left will alternately set all filters on and off.

Text searches are done by just typing the the text box. The results will update as you type. Use the combo box to the right of the text box to set whether the search should be for cards with or without the specified text. The three toggle buttons to the right of the combo box allow you to specify where in the card the text should (or should not) match. Complex text searches, such as for goblin creatures with haste but not with kicker costs, are possible by stacking the search filters. Put the current search filter on the stack by selecting the "Add filter" button to the left of the text box and choosing the "Current text search" option. You can also use the keyboard shortcut Ctrl+Enter (Cmd+Enter on OSX). This will keep your current text search applied, but will clear the text box and allow you to input the next search filter. To perform the example goblin search above, you would:
1) Ensure "in" is displayed in the the combo box. Enter "goblin" in the text box and deselect Name and Text so that only Type is selected.
2) Hit Ctrl+Enter (Cmd+Enter on OSX). Notice the "Contains: 'goblin" in: type" filter appears below the text box.
3) Type "haste" in the text box. Deselect Type and select Text. Hit Ctrl+Enter.
4) Change the combo box from "in" to "not in". Type "kicker" in the text box.
The shown entries match your combined search filter. Click the little 'x' in the upper right corner of each search widget to remove that filter from the filter stack.

Format filters allow you to restrict the shown cards to those that are legal in a particular format. Select the "Add filter" button, hover over the "Format" submenu, and choose one of the defined formats. The filter will appear below the text box and will be combined with whatever other filters you have on the stack. Hover the mouse over the filter widget to see details on allowed sets and banned cards. Keep in mind that cards from other, non-listed sets may still appear in the results if they are just reprints of allowed cards.

Set filters are similar to format filters except that a window will come up with a grid of checkboxes so you can select exactly which sets you would like shown. There is a checkbox at the bottom (off by default) that will allow the filter to include cards reprinted in unselected sets, just like the format filter above. If you don't check this checkbox, only cards printed in the selected sets will be shown, allowing you to focus on just that set's version of a particular card. This is very useful in quest mode when determining which cards in a set you have yet to collect.

Value range filters allow you to restrict the cards by power, toughness, and/or converted mana cost (CMC). The text boxes that appear in the filter widget are editable and update the shown cards in realtime as you modify the numbers.

Quest World filters are similar to Format filters in that they restrict the shown cards to a group of sets, respecting lists of banned cards. They are useful when constructing decks that will be valid in the various quest worlds. You can have more than one quest world filter active at the same time; useful if you are constructing a deck that will be used in multiple worlds.

SPELL SHOP
The spell shop interface has also received some usability enhancements. The first one you may notice is the addition of a new column named "Owned". This column is intended to help players decide whether buying an item is likely to be beneficial. The data in this column varies depending on what kind of item the row represents:
Cards: A number indicating how many of a particular card a player already owns
Preconstructed decks: A "YES" or "NO" indicating whether the deck exists in the player's deck list
Booster/fat packs: A percentage indicating how close a player is to completing the related set.  "Complete" means at least one of every basic land and at least 4 of every other card.
If you don't want this column, it can be turned off in the editor preferences.

The new "Sell excess cards" button appears above the player's library. Clicking it will sell all cards that are not basic lands until only four copies of the cards remain. It's a one-click "cleanup" of the library and a great way to safely and quickly regain some cash.

The "Full catalog view" button appears to the left of the "Buy Card" button. Toggling this button will switch between showing the store's inventory and the full catalog. By applying a filter to show only a particular set (or group of sets), players can use this view to discover exactly which cards they do not own. Buying and selling cards is disabled while in this view.

Multibuy: By selecting any number of items and hitting space (or selecting the "Buy Card" or "Sell Card" buttons), a player can buy one of everything selected.


Our Lawyers Made Us Do This:

This product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).
