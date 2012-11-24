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
package forge.deck.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Constant;
import forge.Singletons;
import forge.card.CardColor;
import forge.card.CardRulesPredicates;
import forge.card.CardRules;
import forge.deck.generate.GenerateDeckUtil.FilterCMC;
import forge.game.player.PlayerType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * <p>
 * Generate2ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id: Generate2ColorDeck.java 14959 2012-03-28 14:03:43Z Chris H. $
 */
public abstract class GenerateColoredDeckBase {
    protected final Random r = MyRandom.getRandom();
    protected final Map<String, Integer> cardCounts = new HashMap<String, Integer>();
    protected int maxDuplicates;

    protected CardColor colors;
    protected final ItemPool<CardPrinted> tDeck;

    // 2-colored deck generator has its own constants. The rest works fine with these ones
    protected float getLandsPercentage() { return 0.44f; }
    protected float getCreatPercentage() { return 0.34f; }
    protected float getSpellPercentage() { return 0.22f; }

    StringBuilder tmpDeck = new StringBuilder();

//    protected final float landsPercentage = 0.42f;
//    protected float creatPercentage = 0.34f;
//    protected float spellPercentage = 0.24f;
    /**
     * <p>
     * Constructor for Generate2ColorDeck.
     * </p>
     * 
     * @param clr1
     *            a {@link java.lang.String} object.
     * @param clr2
     *            a {@link java.lang.String} object.
     */
    public GenerateColoredDeckBase() {
        this.maxDuplicates = Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS) ? 1 : 4;
        tDeck = new ItemPool<CardPrinted>(CardPrinted.class);
    }

    protected void addCreaturesAndSpells(int size, List<FilterCMC> cmcLevels, int[] cmcAmounts, PlayerType pt) {
        final Iterable<CardPrinted> cards = selectCardsOfMatchingColorForPlayer(pt);
        // build subsets based on type

        final Iterable<CardPrinted> creatures = Iterables.filter(cards, Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, CardPrinted.FN_GET_RULES));
        final int creatCnt = (int) (getCreatPercentage() * size);
        tmpDeck.append("Creature Count:").append(creatCnt).append("\n");
        addCmcAdjusted(creatures, creatCnt, cmcLevels, cmcAmounts);

        Predicate<CardPrinted> preSpells = Predicates.compose(CardRulesPredicates.Presets.IS_NONCREATURE_SPELL_FOR_GENERATOR, CardPrinted.FN_GET_RULES);
        final Iterable<CardPrinted> spells = Iterables.filter(cards, preSpells);
        final int spellCnt = (int) (getSpellPercentage() * size);
        tmpDeck.append("Spell Count:").append(spellCnt).append("\n");
        addCmcAdjusted(spells, spellCnt, cmcLevels, cmcAmounts);
    }


    protected void addSome(int cnt, List<CardPrinted> source) {
        for (int i = 0; i < cnt; i++) {
            CardPrinted c;
            int lc = 0;
            do {
                c = source.get(this.r.nextInt(source.size()));
                lc++;
            } while ((this.cardCounts.get(c.getName()) > (this.maxDuplicates - 1)) && (lc <= 100));

            if (lc > 100) {
                throw new RuntimeException("Generate2ColorDeck : get2ColorDeck -- looped too much -- Cr12");
            }

            tDeck.add(c);
            final int n = this.cardCounts.get(c.getName());
            this.cardCounts.put(c.getName(), n + 1);
            tmpDeck.append(c.getName() + " " + c.getCard().getManaCost() + "\n");
        }
    }

    protected int addSomeStr(int cnt, List<String> source) {
        int res = 0;
        for (int i = 0; i < cnt; i++) {
            String s;
            int lc = 0;
            do {
                s = source.get(this.r.nextInt(source.size()));
                lc++;
            } while ((this.cardCounts.get(s) > 3) && (lc <= 20));
            // not an error if looped too much - could play singleton mode, with 6 slots for 3 non-basic lands.

            tDeck.add(CardDb.instance().getCard(s));
            final int n = this.cardCounts.get(s);
            this.cardCounts.put(s, n + 1);
            tmpDeck.append(s + "\n");
            res++;
        }
        return res;
    }

    protected void addBasicLand(int cnt) {
        // attempt to optimize basic land counts according to colors of picked cards
        final Map<String, Integer> clrCnts = countLands(tDeck);
        // total of all ClrCnts
        float totalColor = 0;
        for (Entry<String, Integer> c : clrCnts.entrySet()) {
            totalColor += c.getValue();
            tmpDeck.append(c.getKey()).append(":").append(c.getValue()).append("\n");
        }

        tmpDeck.append("totalColor:").append(totalColor).append("\n");

        for (Entry<String, Integer> c : clrCnts.entrySet()) {
            String color = c.getKey();


            // calculate number of lands for each color
            float p = (float) c.getValue() / totalColor;
            final int nLand = (int) (cnt * p);
            tmpDeck.append("nLand-").append(color).append(":").append(nLand).append("\n");

            // just to prevent a null exception by the deck size fixing
            // code
            this.cardCounts.put(color, nLand);

            for (int j = 0; j <= nLand; j++) {
                tDeck.add(CardDb.instance().getCard(color));
            }
        }
    }

    protected void adjustDeckSize(int targetSize) {
        // fix under-sized or over-sized decks, due to integer arithmetic
        int actualSize = tDeck.countAll();
        if (actualSize < targetSize) {
            final int diff = targetSize - actualSize;
            addSome(diff, tDeck.toFlatList());
        } else if (actualSize > targetSize) {

            Predicate<CardPrinted> exceptBasicLand = Predicates.not(Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, CardPrinted.FN_GET_RULES));

            for (int i = 0; i < 3 && actualSize > targetSize; i++) {
                Iterable<CardPrinted> matchingCards = Iterables.filter(tDeck.toFlatList(), exceptBasicLand);
                List<CardPrinted> toRemove = Aggregates.random(matchingCards,  actualSize - targetSize);
                tDeck.removeAllFlat(toRemove);

                for (CardPrinted c : toRemove) {
                    tmpDeck.append("Removed:").append(c.getName()).append("\n");
                }
                actualSize = tDeck.countAll();
            }
        }
    }

    protected void addCmcAdjusted(Iterable<CardPrinted> source, int cnt, List<FilterCMC> cmcLevels, int[] cmcAmounts) {
        final List<CardPrinted> curved = new ArrayList<CardPrinted>();

        for (int i = 0; i < cmcAmounts.length; i++) {
            Iterable<CardPrinted> matchingCards = Iterables.filter(source, Predicates.compose(cmcLevels.get(i), CardPrinted.FN_GET_RULES));
            curved.addAll(Aggregates.random(matchingCards, cmcAmounts[i]));
        }

        for (CardPrinted c : curved) {
            this.cardCounts.put(c.getName(), 0);
        }

        addSome(cnt, curved);
    }

    protected Iterable<CardPrinted> selectCardsOfMatchingColorForPlayer(PlayerType pt) {

        // start with all cards
        // remove cards that generated decks don't like
        Predicate<CardRules> canPlay = pt == PlayerType.HUMAN ? GenerateDeckUtil.HUMAN_CAN_PLAY : GenerateDeckUtil.AI_CAN_PLAY;
        Predicate<CardRules> hasColor = new GenerateDeckUtil.ContainsAllColorsFrom(colors);

        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS)) {
            hasColor = Predicates.or(hasColor, GenerateDeckUtil.COLORLESS_CARDS);
        }
        return Iterables.filter(CardDb.instance().getTraditionalCards(), Predicates.compose(Predicates.and(canPlay, hasColor), CardPrinted.FN_GET_RULES));
    }

    protected static Map<String, Integer> countLands(ItemPool<CardPrinted> outList) {
        // attempt to optimize basic land counts according
        // to color representation

        Map<String, Integer> res = new TreeMap<String, Integer>();
        // count each card color using mana costs
        // TODO: count hybrid mana differently?
        for (Entry<CardPrinted, Integer> cpe : outList) {

            int profile = cpe.getKey().getCard().getManaCost().getColorProfile();

            if ((profile & CardColor.WHITE) != 0) {
                increment(res, Constant.Color.BASIC_LANDS.get(0), cpe.getValue());
            } else if ((profile & CardColor.BLUE) != 0) {
                increment(res, Constant.Color.BASIC_LANDS.get(1), cpe.getValue());
            } else if ((profile & CardColor.BLACK) != 0) {
                increment(res, Constant.Color.BASIC_LANDS.get(2), cpe.getValue());
            } else if ((profile & CardColor.RED) != 0) {
                increment(res, Constant.Color.BASIC_LANDS.get(3), cpe.getValue());
            } else if ((profile & CardColor.GREEN) != 0) {
                increment(res, Constant.Color.BASIC_LANDS.get(4), cpe.getValue());
            }

        }
        return res;
    }

    protected static void increment(Map<String, Integer> map, String key, int delta)
    {
        final Integer boxed = map.get(key);
        map.put(key, boxed == null ? delta : boxed.intValue() + delta);
    }
}
