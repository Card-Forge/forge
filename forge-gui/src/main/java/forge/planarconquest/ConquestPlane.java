/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.planarconquest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;

import forge.GuiBase;
import forge.assets.ISkinImage;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardEdition.CardInSet;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.util.Aggregates;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;


public enum ConquestPlane {
    Alara("Alara", new String[] {
            "ALA", "CON", "ARB"
    }, new String[] {
            "Bant", "Grixis", "Jund", "Naya"
    }, new String[] {
            "Unstable Obelisk", "Baleful Strix", "Shardless Agent", "Etherium-Horn Sorcerer",
            "Patron of the Valiant", "Sublime Archangel", "Naya Soulbeast", "Stalwart Aven",
            "Pharika's Disciple", "Acolyte of the Inferno", "Citadel Castellan", "Ajani's Mantra",
            "Consul's Lieutenant", "Firefiend Elemental", "Goblin Glory Chaser", "Honored Hierarch",
            "Knight of the Pilgrim's Road", "Outland Colossus", "Relic Seeker", "Rhox Maulers",
            "Scab-Clan Berserker", "Topan Freeblade", "Undercity Troll", "Valeron Wardens",
            "War Oracle", "Derevi, Empyrial Tactician", "Kaalia of the Vast", "Bloodspore Thrinax",
            "Cathedral of War", "Duskmantle Prowler", "Duty-Bound Dead", "Knight of Glory", "Knight of Infamy",
            "Nefarox, Overlord of Grixis", "Servant of Nefarox", "Celestial Flare", "Viscera Seer",
            "Meren of Clan Nel Toth", "Jazal Goldmane", "Jhessian Thief", "Maelstrom Wanderer",
            "Onyx Mage", "Preyseizer Dragon", "Thromok the Insatiable", "Restore", "Rhox Faithmender",
            "Rhox Pikemaster", "Roon of the Hidden Realm", "Scourge of Nel Toth"
    }, new Region[] {
            new Region("Bant", "Bant Panorama", MagicColor.GREEN | MagicColor.WHITE | MagicColor.BLUE),
            new Region("Esper", "Esper Panorama", MagicColor.WHITE | MagicColor.BLUE | MagicColor.BLACK),
            new Region("Grixis", "Grixis Panorama", MagicColor.BLUE | MagicColor.BLACK | MagicColor.RED),
            new Region("Jund", "Jund Panorama", MagicColor.BLACK | MagicColor.RED | MagicColor.GREEN),
            new Region("Naya", "Naya Panorama", MagicColor.RED | MagicColor.GREEN | MagicColor.WHITE)
    }),
    Dominaria("Dominaria", new String[] {
            "ICE", "ALL", "CSP",
            "MIR", "VIS", "WTH",
            "USG", "ULG", "UDS",
            "INV", "PLS", "APC",
            "ODY", "TOR", "JUD",
            "ONS", "LGN", "SCG",
            "TSP", "PLC", "FUT"
    }, new String[] {
            "Academy at Tolaria West", "Isle of Vesuva", "Krosa", "Llanowar", "Otaria", "Shiv", "Talon Gates"
    }, new String[] {
            "Crown of Empires", "Scepter of Empires", "Throne of Empires", "Roc Egg", "Brindle Boar",
            "Armored Cancrix", "Academy Raider", "Alaborn Cavalier", "Prossh, Skyraider of Kher",
            "Balance of Power", "Beetleback Chief", "Crimson Mage", "Cruel Edict", "Dakmor Lancer",
            "Famine", "Firewing Phoenix", "Flesh to Dust", "Flusterstorm", "Freyalise, Llanowar's Fury",
            "Gaea's Revenge", "Ice Cage", "Liliana, Heretical Healer", "Mwonvuli Beast Tracker",
            "Teferi, Temporal Archmage", "Titania, Protector of Argoth"
    }, new Region[] {
            new Region("Ice Age", "Dark Depths", inSet("ICE", "ALL", "CSP")),
            new Region("Mirage", "Teferi's Isle", inSet("MIR", "VIS", "WTH")),
            new Region("Urza's Saga", "Tolarian Academy", inSet("USG", "ULG", "UDS")),
            new Region("Invasion", "Legacy Weapon", inSet("INV", "PLS", "APC")),
            new Region("Odyssey", "Cabal Coffers", inSet("ODY", "TOR", "JUD")),
            new Region("Onslaught", "Grand Coliseum", inSet("ONS", "LGN", "SCG")),
            new Region("Time Spiral", "Vesuva", inSet("TSP", "TSB", "PLC", "FUT"))
    }),
    Innistrad("Innistrad", new String[] {
            "ISD", "DKA", "AVR"
    }, new String[] {
            "Gavony", "Kessig", "Nephalia"
    }, new String[] {
            "Profane Memento", "Strionic Resonator", "Guardian Seraph", "Seraph of the Sword",
            "Soul of Innistrad", "Ajani's Pridemate", "Jeleva, Nephalia's Scourge", "Blood Bairn",
            "Blood Host", "Call of the Full Moon", "Captivating Vampire", "Child of Night",
            "Jeleva, Nephalia's Scourge", "Vampire Nocturnus", "Crusader of Odric", "Odric, Master Tactician",
            "Curse of Chaos", "Curse of Inertia", "Curse of Predation", "Curse of Shallow Graves",
            "Curse of the Forsaken", "Deathreap Ritual", "Malicious Affliction", "Predator's Howl",
            "Geist of the Moors", "Ghoulcaller Gisa", "Hushwing Gryff", "Possessed Skaab", "Predator's Howl",
            "Sign in Blood", "Stitcher Geralf"
    }, new Region[] {
            new Region("Moorland", "Moorland Haunt", MagicColor.WHITE | MagicColor.BLUE),
            new Region("Nephalia", "Nephalia Drownyard", MagicColor.BLUE | MagicColor.BLACK),
            new Region("Stensia", "Stensia Bloodhall", MagicColor.BLACK | MagicColor.RED),
            new Region("Kessig", "Kessig Wolf Run", MagicColor.RED | MagicColor.GREEN),
            new Region("Gavony", "Gavony Township", MagicColor.GREEN | MagicColor.WHITE),
    }),
    Kamigawa("Kamigawa", new String[] {
            "CHK", "BOK", "SOK"
    }, new String[] {
            "Minamo", "Orochi Colony", "Sokenzan", "Takenuma"
    }, new String[] {
            "Champion's Helm", "Haunted Plate Mail", "Sai of the Shinobi", "Kaseto, Orochi Archmage",
            "Gahiji, Honored One", "Kurkesh, Onakke Ancient", "Sakashima's Student", "Silent-Blade Oni",
            "Vela the Night-Clad"
    }, new Region[] {
            new Region("Towabara", "Eiganjo Castle", MagicColor.WHITE),
            new Region("Minamo Academy", "Minamo, School at Water's Edge", MagicColor.BLUE),
            new Region("Takenuma", "Shizo, Death's Storehouse", MagicColor.BLACK),
            new Region("Sokenzan Mountains", "Shinka, the Bloodsoaked Keep", MagicColor.RED),
            new Region("Jukai Forest", "Okina, Temple to the Grandfathers", MagicColor.GREEN),
    }),
    LorwynShadowmoor("Lorwyn-Shadowmoor", new String[] {
            "LRW", "MOR", "SHM", "EVE"
    }, new String[] {
            "Goldmeadow", "The Great Forest", "Velis Vel", "Raven's Run", 
    }, new String[] {
            "Throwing Knife", "Awakener Druid", "Boonweaver Giant", "Cruel Sadist", "Dungrove Elder",
            "Great Oak Guardian", "Dwynen's Elite", "Dwynen, Gilt-Leaf Daen", "Eyeblight Assassin",
            "Eyeblight Massacre", "Flamekin Village", "Fleshpulper Giant", "Gilt-Leaf Winnower",
            "Gnarlroot Trapper", "Harbinger of the Tides", "Marath, Will of the Wild", "Shaman of the Pack",
            "Thornbow Archer"
    }, new Region[] {
            new Region("Ancient Amphitheater", "Ancient Amphitheater", MagicColor.RED | MagicColor.WHITE),
            new Region("Auntie's Hovel", "Auntie's Hovel", MagicColor.BLACK | MagicColor.RED),
            new Region("Gilt-Leaf Palace", "Gilt-Leaf Palace", MagicColor.BLACK | MagicColor.GREEN),
            new Region("Murmuring Bosk", "Murmuring Bosk", MagicColor.WHITE | MagicColor.BLACK | MagicColor.GREEN),
            new Region("Primal Beyond", "Primal Beyond", ColorSet.ALL_COLORS.getColor()),
            new Region("Rustic Clachan", "Rustic Clachan", MagicColor.GREEN | MagicColor.WHITE),
            new Region("Secluded Glen", "Secluded Glen", MagicColor.BLUE | MagicColor.BLACK),
            new Region("Wanderwine Hub", "Wanderwine Hub", MagicColor.WHITE | MagicColor.BLUE),
    }),
    Mercadia("Mercadia", new String[] {
            "MMQ", "NEM", "PCY"
    }, new String[] {
            "Cliffside Market"
    }, new String[] {
            
    }, new Region[] {
            new Region("Fountain of Cho", "Fountain of Cho", MagicColor.WHITE),
            new Region("Saprazzan Cove", "Saprazzan Cove", MagicColor.BLUE),
            new Region("Subterranean Hangar", "Subterranean Hangar", MagicColor.BLACK),
            new Region("Mercadian Bazaar", "Mercadian Bazaar", MagicColor.RED),
            new Region("Rushwood Grove", "Rushwood Grove", MagicColor.GREEN)
    }),
    Mirrodin("Mirrodin", new String[] {
            "MRD", "DST", "5DN", "SOM", "MBS", "NPH"
    }, new String[] {
            "Panopticon", "Quicksilver Sea", "Furnace Layer", "Norn's Dominion"
    }, new String[] {
            "Crystal Ball", "Vial of Poison", "Avarice Amulet", "Masterwork of Ingenuity", "Scytheclaw",
            "Soul of New Phyrexia", "Adaptive Automaton", "Bonded Construct", "Chief of the Foundry",
            "Guardian Automaton", "Hangarback Walker", "Scuttling Doom Engine", "Steel Overseer",
            "Chronomaton", "Ramroller", "Augury Owl", "Healer of the Pride", "Aeronaut Tinkerer",
            "Aspiring Aeronaut", "Foundry of the Consuls", "Ghirapur Gearcrafter", "Pia and Kiran Nalaar",
            "Thopter Engineer", "Thopter Spy Network", "Whirler Rogue", "Ajani, Caller of the Pride",
            "Blastfire Bolt", "Buried Ruin", "Chief Engineer", "Artificer's Hex", "Artificer's Epiphany",
            "Feldon of the Third Path", "Flamewright", "Muzzio, Visionary Architect", "Reclusive Artificer",
            "Sydri, Galvanic Genius", "Darksteel Mutation", "Ensoul Artifact", "Ezuri, Claw of Progress",
            "Ghirapur AEther Grid", "Hoarding Dragon", "Manic Vandal", "Molten Birth", "Phylactery Lich",
            "Preordain", "Scrap Mastery", "Scrapyard Mongrel", "Smelt"
    }, new Region[] {
            new Region("Panopticon", "Darksteel Citadel", MagicColor.COLORLESS),
            new Region("Taj-Nar", "Ancient Den", MagicColor.WHITE),
            new Region("Lumengrid", "Seat of the Synod", MagicColor.BLUE),
            new Region("Ish Sah", "Vault of Whispers", MagicColor.BLACK),
            new Region("Kuldotha", "Great Furnace", MagicColor.RED),
            new Region("Tel-Jilad", "Tree of Tales", MagicColor.GREEN),
            new Region("Glimmervoid", "Glimmervoid", ColorSet.ALL_COLORS.getColor())
    }),
    Rath("Rath", new String[] {
            "TMP", "STH", "EXO"
    }, new String[] {
            "Stronghold Furnace"
    }, new String[] {
            "Battle Sliver", "Belligerent Sliver", "Blur Sliver", "Bonescythe Sliver", "Constricting Sliver",
            "Diffusion Sliver", "Galerider Sliver", "Groundshaker Sliver", "Hive Stirrings", "Leeching Sliver",
            "Manaweft Sliver", "Megantic Sliver", "Predatory Sliver", "Sentinel Sliver", "Sliver Construct",
            "Sliver Hive", "Sliver Hivelord", "Steelform Sliver", "Striking Sliver", "Syphon Sliver",
            "Thorncaster Sliver", "Venom Sliver"
    }, new Region[] {
            new Region("Caldera Lake", "Caldera Lake", MagicColor.BLUE | MagicColor.RED),
            new Region("Cinder Marsh", "Cinder Marsh", MagicColor.BLACK | MagicColor.RED),
            new Region("Mogg Hollows", "Mogg Hollows", MagicColor.RED | MagicColor.GREEN),
            new Region("Pine Barrens", "Pine Barrens", MagicColor.BLACK | MagicColor.GREEN),
            new Region("Rootwater Depths", "Rootwater Depths", MagicColor.BLUE | MagicColor.BLACK),
            new Region("Salt Flats", "Salt Flats", MagicColor.WHITE | MagicColor.BLACK),
            new Region("Scabland", "Scabland", MagicColor.RED | MagicColor.WHITE),
            new Region("Skyshroud Forest", "Skyshroud Forest", MagicColor.GREEN | MagicColor.BLUE),
            new Region("Thalakos Lowlands", "Thalakos Lowlands", MagicColor.WHITE | MagicColor.BLUE),
            new Region("Vec Townships", "Vec Townships", MagicColor.GREEN | MagicColor.WHITE)
    }),
    Ravnica("Ravnica", new String[] {
            "RAV", "GPT", "DIS", "RTR", "GTC", "DGM"
    }, new String[] {
            "Agyrem", "Grand Ossuary", "Izzet Steam Maze", "Orzhova", "Prahv", "Selesnya Loft Gardens", "Undercity Reaches"
    }, new String[] {
            "Druidic Satchel", "Gem of Becoming", "Obelisk of Urd", "Will-Forged Golem", "Seraph of the Masses",
            "Avatar of Slaughter", "Basandra, Battle Seraph", "Soul of Ravnica", "Duskhunter Bat",
            "Shattergang Brothers", "Blood Ogre", "Bloodlord of Vaasgoth", "Bloodrage Vampire", "Carnage Wurm",
            "Furyborn Hellkite", "Gorehorn Minotaurs", "Lurking Crocodile", "Stormblood Berserker",
            "Vampire Outcasts", "Bounding Krasis", "Conclave Naturalists", "Covenant of Blood",
            "Crowd's Favor", "Endless Obedience", "Ephemeral Shields", "Feral Incarnation", "Living Totem",
            "Meditation Puzzle", "Return to the Ranks", "Stain the Mind", "Stoke the Flames", "Triplicate Spirits",
            "Unmake the Graves", "Deadbridge Shaman", "Extract from Darkness", "Mizzium Meddler", "Mizzix of the Izmagnus",
            "Karlov of the Ghost Council", "Mazirek, Kraul Death Priest", "Fungal Sprouting", "Ghave, Guru of Spores",
            "Jade Mage", "Sporemound", "Jace, the Living Guildpact", "Krenko's Command", "Krenko's Enforcer", "Krenko, Mob Boss",
            "Leyline of Anticipation", "Leyline of Punishment", "Leyline of Sanctity", "Leyline of Vitality", "Mantle of Webs",
            "Nightsnare", "Shattergang Brothers", "Yeva's Forcemage", "Yeva, Nature's Herald"
    }, new Region[] {
            new Region("Azorius Chancery", "Azorius Chancery", MagicColor.WHITE | MagicColor.BLUE),
            new Region("Boros Garrison", "Boros Garrison", MagicColor.RED | MagicColor.WHITE),
            new Region("Dimir Aqueduct", "Dimir Aqueduct", MagicColor.BLUE | MagicColor.BLACK),
            new Region("Golgari Rot Farm", "Golgari Rot Farm", MagicColor.BLACK | MagicColor.GREEN),
            new Region("Gruul Turf", "Gruul Turf", MagicColor.RED | MagicColor.GREEN),
            new Region("Izzet Boilerworks", "Izzet Boilerworks", MagicColor.BLUE | MagicColor.RED),
            new Region("Orzhov Basilica", "Orzhov Basilica", MagicColor.WHITE | MagicColor.BLACK),
            new Region("Rakdos Carnarium", "Rakdos Carnarium", MagicColor.BLACK | MagicColor.RED),
            new Region("Selesnya Sanctuary", "Selesnya Sanctuary", MagicColor.GREEN | MagicColor.WHITE),
            new Region("Simic Growth Chamber", "Simic Growth Chamber", MagicColor.GREEN | MagicColor.BLUE)
    }),
    Shandalar("Shandalar", new String[] {
            "2ED", "3ED", "4ED", "ARN", "ATQ", "LEG", "DRK", "FEM"
    }, new String[] {
            "Eloren Wilds", "Onakke Catacomb"
    }, new String[] {
            "Acorn Catapult", "Tyrant's Machine", "Brittle Effigy", "Kird Chieftain", "Soul of Shandalar",
            "Ring of Evos Isle", "Ring of Kalonia", "Ring of Thune", "Ring of Valkas", "Ring of Xathrid",
            "Kalonian Behemoth", "Kalonian Tusker", "Kalonian Hydra", "Kalonian Twingrove", "Roaring Primadox",
            "Thragtusk", "Warden of Evos Isle", "Initiates of the Ebon Hand", "Deathgaze Cockatrice",
            "Acolyte of Xathrid", "Xathrid Demon", "Xathrid Gorgon", "Xathrid Necromancer", "Xathrid Slyblade",
            "Downpour", "Talrand's Invocation", "Talrand, Sky Summoner", "Encrust", "Sentinel Spider",
            "Faith's Reward", "Garruk, Apex Predator", "Garruk, Primal Hunter", "Griffin Rider",
            "Hunter's Insight", "In Garruk's Wake", "Jalira, Master Polymorphist", "Kothophed, Soul Hoarder",
            "Magmatic Force", "Master of the Pearl Trident", "Polymorphist's Jest", "Scroll Thief",
            "The Chain Veil", "Yisan, the Wanderer Bard"
    }, new Region[] {
            new Region("Core", "Black Lotus", inSet("2ED", "3ED", "4ED")),
            new Region("Arabian Nights", "Library of Alexandria", inSet("ARN")),
            new Region("Antiquities", "Mishra's Workshop", inSet("ATQ")),
            new Region("Legends", "Karakas", inSet("LEG")),
            new Region("The Dark", "City of Shadows", inSet("DRK")),
            new Region("Fallen Empires", "Ruins of Trokair", inSet("FEM"))
    }),
    Tarkir("Tarkir", new String[] {
            "KTK", "FRF", "DTK"
    }, new String[] {
            "Kharasha Foothills"
    }, new String[] {
            "Ringwarden Owl", "Aven Battle Priest", "Abbot of Keral Keep", "Mage-Ring Bully"
    }, new Region[] {
            new Region("Abzan Houses", "Sandsteppe Citadel", MagicColor.WHITE | MagicColor.BLACK | MagicColor.GREEN),
            new Region("Jeskai Way", "Mystic Monastery", MagicColor.BLUE | MagicColor.RED | MagicColor.WHITE),
            new Region("Mardu Horde", "Nomad Outpost", MagicColor.RED | MagicColor.WHITE | MagicColor.BLACK),
            new Region("Sultai Brood", "Opulent Palace", MagicColor.BLACK | MagicColor.GREEN | MagicColor.BLUE),
            new Region("Temur Frontier", "Frontier Bivouac", MagicColor.GREEN | MagicColor.BLUE | MagicColor.RED)
    }),
    Theros("Theros", new String[] {
            "THS", "BNG", "JOU"
    }, new String[] {
            "Lethe Lake"
    }, new String[] {
            "Gorgon Flail", "Helm of the Gods", "Sigil of Valor", "Aegis Angel", "Soul of Theros",
            "Enlightened Ascetic", "Ajani's Chosen", "Herald of the Pantheon", "Ajani Steadfast",
            "Ajani's Sunstriker", "Akroan Jailer", "Akroan Sergeant", "Anchor to the AEther",
            "Blood-Cursed Knight", "Daxos the Returned", "Daxos's Torment", "Kalemne, Disciple of Iroas",
            "Gideon's Avenger", "Gideon's Lawkeeper", "Gideon's Phalanx", "Grasp of the Hieromancer",
            "Kytheon's Irregulars", "Kytheon's Tactics", "Kytheon, Hero of Akros", "Hixus, Prison Warden",
            "Iroas's Champion", "Kalemne's Captain", "Magmatic Insight", "Oath of the Ancient Wood",
            "Prickleboar", "Shadows of the Past", "Starfield of Nyx", "Suppression Bonds", "Valor in Akros"
    }, new Region[] {
            new Region("", "", inSet("THS", "BNG", "JOU"))
    }),
    Ulgrotha("Ulgrotha", new String[] {
            "HML"
    }, new String[] {
            "The Dark Barony"
    }, new String[] {
            "Elixir of Immortality"
    }, new Region[] {
            new Region("", "", inSet("HML"))
    }),
    Zendikar("Zendikar", new String[] {
            "ZEN", "WWK", "ROE", "BFZ"
    }, new String[] {
            "Akoum", "Hedron Fields of Agadeem", "Murasa", "Tazeem"
    }, new String[] {
            "Perilous Vault", "Archangel of Thune", "Soul of Zendikar", "Boundless Realms", "Malakir Cullblade",
            "Nissa's Expedition", "Dismiss into Dream", "Elemental Bond", "Elvish Archdruid", "Elvish Mystic",
            "Nahiri, the Lithomancer", "Felidar Umbra", "Indrik Umbra", "Into the Wilds", "Joraga Invocation",
            "Mind Control", "Nissa's Pilgrimage", "Nissa's Revelation", "Nissa, Vastwood Seer", "Nissa, Worldwaker",
            "Ob Nixilis of the Black Oath", "Ob Nixilis, Unshackled", "Sword of the Animist", "Vastwood Hydra",
            "Wild Instincts", "Woodborn Behemoth", "Zendikar Incarnate", "Zendikar's Roil"
    }, new Region[] {
            new Region("", "", inSet("ZEN", "WWK", "ROE"))
    });

    private final String name;
    private final FCollection<CardEdition> editions = new FCollection<CardEdition>();
    private final FCollection<Region> regions;
    private final FCollection<String> bannedCards = new FCollection<String>();
    private final DeckGenPool cardPool = new DeckGenPool();
    private final FCollection<PaperCard> planeCards = new FCollection<PaperCard>();
    private final FCollection<PaperCard> commanders = new FCollection<PaperCard>();
    private AwardPool awardPool;

    private ConquestPlane(String name0, String[] setCodes0, String[] planeCards0, String[] additionalCards0, Region[] regions0) {
        this(name0, setCodes0, planeCards0, additionalCards0, regions0, null);
    }
    private ConquestPlane(String name0, String[] setCodes0, String[] planeCards0, String[] additionalCards0, Region[] regions0, String[] bannedCards0) {
        name = name0;
        regions = new FCollection<Region>(regions0);
        if (bannedCards0 != null) {
            bannedCards.addAll(bannedCards0);
        }

        CardDb commonCards = FModel.getMagicDb().getCommonCards();
        for (String setCode : setCodes0) {
            CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
            if (edition != null) {
                editions.add(edition);
                for (CardInSet card : edition.getCards()) {
                    if (!bannedCards.contains(card.name)) {
                        addCard(commonCards.getCard(card.name, setCode));
                    }
                }
            }
        }

        for (String cardName : additionalCards0) {
            addCard(commonCards.getCard(cardName));
        }

        CardDb variantCards = FModel.getMagicDb().getVariantCards();
        for (String planeCard : planeCards0) {
            PaperCard pc = variantCards.getCard(planeCard);
            if (pc == null) {
                System.out.println("\"" + planeCard + "\" does not correspond to a valid Plane card");
                continue;
            }
            planeCards.add(pc);
        }
    }

    private void addCard(PaperCard pc) {
        if (pc == null) { return; }

        CardRules rules = pc.getRules();
        boolean isCommander = pc.getRules().canBeCommander();
        cardPool.add(pc);
        if (isCommander) {
            commanders.add(pc);
        }
        int count = 0;
        if (!rules.getType().isBasicLand()) { //add all basic lands to all regions below
            for (Region region : regions) {
                if (region.pred.apply(pc)) {
                    region.cardPool.add(pc);
                    if (isCommander) {
                        region.commanders.add(pc);
                    }
                    count++;
                }
            }
        }
        //if card doesn't match any region's predicate,
        //make card available to all regions
        if (count == 0) {
            for (Region region : regions) {
                region.cardPool.add(pc);
                if (isCommander) {
                    region.commanders.add(pc);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public FCollectionView<CardEdition> getEditions() {
        return editions;
    }

    public FCollectionView<String> getBannedCards() {
        return bannedCards;
    }

    public FCollectionView<Region> getRegions() {
        return regions;
    }

    public DeckGenPool getCardPool() {
        return cardPool;
    }

    public FCollectionView<PaperCard> getCommanders() {
        return commanders;
    }

    public FCollectionView<PaperCard> getPlaneCards() {
        return planeCards;
    }

    public int getEventCount() {
        return regions.size() * Region.ROWS_PER_REGION * Region.COLS_PER_REGION;
    }

    public String toString() {
        return name;
    }

    public static class Region {
        public static final int ROWS_PER_REGION = 3;
        public static final int COLS_PER_REGION = 3;
        public static final int START_COL = (COLS_PER_REGION - 1) / 2;

        private final String name, artCardName;
        private final ColorSet colorSet;
        private final Predicate<PaperCard> pred;
        private final DeckGenPool cardPool = new DeckGenPool();
        private final FCollection<PaperCard> commanders = new FCollection<PaperCard>();

        private ISkinImage art;

        private Region(String name0, String artCardName0, final int colorMask) {
            name = name0;
            artCardName = artCardName0;
            pred = new Predicate<PaperCard>() {
                @Override
                public boolean apply(PaperCard pc) {
                    return pc.getRules().getColorIdentity().hasNoColorsExcept(colorMask);
                }
            };
            colorSet = ColorSet.fromMask(colorMask);
        }
        private Region(String name0, String artCardName0, Predicate<PaperCard> pred0) {
            name = name0;
            artCardName = artCardName0;
            pred = pred0;
            colorSet = ColorSet.fromMask(ColorSet.ALL_COLORS.getColor());
        }

        public String getName() {
            return name;
        }

        public ISkinImage getArt() {
            if (art == null) {
                art = GuiBase.getInterface().getCardArt(cardPool.getCard(artCardName));
            }
            return art;
        }

        public ColorSet getColorSet() {
            return colorSet;
        }

        public DeckGenPool getCardPool() {
            return cardPool;
        }

        public FCollectionView<PaperCard> getCommanders() {
            return commanders;
        }

        public String toString() {
            return name;
        }
    }

    private static Predicate<PaperCard> inSet(final String... sets) {
        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard pc) {
                for (String set : sets) {
                    if (pc.getEdition().equals(set)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public AwardPool getAwardPool() {
        if (awardPool == null) { //delay initializing until needed
            awardPool = new AwardPool();
        }
        return awardPool;
    }

    public class AwardPool {
        private final BoosterPool commons, uncommons, rares, mythics;
        private final int commonValue, uncommonValue, rareValue, mythicValue;

        private AwardPool() {
            Iterable<PaperCard> cards = cardPool.getAllCards();

            ConquestPreferences prefs = FModel.getConquestPreferences();

            commons = new BoosterPool();
            uncommons = new BoosterPool();
            rares = new BoosterPool();
            mythics = new BoosterPool();

            for (PaperCard c : cards) {
                switch (c.getRarity()) {
                case Common:
                    commons.add(c);
                    break;
                case Uncommon:
                    uncommons.add(c);
                    break;
                case Rare:
                case Special: //lump special cards in with rares for simplicity
                    rares.add(c);
                    break;
                case MythicRare:
                    mythics.add(c);
                    break;
                default:
                    break;
                }
            }

            //calculate odds of each rarity
            float commonOdds = commons.getOdds(prefs.getPrefInt(CQPref.BOOSTER_COMMONS));
            float uncommonOdds = uncommons.getOdds(prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS));
            int raresPerBooster = prefs.getPrefInt(CQPref.BOOSTER_RARES);
            float rareOdds = rares.getOdds(raresPerBooster);
            float mythicOdds = mythics.getOdds((float)raresPerBooster / (float)prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC));

            //determine value of each rarity based on the base value of a common
            commonValue = prefs.getPrefInt(CQPref.AETHER_BASE_VALUE);
            uncommonValue = Math.round(commonValue / (uncommonOdds / commonOdds));
            rareValue = Math.round(commonValue / (rareOdds / commonOdds));
            mythicValue = mythics.isEmpty() ? 0 : Math.round(commonValue / (mythicOdds / commonOdds));
        }

        public int getShardValue(PaperCard card) {
            switch (card.getRarity()) {
            case Common:
                return commonValue;
            case Uncommon:
                return uncommonValue;
            case Rare:
            case Special:
                return rareValue;
            case MythicRare:
                return mythicValue;
            default:
                return 0;
            }
        }

        public BoosterPool getCommons() {
            return commons;
        }
        public BoosterPool getUncommons() {
            return uncommons;
        }
        public BoosterPool getRares() {
            return rares;
        }
        public BoosterPool getMythics() {
            return mythics;
        }

        public class BoosterPool {
            private final List<PaperCard> cards = new ArrayList<PaperCard>();

            private BoosterPool() {
            }

            public boolean isEmpty() {
                return cards.isEmpty();
            }

            private float getOdds(float perBoosterCount) {
                int count = cards.size();
                if (count == 0) { return 0; }
                return (float)perBoosterCount / (float)count;
            }

            private void add(PaperCard c) {
                cards.add(c);
            }

            public void rewardCard(List<PaperCard> rewards) {
                int index = Aggregates.randomInt(0, cards.size() - 1);
                PaperCard c = cards.get(index);
                cards.remove(index);
                rewards.add(c);
            }
        }
    }

    public static Set<ConquestPlane> getAllPlanesOfCard(PaperCard card) {
        EnumSet<ConquestPlane> planes = EnumSet.noneOf(ConquestPlane.class);
        for (ConquestPlane plane : values()) {
            if (plane.cardPool.contains(card)) {
                planes.add(plane);
            }
        }
        return planes;
    }
}
