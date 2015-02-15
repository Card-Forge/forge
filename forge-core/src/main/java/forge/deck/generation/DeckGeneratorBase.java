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
package forge.deck.generation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.*;
import forge.card.mana.ManaCost;
import forge.deck.CardPool;
import forge.deck.DeckFormat;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ItemPool;
import forge.util.MyRandom;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * Generate2ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id: Generate2ColorDeck.java 14959 2012-03-28 14:03:43Z Chris H. $
 */
public abstract class DeckGeneratorBase {
    protected final Random r = MyRandom.getRandom();
    protected final Map<String, Integer> cardCounts = new HashMap<String, Integer>();
    protected int maxDuplicates = 4;
    protected boolean useArtifacts = true;

    protected ColorSet colors;
    protected final CardPool tDeck = new CardPool();
    protected final IDeckGenPool pool;
    protected final DeckFormat format;

    // 2-colored deck generator has its own constants. The rest works fine with these ones
    protected float getLandsPercentage() { return 0.44f; }
    protected float getCreatPercentage() { return 0.34f; }
    protected float getSpellPercentage() { return 0.22f; }

    StringBuilder tmpDeck = new StringBuilder();

    public DeckGeneratorBase(IDeckGenPool pool0, DeckFormat format0) {
        pool = format0.getCardPool(pool0);
        format = format0;
    }

    public void setSingleton(boolean singleton){
        maxDuplicates = singleton ? 1 : 4;
    }
    public void setUseArtifacts(boolean value) {
        useArtifacts = value;
    }

    protected void addCreaturesAndSpells(int size, List<ImmutablePair<FilterCMC, Integer>> cmcLevels, boolean forAi) {
        tmpDeck.append("Building deck of ").append(size).append("cards\n");
        
        final Iterable<PaperCard> cards = selectCardsOfMatchingColorForPlayer(forAi);
        // build subsets based on type

        final Iterable<PaperCard> creatures = Iterables.filter(cards, Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES));
        final int creatCnt = (int) Math.ceil(getCreatPercentage() * size);
        tmpDeck.append("Creatures to add:").append(creatCnt).append("\n");
        addCmcAdjusted(creatures, creatCnt, cmcLevels);

        Predicate<PaperCard> preSpells = Predicates.compose(CardRulesPredicates.Presets.IS_NONCREATURE_SPELL_FOR_GENERATOR, PaperCard.FN_GET_RULES);
        final Iterable<PaperCard> spells = Iterables.filter(cards, preSpells);
        final int spellCnt = (int) Math.ceil(getSpellPercentage() * size);
        tmpDeck.append("Spells to add:").append(spellCnt).append("\n");
        addCmcAdjusted(spells, spellCnt, cmcLevels);
        
        tmpDeck.append(String.format("Current deck size: %d... should be %f%n", tDeck.countAll(), size * (getCreatPercentage() + getSpellPercentage())));
    }

    public CardPool getDeck(final int size, final boolean forAi) {
        return null; // all but theme deck do override this method
    }

    protected boolean addSome(int cnt, List<PaperCard> source) {
        int srcLen = source.size();
        if (srcLen == 0) { return false; }

        for (int i = 0; i < cnt; i++) {
            PaperCard cp;
            int lc = 0;
            do {
                cp = source.get(r.nextInt(srcLen));
                lc++;
            } while (cardCounts.get(cp.getName()) > maxDuplicates - 1 && lc <= 100);

            if (lc > 100) {
                return false;
            }

            tDeck.add(pool.getCard(cp.getName(), cp.getEdition()));

            final int n = cardCounts.get(cp.getName());
            cardCounts.put(cp.getName(), n + 1);
            if (n + 1 == maxDuplicates) {
                if (source.remove(cp)) {
                    srcLen--;
                    if (srcLen == 0) { return false; }
                }
            }
            tmpDeck.append(String.format("(%d) %s [%s]%n", cp.getRules().getManaCost().getCMC(), cp.getName(), cp.getRules().getManaCost()));
        }
        return true;
    }

    protected int addSomeStr(int cnt, List<String> source) {
        int res = 0;
        for (int i = 0; i < cnt; i++) {
            String s;
            int lc = 0;
            do {
                s = source.get(r.nextInt(source.size()));
                lc++;
            } while ((cardCounts.get(s) >= maxDuplicates) && (lc <= 50));
            // not an error if looped too much - could play singleton mode, with 6 slots for 3 non-basic lands.

            if (lc > 50) {
            	break;
            }
            
            tDeck.add(pool.getCard(s));

            final int n = cardCounts.get(s);
            cardCounts.put(s, n + 1);
            tmpDeck.append(s + "\n");
            res++;
        }
        return res;
    }
    
    protected void addBasicLand(int cnt)
    {
    	addBasicLand(cnt, null);
    }

    protected void addBasicLand(int cnt, String edition) {
        tmpDeck.append(cnt).append(" basic lands remain").append("\n");
        
        // attempt to optimize basic land counts according to colors of picked cards
        final Map<String, Integer> clrCnts = countLands(tDeck);
        // total of all ClrCnts
        float totalColor = 0;
        for (Entry<String, Integer> c : clrCnts.entrySet()) {
            totalColor += c.getValue();
            tmpDeck.append(c.getKey()).append(":").append(c.getValue()).append("\n");
        }

        tmpDeck.append("totalColor:").append(totalColor).append("\n");

        int landsLeft = cnt;
        for (Entry<String, Integer> c : clrCnts.entrySet()) {
            String basicLandName = c.getKey();

            // calculate number of lands for each color
            final int nLand = Math.min(landsLeft, Math.round(cnt * c.getValue() / totalColor));
            tmpDeck.append("nLand-").append(basicLandName).append(":").append(nLand).append("\n");

            // just to prevent a null exception by the deck size fixing code
            cardCounts.put(basicLandName, nLand);

            PaperCard cp;
            if (edition != null) {
            	cp = pool.getCard(basicLandName, edition);
            }
            else {
            	cp = pool.getCard(basicLandName);
            }

            String basicLandSet = cp.getEdition();

            for (int i = 0; i < nLand; i++) {
                tDeck.add(pool.getCard(cp.getName(), basicLandSet, -1), 1);
            }

            landsLeft -= nLand;
        }
    }

    protected void adjustDeckSize(int targetSize) {
        // fix under-sized or over-sized decks, due to integer arithmetic
        int actualSize = tDeck.countAll();
        if (actualSize < targetSize) {
            addSome(targetSize - actualSize, tDeck.toFlatList());
        }
        else if (actualSize > targetSize) {
            Predicate<PaperCard> exceptBasicLand = Predicates.not(Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));

            for (int i = 0; i < 3 && actualSize > targetSize; i++) {
                Iterable<PaperCard> matchingCards = Iterables.filter(tDeck.toFlatList(), exceptBasicLand);
                List<PaperCard> toRemove = Aggregates.random(matchingCards,  actualSize - targetSize);
                tDeck.removeAllFlat(toRemove);

                for (PaperCard c : toRemove) {
                    tmpDeck.append("Removed:").append(c.getName()).append("\n");
                }
                actualSize = tDeck.countAll();
            }
        }
    }

    protected void addCmcAdjusted(Iterable<PaperCard> source, int cnt, List<ImmutablePair<FilterCMC, Integer>> cmcLevels) {
        int totalWeight = 0;
        for (ImmutablePair<FilterCMC, Integer> pair : cmcLevels) {
            totalWeight += pair.getRight();
        }
        
        float variability = 0.6f; // if set to 1, you'll get minimum cards to choose from
        float desiredWeight = (float)cnt / ( maxDuplicates * variability ); 
        float desiredOverTotal = desiredWeight / totalWeight;
        float requestedOverTotal = (float)cnt / totalWeight;

        for (ImmutablePair<FilterCMC, Integer> pair : cmcLevels) {
            Iterable<PaperCard> matchingCards = Iterables.filter(source, Predicates.compose(pair.getLeft(), PaperCard.FN_GET_RULES));
            int cmcCountForPool = (int) Math.ceil(pair.getRight().intValue() * desiredOverTotal);
            
            int addOfThisCmc = Math.round(pair.getRight().intValue() * requestedOverTotal);
            tmpDeck.append(String.format("Adding %d cards for cmc range from a pool with %d cards:%n", addOfThisCmc, cmcCountForPool));

            final List<PaperCard> curved = Aggregates.random(matchingCards, cmcCountForPool);
            final List<PaperCard> curvedRandomized = Lists.newArrayList();
            for (PaperCard c : curved) {
                cardCounts.put(c.getName(), 0);
                curvedRandomized.add(pool.getCard(c.getName()));
            }

            addSome(addOfThisCmc, curvedRandomized);
        }
    }

    protected Iterable<PaperCard> selectCardsOfMatchingColorForPlayer(boolean forAi) {

        // start with all cards
        // remove cards that generated decks don't like
        Predicate<CardRules> canPlay = forAi ? AI_CAN_PLAY : HUMAN_CAN_PLAY;
        Predicate<CardRules> hasColor = new MatchColorIdentity(colors);

        if (useArtifacts) {
            hasColor = Predicates.or(hasColor, COLORLESS_CARDS);
        }
        return Iterables.filter(pool.getAllCards(), Predicates.compose(Predicates.and(canPlay, hasColor), PaperCard.FN_GET_RULES));
    }

    protected static Map<String, Integer> countLands(ItemPool<PaperCard> outList) {
        // attempt to optimize basic land counts according
        // to color representation

        Map<String, Integer> res = new TreeMap<String, Integer>();
        // count each card color using mana costs
        // TODO: count hybrid mana differently?
        for (Entry<PaperCard, Integer> cpe : outList) {

            int profile = cpe.getKey().getRules().getManaCost().getColorProfile();

            if ((profile & MagicColor.WHITE) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(0), cpe.getValue());
            } else if ((profile & MagicColor.BLUE) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(1), cpe.getValue());
            } else if ((profile & MagicColor.BLACK) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(2), cpe.getValue());
            } else if ((profile & MagicColor.RED) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(3), cpe.getValue());
            } else if ((profile & MagicColor.GREEN) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(4), cpe.getValue());
            }

        }
        return res;
    }

    protected static void increment(Map<String, Integer> map, String key, int delta)
    {
        final Integer boxed = map.get(key);
        map.put(key, boxed == null ? delta : boxed.intValue() + delta);
    }
    
    public static final Predicate<CardRules> AI_CAN_PLAY = new Predicate<CardRules>() {
        @Override
        public boolean apply(CardRules c) {
            return !c.getAiHints().getRemAIDecks() && !c.getAiHints().getRemRandomDecks();
        }
    };

    public static final Predicate<CardRules> HUMAN_CAN_PLAY = new Predicate<CardRules>() {
        @Override
        public boolean apply(CardRules c) {
            return !c.getAiHints().getRemRandomDecks();
        }
    };

    public static final Predicate<CardRules> COLORLESS_CARDS = new Predicate<CardRules>() {
        @Override
        public boolean apply(CardRules c) {
            ManaCost mc = c.getManaCost();
            return c.getColorIdentity().isColorless() && !mc.isNoCost();
        }
    };

    public static class MatchColorIdentity implements Predicate<CardRules> {
        private final ColorSet allowedColor;

        public MatchColorIdentity(ColorSet color) {
            allowedColor = color;
        }

        @Override
        public boolean apply(CardRules subject) {
            ManaCost mc = subject.getManaCost();
            return !mc.isPureGeneric() && allowedColor.containsAllColorsFrom(subject.getColorIdentity().getColor());
            //return  mc.canBePaidWithAvaliable(allowedColor);
            // return allowedColor.containsAllColorsFrom(mc.getColorProfile());
        }
    }

    public static class FilterCMC implements Predicate<CardRules> {
        private final int min;
        private final int max;

        public FilterCMC(int from, int to) {
            min = from;
            max = to;
        }

        @Override
        public boolean apply(CardRules c) {
            ManaCost mc = c.getManaCost();
            int cmc = mc.getCMC();
            return cmc >= min && cmc <= max && !mc.isNoCost();
        }
    }

    private static Map<Integer, String[]> dualLands = new HashMap<Integer, String[]>();
    static {
        dualLands.put(MagicColor.WHITE | MagicColor.BLUE, new String[] { "Tundra", "Hallowed Fountain", "Flooded Strand" });
        dualLands.put(MagicColor.BLACK | MagicColor.BLUE, new String[] { "Underground Sea", "Watery Grave", "Polluted Delta" });
        dualLands.put(MagicColor.BLACK | MagicColor.RED, new String[] { "Badlands", "Blood Crypt", "Bloodstained Mire" });
        dualLands.put(MagicColor.GREEN | MagicColor.RED, new String[] { "Taiga", "Stomping Ground", "Wooded Foothills" });
        dualLands.put(MagicColor.GREEN | MagicColor.WHITE, new String[] { "Savannah", "Temple Garden", "Windswept Heath" });

        dualLands.put(MagicColor.WHITE | MagicColor.BLACK, new String[] { "Scrubland", "Godless Shrine", "Marsh Flats" });
        dualLands.put(MagicColor.BLUE  | MagicColor.RED, new String[] { "Volcanic Island", "Steam Vents", "Scalding Tarn" });
        dualLands.put(MagicColor.BLACK | MagicColor.GREEN, new String[] { "Bayou", "Overgrown Tomb", "Verdant Catacombs" });
        dualLands.put(MagicColor.WHITE | MagicColor.RED, new String[] { "Plateau", "Sacred Foundry", "Arid Mesa" });
        dualLands.put(MagicColor.GREEN | MagicColor.BLUE, new String[] { "Tropical Island", "Breeding Pool", "Misty Rainforest" });
    }

    /**
     * Get list of dual lands for this color combo.
     * 
     * @param color
     *            the color
     * @return dual land names
     */
    protected List<String> getDualLandList() {

        final List<String> dLands = new ArrayList<String>();

        if (colors.countColors() > 3) {
            dLands.add("Rupture Spire");
            dLands.add("Undiscovered Paradise");
        }

        if (colors.countColors() > 2) {
            dLands.add("Evolving Wilds");
            dLands.add("Terramorphic Expanse");
        }
        for (Entry<Integer, String[]> dual : dualLands.entrySet()) {
            if (colors.hasAllColors(dual.getKey())) {
                for (String s : dual.getValue()) {
                    dLands.add(s);
                }
            }
        }

        return dLands;
    }

    /**
     * Get all dual lands that do not match this color combo.
     * 
     * @param color
     *            the color
     * @return dual land names
     */
    protected List<String> getInverseDualLandList() {

        final List<String> dLands = new ArrayList<String>();

        for (Entry<Integer, String[]> dual : dualLands.entrySet()) {
            if (!colors.hasAllColors(dual.getKey())) {
                for (String s : dual.getValue()) {
                    dLands.add(s);
                }
            }
        }

        return dLands;
    }    
}
