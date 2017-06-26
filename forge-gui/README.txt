Forge Beta: 06/26/2017 ver 1.5.65

16712 cards in total.


--------------
Release Notes:
--------------

- New Cards -
The Mighty Will Fall, There Is No Refuge, This World Belongs to Me, What's Yours Is Now Mine, When Will You Learn?, Power Without Equal, Pay Tribute to Me, No One Will Hear Your Cries, Because I Have Willed It, Behold My Grandeur

- New Counter Display -
Counters now have a new, text-based display option alongside the old counter images. It provides a textual overlay on top of each card (that has counters) showing all of the counters on the card with their names and exact counts, each type being displayed in a unique color. This can be customized to show only text-based counters (default), only the old style, both at the same time, or show the old image when cards are small to fit the textual display.

- Automatically Suggest Basic Lands in Deck Editor -
It is now possible to ask Forge to automatically suggest basic lands for your deck in the Add Basic Lands dialog of the deck editor. In desktop Forge, this is accomplished by double-clicking the statistics text in the bottom part of the dialog (hopefully someone will be able to code in a better, more intuitive way of doing this eventually). In mobile Forge, this is done by double tapping the statistics text. A tooltip is available in desktop Forge (and a hint text in mobile Forge) to make this feature more visible. Three deck sizes are supported by this algorithm: 40-card decks (if the number of cards in the main portion of the deck is less than 30, the algorithm will target a 40-card deck), 60-card decks (if the number of cards in the main portion is between 30 and 60, a 60-card deck will be targeted), and 100-card decks (if the deck is already above 60 cards when the feature is used, a 100-card deck will be targeted).

- Planar Conquest Updates -
In Planar Conquest, if you have already unlocked all the planes, receiving a green reward will cause a Chaos Battle to start instead of awarding 5 planeswalker emblems.

- Java 8 -
As part of increased privacy protections (cardforge.org now uses SSL), the content downloaders now require a minimum Java version of 8u101. In the future, all of Forge will be updated to require Java 8. If you're running the latest version of Java, but Forge does not let you use the content downloaders, check to make sure that you've uninstalled old versions. Additionally, check any JDK installations you have and ensure Forge is using the most up-to-date version.

- Hour of Devastation Spoiler Period -
This release contains some cards from the upcoming Hour of Devastation set. They are an early effort and may thus contain errors or provoke crashes. Please test them if you'd like and provide feedback if you encounter any issues. These cards should only be visible and playable if Developer Mode is enabled in the game options.

- Quest UI Improvements -
When selecting sets for a custom quest format, you can now hold shift and select two sets and it will select all sets between your two clicks. You can also now click on any part of a duel or event's box to select it; you don't need to click just the name/radio button anymore.

- Bug fixes -
As always, this release of Forge features an assortment of bug fixes and improvements based on user feedback during the 1.5.64 release run.

-------------
Known Issues:
-------------

Images for the latest sets will be available soon.

Aluren currently does not allow to cast creatures from other zones (e.g. Graveyard) if there is an active effect from another card permitting to cast it from there. This interaction is rather tricky to implement properly, but hopefully it will be resolved soon.

There is a known issue with the cost reduction for cards that have color-locked X in their mana cost (e.g. Drain Life, Soul Burn). Cost reduction will not apply correctly to these cards if the amount by which the cost is reduced is greater than the amount of colorless mana in the mana cost specified on the card (e.g. 1 for Drain Life, 2 for Soul Burn). Fixing this issue likely requires rewriting the way announced color-locked X is interpreted and paid (most likely it has to be represented with colorless mana shards but still locked to the colors required by the card).

Currently Improvise is implemented as a "clone" of Convoke keyword, which does not work correctly in corner cases (for example, together with an instance of Convoke or Delve). This is planned to be addressed soon.

Replacement effects that happen when a card moves from zone to zone (e.g. ETB replacement effect of Essence of the Wild; Kalitas, Traitor of Ghet replacement effect for a dying creature) need some rework to allow all of them to work in a rule-exact way without the need for special exclusions and hacks (see ReplacementHandler.java:120).

There is a known issue that allows Qasali Ambusher to be cast from any zone for its ambush ability (requires MayPlay update to be fixed). For now, a temporary measure was set up to prevent the AI from abusing this issue, but it is up to the human player to deliberately choose not to abuse this when possible.

Several people have noticed that the cards displayed on the battlefield will fail to be displayed when the number of cards on the battlefield increases. Maximizing the human panel can help to re-display the cards.

Some time was spent turning the static ETB triggers into the proper ETB replacement effects they should be, mainly to interact correctly with each other. This work is not yet finished. As a result there is currently some inconsistencies with "Enters the battlefield with counters" (Not incredibly noticeable).

A recent contribution to the code base should fix some of the bugs that people noticed with cloning type abilities. At this time there is one remaining issue that we hope will be addressed in the near future:
Copies of cards that setup Zone Change triggers via addComesIntoPlayCommand and addLeavesPlayCommand will not function correctly.

-------------
Installation:
-------------

The Forge archive includes a MANUAL.txt file and we ask that you spend a few minutes reading this file as it contains some information that may prove useful. We do tend to update this file at times and you should quickly read this file and look for new information for each and every new release. Thank you.

The archive format used for the Forge distribution is ".tar.bz2". There are utilities for Windows, Mac OS and the various *nix's that can be used to extract/decompress these ".tar.bz2" archives. We recommend that you extract/decompress the Forge archive into a new and unused folder.

Some people use the Windows application 7zip. This utility can be found at http://www.7-zip.org/download.html. Mac users can double click on the archive and the application Archive Utility will launch and extract the archive. Mac users do not need to download a separate utility.

Once the Forge archive has been decompressed you should then be able to launch Forge by using the included launcher. Launching Forge by double clicking on the forge jar file in the past caused a java heap space error. Forge's memory requirements have increased over time and the launchers increase the java heap space available to Forge. Currently you can launch Forge by double clicking on the forge jar file without a java heap space error but this is likely to change as we add in more sounds, icons, etc.

- The Mac OS application version -
We haven't been able to distribute the OS X Application version of Forge in sometime. We've recently automated our release tools, and will continue to look in the viability of creating this file now that things are autoamted.

--------------------
Active Contributors:
--------------------

Agetian
Austinio7116
DrDev
excessum
Gos
Hanmac
Indigo Dragon
KrazyTheFox
Marek14
mcrawford620
Myrd
nefigah
pfps
Sloth
slyfox7777777
Sol
Swordshine
tjtillman
tojammot
torridus
Xyx
Zuchinni

(Quest icons used created by Teekatas, from his Legendora set http://raindropmemory.deviantart.com)
(Thanks to the XMage team for permission to use their targeting arrows.)
(Thanks to http://www.freesound.org/browse/ for providing some sound files.)