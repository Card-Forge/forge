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

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardEdition.CardInSet;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.FCollection;
import forge.util.FCollectionView;


public enum ConquestPlane {
    Alara("Alara", new String[] {
            "ALA", "CON", "ARB"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Azoria("Azoria", new String[] {
            
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    BolasMeditationRealm("Bolas's Meditation Realm", new String[] {
            
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Dominaria("Dominaria", new String[] {
            
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Equilor("Equilor", new String[] {
            
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Gastal("Gastal", new String[] {
            
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Innistrad("Innistrad", new String[] {
            "ISD", "DKA", "AVR"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Kamigawa("Kamigawa", new String[] {
            "CHK", "BOK", "SOK"
    }, new Region[] {
            new Region("Towabara", "", CardRulesPredicates.hasColorIdentity(MagicColor.WHITE),
                    new String[] { "Towabara", "Plains" }),
            new Region("Minamo Academy", "", CardRulesPredicates.hasColorIdentity(MagicColor.BLUE),
                    new String[] { "Minamo", "Academy", "Island" }),
            new Region("Takenuma", "", CardRulesPredicates.hasColorIdentity(MagicColor.BLACK),
                    new String[] { "Takenuma", "Swamp" }),
            new Region("Sokenzan Mountains", "", CardRulesPredicates.hasColorIdentity(MagicColor.RED),
                    new String[] { "Sokenzan", "Mountain" }),
            new Region("Jukai Forest", "", CardRulesPredicates.hasColorIdentity(MagicColor.GREEN),
                    new String[] { "Jukai", "Forest" })
    }, new String[] {
            
    }),
    LorwynShadowmoor("Lorwyn-Shadowmoor", new String[] {
            "LRW", "MOR", "SHM", "EVE"
    }, new Region[] {
            new Region("Gilt Leaf Wood {B}{G}", "", CardRulesPredicates.hasCreatureType("Elf"),
                    new String[] { "Gilt Leaf", "Wood", "Elf", "Elves" }),
            new Region("Glen Elendra {U}{B}", "", CardRulesPredicates.hasCreatureType("Faerie"),
                    new String[] { "Glen", "Elendra", "Oona", "Fae" }),
            new Region("Mount Tanufel {W}{U}{B}{R}{G}", "", CardRulesPredicates.hasCreatureType("Elemental"),
                    new String[] { "Elemental", "Flamekin" }),
            new Region("Murmuring Bosk {W}{B}{G}", "", CardRulesPredicates.hasCreatureType("Treefolk"),
                    new String[] { "Treefolk" }),
            new Region("Gilt Leaf Wood", "", null,
                    new String[] { }),
            new Region("Gilt Leaf Wood", "", null,
                    new String[] { }),
    }, new String[] {
            
    }),
    Mercadia("Mercadia", new String[] {
            "MMQ", "NEM", "PCY"
    }, new Region[] {
            new Region("Deepwood", "", CardRulesPredicates.hasCreatureType("Zombie", "Ghoul", "Dryad"),
                    new String[] { }),
            new Region("Mercadia City", "", CardRulesPredicates.hasCreatureType("Goblin"),
                    new String[] { }),
            new Region("Rishada", "", CardRulesPredicates.hasCreatureType("Pirate", "Rebel", "Mercenary"),
                    new String[] { }),
            new Region("Rushwood", "", CardRulesPredicates.hasCreatureType("Beast", "Troll"),
                    new String[] { }),
            new Region("Saprazzo", "", CardRulesPredicates.hasCreatureType("Merfolk", "Human"),
                    new String[] { })
    }, new String[] {
            
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
    }, new String[] {
            
    }),
    Rabiah("Rabiah", new String[] {
            "ARN"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Rath("Rath", new String[] {
            "TMP", "STH", "EXO"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Ravnica("Ravnica", new String[] {
            "RAV", "GPT", "DIS", "RTR", "GTC", "DGM"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
    }),
    Shandalar("Shandalar", new String[] {
            "2ED", "ARN", "ATQ", "3ED", "LEG", "DRK", "4ED"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { })
    }, new String[] {
            
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
    }, new String[] {
            
    }),
    Theros("Theros", new String[] {
            "THS", "BNG", "JOU"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { }),
    }, new String[] {
            
    }),
    Ulgrotha("Ulgrotha", new String[] {
            "HML"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { }),
    }, new String[] {
            
    }),
    Zendikar("Zendikar", new String[] {
            "ZEN", "WWK", "ROE"
    }, new Region[] {
            new Region("", "", null,
                    new String[] { }),
    }, new String[] {
            
    });

    private final String name;
    private final FCollection<CardEdition> editions = new FCollection<CardEdition>();
    private final FCollection<Region> regions;
    private final FCollection<String> bannedCards;
    private final CardPool cardPool = new CardPool();
    private final FCollection<PaperCard> commanders = new FCollection<PaperCard>();

    private ConquestPlane(String name0, String[] setCodes0, Region[] regions0, String[] bannedCards0) {
        name = name0;
        regions = new FCollection<Region>(regions0);
        bannedCards = new FCollection<String>(bannedCards0);
        for (String setCode : setCodes0) {
            CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
            if (edition != null) {
                editions.add(edition);
                for (CardInSet card : edition.getCards()) {
                    if (!bannedCards.contains(card.name)) {
                        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(card.name, setCode);
                        if (pc != null) {
                            CardType type = pc.getRules().getType();
                            if (!type.isBasicLand()) { //don't include basic lands
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

    public CardPool getCardPool() {
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
        private final CardPool cardPool = new CardPool();
        private final FCollection<PaperCard> commanders = new FCollection<PaperCard>();

        private Region(String name0, String artCardName0, final Predicate<CardRules> rulesPred, final String[] keywords) {
            name = name0;
            artCardName = artCardName0;

            pred = new Predicate<PaperCard>() {
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
            };
        }

        public String getName() {
            return name;
        }

        public CardPool getCardPool() {
            return cardPool;
        }

        public FCollectionView<PaperCard> getCommanders() {
            return commanders;
        }

        public String toString() {
            return name;
        }
    }
}
