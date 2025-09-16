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
import forge.util.*;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

public enum ColumnDef {
    /**
     * The column containing the inventory item name.
     */
    STRING("", "", 0, false, SortState.ASC,
            from -> from.getKey() instanceof Comparable<?> ? (Comparable<?>) from.getKey() : from.getKey().getName(),
            from -> from.getKey().toString()),
    /**
     * The name column.
     */
    NAME("lblName", "lblName", 180, false, SortState.ASC,
            from -> {
                if (from.getKey() instanceof PaperCard) {
                    String spire = ((PaperCard) from.getKey()).getMarkedColors() == null ? "" : ((PaperCard) from.getKey()).getMarkedColors().toString();
                    String sortableName = ((PaperCard)from.getKey()).getSortableName();
                    return sortableName == null ? TextUtil.toSortableName(from.getKey().getName() + spire) : sortableName + spire;
                }
                return TextUtil.toSortableName(from.getKey().getName());
            },
            from -> {
                if (from.getKey() instanceof PaperCard)
                    return from.getKey().toString();
                return from.getKey().getName();
            }),

    /**
     * The column for sorting cards in collector order.
     */
    COLLECTOR_ORDER("lblCN", "ttCN", 20, false, SortState.ASC,
            from -> toCollectorPrefix(from.getKey()),
            from -> {
                InventoryItem item = from.getKey();
                return item instanceof PaperCard ? ((PaperCard) item).getCollectorNumber() : IPaperCard.NO_COLLECTOR_NUMBER;
            }),
    /**
     * The type column.
     */
    TYPE("lblType", "ttType", 100, false, SortState.ASC,
            from -> CardTranslation.getTranslatedType(from.getKey()),
            from -> CardTranslation.getTranslatedType(from.getKey())),
    /**
     * The mana cost column.
     */
    COST("lblCost", "ttCost", 70, true, SortState.ASC,
            from -> toManaCost(from.getKey()),
            from -> toCardRules(from.getKey())),
    /**
     * The color column.
     */
    COLOR("lblColor", "ttColor", 46, true, SortState.ASC,
            from -> toColor(from.getKey()),
            from -> toColor(from.getKey())),
    /**
     * The power column.
     */
    POWER("lblPower", "ttPower", 20, true, SortState.DESC,
            from -> toPower(from.getKey()),
            from -> toPower(from.getKey())),
    /**
     * The toughness column.
     */
    TOUGHNESS("lblToughness", "ttToughness", 20, true, SortState.DESC,
            from -> toToughness(from.getKey()),
            from -> toToughness(from.getKey())),
    /**
     * The converted mana cost column.
     */
    CMC("lblCMC", "ttCMC", 20, true, SortState.ASC,
            from -> toCMC(from.getKey()),
            from -> toCMC(from.getKey())),
    ATTRACTION_LIGHTS("lblLights", "lblLights", 94, true, SortState.NONE,
            from -> toAttractionLightSort(from.getKey()),
            from -> toAttractionLights(from.getKey())
    ),
    /**
     * The rarity column.
     */
    RARITY("lblRarity", "lblRarity", 20, true, SortState.DESC,
            from -> toRarity(from.getKey()),
            from -> toRarity(from.getKey())),
    /**
     * The set code column.
     */
    SET("lblSet", "lblSet", 38, true, SortState.DESC,
            from -> {
                InventoryItem i = from.getKey();
                if (!(i instanceof InventoryItemFromSet))
                        return CardEdition.UNKNOWN;
                String editionCode = ((InventoryItemFromSet) i).getEdition();
                return FModel.getMagicDb().getCardEdition(editionCode);
            },
            from -> {
                InventoryItem i = from.getKey();
                return i instanceof InventoryItemFromSet ? ((InventoryItemFromSet) i).getEdition() : "n/a";
            }),
    /**
     * The AI compatibility flag column
     */
    AI("lblAI", "lblAIStatus", 30, true, SortState.ASC,
            from -> {
                InventoryItem i = from.getKey();
                return i instanceof PaperCard ? ((IPaperCard) i).getRules().getAiHints().getAiStatusComparable() : Integer.valueOf(-1);
            },
            from -> {
                InventoryItem i = from.getKey();
                if (!(i instanceof PaperCard)) {
                    return "n/a";
                }
                IPaperCard cp = (IPaperCard) i;
                CardAiHints ai = cp.getRules().getAiHints();

                return ai.getRemAIDecks() ? (ai.getRemRandomDecks() ? "X?" : "X")
                        : (ai.getRemRandomDecks() ? "?" : "");
            }),
    /**
     * The card format column.
     */
    FORMAT("lblFormat", "ttFormats", 60, false, SortState.DESC,
            from -> {
                PaperCard card = toPaperCard(from.getKey());
                if (card == null) {
                    return -1;
                }
                Iterable<GameFormat> formats = FModel.getFormats().getAllFormatsOfCard(card);
                int acc = 0;
                for (GameFormat gf : formats) {
                    if (!gf.getFormatType().equals(GameFormat.FormatType.SANCTIONED)) {
                        continue;
                    }
                    int ix = gf.getIndex();
                    if (ix < 30 && ix > 0)
                        acc |= 0x40000000 >> (ix - 1);
                }
                return acc;
            },
            from -> {
                PaperCard card = toPaperCard(from.getKey());
                if (card == null) {
                    return -1;
                }
                Iterable<GameFormat> formats = FModel.getFormats().getAllFormatsOfCard(card);
                Set<GameFormat> sanctioned = new HashSet<>();
                for (GameFormat gf : formats) {
                    if (gf.getFormatType().equals(GameFormat.FormatType.SANCTIONED)) {
                        sanctioned.add(gf);
                    }
                }
                return StringUtils.join(IterableUtil.transform(sanctioned, GameFormat::getName), ", ");
            }),
    /**
     * The Draft ranking column.
     */
    RANKING("lblRanking", "lblDraftRanking", 50, true, SortState.ASC,
            from -> toRanking(from.getKey(), false),
            from -> toRanking(from.getKey(), true)),
    /**
     * The quantity column.
     */
    QUANTITY("lblQty", "lblQuantity", 25, true, SortState.ASC,
            Entry::getValue, Entry::getValue),
    /**
     * The quantity in deck column.
     */
    DECK_QUANTITY("lblQuantity", "lblQuantity", 50, true, SortState.ASC,
            Entry::getValue, Entry::getValue),
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
            from -> {
                IPaperCard card = toCard(from.getKey());
                if (card == null) {
                    return -1;
                }
                return CardPreferences.getPrefs(card).getStarCount();
            },
            from -> {
                IPaperCard card = toCard(from.getKey());
                if (card == null)
                    return 0;
                return CardPreferences.getPrefs(card).getStarCount();
            }),
    /**
     * The favorite deck flag column.
     */
    DECK_FAVORITE("", "ttFavorite", 18, true, SortState.DESC,
            from -> {
                DeckProxy deck = toDeck(from.getKey());
                if (deck == null) {
                    return -1;
                }
                return DeckPreferences.getPrefs(deck).getStarCount();
            },
            from -> toDeck(from.getKey())),
    /**
     * The edit/delete deck column.
     */
    DECK_ACTIONS("", "lblDeleteEdit", 40, true, SortState.DESC,
            from -> 0,
            from -> toDeck(from.getKey())),
    /**
     * The deck folder column.
     */
    DECK_FOLDER("lblFolder", "lblFolder", 80, false, SortState.ASC,
            from -> toDeckFolder(from.getKey()),
            from -> toDeckFolder(from.getKey())),
    /**
     * The deck color column.
     */
    DECK_COLOR("lblColor", "ttColor", 70, true, SortState.ASC,
            from -> toDeckColor(from.getKey()),
            from -> toDeckColor(from.getKey())),
    /**
     * The deck format column.
     */
    DECK_FORMAT("lblFormat", "ttFormats", 60, false, SortState.DESC,
            from -> {
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
            },
            from -> {
                DeckProxy deck = toDeck(from.getKey());
                if (deck == null) {
                    return null;
                }
                return deck.getFormatsString();
            }),
    /**
     * The deck edition column, a mystery to us all.
     */
    DECK_EDITION("lblSet", "lblSet", 38, true, SortState.DESC,
            from -> toDeck(from.getKey()).getEdition(),
            from -> {
                CardEdition deckEdition = toDeck(from.getKey()).getEdition();
                if (deckEdition != null)
                    return deckEdition.getCode();
                return null;
            }),
    DECK_AI("lblAI", "lblAIStatus", 38, true, SortState.DESC,
            from -> toDeck(from.getKey()).getAI().inMainDeck,
            from -> toDeck(from.getKey()).getAI()),
    /**
     * The main library size column.
     */
    DECK_MAIN("lblMain", "ttMain", 30, true, SortState.ASC,
            from -> toDeck(from.getKey()).getMainSize(),
            from -> toDeck(from.getKey()).getMainSize()),
    /**
     * The sideboard size column.
     */
    DECK_SIDE("lblSide", "lblSideboard", 30, true, SortState.ASC,
            from -> toDeck(from.getKey()).getSideSize(),
            from -> toDeck(from.getKey()).getSideSize());

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

    private static IPaperCard toCard(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i) : null;
    }

    private static PaperCard toPaperCard(final InventoryItem i) {
        return i instanceof PaperCard ? ((PaperCard) i) : null;
    }

    private static ManaCost toManaCost(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules().getManaCost() : ManaCost.NO_COST;
    }

    private static CardRules toCardRules(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules() : null;
    }

    private static ColorSet toColor(final InventoryItem i) {
        return i instanceof IPaperCard ? ((IPaperCard) i).getRules().getColor() : ColorSet.NO_COLORS;
    }

    private static Integer toPower(final InventoryItem i) {
        int result = Integer.MAX_VALUE;
        if (i instanceof PaperCard) {
            ICardFace face = ((IPaperCard) i).getMainFace();
            result = face.getIntPower();
            if (result == Integer.MAX_VALUE) {
                if (face.getType().isPlaneswalker()) {
                    String loy = face.getInitialLoyalty();
                    result = StringUtils.isNumeric(loy) ? Integer.parseInt(loy) : 0;
                }
            }
        }
        return result;
    }

    private static Integer toToughness(final InventoryItem i) {
        return i instanceof PaperCard ? ((IPaperCard) i).getMainFace().getIntToughness() : Integer.MAX_VALUE;
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
        ManaCost manaCost = ((IPaperCard) i).getRules().getManaCost();

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
        ManaCost manaCost = ((IPaperCard) i).getRules().getManaCost();

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
        ICardFace mainPart = ((IPaperCard) i).getMainFace();
        ICardFace otherPart = ((IPaperCard) i).getOtherFace();
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

    private static Set<Integer> toAttractionLights(final InventoryItem i) {
        return i instanceof PaperCard ? ((PaperCard) i).getMainFace().getAttractionLights() : null;
    }

    private static String toAttractionLightSort(final InventoryItem i) {
        if(!(i instanceof PaperCard))
            return "";
        Set<Integer> lights = ((PaperCard) i).getRules().getAttractionLights();
        return (lights.contains(1) ? "0" : "1") +
                (lights.contains(2) ? "0" : "1") +
                (lights.contains(3) ? "0" : "1") +
                (lights.contains(4) ? "0" : "1") +
                (lights.contains(5) ? "0" : "1") +
                (lights.contains(6) ? "0" : "1");
    }
}
