Forge: 07/08/2017 ver 1.6.0

16895 cards in total.


--------------
Release Notes:
--------------

- Java 8 -
Forge now requires Java 8 (1.8.0) as a bare minimum. If you haven't updated to this version of Java yet, you would need to do so before you are able to run this and all subsequent Forge releases. Forge is compatible both with Oracle Java 8 and with OpenJDK 8. If you're running the latest version of Java, but Forge does not run for you, check to make sure that you've uninstalled old versions. Additionally, check any JDK installations you have and ensure Forge is using the most up-to-date version.

- Optional More Duels Setting for Quest Mode -
In Quest Preferences, there is now an option called More Duel Choices which, when set to 1, makes the game produce additional duel choices from the lower difficulties. For example, at the Hard tier, you will still get an extra Easy and an extra Medium opponent to face.

- Foil Card Filter -
In the deck editor, there is now an additional Foil filter which allows one to sort cards by their foil overlay, choosing from three options: cards with the Modern style foil, cards with the Old style foil, and non-foil cards.

- Unconstrained ("Far") Set Unlocks in Quest Mode -
There are now two additional options in Quest Mode Preferences. "Allow Far Unlocks" allows the player to unlock any set, regardless of distance of the set (from the sets you currently have unlocked). "Unlock Distance Multiplier" is a multipler that gets applied for each step the set is further away, starting after the fifth. For example, if it's at the default of 1.25, that means, the first five sets on the list will have the default cost. The sixth costs 1.25 times more. The seventh costs 1.25^2 more, the next 1.25^3 and so on, the multiplier is capped at 500 to prevent overflow, but if there is demand for higher (default card price data has some crazy numbers, you can actually expect to be able pay the 2 million for the 500x multiplier), it can easily be changed to a different number before adding the patch. It's important to note that the order in the window counts. This means if you already did some "far" unlocks, your list might put sets you'd expect to be cheap at a high price. Why? Because with 3 distant sets, there are 3 times as many sets at a "near" distance to you, that get put in front of your intended set. So when doing far unlocks, keep that in mind and don't expect the set that was 5th on your list to stay there. It might get pushed down by another 5 or even more slots, as sets close to your new unlock are added to it.

- More Puzzles -
This release features over 30 puzzles to crack in Puzzle Mode. All the included puzzles rely on the features that Forge should support well, but because the mode is currently in its early development stages, some issues are to be expected. Please report any errors and inconsistencies you may find.

- Planar Conquest Updates -
A new plane is now available in Planar Conquest (Kaladesh), implemented by Sirspud. Also, if you have already unlocked all the planes, receiving a green reward on the chaos wheel will cause a Chaos Battle to start instead of awarding 5 planeswalker emblems.

- Hour of Devastation Pre-release -
This release contains all cards from the upcoming Hour of Devastation set. We did our best to fix the issues you reported so far with these cards, but there may still be more, so please consider these cards beta quality. Please test them if you'd like and provide feedback if you encounter any issues. These cards should only be visible and playable if Developer Mode is enabled in the game options.

- Bug fixes -
As always, this release of Forge features an assortment of bug fixes and improvements based on user feedback during the previous release run.

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
Seravy
Sirspud
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