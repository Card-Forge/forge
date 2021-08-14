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
import forge.card.mana.ManaCost;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.gamemodes.limited.CardRanker;
import forge.gui.card.CardPreferences;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.item.PaperCard;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.model.FModel;
import forge.util.CardTranslation;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map.Entry;

public enum ColumnDef {
    /**
     * The column containing the inventory item name.
     */
    STRING("", "", 0, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return from.getKey() instanceof Comparable<?> ? (Comparable<?>) from.getKey() : from.getKey().getName();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return from.getKey().toString();
                }
            }),
    /**
     * The name column.
     */
    NAME("lblName", "lblName", 180, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    if (from.getKey() instanceof PaperCard)
                        return toSortableName(from.getKey().toString());
                    return toSortableName(from.getKey().getName());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    if (from.getKey() instanceof PaperCard)
                        return from.getKey().toString();
                    return from.getKey().getName();
                }
            }),

    /**
     * The column for sorting cards in collector order.
     */
    COLLECTOR_ORDER("lblCN", "ttCN", 20, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toCollectorPrefix(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    InventoryItem item = from.getKey();
                    return item instanceof PaperCard ?
                            ((PaperCard) item).getCollectorNumber() : IPaperCard.NO_COLLECTOR_NUMBER;
                }
            }),
    /**
     * The type column.
     */
    TYPE("lblType", "ttType", 100, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return CardTranslation.getTranslatedType(from.getKey().getName(), toType(from.getKey()));
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return CardTranslation.getTranslatedType(from.getKey().getName(), toType(from.getKey()));
                }
            }),
    /**
     * The mana cost column.
     */
    COST("lblCost", "ttCost", 70, true, SortState.ASC,
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
    /**
     * The color column.
     */
    COLOR("lblColor", "ttColor", 46, true, SortState.ASC,
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
    /**
     * The power column.
     */
    POWER("lblPower", "ttPower", 20, true, SortState.DESC,
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
    /**
     * The toughness column.
     */
    TOUGHNESS("lblToughness", "ttToughness", 20, true, SortState.DESC,
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
    /**
     * The converted mana cost column.
     */
    CMC("lblCMC", "ttCMC", 20, true, SortState.ASC,
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
    /**
     * The rarity column.
     */
    RARITY("lblRarity", "lblRarity", 20, true, SortState.DESC,
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
    /**
     * The set code column.
     */
    SET("lblSet", "lblSet", 38, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    if (!(i instanceof InventoryItemFromSet))
                            return CardEdition.UNKNOWN;
                    String editionCode = ((InventoryItemFromSet) i).getEdition();
                    return FModel.getMagicDb().getCardEdition(editionCode);
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    InventoryItem i = from.getKey();
                    return i instanceof InventoryItemFromSet ? ((InventoryItemFromSet) i).getEdition() : "n/a";
                }
            }),
    /**
     * The AI compatibility flag column
     */
    AI("lblAI", "lblAIStatus", 30, true, SortState.ASC,
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
    /**
     * The Draft ranking column.
     */
    RANKING("lblRanking", "lblDraftRanking", 50, true, SortState.ASC,
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
    /**
     * The quantity column.
     */
    QUANTITY("lblQty", "lblQuantity", 25, true, SortState.ASC,
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
    /**
     * The quantity in deck column.
     */
    DECK_QUANTITY("lblQuantity", "lblQuantity", 50, true, SortState.ASC,
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
    /**
     * The new inventory flag column.
     */
    NEW("lblNew", "lblNew", 30, true, SortState.DESC,
            null, null), //functions will be set later
    /**
     * The price column.
     */
    PRICE("lblPrice", "ttPrice", 35, true, SortState.DESC,
            null, null),
    /**
     * The quantity owned column.
     */
    OWNED("lblOwned", "lblOwned", 20, true, SortState.ASC,
            null, null),
    /**
     * The deck name column.
     */
    DECKS("lblDecks", "lblDecks", 20, true, SortState.ASC,
            null, null),
    /**
     * The favorite flag column.
     */
    FAVORITE("", "ttFavorite", 18, true, SortState.DESC,
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
    /**
     * The favorite deck flag column.
     */
    DECK_FAVORITE("", "ttFavorite", 18, true, SortState.DESC,
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
    /**
     * The edit/delete deck column.
     */
    DECK_ACTIONS("", "lblDeleteEdit", 40, true, SortState.DESC,
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
    /**
     * The deck folder column.
     */
    DECK_FOLDER("lblFolder", "lblFolder", 80, false, SortState.ASC,
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
    /**
     * The deck color column.
     */
    DECK_COLOR("lblColor", "ttColor", 70, true, SortState.ASC,
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
    /**
     * The deck format column.
     */
    DECK_FORMAT("lblFormat", "ttFormats", 60, false, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    DeckProxy deck = toDeck(from.getKey());
                    if (deck == null) {
                        return -1;
                    }
                    Iterable<GameFormat> all = deck.getExhaustiveFormats();
                    int acc = 0;
                    for (GameFormat gf : all) {
                        int ix = gf.getIndex();
                        if (ix < 30 && ix > 0)
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
    /**
     * The deck edition column, a mystery to us all.
     */
    DECK_EDITION("lblSet", "lblSet", 38, true, SortState.DESC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toDeck(from.getKey()).getEdition();
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    CardEdition deckEdition = toDeck(from.getKey()).getEdition();
                    if (deckEdition != null)
                        return deckEdition.getCode();
                    return null;
                }
            }),
    /**
     * The main library size column.
     */
    DECK_MAIN("lblMain", "ttMain", 30, true, SortState.ASC,
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
    /**
     * The sideboard size column.
     */
    DECK_SIDE("lblSide", "lblSideboard", 30, true, SortState.ASC,
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
        final Localizer localizer = Localizer.getInstance();

        if (shortName0 != null && !shortName0.isEmpty()) {
            this.shortName = localizer.getMessage(shortName0);
        } else {
            this.shortName = shortName0;
        }
        if (longName0 != null && !longName0.isEmpty()) {
            this.longName = localizer.getMessage(longName0);
        } else {
            this.longName = longName0;
        }

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

    /**
     * Converts a card name to a sortable name.
     * Trim leading quotes, then move article last, then replace characters.
     * Because An-Havva Constable.
     * Capitals and lowercase sorted as one: "my deck" before "Myr Retribution"
     * Apostrophes matter, though: "D'Avenant" before "Danitha"
     * TO DO: Commas before apostrophes: "Rakdos, Lord of Riots" before "Rakdos's Return"
     *
     * @param printedName The name of the card.
     * @return A sortable name.
     */
    private static String toSortableName(String printedName) {
        if (printedName.startsWith("\"")) printedName = printedName.substring(1);
        return moveArticleToEnd(printedName).toLowerCase().replaceAll("[^\\s'0-9a-z]", "");
    }


    /**
     * Article words. These words get kicked to the end of a sortable name.
     * For localization, simply overwrite this array with appropriate words.
     * Words in this list are used by the method String moveArticleToEnd(String), useful
     * for alphabetizing phrases, in particular card or other inventory object names.
     */
    private static final String[] ARTICLE_WORDS = {
            "A",
            "An",
            "The"
    };

    /**
     * Detects whether a string begins with an article word
     *
     * @param str The name of the card.
     * @return The sort-friendly name of the card. Example: "The Hive" becomes "Hive The".
     */
    private static String moveArticleToEnd(String str) {
        String articleWord;
        for (int i = 0; i < ARTICLE_WORDS.length; i++) {
            articleWord = ARTICLE_WORDS[i];
            if (str.startsWith(articleWord + " ")) {
                str = str.substring(articleWord.length() + 1) + " " + articleWord;
                return str;
            }
        }
        return str;
    }

    private static String toType(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules().getType().toString() : i.getItemType();
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
        int result = Integer.MAX_VALUE;
        if (i instanceof PaperCard) {
            result = ((IPaperCard) i).getRules().getIntPower();
            if (result == Integer.MAX_VALUE) {
                if (((IPaperCard) i).getRules().getType().isPlaneswalker()) {
                    String loy = ((IPaperCard) i).getRules().getInitialLoyalty();
                    result = StringUtils.isNumeric(loy) ? Integer.valueOf(loy) : 0;
                }
            }
        }
        return result;
    }

    private static Integer toToughness(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getRules().getIntToughness() : Integer.MAX_VALUE;
    }

    private static Integer toCMC(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getRules().getManaCost().getCMC() : -1;
    }

    private static CardRarity toRarity(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getRarity() : CardRarity.Unknown;
    }

    private static Double toRanking(final InventoryItem i, boolean truncate) {
        if (i instanceof PaperCard) {
            PaperCard cp = (PaperCard) i;
            double ranking = CardRanker.getRawScore(cp);
            if (truncate) {
                return new BigDecimal(ranking).setScale(4, RoundingMode.HALF_UP).doubleValue();
            }
            return ranking;
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

    /**
     * Generates a sortable numeric string based on a card's attributes.
     * This is a multi-layer sort. It is coded in layers to make it easier to manipulate.
     * This method can be fed any inventory item, but is only useful for paper cards.
     *
     * @param i An inventory item.
     * @return A sortable numeric string based on the item's attributes.
     */
    private static String toCollectorPrefix(final InventoryItem i) {
        //make sure it's a card. if not, pointless to proceed.
        String collectorNumber;
        if (i instanceof PaperCard) {
            collectorNumber = ((PaperCard) i).getCollectorNumberSortingKey();
        } else {
            collectorNumber = IPaperCard.NO_COLLECTOR_NUMBER;
        }
        return collectorNumber;
    }

    /**
     * Returns 1 for land, otherwise 0 and continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toLandsLast(final InventoryItem i) {
        //nonland?
        return !(((IPaperCard) i).getRules().getType().isLand()) ?
                "0" + toArtifactsWithColorlessCostsLast(i)
                //land
                : "1";
    }

    /**
     * Returns 1 for artifacts without color shards in their mana cost, otherwise 0 and continues sorting.
     * As of 2019, colored artifacts appear here if there are no colored shards in their casting cost.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toArtifactsWithColorlessCostsLast(final InventoryItem i) {
        forge.card.mana.ManaCost manaCost = ((IPaperCard) i).getRules().getManaCost();

        return !(((IPaperCard) i).getRules().getType().isArtifact() && (toColor(i).isColorless() ||
                //If it isn't colorless, see if it can be paid with only white, only blue, only black.
                //No need to check others since three-color hybrid shards don't exist.
                manaCost.canBePaidWithAvailable(MagicColor.WHITE) &&
                        manaCost.canBePaidWithAvailable(MagicColor.BLUE) &&
                        manaCost.canBePaidWithAvailable(MagicColor.BLACK)))
                ? "0" + toSplitLast(i) : "1";
    }

    /**
     * Returns 1 for split cards or 0 for other cards; continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toSplitLast(final InventoryItem i) {
        return ((IPaperCard) i).getRules().getSplitType() != CardSplitType.Split ?
                "0" + toConspiracyFirst(i) : "1" + toSplitCardSort(i);
    }

    /**
     * Returns 0 for Conspiracy cards, otherwise 1 and continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toConspiracyFirst(final InventoryItem i) {
        return ((IPaperCard) i).getRules().getType().isConspiracy()
                ? "0" //is a Conspiracy
                : "1" + toColorlessFirst(i); //isn't a Conspiracy
    }

    /**
     * Returns 0 for colorless cards, otherwise 1 and continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toColorlessFirst(final InventoryItem i) {
        return toColor(i).isColorless() ?
                "0" : "1" + toMonocolorFirst(i);
    }

    /**
     * Returns 0 for monocolor cards, 1 for multicolor cards; continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toMonocolorFirst(final InventoryItem i) {
        return toColor(i).isMonoColor() ?
                "0" + toWubrgOrder(i) : "1" + toGoldFirst(i);
    }

    /**
     * Returns 0 for gold cards and continues sorting, 1 otherwise.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toGoldFirst(final InventoryItem i) {
        forge.card.mana.ManaCost manaCost = ((IPaperCard) i).getRules().getManaCost();

        return !(manaCost.canBePaidWithAvailable(MagicColor.WHITE) | manaCost.canBePaidWithAvailable(MagicColor.BLUE) |
                manaCost.canBePaidWithAvailable(MagicColor.BLACK) | manaCost.canBePaidWithAvailable(MagicColor.RED) |
                manaCost.canBePaidWithAvailable(MagicColor.GREEN)) ? "0" : "1";
    }

    /**
     * Entry point for generating split card sortable strings.
     * Splits the card into two card faces, then sends it to the next
     * sorting method.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    //Split card sorting is probably as complex as sorting gets.
    //This method serves as an entry point only, separating the two card parts for convenience.
    private static String toSplitCardSort(final InventoryItem i) {
        CardRules rules = ((IPaperCard) i).getRules();
        forge.card.ICardFace mainPart = rules.getMainPart();
        forge.card.ICardFace otherPart = rules.getOtherPart();
        return toSplitSort(mainPart, otherPart);
    }

    /**
     * Generates a sortable numeric string for split cards.
     * Split cards are sorted by color on both halves.
     * Sort order is C//C, W//W, U//U, B//B, R//R, G//G,
     * Gold/Gold,
     * W//U, U//B, B//R, R//G, G//W,
     * W//B, U//R, B//G, R//W, G//U,
     * W//R, U//G, B//W, R//U, G//B,
     * W//G, U//W, B//U, R//B, G//R.
     * Any that do not conform will sort at the end.
     *
     * @param mainPart  The first half of the card.
     * @param otherPart The other half of the card.
     * @return Part of a sortable numeric string.
     */
    private static String toSplitSort(final ICardFace mainPart, final ICardFace otherPart) {
        ColorSet mainPartColor = mainPart.getColor();
        ColorSet otherPartColor = otherPart.getColor();

        return mainPartColor.isEqual(otherPartColor.getColor())

                ? //both halves match

                (mainPartColor.isEqual(MagicColor.WHITE) ? "01" :
                        mainPartColor.isEqual(MagicColor.BLUE) ? "02" :
                                mainPartColor.isEqual(MagicColor.BLACK) ? "03" :
                                        mainPartColor.isEqual(MagicColor.RED) ? "04" :
                                                mainPartColor.isEqual(MagicColor.GREEN) ? "05" : "00")

                : //halves don't match

                //both halves gold
                mainPartColor.isMulticolor() && otherPartColor.isMulticolor() ? "06" :

                        //second color is << 1
                        mainPartColor.isEqual(MagicColor.WHITE) && otherPartColor.isEqual(MagicColor.BLUE) ? "11" :
                                mainPartColor.isEqual(MagicColor.BLUE) && otherPartColor.isEqual(MagicColor.BLACK) ? "12" :
                                        mainPartColor.isEqual(MagicColor.BLACK) && otherPartColor.isEqual(MagicColor.RED) ? "13" :
                                                mainPartColor.isEqual(MagicColor.RED) && otherPartColor.isEqual(MagicColor.GREEN) ? "14" :
                                                        mainPartColor.isEqual(MagicColor.GREEN) && otherPartColor.isEqual(MagicColor.WHITE) ? "15" :

                                                                //second color is << 2
                                                                mainPartColor.isEqual(MagicColor.WHITE) && otherPartColor.isEqual(MagicColor.BLACK) ? "21" :
                                                                        mainPartColor.isEqual(MagicColor.BLUE) && otherPartColor.isEqual(MagicColor.RED) ? "22" :
                                                                                mainPartColor.isEqual(MagicColor.BLACK) && otherPartColor.isEqual(MagicColor.GREEN) ? "23" :
                                                                                        mainPartColor.isEqual(MagicColor.RED) && otherPartColor.isEqual(MagicColor.WHITE) ? "24" :
                                                                                                mainPartColor.isEqual(MagicColor.GREEN) && otherPartColor.isEqual(MagicColor.BLUE) ? "25" :

                                                                                                        //second color is << 3
                                                                                                        mainPartColor.isEqual(MagicColor.WHITE) && otherPartColor.isEqual(MagicColor.RED) ? "31" :
                                                                                                                mainPartColor.isEqual(MagicColor.BLUE) && otherPartColor.isEqual(MagicColor.GREEN) ? "32" :
                                                                                                                        mainPartColor.isEqual(MagicColor.BLACK) && otherPartColor.isEqual(MagicColor.WHITE) ? "33" :
                                                                                                                                mainPartColor.isEqual(MagicColor.RED) && otherPartColor.isEqual(MagicColor.BLUE) ? "34" :
                                                                                                                                        mainPartColor.isEqual(MagicColor.GREEN) && otherPartColor.isEqual(MagicColor.BLACK) ? "35" :

                                                                                                                                                //second color is << 4
                                                                                                                                                mainPartColor.isEqual(MagicColor.WHITE) && otherPartColor.isEqual(MagicColor.GREEN) ? "41" :
                                                                                                                                                        mainPartColor.isEqual(MagicColor.BLUE) && otherPartColor.isEqual(MagicColor.WHITE) ? "42" :
                                                                                                                                                                mainPartColor.isEqual(MagicColor.BLACK) && otherPartColor.isEqual(MagicColor.BLUE) ? "43" :
                                                                                                                                                                        mainPartColor.isEqual(MagicColor.RED) && otherPartColor.isEqual(MagicColor.BLACK) ? "44" :
                                                                                                                                                                                mainPartColor.isEqual(MagicColor.GREEN) && otherPartColor.isEqual(MagicColor.RED) ? "45"

                                                                                                                                                                                        ://No split cards have been printed that don't fall into one of these groups.

                                                                                                                                                                                        "99";
    }

    /**
     * Returns 0 for white, 1 for blue, 2 for black, 3 for red, or 4 for green.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toWubrgOrder(final InventoryItem i) {
        ColorSet color = toColor(i);
        return color.hasWhite() ? "0" : color.hasBlue() ? "1" : color.hasBlack() ? "2" :
                color.hasRed() ? "3" : "4";
    }

    /**
     * Returns 1 for Contraptions, otherwise 0 and continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toContraptionsLast(final InventoryItem i) {
        return !(((IPaperCard) i).getRules().getType().hasSubtype("Contraption")) ?
                "0" + toLandsLast(i) : "1";
    }

    /**
     * Returns 1 for basic lands, 0 otherwise, and continues sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toBasicLandsLast(final InventoryItem i) {
        return !(((IPaperCard) i).getRules().getType().isBasicLand())
                ? "0" + toContraptionsLast(i)
                : "1" + toFullArtFirst(i);
    }

    /**
     * Currently only continues sorting. If Forge is updated to
     * use a flag for full-art lands, this method should be updated
     * to assign those 0 and regular lands 1, then continue sorting.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toFullArtFirst(final InventoryItem i) {
        return toBasicLandSort(i);
    }

    /**
     * Returns 0 for wastes, 1 for plains, 2 for island,
     * 3 for swamp, 4 for mountain, 5 for forest. Snow
     * lands are treated like nonsnow.
     *
     * @param i A paper card.
     * @return Part of a sortable numeric string.
     */
    private static String toBasicLandSort(final InventoryItem i) {
        CardType basicLandType = ((IPaperCard) i).getRules().getType();
        return basicLandType.hasStringType("Plains") ? "1" : (
                basicLandType.hasStringType("Island") ? "2" : (
                        basicLandType.hasStringType("Swamp") ? "3" : (
                                basicLandType.hasStringType("Mountain") ? "4" : (
                                        basicLandType.hasStringType("Forest") ? "5" : "0"
                                )
                        )
                )
        );
    }
}
