Forge: 08/18/2017 ver 1.6.2

16964 cards in total.


--------------
Release Notes:
--------------

- New Cards -
Vindictive Lich, Kindred Charge, Kess, Dissident Mage, Kheru Mind-Eater, Kindred Discovery, Kindred Summons, Kindred Dominance, Kindred Boon, Taigam, Ojutai Master, Traverse the Outlands, Taigam, Sidisi's Hand, Teferi's Protection, The Ur-Dragon, Crimson Honor Guard, Chain of Acid, Curse of Disturbance, Curse of Vitality, Curse of Opulence, Curse of Bounty, Choose Your Demise, Chain of Silence, Curse of Verbosity, Galecaster Colossus, Wasitora, Nekoru Queen, Worms of the Earth, Izzet Chemister, Inalla, Archmage Ritualist, Delight in the Hunt, Disrupt Decorum, A Reckoning Approaches, Arahbo, Roar of the World, Power Leak, Patron of the Vein, Path of Ancestry, Ramos, Dragon Engine, Liege of the Hollows, Licia, Sanguine Tribune, Hungry Lynx, Herald's Horn, Hazduhr the Abbot, Heirloom Blade, Hammer of Nazahn, Nazahn, Revered Bladesmith, New Blood, Shifting Shadow, Scalelord Reckoner, Bloodsworn Steward, Balan, Wandering Knight, Bloodforged Battle-Axe, Boneyard Scourge, Bow to My Command, Bloodline Necromancer, Errant Minion, Edgar Markov, Every Dream a Nightmare, O-Kagachi, Vengeful Kami, For Each of You, a Gift, Fortunate Few, Fractured Identity, Mairsil, the Pretender, Mirror of the Forebears, Make Yourself Useful, My Forces Are Innumerable, Mirri, Weatherlight Duelist, Magus of the Mind, My Laughter Echoes, Mathas, Fiend Seeker, Qasali Slingers

- Commander 2017 Spoiler Season -
Most Commander 2017 cards have been implemented and are ready for testing. Some of the remaining few unimplemented cards may be scripted in the future.

- Conspiracy -
Conspiracy and Conspiracy: Take the Crown are now available for drafting in Booster Draft mode. Support for Conspiracy is still somewhat experimental and is a little rough around the edges - in particular, choosing card names in the beginning of the game is a little awkward, not all Conspiracies are implemented, and the AI could use some improvement. Only 1v1 matches are available at the moment (like in any other Forge booster draft). Please note that because the "draft matters" cards are not yet implemented in Forge, drafting Conspiracy will feel largely incomplete, so please try this mode at your own risk and consider it a work in progress.

- Puzzle Mode -
Many new puzzles were implemented, including several that start in combat with attackers declared and some that require you to survive for a certain set of turns. Over 75 puzzles are available now in Puzzle Mode.

- New Quest World: Caliman -
In Quest mode, a new world has been implemented by Xyx: Caliman, based on Portal: the Second Age. The author describes this new world in the following way.

Portal Second Age is the first official "Starter Level" set (a term not yet in use when the original Portal was released.) It outdoes its predecessor in easy-to-understand cards. Portal Second Age has a unique steampunk flavor that hasn't been replicated since, featuring firearms, steam engines and zeppelins. The open-ended story is staged in the Dominarian island of Caliman and focuses on the conflicts between the kingdom of Alaborn, the Talas merchant/pirates (some say there is no difference), the goblin tribes, the elves of Norwood forest and the nightstalkers created by the evil Tojira, swamp queen of Dakmor.

- Card Zoomer Shortcut -
There is a new configurable keyboard shortcut available in desktop Forge: toggle card zoom on the current card (default "Z").

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