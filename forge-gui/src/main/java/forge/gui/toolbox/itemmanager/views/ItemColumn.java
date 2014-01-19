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

import javax.swing.table.TableColumn;

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
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.gui.CardPreferences;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.item.PaperCard;
import forge.limited.DraftRankCache;

/**
 * A column object in a EditorTableModel in the card editor.
 * Requires a sorting function and a display function
 * (to extract information as appropriate for table row data).
 * 
 * @param <T> a generic type
 */
public class ItemColumn extends TableColumn {
    private static final long serialVersionUID = 3749431834643427572L;

    public enum SortState {
        NONE,
        ASC,
        DESC
    }

    private final ColumnDef def;
    private SortState sortState = SortState.NONE;
    private int sortPriority = 0;
    private boolean visible = true;
    private int index = 0;
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

    public ItemColumn(ColumnDef def0) {
        this(def0, def0.fnSort, def0.fnDisplay);
    }
    public ItemColumn(ColumnDef def0,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        super();

        if (fnSort0 == null) {
            throw new NullPointerException("A sort function hasn't been set for Column " + this);
        }
        if (fnDisplay0 == null) {
            throw new NullPointerException("A display function hasn't been set for Column " + this);
        }

        this.def = def0;
        this.setIdentifier(def0);
        this.setHeaderValue(def.shortName);

        this.setPreferredWidth(def.preferredWidth);
        if (def.minWidth > 0) {
            this.setMinWidth(def.minWidth);
        }
        if (def.maxWidth > 0) {
            this.setMaxWidth(def.maxWidth);
        }
        this.fnSort = fnSort0;
        this.fnDisplay = fnDisplay0;
        this.setCellRenderer(def.cellRenderer);
    }

    public String getShortName() {
        return this.def.shortName;
    }

    public String getLongName() {
        return this.def.longName;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(final int index0) {
        this.index = index0;
    }

    public int getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(final int sortPriority0) {
        int oldSortPriority = this.sortPriority;
        this.sortPriority = sortPriority0;
        if (sortPriority0 == 0) {
            this.sortState = SortState.NONE;
        }
        else if (oldSortPriority == 0) {
            this.sortState = def.sortState;
        }
    }

    public SortState getSortState() {
        return this.sortState;
    }

    public void setSortState(final SortState state0) {
        this.sortState = state0;
    }

    public SortState getDefaultSortState() {
        return this.def.sortState;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible0) {
        this.visible = visible0;
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return this.fnSort;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return this.fnDisplay;
    }

    @Override
    public String toString() {
        return this.getLongName();
    }

    public enum ColumnDef {
        STRING("", "", 0, -1, -1, SortState.ASC, new ItemCellRenderer(),
                new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                    @Override
                    public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                        return from.getKey().getCompareValue();
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
                        Deck deck = toDeck(from.getKey());
                        if (deck == null) {
                            return -1;
                        }
                        Iterable<GameFormat> all = Singletons.getModel().getFormats().getAllFormatsOfDeck(deck);
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
                        Deck deck = toDeck(from.getKey());
                        if (deck == null) {
                            return null;
                        }
                        Iterable<GameFormat> all = Singletons.getModel().getFormats().getAllFormatsOfDeck(deck);
                        return StringUtils.join(Iterables.transform(all, GameFormat.FN_GET_NAME) , ", ");
                    }
                }),
        DECK_MAIN("Main", "Main Deck", 30, 30, 30, SortState.ASC, new IntegerRenderer(),
                new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                    @Override
                    public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                        return toDeckCount(from.getKey(), DeckSection.Main);
                    }
                },
                new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                    @Override
                    public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                        return toDeckCount(from.getKey(), DeckSection.Main);
                    }
                }),
        DECK_SIDE("Side", "Sideboard", 30, 30, 30, SortState.ASC, new IntegerRenderer(),
                new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                    @Override
                    public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                        return toDeckCount(from.getKey(), DeckSection.Sideboard);
                    }
                },
                new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                    @Override
                    public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                        return toDeckCount(from.getKey(), DeckSection.Sideboard);
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

        private final String shortName, longName;
        private final int preferredWidth, minWidth, maxWidth;
        private final SortState sortState;
        private final ItemCellRenderer cellRenderer;
        private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
        private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

        private static final Pattern AE_FINDER = Pattern.compile("AE", Pattern.LITERAL);

        private static String toType(final InventoryItem i) {
            return i instanceof PaperCard ? ((IPaperCard)i).getRules().getType().toString() : i.getItemType();
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

        private static Deck toDeck(final InventoryItem i) {
            return i instanceof Deck ? ((Deck) i) : null;
        }
        private static ColorSet toDeckColor(final InventoryItem i) {
            return i instanceof Deck ? ((Deck) i).getColor() : null;
        }
        private static int toDeckCount(final InventoryItem i, DeckSection section) {
            Deck deck = toDeck(i);
            if (deck == null) { return -1; }
            CardPool cards = deck.get(section);
            if (cards == null) { return -1; }
            int count = cards.countAll();
            if (count == 0) { return -1; }
            return count;
        }
    }
}
