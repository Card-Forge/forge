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

import com.google.common.base.Predicate;

import forge.GuiBase;
import forge.assets.ISkinImage;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardEdition.CardInSet;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.FCollection;
import forge.util.FCollectionView;


public enum ConquestPlane {
    Alara("Alara", new String[] {
            "ALA", "CON", "ARB"
    }, new Region[] {
            new Region("Bant {G}{W}{U}", "Seaside Citadel", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.WHITE | MagicColor.BLUE),
                    new String[] { "Bant" }),
            new Region("Esper {W}{U}{B}", "Arcane Sanctum", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLUE | MagicColor.BLACK),
                    new String[] { "Esper" }),
            new Region("Grixis {U}{B}{R}", "Crumbling Necropolis", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.BLACK | MagicColor.RED),
                    new String[] { "Grixis" }),
            new Region("Jund {B}{R}{G}", "Savage Lands", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.RED | MagicColor.GREEN),
                    new String[] { "Jund" }),
            new Region("Naya {R}{G}{W}", "Jungle Shrine", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.GREEN | MagicColor.WHITE),
                    new String[] { "Naya" })
    }),
    Dominaria("Dominaria", new String[] {
            "ICE", "ALL", "CSP",
            "USG", "ULG", "UDS",
            "INV", "PLS", "APC",
            "ODY", "TOR", "JUD",
            "ONS", "LGN", "SCG",
            "TSP", "PLC", "FUT"
    }, new Region[] {
            new Region("Ice Age", "Dark Depths", inSet("ICE", "ALL", "CSP")),
            new Region("Urza's Saga", "Tolarian Academy", inSet("USG", "ULG", "UDS")),
            new Region("Invasion", "Legacy Weapon", inSet("INV", "PLS", "APC")),
            new Region("Odyssey", "Cabal Coffers", inSet("ODY", "TOR", "JUD")),
            new Region("Onslaught", "Grand Coliseum", inSet("ONS", "LGN", "SCG")),
            new Region("Time Spiral", "Vesuva", inSet("TSP", "TSB", "PLC", "FUT"))
    }),
    Innistrad("Innistrad", new String[] {
            "ISD", "DKA", "AVR"
    }, new Region[] {
            new Region("Moorland {W}{U}", "Moorland Haunt", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLUE),
                    new String[] { "Moorland" }),
            new Region("Nephalia {U}{B}", "Nephalia Drownyard", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.BLACK),
                    new String[] { "Nephalia" }),
            new Region("Stensia {B}{R}", "Stensia Bloodhall", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.RED),
                    new String[] { "Stensia" }),
            new Region("Kessig {R}{G}", "Kessig Wolf Run", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.GREEN),
                    new String[] { "Kessig" }),
            new Region("Gavony {G}{W}", "Gavony Township", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.WHITE),
                    new String[] { "Gavony" })
    }),
    Jamuraa("Jamuraa", new String[] {
            "MIR", "VIS", "WTH"
    }, new Region[] {
            new Region("Karoo {W}", "Karoo", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE),
                    new String[] { "Karoo" }),
            new Region("Coral Atoll {U}", "Coral Atoll", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE),
                    new String[] { "Coral" }),
            new Region("Everglades {B}", "Everglades", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK),
                    new String[] { "Everglades" }),
            new Region("Dormant Volcano {R}", "Dormant Volcano", CardRulesPredicates.hasColorIdentity(MagicColor.RED),
                    new String[] { "Volcano" }),
            new Region("Jungle Basin {G}", "Jungle Basin", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN),
                    new String[] { "Jungle" })
    }),
    Kamigawa("Kamigawa", new String[] {
            "CHK", "BOK", "SOK"
    }, new Region[] {
            new Region("Towabara {W}", "Eiganjo Castle", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE),
                    new String[] { "Towabara", "Eiganjo", "Plains" }),
            new Region("Minamo Academy {U}", "Minamo, School at Water's Edge", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE),
                    new String[] { "Minamo", "Water", "Island" }),
            new Region("Takenuma {B}", "Shizo, Death's Storehouse", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK),
                    new String[] { "Takenuma", "Shizo", "Swamp" }),
            new Region("Sokenzan Mountains {R}", "Shinka, the Bloodsoaked Keep", CardRulesPredicates.hasColorIdentity(MagicColor.RED),
                    new String[] { "Sokenzan", "Shinka", "Mountain" }),
            new Region("Jukai Forest {G}", "Okina, Temple to the Grandfathers", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN),
                    new String[] { "Jukai", "Okina", "Forest" })
    }),
    LorwynShadowmoor("Lorwyn-Shadowmoor", new String[] {
            "LRW", "MOR", "SHM", "EVE"
    }, new Region[] {
            new Region("Ancient Amphitheater {R}{W}", "Ancient Amphitheater", CardRulesPredicates.hasCreatureType("Giant"),
                    new String[] { }),
            new Region("Auntie's Hovel {B}{R}", "Auntie's Hovel", CardRulesPredicates.hasCreatureType("Goblin"),
                    new String[] { "Auntie", "Boggart", "Hobgoblin", "Redcap", "Spriggan" }),
            new Region("Gilt-Leaf Palace {B}{G}", "", CardRulesPredicates.hasCreatureType("Elf"),
                    new String[] { "Gilt-Leaf", "Elves", "Wilt-Leaf" }),
            new Region("Murmuring Bosk {W}{B}{G}", "Murmuring Bosk", CardRulesPredicates.hasCreatureType("Treefolk"),
                    new String[] { "Bosk", "Treefolk" }),
            new Region("Primal Beyond {W}{U}{B}{R}{G}", "Primal Beyond", CardRulesPredicates.hasCreatureType("Elemental"),
                    new String[] { "Elemental", "Flamekin", "Tanufel", "Cinder", "Kulrath" }),
            new Region("Rustic Clachan {W}", "Rustic Clachan", CardRulesPredicates.hasCreatureType("Kithkin"),
                    new String[] { "Ballynock", "Ballyrush", "Barrenton", "Burrenton", "Cloverdell", "Goldmeadow", "Kinsbaile", "Kinscaer", "Mistmeadow" }),
            new Region("Secluded Glen {U}{B}", "Secluded Glen", CardRulesPredicates.hasCreatureType("Faerie"),
                    new String[] { "Glen", "Oona", "Fae" }),
            new Region("Wanderwine Hub {W}{U}", "Wanderwine Hub", CardRulesPredicates.hasCreatureType("Merfolk"),
                    new String[] { "Merrow", "Selkie", "Pirate" }),
    }),
    Mercadia("Mercadia", new String[] {
            "MMQ", "NEM", "PCY"
    }, new Region[] {
            new Region("Fountain of Cho {W}", "Fountain of Cho", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE),
                    new String[] { "Cho" }),
            new Region("Saprazzan Cove {U}", "Saprazzan Cove", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE),
                    new String[] { "Saprazzan", "Saprazzo" }),
            new Region("Subterranean Hangar {B}", "Subterranean Hangar", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK),
                    new String[] { "Subterranean" }),
            new Region("Mercadian Bazaar {R}", "Mercadian Bazaar", CardRulesPredicates.hasColorIdentity(MagicColor.RED),
                    new String[] { "Mercadian" }),
            new Region("Rushwood Grove {G}", "Rushwood Grove", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN),
                    new String[] { "Rushwood" })
    }),
    Mirrodin("Mirrodin", new String[] {
            "MRD", "DST", "5DN", "SOM", "MBS", "NPH"
    }, new Region[] {
            new Region("Mirrodin's Core", "Mirrodin's Core", null,
                    new String[] { "Core", "Mycosynth", "Memnarch" }),
            new Region("The Glimmervoid", "Glimmervoid", CardRulesPredicates.hasKeyword("Sunburst"),
                    new String[] { "Glimmervoid" }),
            new Region("Mephidross", "", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK),
                    new String[] { "Dross", "Mephidross" }),
            new Region("The Oxidda Chain", "", CardRulesPredicates.hasColorIdentity(MagicColor.RED),
                    new String[] { "Oxidda", "Chain", "Mountain" }),
            new Region("The Quicksilver Sea", "", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE),
                    new String[] { "Quicksilver", "Sea", "Island" }),
            new Region("The Razor Fields", "", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE),
                    new String[] { "Razor", "Fields", "Plains" }),
            new Region("The Tangle", "", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN),
                    new String[] { "Tangle", "Forest" })
    }),
    Rath("Rath", new String[] {
            "TMP", "STH", "EXO"
    }, new Region[] {
            new Region("Caldera Lake {U}{R}", "Caldera Lake", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.RED),
                    new String[] { "Caldera" }),
            new Region("Cinder Marsh {B}{R}", "Cinder Marsh", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.RED),
                    new String[] { "Cinder", "Marsh" }),
            new Region("Mogg Hollows {R}{G}", "Mogg Hollows", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.GREEN),
                    new String[] { "Mogg", "Hollow" }),
            new Region("Pine Barrens {B}{G}", "Pine Barrens", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.GREEN),
                    new String[] { "Barrens" }),
            new Region("Rootwater Depths {U}{B}", "Rootwater Depths", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.BLACK),
                    new String[] { "Rootwater" }),
            new Region("Salt Flats {W}{B}", "Salt Flats", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLACK),
                    new String[] { "Salt Flat" }),
            new Region("Scabland {R}{W}", "Scabland", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.WHITE),
                    new String[] { "Scabland" }),
            new Region("Skyshroud Forest {G}{U}", "Skyshroud Forest", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.BLUE),
                    new String[] { "Skyshroud" }),
            new Region("Thalakos Lowlands {W}{U}", "Thalakos Lowlands", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLUE),
                    new String[] { "Thalakos" }),
            new Region("Vec Townships {G}{W}", "Vec Townships", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.WHITE),
                    new String[] { "Vec" })
    }),
    Ravnica("Ravnica", new String[] {
            "RAV", "GPT", "DIS", "RTR", "GTC", "DGM"
    }, new Region[] {
            new Region("Azorius Chancery {W}{U}", "Azorius Chancery", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLUE),
                    new String[] { "Azorius" }),
            new Region("Boros Garrison {R}{W}", "Boros Garrison", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.WHITE),
                    new String[] { "Boros" }),
            new Region("Dimir Aqueduct {U}{B}", "Dimir Aqueduct", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.BLACK),
                    new String[] { "Dimir" }),
            new Region("Golgari Rot Farm {B}{G}", "Golgari Rot Farm", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.GREEN),
                    new String[] { "Golgari" }),
            new Region("Gruul Turf {R}{G}", "Gruul Turf", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.GREEN),
                    new String[] { "Gruul" }),
            new Region("Izzet Boilerworks {U}{R}", "Izzet Boilerworks", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.RED),
                    new String[] { "Izzet" }),
            new Region("Orzhov Basilica {W}{B}", "Orzhov Basilica", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLACK),
                    new String[] { "Orzhov" }),
            new Region("Rakdos Carnarium {R}{B}", "Rakdos Carnarium", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.RED),
                    new String[] { "Rakdos" }),
            new Region("Selesnya Sanctuary {G}{W}", "Selesnya Sanctuary", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.WHITE),
                    new String[] { "Selesnya" }),
            new Region("Simic Growth Chamber {G}{U}", "Simic Growth Chamber", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.BLUE),
                    new String[] { "Simic" })
    }),
    Shandalar("Shandalar", new String[] {
            "2ED", "3ED", "4ED", "ARN", "ATQ", "LEG", "DRK"
    }, new Region[] {
            new Region("Core", "Black Lotus", inSet("2ED", "3ED", "4ED")),
            new Region("Arabian Nights", "Library of Alexandria", inSet("ARN")),
            new Region("Antiquities", "Mishra's Workshop", inSet("ATQ")),
            new Region("Legends", "Karakas", inSet("LEG")),
            new Region("The Dark", "City of Shadows", inSet("DRK"))
    }),
    Tarkir("Tarkir", new String[] {
            "KTK", "FRF", "DTK"
    }, new Region[] {
            new Region("Abzan Houses {W}{B}{G}", "Sandsteppe Citadel", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE | MagicColor.BLACK | MagicColor.GREEN),
                    new String[] { "Abzan", "House", "Citadel", "Arashin", "Wastes", "Mer-Ek" }),
            new Region("Jeskai Way {U}{R}{W}", "Mystic Monastery", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE | MagicColor.RED | MagicColor.WHITE),
                    new String[] { "Jeskai", "Way", "Mystic", "Monastery", "Stronghold", "Purugir" }),
            new Region("Mardu Horde {R}{W}{B}", "Nomad Outpost", CardRulesPredicates.hasColorIdentity(MagicColor.RED | MagicColor.WHITE | MagicColor.BLACK),
                    new String[] { "Mardu", "Horde", "Nomad", "Outpost", "Wingthrone", "Goldengrave", "Scour", "Screamreach" }),
            new Region("Sultai Brood {B}{G}{U}", "Opulent Palace", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK | MagicColor.GREEN | MagicColor.BLUE),
                    new String[] { "Sultai", "Brood", "Opulent", "Palace", "Sagu", "Jungle", "Kheru", "Gudul", "Gurmag", "Marang" }),
            new Region("Temur Frontier {G}{U}{R}", "Frontier Bivouac", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN | MagicColor.BLUE | MagicColor.RED),
                    new String[] { "Temur", "Frontier", "Bivouac", "Qal Sisma", "Dragon's Throat", "Karakyk Valley", "Staircase of Bones" })
    }),
    Theros("Theros", new String[] {
            "THS", "BNG", "JOU"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { }),
    }),
    Ulgrotha("Ulgrotha", new String[] {
            "HML"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { }),
    }),
    Zendikar("Zendikar", new String[] {
            "ZEN", "WWK", "ROE"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { }),
    });

    private final String name;
    private final FCollection<CardEdition> editions = new FCollection<CardEdition>();
    private final FCollection<Region> regions;
    private final FCollection<String> bannedCards = new FCollection<String>();
    private final DeckGenPool cardPool = new DeckGenPool();
    private final FCollection<PaperCard> commanders = new FCollection<PaperCard>();

    private ConquestPlane(String name0, String[] setCodes0, Region[] regions0) {
        this(name0, setCodes0, regions0, null);
    }
    private ConquestPlane(String name0, String[] setCodes0, Region[] regions0, String[] bannedCards0) {
        name = name0;
        regions = new FCollection<Region>(regions0);
        if (bannedCards0 != null) {
            bannedCards.addAll(bannedCards0);
        }
        for (String setCode : setCodes0) {
            CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
            if (edition != null) {
                editions.add(edition);
                for (CardInSet card : edition.getCards()) {
                    if (!bannedCards.contains(card.name)) {
                        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(card.name, setCode);
                        if (pc != null) {
                            CardType type = pc.getRules().getType();
                            boolean isCommander = type.isLegendary() && type.isCreature();
                            cardPool.add(pc);
                            if (isCommander) {
                                commanders.add(pc);
                            }
                            int count = 0;
                            for (Region region : regions) {
                                if (region.pred.apply(pc)) {
                                    region.cardPool.add(pc);
                                    if (isCommander) {
                                        region.commanders.add(pc);
                                    }
                                    count++;
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
                    }
                }
            }
        }
        commanders.sort(); //sort main commanders list for the sake of UI
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

    public String toString() {
        return name;
    }

    public static class Region {
        private final String name;
        private final String artCardName;
        private final Predicate<PaperCard> pred;
        private final DeckGenPool cardPool = new DeckGenPool();
        private final FCollection<PaperCard> commanders = new FCollection<PaperCard>();

        private ISkinImage art;

        private Region(String name0, String artCardName0, Predicate<PaperCard> pred0) {
            name = name0;
            artCardName = artCardName0;
            pred = pred0;
        }
        private Region(String name0, String artCardName0, final Predicate<CardRules> rulesPred, final String[] keywords) {
            this(name0, artCardName0, new Predicate<PaperCard>() {
                @Override
                public boolean apply(PaperCard pc) {
                    if (rulesPred != null && rulesPred.apply(pc.getRules())) {
                        return true;
                    }
                    for (String s : keywords) {
                        if (pc.getName().contains(s)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
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
}
