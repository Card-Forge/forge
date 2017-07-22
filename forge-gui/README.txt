Forge: 07/22/2017 ver 1.6.1

16896 cards in total.


--------------
Release Notes:
--------------

- New Cards -
Vizier of the True, Vizier of the Anointed, Visage of Bolas, Vile Manifestation, Zealot of the God-Pharaoh, Khenra Scrapper, Kefnet's Last Word, Khenra Eternal, Torment of Scarabs, Torment of Venom, Torment of Hailfire, Tragic Lesson, Tenacious Hunter, The Scarab God, Thorned Moloch, The Scorpion God, The Locust God, Claim, Crested Sunmare, Chandra's Defeat, Countervailing Winds, Crash Through, Carrion Screecher, Champion of Wits, Chaos Maw, Crypt of the Eternals, Consign, Crook of Condemnation, Cunning Survivor, Grisly Survivor, Grind, Granitic Titan, Graven Abomination, Gideon's Defeat, God-Pharaoh's Faithful, God-Pharaoh's Gift, Gilded Cerodon, Gift of Strength, Wildfire Eternal, Without Weakness, Wall of Forgotten Pharaohs, Wretched Camel, Wasp of the Bitter End, Imaginary Threats, Inferno Jet, Imminent Doom, Ifnir Deadlands, Ipnu Rivulet, Desert of the True, Defiant Khenra, Desert of the Fervent, Driven, Desert of the Mindful, Djeru, With Eyes Open, Dune Diviner, Dagger of the Worthy, Dunes of the Dead, Dreamstealer, Djeru's Renunciation, Dauntless Aven, Desert's Hold, Desert of the Indomitable, Doomfall, Disposal Mummy, Desert of the Glorified, Devotee of Strength, Dutiful Servants, Jace's Defeat, Angel of the God-Pharaoh, Adorned Pouncer, Aven Reedstalker, Abrade, Act of Heroism, Abandoned Sarcophagus, Accursed Horde, Avid Reclaimer, Ambuscade, Ammit Eternal, Appeal, Angel of Condemnation, Aven of Enduring Hope, Apocalypse Demon, Aerial Guide, Puncturing Blow, Pride Sovereign, Proven Combatant, Rhonas's Last Stand, Riddleform, Razaketh's Rite, Ramunap Hydra, Resilient Khenra, Refuse, Ruin Rat, River Hoopoe, Rampaging Hippo, Resolute Survivors, Razaketh, the Foulblooded, Ramunap Ruins, Rhonas's Stalwart, Reason, Ramunap Excavator, Lurching Rotbeast, Leave, Life Goes On, Liliana's Defeat, Lethal Sting, Hour of Devastation, Hour of Eternity, Hostile Desert, Hour of Revelation, Hollow One, Hashep Oasis, Hour of Promise, Hour of Glory, Hope Tender, Harrier Naga, Hazoret's Undying Fury, Nissa's Defeat, Nissa's Encouragement, Nicol Bolas, God-Pharaoh, Nimble Obstructionist, Nicol Bolas, the Deceiver, Neheb, the Eternal, Nissa, Genesis Mage, Steward of Solidarity, Seer of the Last Tomorrow, Swarm Intelligence, Sinuous Striker, Solemnity, Sidewinder Naga, Sunscourge Champion, Samut, the Tested, Scrounger of Souls, Saving Grace, Solitary Camel, Survivors' Encampment, Sunset Pyramid, Scavenger Grounds, Sifter Wurm, Striped Riverwinder, Sand Strangler, Spellweaver Eternal, Supreme Will, Struggle, Shefet Dunes, Steadfast Sentinel, Burning-Fist Minotaur, Bloodwater Entity, Beneath the Sands, Brambleweft Behemoth, Blur of Blades, Banewhip Punisher, Bitterbow Sharpshooters, Bontu's Last Reckoning, Eternal of Harsh Truths, Earthshaker Khenra, Endless Sands, Unesh, Criosphinx Sovereign, Unquenchable Thirst, Unraveling Mummy, Unconventional Tactics, Uncage the Menagerie, Oasis Ritualist, Obelisk Spider, Overcome, Oketra's Avenger, Oketra's Last Mercy, Ominous Sphinx, Overwhelming Splendor, Open Fire, Frontline Devastator, Firebrand Archer, Frilled Sandwalla, Farm, Fervent Paincaster, Feral Prowler, Fraying Sanity, Mirage Mirror, Manticore Eternal, Marauding Boneslasher, Mummy Paramount, Moaning Wall, Merciless Eternal, Meddle, Magmaroth, Majestic Myriarch, Quarry Beetle

- New Quest World: Urza's Block -
This release features a new quest mode world based on one of the most powerful blocks of the first decade of Magic: the Gathering, the Urza's block. The new world was designed by Seravy and it features 30 duels and 20 challenges for you to attempt.

- Planar Conquest (Mobile Forge only) -
There is a new plane available in Planar Conquest mode in mobile Forge - Amonkhet, submitted by Sirspud. The plane contains cards from AKH, HOU, as well as Amonkhet Invocations.

- Puzzle Mode -
Puzzle Mode is now available in mobile Forge.

- Foil Chance in Boosters -
This release features an overhaul of the chance of generating foils of various rarities in booster packs, making foils of higher rarity more rare compared to those of lower rarity instead of offering an equal chance of finding a foil card of each rarity. Also, Masterpieces are now more rare (although most of them are still more commonly found than in real life and this needs further tweaking). For the first time in Forge, in Vintage Masters you'll be able to find not only foil Power Nine cards, but also non-foil ones (the game now correctly differentiates between the two), but both are very rare.

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