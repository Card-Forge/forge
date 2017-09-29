Forge: 09/29/2017 ver 1.6.4

17223 cards in total.


--------------
Release Notes:
--------------

- New Cards -
Celestial Convergence, Divine Intervention, Bosium Strip

- Ixalan -
All 279 cards of the newly released Ixalan set are available in Forge. We've done our best to fix the issues that you reported with the cards in the pre-release version. If you still see anything wrong with the new cards, don't hesitate to report!

- Allow Ordering Cards Put in Graveyard (option) -
A new option is available in Forge that makes the game determine when to offer you to order the cards as they go simultaneously into graveyard if, for example, several cards are destroyed, sacrificed or milled at the same time. There are three states: 1) "Never" (which is the default, when Forge does not let you choose the order of cards going to graveyard since in most cases it doesn't matter); 2) "With Relevant Cards" (you will be allowed to order cards in case there is at least one card in your library or on your side of the battlefield that cares about the order of cards in graveyard; currently the following cards are marked as caring about graveyard order: Nether Shadow, Spinning Darkness, Corpse Dance, Shallow Grave, Phyrexian Furnace, Krovikan Horror, Volrath's Shapeshifter, Ashen Ghoul, Phyrexian Grimoire, Nature's Kiss, Soldevi Digger, Guiding Spirit, Barrow Ghoul, Circling Vultures, Zombie Scavengers, Necratog, Mistmoon Griffin, Bone Dancer, Bosium Strip, Alms, Death Spark; 3) "Always" (you will always be prompted to order cards simultaneously put in graveyard, even if there is no immediate detected need for it judging by the contents of your library and battlefield). Note that this option does not affect cards that reorder the graveyard as a part of their effect (Fossil Find). If this option is set to "Never", which is also the default, then no ordering is performed for cards like Volrath's Shapeshifter and the cards go into graveyards in whatever order the game automatically determines them to do so (this is the original Forge behavior). This mechanism is not perfect yet (please report cases in which you were not allowed to order cards in the graveyard, as well as any strange behavior in corner cases, e.g. when some permanents are indestructible, etc.).
NOTE: in Mobile Forge, this option is called "Order Graveyard" due to space constraints in portrait mode common on cellphones and smaller tablets.

- Desktop Forge: Personal Card Ratings in Quest Mode -
In Desktop Forge, it is now possible to assign personal ratings (from 1 star to 5 stars) to cards in Quest Mode by right clicking them and choosing the relevant context menu entry. It is then possible to filter cards by rating in both the deck editor and the quest shop, which should simplify managing bigger inventories. This patch was provided by Seravy.

- Desktop Forge: Rotate Split Card when Zoomed -
It is now possible to visually rotate the split card (visualize it vertically or horizontally) when zooming in on it either with the help of a mouse wheel or by pressing the Control button on your keyboard. This may be useful for Aftermath cards. There is a limitation right now: this will not work if a split card is manifested or otherwise face down (a card like that will always be visualized vertically).

- Mobile Forge: Partner Commander support -
It is now possible to set partner commanders through the user interface in mobile Forge, both in Constructed EDH and in Planar Conquest. When you have a commander that can be a legal partner for your current commander, you will have a new option "Set as Partner Commander" available to you in the dropdown menu that you get when you tap the card in the deck editor.

- Mobile Forge: Rotate Split Cards on Zoom (option) -
By default, mobile Forge will now rotate split cards when zooming in on them, to aid in seeing their halves without having to rotate the device. This may look too small in three-card zoom mode and escape the screen bounds in single-card zoom when in portrait mode, especially on a smaller device, so if you see undesirable side effects like that, please disable the option "Rotate Zoom Image of Split Cards".

- Bug fixes -
As always, this release of Forge features an assortment of bug fixes and improvements based on user feedback during the previous release run.

-------------
Known Issues:
-------------

Images for the latest sets will be available soon.

Online play is unfinished and is not fully operational. While several users have reported moderate success in getting a simple online match going, most users have experienced crashes and/or inability to start a server or connect to it. At the moment, we do not have a dedicated developer actively working on the online play feature, so we do not have an ETA as to when this feature will become finished. If you have working knowledge of Java that, you believe, is adequate to help seeing this feature through to completion, please consider offering your help in our Discord channel.

At least two cards that have a clause that says "if a card cast this way would be put into a graveyard this turn, exile it instead" (Kess, Dissident Mage and Bosium Strip) will fail to interact correctly in corner cases where another card is allowing you to also cast cards from the graveyard at the same time, but allows you not to exile them. This needs a better implementation of a check that would allow to determine that the card is being cast via a MayPlay effect from a particular source.

There is a known issue with the cost reduction for cards that have color-locked X in their mana cost (e.g. Drain Life, Soul Burn). Cost reduction will not apply correctly to these cards if the amount by which the cost is reduced is greater than the amount of colorless mana in the mana cost specified on the card (e.g. 1 for Drain Life, 2 for Soul Burn). Fixing this issue likely requires rewriting the way announced color-locked X is interpreted and paid (most likely it has to be represented with colorless mana shards but still locked to the colors required by the card).

Currently Improvise is implemented as a "clone" of Convoke keyword, which does not work correctly in corner cases (for example, together with an instance of Convoke or Delve). This is planned to be addressed soon.

Replacement effects that happen when a card moves from zone to zone (e.g. ETB replacement effect of Essence of the Wild; Kalitas, Traitor of Ghet replacement effect for a dying creature) need some rework to allow all of them to work in a rule-exact way without the need for special exclusions and hacks (see ReplacementHandler.java:120).

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