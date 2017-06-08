Forge Beta: 06/08/2017 ver 1.5.64

16699 cards in total.


--------------
Release Notes:
--------------

- IMPORTANT: Partial backwards incompatibility with older decks that contain Masterpieces -
In case you have decks in your profile folder that contain one or more Masterpieces from the Kaladesh/Aether Revolt masterpiece series (previously the set code was MPS), you may experience an issue with cards that are present in the Masterpieces set disappearing visually and malfunctioning in the game (even if you use a version of the same card from a different set). The two primary ways this manifests itself are: a) you get a message stating that there is an unsupported card in one of your decks (this message shows in the console only); b) the card picture does not show in the game or in the deck editor and the card is reported as non-functional. If this happens to you, you have several options: either remove the offending deck altogether (you may have to do a search inside the profile folder to determine which decks have the mentioned card from the MPS set), or manually edit the deck file and change the set code MPS to MPS_KLD for such cards, or start with a fresh profile folder or at least fresh deck subfolder (but the latter obviously only works if you don't mind losing your entire progress and would like to start fresh). We apologize for the inconvenience.

- Planar Conquest Changes -
Planar Conquest received a rather major overhaul of planes. Additional cards from Commander-specific sets and other variant sets (most notably Conspiracy and Planechase) have been added to planes where they are relevant according to the plane theme and lore. Implemented planes now have descriptions that are displayed on the "Select a Plane" and "Planeswalk" screens when the player taps the plane art. Four-color commanders from C16 are now supported in Planar Conquest (the player will start with a random 3-color deck matching random three of the four colors of the color identity of such a planeswalker). There is also experimental support for planeswalkers that can also be commanders (e.g. Daretti, Scrap Savant), although this hasn't been extensively tested yet, so certain issues are possible.
Some adjustments have been made to the algorithm used for calculating probabilities of drawing a card from the Aether which is of rarity that is higher than selected in the filter. Higher rarity cards should now be considerably less common and thus the Aether should not be nearly as susceptible to being exploited for shards.

- Achievement System updates -
Planar Conquest now uses its own set of achievements separate from Constructed achievements (this includes the Planar Conquest-exclusive Planeswalker game mode). Puzzle Mode should no longer give Constructed achievements and uses its own achievement set (currently only the number of solved puzzles is tracked, but this may improve in the future).

- Java 8 -
As part of increased privacy protections (cardforge.org now uses SSL), the content downloaders now require a minimum Java version of 8u101. In the future, all of Forge will be updated to require Java 8. If you're running the latest version of Java, but Forge does not let you use the content downloaders, check to make sure that you've uninstalled old versions. Additionally, check any JDK installations you have and ensure Forge is using the most up-to-date version.

- Amonkhet Release -
This release contains all but one of the cards (As Foretold) from the newly released Amonkhet set. Hopefully we'll get the last one out soon enough.

- Bug fixes -
As always, this release of Forge features an assortment of bug fixes and improvements based on user feedback during the 1.5.61 release run.

- AI Generated Decks -
This release contains a new way to generate random Standard or Modern decks that are themed around a specific card.  In the deck selector you choose a card and the AI will build a deck around that card based on associations learnt from recent human generated online decks.  The algorithm is non-deterministic, so the decks will vary every time they are generated for a unique deck virtually every time.

- User Created Quest Worlds -
If you have created some of your own quest worlds and don't want them to be overwritten when Forge updates, you can move the world folders to the user quest folder (in USER_FOLDER/quest/worlds) - you will also need to add the world definitions to a customworlds.txt file in that same folder

- Quest Shop Singles Update -
The quest shop will now generate singles using the full availability of cards in a set, including things like Amonkhet Invocations and Kaladesh Inventions. The rarity of these additional cards is unchanged and it may take some time for them to finally appear.

- Booster Pack Quest Starting Pool -
Quests can now use booster packs as a starting pool. A number of random packs will be opened and used instead of the normal card generation. You can find this option in the "Choose Colors" section when creating a new quest. This will override any other starting pool settings.

- Card Image Updates -
Most (if not all) of the missing card images have been added to the in-game content downloaders.

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