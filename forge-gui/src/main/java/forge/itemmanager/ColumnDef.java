/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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
package forge.itemmanager;

import com.google.common.base.Function;
import forge.card.*;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.card.mana.ManaCost;
import forge.game.GameFormat;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.item.PaperCard;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.limited.DraftRankCache;
import forge.model.FModel;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public enum ColumnDef {
    STRING("", "", 0, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return from.getKey() instanceof Comparable<?> ? (Comparable<?>)from.getKey() : from.getKey().getName();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return from.getKey().toString();
                }
            }),
    NAME("Name", "Name", 180, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return from.getKey().getName();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    final String name = from.getKey().getName();
                    return name.contains("AE") ? AE_FINDER.matcher(name).replaceAll("\u00C6") : name;
                }
            }),
    TYPE("Type", "Type", 100, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toType(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toType(from.getKey());
                }
            }),
    COST("Cost", "Cost", 70, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toManaCost(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toCardRules(from.getKey());
                }
            }),
    COLOR("Color", "Color", 46, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toColor(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toColor(from.getKey());
                }
            }),
    POWER("Power", "Power", 20, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toPower(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toPower(from.getKey());
                }
            }),
    TOUGHNESS("Toughness", "Toughness", 20, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toToughness(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toToughness(from.getKey());
                }
            }),
    CMC("CMC", "CMC", 20, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toCMC(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toCMC(from.getKey());
                }
            }),
    RARITY("Rarity", "Rarity", 20, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toRarity(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toRarity(from.getKey());
                }
            }),
    SET("Set", "Set", 38, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    return i instanceof InventoryItemFromSet ? FModel.getMagicDb().getEditions()
                            .get(((InventoryItemFromSet) i).getEdition()) : CardEdition.UNKNOWN;
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    return i instanceof InventoryItemFromSet ? ((InventoryItemFromSet) i).getEdition() : "n/a";
                }
            }),
    AI("AI", "AI Status", 30, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    return i instanceof PaperCard ? ((IPaperCard) i).getRules().getAiHints().getAiStatusComparable() : Integer.valueOf(-1);
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    if (!(i instanceof PaperCard)) {
                        return "n/a";
                    }
                    IPaperCard cp = (IPaperCard) i;
                    CardAiHints ai = cp.getRules().getAiHints();

                    return ai.getRemAIDecks() ? (ai.getRemRandomDecks() ? "AI ?" : "AI")
                            : (ai.getRemRandomDecks() ? "?" : "");
                }
            }),
    RANKING("Ranking", "Draft Ranking", 50, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toRanking(from.getKey(), false);
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toRanking(from.getKey(), true);
                }
            }),
    QUANTITY("Qty", "Quantity", 25, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return from.getValue();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return from.getValue();
                }
            }),
    DECK_QUANTITY("Quantity", "Quantity", 50, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return from.getValue();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return from.getValue();
                }
            }),
    NEW("New", "New", 30, true, SortState.DESC,
            null, null), //functions will be set later
    PRICE("Price", "Price", 35, true, SortState.DESC,
            null, null),
    OWNED("Owned", "Owned", 20, true, SortState.ASC,
            null, null),
    DECKS("Decks", "Decks", 20, true, SortState.ASC,
            null, null),
    FAVORITE("", "Favorite", 18, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    IPaperCard card = toCard(from.getKey());
                    if (card == null) {
                        return -1;
                    }
                    return CardPreferences.getPrefs(card).getStarCount();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toCard(from.getKey());
                }
            }), 
    DECK_FAVORITE("", "Favorite", 18, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    DeckProxy deck = toDeck(from.getKey());
                    if (deck == null) {
                        return -1;
                    }
                    return DeckPreferences.getPrefs(deck).getStarCount();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeck(from.getKey());
                }
            }),
    DECK_ACTIONS("", "Delete/Edit", 40, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return 0;
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeck(from.getKey());
                }
            }),
    DECK_FOLDER("Folder", "Folder", 80, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toDeckFolder(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeckFolder(from.getKey());
                }
            }),
    DECK_COLOR("Color", "Color", 70, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toDeckColor(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeckColor(from.getKey());
                }
            }),
    DECK_FORMAT("Format", "Formats deck is legal in", 60, false, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    DeckProxy deck = toDeck(from.getKey());
                    if (deck == null) {
                        return -1;
                    }
                    Iterable<GameFormat> all = deck.getFormats();
                    int acc = 0;
                    for(GameFormat gf : all) {
                        int ix = gf.getIndex();
                        if( ix < 30 && ix > 0)
                            acc |= 0x40000000 >> (ix - 1);
                    }
                    return acc;
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    DeckProxy deck = toDeck(from.getKey());
                    if (deck == null) {
                        return null;
                    }
                    return deck.getFormatsString();
                }
            }),
    DECK_EDITION("Set", "Set of oldest card in deck", 38, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getEdition();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getEdition().getCode();
                }
            }),
    DECK_MAIN("Main", "Main Deck", 30, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getMainSize();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getMainSize();
                }
            }),
    DECK_SIDE("Side", "Sideboard", 30, true, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getSideSize();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getSideSize();
                }
            });

    ColumnDef(String shortName0, String longName0, int preferredWidth0, boolean isWidthFixed0, SortState sortState0,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        this.shortName = shortName0;
        this.longName = longName0;
        this.preferredWidth = preferredWidth0;
        this.isWidthFixed = isWidthFixed0;
        this.sortState = sortState0;
        this.fnSort = fnSort0;
        this.fnDisplay = fnDisplay0;
    }

    public final String shortName, longName;
    public final int preferredWidth;
    public final boolean isWidthFixed;
    public final SortState sortState;
    public final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
    public final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

    @Override
    public String toString() {
        return this.longName;
    }

    private static final Pattern AE_FINDER = Pattern.compile("AE", Pattern.LITERAL);

    private static String toType(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard)i).getRules().getType().toString() : i.getItemType();
    }

    private static IPaperCard toCard(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i) : null;
    }
    private static ManaCost toManaCost(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules().getManaCost() : ManaCost.NO_COST;
    }
    private static CardRules toCardRules(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules() : null;
    }

    private static ColorSet toColor(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules().getColor() : ColorSet.getNullColor();
    }

    private static Integer toPower(final InventoryItem i) {
        int result = -1;
        if (i instanceof PaperCard) {
            result = ((IPaperCard) i).getRules().getIntPower();
            if (result == -1) {
                result = ((IPaperCard) i).getRules().getInitialLoyalty();
            }
        }
        return result;
    }

    private static Integer toToughness(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getRules().getIntToughness() : -1;
    }

    private static Integer toCMC(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getRules().getManaCost().getCMC() : -1;
    }

    private static CardRarity toRarity(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getRarity() : CardRarity.Unknown;
    }

    private static Double toRanking(final InventoryItem i, boolean truncate) {
        if (i instanceof IPaperCard){
            IPaperCard cp = (IPaperCard) i;
            Double ranking = DraftRankCache.getRanking(cp.getName(), cp.getEdition());
            if (ranking != null) {
                if (truncate) {
                    return new BigDecimal(ranking).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
                return ranking;
            }
        }
        return 500D;
    }

    private static DeckProxy toDeck(final InventoryItem i) {
        return i instanceof DeckProxy ? ((DeckProxy) i) : null;
    }
    private static ColorSet toDeckColor(final InventoryItem i) {
        return i instanceof DeckProxy ? ((DeckProxy) i).getColor() : null;
    }
    private static String toDeckFolder(final InventoryItem i) {
        return i instanceof DeckProxy ? ((DeckProxy) i).getPath() + "/" : null;
    }
}

