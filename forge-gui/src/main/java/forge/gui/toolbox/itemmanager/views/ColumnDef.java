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
package forge.gui.toolbox.itemmanager.views;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.card.CardAiHints;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.GameFormat;
import forge.gui.CardPreferences;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.toolbox.itemmanager.views.ItemColumn.SortState;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.item.PaperCard;
import forge.limited.DraftRankCache;

public enum ColumnDef {
    STRING("", "", 0, -1, -1, SortState.ASC, new ItemCellRenderer(),
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
    NAME("Name", "Name", 180, -1, -1, SortState.ASC, new ItemCellRenderer(),
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
    TYPE("Type", "Type", 100, -1, -1, SortState.ASC, new ItemCellRenderer(),
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
    COST("Cost", "Cost", 70, -1, 140, SortState.ASC, new ManaCostRenderer(),
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
    COLOR("Color", "Color", 46, -1, 60, SortState.ASC, new ItemCellRenderer(),
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
    POWER("Power", "Power", 20, 20, 20, SortState.ASC, new IntegerRenderer(),
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
    TOUGHNESS("Toughness", "Toughness", 20, 20, 20, SortState.ASC, new IntegerRenderer(),
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
    CMC("CMC", "CMC", 20, 20, 20, SortState.ASC, new IntegerRenderer(),
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
    RARITY("Rarity", "Rarity", 20, 20, 20, SortState.ASC, new ItemCellRenderer(),
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
    SET("Set", "Set", 38, 38, 38, SortState.ASC, new SetCodeRenderer(),
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    return i instanceof InventoryItemFromSet ? Singletons.getMagicDb().getEditions()
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
    AI("AI", "AI Status", 30, 30, 30, SortState.ASC, new ItemCellRenderer(),
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
    RANKING("Ranking", "Ranking", 30, -1, -1, SortState.ASC, new ItemCellRenderer(),
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toRanking(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return String.valueOf(toRanking(from.getKey()));
                }
            }),
    QUANTITY("Qty", "Quantity", 25, 25, 25, SortState.ASC, new ItemCellRenderer(),
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
    DECK_QUANTITY("Quantity", "Quantity", 50, 50, 50, SortState.ASC, new DeckQuantityRenderer(),
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
    NEW("New", "New", 30, 30, 30, SortState.ASC, new ItemCellRenderer(),
            null, null), //functions will be set later
    PRICE("Price", "Price", 35, 35, 35, SortState.ASC, new ItemCellRenderer(),
            null, null),
    OWNED("Owned", "Owned", 20, 20, 45, SortState.ASC, new ItemCellRenderer(),
            null, null),
    DECKS("Decks", "Decks Containing Card", 20, 20, 45, SortState.ASC, new ItemCellRenderer(),
            null, null),
    FAVORITE("", "Favorite", 18, 18, 18, SortState.DESC, new StarRenderer(),
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    IPaperCard card = toCard(from.getKey());
                    if (card == null) {
                        return -1;
                    }
                    return CardPreferences.getPrefs(card.getName()).getStarCount();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return toCard(from.getKey());
                }
            }),
    DECK_ACTIONS("", "", 40, 40, 40, SortState.DESC, new ItemCellRenderer(),
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
    DECK_FOLDER("Folder", "Folder", 80, -1, -1, SortState.ASC, new ItemCellRenderer(),
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
    DECK_COLOR("Color", "Color", 70, 70, 70, SortState.ASC, new ColorSetRenderer(),
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
    DECK_FORMAT("Format", "Format", 60, -1, -1, SortState.DESC, new ItemCellRenderer(),
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
                    return StringUtils.join(Iterables.transform(deck.getFormats(), GameFormat.FN_GET_NAME) , ", ");
                }
            }),
    DECK_EDITION("Min.Set", "Min.Set", 30, 30, 30, SortState.ASC, new ItemCellRenderer(),
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
    DECK_MAIN("Main", "Main Deck", 30, 30, 30, SortState.ASC, new IntegerRenderer(),
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
    DECK_SIDE("Side", "Sideboard", 30, 30, 30, SortState.ASC, new IntegerRenderer(),
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

    ColumnDef(String shortName0, String longName0, int preferredWidth0, int minWidth0, int maxWidth0, SortState sortState0, ItemCellRenderer cellRenderer0,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        this.shortName = shortName0;
        this.longName = longName0;
        this.preferredWidth = preferredWidth0;
        this.minWidth = minWidth0;
        this.maxWidth = maxWidth0;
        this.sortState = sortState0;
        this.fnSort = fnSort0;
        this.fnDisplay = fnDisplay0;
        this.cellRenderer = cellRenderer0;
    }

    final String shortName, longName;
    final int preferredWidth, minWidth, maxWidth;
    final SortState sortState;
    final ItemCellRenderer cellRenderer;
    final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
    final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

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

    private static Double toRanking(final InventoryItem i) {
        Double ranking = 500D;
        if (i != null && i instanceof PaperCard){
            PaperCard cp = (PaperCard) i;
            ranking = DraftRankCache.getRanking(cp.getName(), cp.getEdition());
            if (ranking == null) {
                ranking = 500D;
            }
        }
        return ranking;
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

