Forge: 12/03/2017 ver 1.6.5

17224 cards in total.


--------------
Release Notes:
--------------

- Quest World: Lorwyn -
The quest world based on Lorwyn and created by Erazmus in 2013 has now been integrated. It contains 48 duels and 10 challenges to overcome.

- AI Improvements -
The AI has learned some new tricks in this version, which may be subtle but will hopefully surprise the player and make the game more exciting and challenging.
Over 700 card scripts have been revised for AI playability. Most of these scripts were created many years ago, when our artificial intelligence was a lot simpler and could not do many of the things it can do now. Therefore, many cards which were marked as "AI-unplayable" are, in fact, playable with the current engine. These cards have now been properly marked as AI playable and their AI flags have been updated as necessary. Note that while tests have been run for every single card, some cards may have been marked erroneously because they may misbehave in corner cases or other unexpected situations. If cases like that are found out, we will try to patch up the AI to make it work correctly, but if it proves to be too difficult or impossible in short term perspective, we will mark those cards as unplayable again.

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