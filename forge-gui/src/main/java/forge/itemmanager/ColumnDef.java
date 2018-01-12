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
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.item.PaperCard;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.limited.DraftRankCache;
import forge.model.FModel;

import java.math.BigDecimal;
import java.util.Map.Entry;

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
                    return toSortableName(from.getKey().getName());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return from.getKey().getName();
                }
            }),
            
    //Sorts cards in the order that is used for assigning collector numbers.
    //Appends a numeric prefix followed by the sortable name.
    COLLECTOR_ORDER("CN", "Collector Number Order", 20, false, SortState.ASC,
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                    return toCollectorPrefix(from.getKey());
                }
            },
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                    return "";
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

    //Trim leading quotes, then move article last, then replace characters.
    //Because An-Havva Constable.
    //Ignore all punctuation marks: "Chandra, Flamecaller" before "Chandra's Firebird"
    //Capitals and lowercase sorted as one: "my deck" before "Myr Retribution"
    private static String toSortableName(String printedName) {
      if (printedName.startsWith("\"")) printedName = printedName.substring(1);
      return moveArticleToEnd(printedName).toLowerCase().replaceAll("[^a-z0-9\\s]","");
    }
    
    /*For localization, simply overwrite this array with appropriate words.
      Words in this list are used by the method String moveArticleToEnd(String), useful
      for alphabetizing phrases, in particular card or other inventory object names.*/
    private static final String[] ARTICLE_WORDS = {
         "A",
         "An",
         "The"
    };
    
    //Detects whether a string begins with an article word
    private static String moveArticleToEnd(String str){
      String articleWord;
      for (int i = 0; i < ARTICLE_WORDS.length; i++){
         articleWord = ARTICLE_WORDS[i];
         if (str.startsWith(articleWord + " ")){
            str = str.substring(articleWord.length()+1) + " " + articleWord;
            return str;
         }
      }
      return str;
    }
    
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
        int result = Integer.MAX_VALUE;
        if (i instanceof PaperCard) {
            result = ((IPaperCard) i).getRules().getIntPower();
            if (result == Integer.MAX_VALUE) {
                if (((IPaperCard)i).getRules().getType().isPlaneswalker()) {
                    result = ((IPaperCard) i).getRules().getInitialLoyalty();
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
    
    //START COLLECTOR-NUMBER-STYLE SORTING CODE//
    //this is a multi-layer sort. coding it in layers to make it easier to manipulate.
    private static String toCollectorPrefix(final InventoryItem i) {
      //make sure it's a card. if not, pointless to proceed.
      return (i instanceof PaperCard ? ((IPaperCard) i).getCollectorIndex() + " ": "") + toSortableName(i.getName());
    }
    
    //lands after other cards
    private static String toLandsLast(final InventoryItem i) {
         //nonland?
         return !(((IPaperCard) i).getRules().getType().isLand()) ?
            "0" + toColorlessArtifactsLast(i)
         //land
         : "1";
    }
    
    //colorless artifacts last
    private static String toColorlessArtifactsLast(final InventoryItem i) {
     return !(((IPaperCard) i).getRules().getType().isArtifact() && toColor(i).isColorless())
            ? "0" + toSplitLast(i): "1";
    }
    
    //split cards last
    private static String toSplitLast(final InventoryItem i) {
      return ((IPaperCard) i).getRules().getSplitType() != CardSplitType.Split ?
            "0" + toConspiracyFirst(i): "1" + to SplitCardSort(i);
    }
    
    //conspiracy first
    private static String toConspiracyFirst(final InventoryItem i) {
        return !(((IPaperCard) i).getRules().getType().hasType("Conspiracy")) ?
            "0": "1" + toSplitCardSort(i);
    }
    
    //colorless first, then colored.
    private static String toColorlessFirst(final InventoryItem i) {
      return toColor(i).isColorless() ?
            "0" : "1" + toMonocolorFirst(i);
    }
    
    //monocolor nonartifact nonland spells are first, then multicolored.
    private static String toMonocolorFirst(final InventoryItem i) {
      return toColor(i).isMonoColor() ?
            "0" + toWubrgOrder(i): "1" + toGoldFirst(i);
    }
    
    //gold cards first
      private static String toGoldFirst(final InventoryItem i) {
      forge.card.mana.ManaCost manaCost = ((IPaperCard) i).getRules().getManaCost();
      
      return !(manaCost.canBePaidWithAvaliable(MagicColor.WHITE) | manaCost.canBePaidWithAvaliable(MagicColor.BLUE) |
            manaCost.canBePaidWithAvaliable(MagicColor.BLACK) | manaCost.canBePaidWithAvaliable(MagicColor.RED) |
            manaCost.canBePaidWithAvaliable(MagicColor.GREEN)) ? "0" : "1";
    }
    
    //Split card sorting is probably as complex as sorting gets.
    //This method serves as an entry point only, separating the two card parts for convenience.
    private static String toSplitCardSort(final InventoryItem i) {
      CardRules rules = ((IPaperCard) i).getRules();
      forge.card.ICardFace mainPart = rules.getMainPart();
      forge.card.ICardFace otherPart = rules.getOtherPart();
      return toSplitSort(mainPart, otherPart);
    }
    
    //Split cards are sorted by color on both halves.
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
    
    //sort by casting cost color
    private static String toWubrgOrder(final InventoryItem i) {
      ColorSet color = toColor(i);
      return color.hasWhite() ? "0" : color.hasBlue() ? "1" : color.hasBlack() ? "2" :
            color.hasRed() ? "3" : "4";
    }
    
    //Contraptions are after all other cards except basic lands
    private static String toContraptionsLast(final InventoryItem i) {
      return !(((IPaperCard) i).getRules().getType().hasSubtype("Contraption")) ?
            "0" + toLandsLast(i) : "1";
    }
    
    //basic lands are after all other cards
    private static String toBasicLandsLast(final InventoryItem i) {
      return !(((IPaperCard) i).getRules().getType().isBasicLand())
            ? "0" + toContraptionsLast(i)
            : "1" + toFullArtFirst(i);
    }
    
    //basic lands are sorted full-art, then normal art.
    //Forge doesn't make this distinction. If it did, this prefix would be added just before
    //the basic land type prefix.
    private static String toFullArtFirst(final InventoryItem i) {
      return toBasicLandSort(i);
    }
    
    //Plains, Island, Swamp, Mountain, Forest.
    //Not sure what to do with Wastes or Snow-Covered lands, so putting the typeless
    //Wastes first and letting Snow-Covered lands fall in with their nonsnow friends.
    //Full-art basic lands are supposed to come before all others, but Forge doesn't distinguish
    //the two.
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
