package forge.game.limited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Constant.Preferences;
import forge.card.CardAiHints;
import forge.card.MagicColor;
import forge.card.ColorSet;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.DeckHints;
import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaCost;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.generate.GenerateDeckUtil;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.util.MyRandom;

/**
 * Limited format deck.
 * 
 */
public class LimitedDeck {

    private int numSpellsNeeded = 22;
    private int landsNeeded = 18;
    private ColorSet colors;
    private final DeckColors deckColors;
    private Predicate<CardRules> hasColor;
    private final List<CardPrinted> availableList;
    private final List<CardPrinted> aiPlayables;
    private List<CardPrinted> deckList = new ArrayList<CardPrinted>();
    private List<String> setsWithBasicLands = new ArrayList<String>();
    // Views for aiPlayable

    private Iterable<CardPrinted> colorList;
    private Iterable<CardPrinted> onColorCreatures;
    private Iterable<CardPrinted> onColorNonCreatures;

    private static ReadDraftRankings draftRankings = new ReadDraftRankings();

    /**
     * 
     * Constructor.
     * 
     * @param dList
     *            Cards to build the deck from.
     * @param pClrs
     *            Chosen colors.
     */
    public LimitedDeck(List<CardPrinted> dList, DeckColors pClrs) {
        this.availableList = dList;
        this.deckColors = pClrs;
        this.colors = pClrs.getChosenColors();

        // removeUnplayables();
        Iterable<CardPrinted> playables = Iterables.filter(availableList,
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, CardPrinted.FN_GET_RULES));
        this.aiPlayables = Lists.newArrayList(playables);
        this.availableList.removeAll(getAiPlayables());

        findBasicLandSets();

    }

    /**
     * Constructor.
     * 
     * @param list
     *            Cards to build the deck from.
     */
    public LimitedDeck(List<CardPrinted> list) {
        this(list, new DeckColors());
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     * 
     * @return the new Deck.
     */
    public Deck buildDeck() {
        // 1. Prepare
        hasColor = Predicates.or(new GenerateDeckUtil.CanBePaidWithColors(colors), GenerateDeckUtil.COLORLESS_CARDS);
        colorList = Iterables.filter(aiPlayables, Predicates.compose(hasColor, CardPrinted.FN_GET_RULES));
        onColorCreatures = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, CardPrinted.FN_GET_RULES));
        onColorNonCreatures = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, CardPrinted.FN_GET_RULES));
        // Guava iterables do not copy the collection contents, instead they act
        // as filters and iterate over _source_ collection each time. So even if
        // aiPlayable has changed, there is no need to create a new iterable.

        // 2. Add any planeswalkers
        Iterable<CardPrinted> onColorWalkers = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_PLANESWALKER, CardPrinted.FN_GET_RULES));
        List<CardPrinted> walkers = Lists.newArrayList(onColorWalkers);
        deckList.addAll(walkers);
        aiPlayables.removeAll(walkers);

        if (walkers.size() > 0) {
            System.out.println("Planeswalker: " + walkers.get(0).getName());
        }

        // 3. Add creatures, trying to follow mana curve
        addManaCurveCreatures(rankCards(onColorCreatures), 15);

        // 4.Try to fill up to 22 with on-color non-creature cards
        addNonCreatures(rankCards(onColorNonCreatures), numSpellsNeeded - deckList.size());

        // 5.If we couldn't get up to 22, try to fill up to 22 with on-color
        // creature cards
        addCreatures(rankCards(onColorCreatures), numSpellsNeeded - deckList.size());

        // 6. If there are still on-color cards, and the average cmc is low, add
        // a 23rd card.
        Iterable<CardPrinted> nonLands = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, CardPrinted.FN_GET_RULES));
        if (deckList.size() == numSpellsNeeded && getAverageCMC(deckList) < 3) {
            List<Pair<Double, CardPrinted>> list = rankCards(nonLands);
            if (!list.isEmpty()) {
                CardPrinted c = list.get(0).getValue();
                deckList.add(c);
                getAiPlayables().remove(c);
                landsNeeded--;
                if (Preferences.DEV_MODE) {
                    System.out.println("Low CMC: " + c.getName());
                }
            }
        }

        // 7. If not enough cards yet, try to add a third color,
        // to try and avoid adding purely random cards.
        addThirdColorCards(numSpellsNeeded - deckList.size());

        // 8. Check for DeckNeeds cards.
        checkRemRandomDeckCards();

        // 9. If there are still less than 22 non-land cards add off-color
        // cards. This should be avoided.
        addRandomCards(numSpellsNeeded - deckList.size());

        // 10. Add non-basic lands that were drafted.
        addNonBasicLands();

        // 11. Fill up with basic lands.
        final CCnt[] clrCnts = calculateLandNeeds();
        if (landsNeeded > 0) {
            addLands(clrCnts);
        }

        fixDeckSize(clrCnts);

        if (deckList.size() == 40) {
            Deck result = new Deck(generateName());
            result.getMain().add(deckList);
            CardPool cp = result.getOrCreate(DeckSection.Sideboard);
            cp.add(aiPlayables);
            cp.add(availableList);
            if (Preferences.DEV_MODE) {
                debugFinalDeck();
            }
            return result;
        } else {
            throw new RuntimeException("BoosterDraftAI : buildDeck() error, decksize not 40");
        }
    }

    /**
     * Generate a descriptive name.
     * 
     * @return name
     */
    private String generateName() {
        return deckColors.toString();
    }

    /**
     * Print out listing of all cards for debugging.
     */
    private void debugFinalDeck() {
        int i = 0;
        System.out.println("DECK");
        for (CardPrinted c : deckList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PLAYABLE");
        for (CardPrinted c : availableList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PICKED");
        for (CardPrinted c : getAiPlayables()) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
    }

    /**
     * If the deck does not have 40 cards, fix it. This method should not be
     * called if the stuff above it is working correctly.
     * 
     * @param clrCnts
     *            color counts needed
     */
    private void fixDeckSize(final CCnt[] clrCnts) {
        while (deckList.size() > 40) {
            System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            final CardPrinted c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            getAiPlayables().add(c);
            System.out.println(" - Removed " + c.getName() + " randomly.");
        }

        while (deckList.size() < 40) {
            System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            if (getAiPlayables().size() > 1) {
                final CardPrinted c = getAiPlayables().get(MyRandom.getRandom().nextInt(getAiPlayables().size() - 1));
                deckList.add(c);
                getAiPlayables().remove(c);
                System.out.println(" - Added " + c.getName() + " randomly.");
            } else if (getAiPlayables().size() == 1) {
                final CardPrinted c = getAiPlayables().get(0);
                deckList.add(c);
                getAiPlayables().remove(c);
                System.out.println(" - Added " + c.getName() + " randomly.");
            } else {
                // if no playable cards remain fill up with basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i].getCount() > 0) {
                        final CardPrinted cp = getBasicLand(clrCnts[i].getColor());
                        deckList.add(cp);
                        System.out.println(" - Added " + cp.getName() + " as last resort.");
                        break;
                    }
                }
            }
        }
    }

    /**
     * Find the sets that have basic lands for the available cards.
     */
    private void findBasicLandSets() {
        Set<String> sets = new HashSet<String>();
        for (CardPrinted cp : aiPlayables) {
            if (null != CardDb.instance().tryGetCard("Plains", cp.getEdition())) {
                sets.add(cp.getEdition());
            }
        }
        setsWithBasicLands.addAll(sets);
        if (setsWithBasicLands.isEmpty()) {
            setsWithBasicLands.add("M13");
        }
    }

    /**
     * Add lands to fulfill the given color counts.
     * 
     * @param clrCnts
     */
    private void addLands(final CCnt[] clrCnts) {

        // total of all ClrCnts
        int totalColor = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i].getCount();
        }
        if (totalColor == 0) {
            throw new RuntimeException("Add Lands to empty deck list!");
        }

        // do not update landsNeeded until after the loop, because the
        // calculation involves landsNeeded
        int landsAdded = 0;
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i].getCount() > 0) {
                // calculate number of lands for each color
                final float p = (float) clrCnts[i].getCount() / (float) totalColor;
                final int nLand = (int) (landsNeeded * p); // desired truncation
                                                           // to int
                if (Preferences.DEV_MODE) {
                    System.out.println("Basics[" + clrCnts[i].getColor() + "]: " + clrCnts[i].getCount() + "/"
                            + totalColor + " = " + p + " = " + nLand);
                }

                for (int j = 0; j < nLand; j++) {
                    deckList.add(getBasicLand(clrCnts[i].getColor()));
                    landsAdded++;
                }
            }
        }

        // Add extra lands to get up to the right number.
        // Start with the smallest CCnt to "even out" a little.
        landsNeeded -= landsAdded;
        Arrays.sort(clrCnts);
        int n = 0;
        while (landsNeeded > 0) {
            if (clrCnts[n].getCount() > 0) {
                final CardPrinted cp = getBasicLand(clrCnts[n].getColor());
                deckList.add(cp);
                landsNeeded--;

                if (Preferences.DEV_MODE) {
                    System.out.println("AddBasics: " + cp.getName());
                }
            }
            if (++n > 4) {
                n = 0;
            }
        }
    }

    /**
     * Get basic land.
     * 
     * @param basicLand
     * @return card
     */
    private CardPrinted getBasicLand(String basicLand) {
        String set;
        if (setsWithBasicLands.size() > 1) {
            set = setsWithBasicLands.get(MyRandom.getRandom().nextInt(setsWithBasicLands.size() - 1));
        } else {
            set = setsWithBasicLands.get(0);
        }
        return CardDb.instance().getCard(basicLand, set);
    }

    /**
     * Attempt to optimize basic land counts according to color representation.
     * Only consider colors that are supposed to be in the deck. It's not worth
     * putting one land in for that random off-color card we had to stick in at
     * the end...
     * 
     * @return CCnt
     */
    private CCnt[] calculateLandNeeds() {
        final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                new CCnt("Mountain", 0), new CCnt("Forest", 0) };

        // count each card color using mana costs
        for (int i = 0; i < deckList.size(); i++) {
            final ManaCost mc = deckList.get(i).getRules().getManaCost();

            // count each mana symbol in the mana cost
            for (ManaCostShard shard : mc.getShards()) {
                byte mask = shard.getColorMask();

                if ((mask & MagicColor.WHITE) > 0 && colors.hasWhite()) {
                    clrCnts[0].setCount(clrCnts[0].getCount() + 1);
                }
                if ((mask & MagicColor.BLUE) > 0 && colors.hasBlue()) {
                    clrCnts[1].setCount(clrCnts[1].getCount() + 1);
                }
                if ((mask & MagicColor.BLACK) > 0 && colors.hasBlack()) {
                    clrCnts[2].setCount(clrCnts[2].getCount() + 1);
                }
                if ((mask & MagicColor.RED) > 0 && colors.hasRed()) {
                    clrCnts[3].setCount(clrCnts[3].getCount() + 1);
                }
                if ((mask & MagicColor.GREEN) > 0 && colors.hasGreen()) {
                    clrCnts[4].setCount(clrCnts[4].getCount() + 1);
                }
            }
        }
        return clrCnts;
    }

    /**
     * Add non-basic lands to the deck.
     */
    private void addNonBasicLands() {
        List<String> inverseDuals = GenerateDeckUtil.getInverseDualLandList(colors);
        Iterable<CardPrinted> lands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, CardPrinted.FN_GET_RULES));
        List<Pair<Double, CardPrinted>> ranked = rankCards(lands);
        for (Pair<Double, CardPrinted> bean : ranked) {
            if (landsNeeded > 0) {
                // Throw out any dual-lands for the wrong colors. Assume
                // everything else is either
                // (a) dual-land of the correct two colors, or
                // (b) a land that generates colorless mana and has some other
                // beneficial effect.
                if (!inverseDuals.contains(bean.getValue().getName())) {
                    deckList.add(bean.getValue());
                    aiPlayables.remove(bean.getValue());
                    landsNeeded--;
                    if (Preferences.DEV_MODE) {
                        System.out.println("NonBasicLand[" + landsNeeded + "]:" + bean.getValue().getName());
                    }
                }
            }
        }
    }

    /**
     * Add a third color to the deck.
     * 
     * @param nCards
     */
    private void addThirdColorCards(int nCards) {
        if (nCards > 0) {
            Iterable<CardPrinted> others = Iterables.filter(aiPlayables,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, CardPrinted.FN_GET_RULES));
            List<Pair<Double, CardPrinted>> ranked = rankCards(others);
            for (Pair<Double, CardPrinted> bean : ranked) {
                // Want a card that has just one "off" color.
                ColorSet off = colors.getOffColors(bean.getValue().getRules().getColor());
                if (off.isWhite() || off.isBlue() || off.isBlack() || off.isRed() || off.isGreen()) {
                    colors = ColorSet.fromMask(colors.getColor() | off.getColor());
                    break;
                }
            }

            hasColor = Predicates.or(new GenerateDeckUtil.CanBePaidWithColors(colors),
                    GenerateDeckUtil.COLORLESS_CARDS);
            Iterable<CardPrinted> threeColorList = Iterables.filter(aiPlayables,
                    Predicates.compose(hasColor, CardPrinted.FN_GET_RULES));
            ranked = rankCards(threeColorList);
            for (Pair<Double, CardPrinted> bean : ranked) {
                if (nCards > 0) {
                    deckList.add(bean.getValue());
                    aiPlayables.remove(bean.getValue());
                    nCards--;
                    if (Preferences.DEV_MODE) {
                        System.out.println("Third Color[" + nCards + "]:" + bean.getValue().getName() + "("
                                + bean.getValue().getRules().getManaCost() + ")");
                    }
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Add random cards to the deck.
     * 
     * @param nCards
     */
    private void addRandomCards(int nCards) {
        Iterable<CardPrinted> others = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, CardPrinted.FN_GET_RULES));
        List<Pair<Double, CardPrinted>> ranked = rankCards(others);
        for (Pair<Double, CardPrinted> bean : ranked) {
            if (nCards > 0) {
                deckList.add(bean.getValue());
                aiPlayables.remove(bean.getValue());
                nCards--;
                if (Preferences.DEV_MODE) {
                    System.out.println("Random[" + nCards + "]:" + bean.getValue().getName() + "("
                            + bean.getValue().getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
    }

    /**
     * Add highest ranked non-creatures to the deck.
     * 
     * @param nonCreatures
     *            cards to choose from
     * @param num
     */
    private void addNonCreatures(List<Pair<Double, CardPrinted>> nonCreatures, int num) {
        for (Pair<Double, CardPrinted> bean : nonCreatures) {
            if (num > 0) {
                CardPrinted cardToAdd = bean.getValue();
                deckList.add(cardToAdd);
                num--;
                getAiPlayables().remove(cardToAdd);
                if (Preferences.DEV_MODE) {
                    System.out.println("Others[" + num + "]:" + cardToAdd.getName() + " ("
                            + cardToAdd.getRules().getManaCost() + ")");
                }
                num = addDeckHintsCards(cardToAdd, num);
            } else {
                break;
            }
        }
    }

    /**
     * Add cards that work well with the given card.
     * 
     * @param cardToAdd
     *            card being checked
     * @param num
     *            number of cards
     * @return number left after adding
     */
    private int addDeckHintsCards(CardPrinted cardToAdd, int num) {
        // cards with DeckHints will try to grab additional cards from the pool
        DeckHints hints = cardToAdd.getRules().getAiHints().getDeckHints();
        if (hints != null && hints.getType() != DeckHints.Type.NONE) {
            Iterable<CardPrinted> onColor = Iterables.filter(aiPlayables, Predicates.compose(hasColor, CardPrinted.FN_GET_RULES));
            List<CardPrinted> comboCards = hints.filter(onColor);
            if (Preferences.DEV_MODE) {
                System.out.println("Found " + comboCards.size() + " cards for " + cardToAdd.getName());
            }
            for (Pair<Double, CardPrinted> comboBean : rankCards(comboCards)) {
                if (num > 0) {
                    // This is not exactly right, because the
                    // rankedComboCards could include creatures and
                    // non-creatures.
                    // This code could add too many of one or the other.
                    CardPrinted combo = comboBean.getValue();
                    deckList.add(combo);
                    num--;
                    getAiPlayables().remove(combo);
                } else {
                    break;
                }
            }
        }

        return num;
    }

    /**
     * Check all cards that should be removed from Random Decks. If they have
     * DeckNeeds or DeckHints, we can check to make sure they have the requisite
     * complementary cards present. Throw it out if it has no DeckNeeds or
     * DeckHints (because we have no idea what else should be in here) or if the
     * DeckNeeds or DeckHints are not met. Replace the removed cards with new
     * cards.
     */
    private void checkRemRandomDeckCards() {
        int numCreatures = 0;
        int numOthers = 0;
        for (ListIterator<CardPrinted> it = deckList.listIterator(); it.hasNext();) {
            CardPrinted card = it.next();
            CardAiHints ai = card.getRules().getAiHints();
            if (ai.getRemRandomDecks()) {
                List<CardPrinted> comboCards = new ArrayList<CardPrinted>();
                if (ai.getDeckNeeds() != null
                        && ai.getDeckNeeds().getType() != DeckHints.Type.NONE) {
                    DeckHints needs = ai.getDeckNeeds();
                    comboCards.addAll(needs.filter(deckList));
                }
                if (ai.getDeckHints() != null
                        && ai.getDeckHints().getType() != DeckHints.Type.NONE) {
                    DeckHints hints = ai.getDeckHints();
                    comboCards.addAll(hints.filter(deckList));
                }
                if (comboCards.isEmpty()) {
                    if (Preferences.DEV_MODE) {
                        System.out.println("No combo cards found for " + card.getName() + ", removing it.");
                    }
                    it.remove();
                    availableList.add(card);
                    if (card.getRules().getType().isCreature()) {
                        numCreatures++;
                    } else {
                        numOthers++;
                    }
                } else {
                    if (Preferences.DEV_MODE) {
                        System.out.println("Found " + comboCards.size() + " cards for " + card.getName());
                    }
                }
            }
        }
        if (numCreatures > 0) {
            addCreatures(rankCards(onColorCreatures), numCreatures);
        }
        if (numOthers > 0) {
            addNonCreatures(rankCards(onColorNonCreatures), numOthers);
        }
        // If we added some replacement cards, and we still have cards available
        // in aiPlayables, call this function again in case the replacement
        // cards are also RemRandomDeck cards.
        if ((numCreatures > 0 || numOthers > 0) && aiPlayables.size() > 0) {
            checkRemRandomDeckCards();
        }
    }

    /**
     * Add creatures to the deck.
     * 
     * @param creatures
     *            cards to choose from
     * @param num
     */
    private void addCreatures(List<Pair<Double, CardPrinted>> creatures, int num) {
        for (Pair<Double, CardPrinted> bean : creatures) {
            if (num > 0) {
                CardPrinted c = bean.getValue();
                deckList.add(c);
                num--;
                getAiPlayables().remove(c);
                if (Preferences.DEV_MODE) {
                    System.out.println("Creature[" + num + "]:" + c.getName() + " (" + c.getRules().getManaCost() + ")");
                }
                num = addDeckHintsCards(c, num);
            } else {
                break;
            }
        }
    }

    /**
     * Add creatures to the deck, trying to follow some mana curve. Trying to
     * have generous limits at each cost, but perhaps still too strict. But
     * we're trying to prevent the AI from adding everything at a single cost.
     * 
     * @param creatures
     *            cards to choose from
     * @param num
     */
    private void addManaCurveCreatures(List<Pair<Double, CardPrinted>> creatures, int num) {
        Map<Integer, Integer> creatureCosts = new HashMap<Integer, Integer>();
        for (int i = 1; i < 7; i++) {
            creatureCosts.put(i, 0);
        }
        Predicate<CardPrinted> filter = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE,
                CardPrinted.FN_GET_RULES);
        for (IPaperCard creature : Iterables.filter(deckList, filter)) {
            int cmc = creature.getRules().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
        }

        for (Pair<Double, CardPrinted> bean : creatures) {
            CardPrinted c = bean.getValue();
            int cmc = c.getRules().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            Integer currentAtCmc = creatureCosts.get(cmc);
            boolean willAddCreature = false;
            if (cmc <= 1 && currentAtCmc < 2) {
                willAddCreature = true;
            } else if (cmc == 2 && currentAtCmc < 4) {
                willAddCreature = true;
            } else if (cmc == 3 && currentAtCmc < 6) {
                willAddCreature = true;
            } else if (cmc == 4 && currentAtCmc < 7) {
                willAddCreature = true;
            } else if (cmc == 5 && currentAtCmc < 3) {
                willAddCreature = true;
            } else if (cmc >= 6 && currentAtCmc < 3) {
                willAddCreature = true;
            }

            if (willAddCreature) {
                deckList.add(c);
                num--;
                getAiPlayables().remove(c);
                creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
                if (Preferences.DEV_MODE) {
                    System.out.println("Creature[" + num + "]:" + c.getName() + " (" + c.getRules().getManaCost() + ")");
                }
                num = addDeckHintsCards(c, num);
            } else {
                if (Preferences.DEV_MODE) {
                    System.out.println(c.getName() + " not added because CMC " + c.getRules().getManaCost().getCMC()
                            + " has " + currentAtCmc + " already.");
                }
            }
            if (num <= 0) {
                break;
            }

        }
    }

    /**
     * Rank cards.
     * 
     * @param cards
     *            CardPrinteds to rank
     * @return List of beans with card rankings
     */
    protected List<Pair<Double, CardPrinted>> rankCards(Iterable<CardPrinted> cards) {
        List<Pair<Double, CardPrinted>> ranked = new ArrayList<Pair<Double, CardPrinted>>();
        for (CardPrinted card : cards) {
            Double rkg = draftRankings.getRanking(card.getName(), card.getEdition());
            if (rkg != null) {
                ranked.add(Pair.of(rkg, card));
            } else {
                ranked.add(Pair.of(0.0, card));
            }
        }
        Collections.sort(ranked, new CardRankingComparator());
        return ranked;
    }

    /**
     * Calculate average CMC.
     * 
     * @param cards
     * @return the average
     */
    private double getAverageCMC(List<CardPrinted> cards) {
        double sum = 0.0;
        for (IPaperCard cardPrinted : cards) {
            sum += cardPrinted.getRules().getManaCost().getCMC();
        }
        return sum / cards.size();
    }

    /**
     * @return the colors
     */
    public ColorSet getColors() {
        return colors;
    }

    /**
     * @param colors
     *            the colors to set
     */
    public void setColors(ColorSet colors) {
        this.colors = colors;
    }

    /**
     * @return the aiPlayables
     */
    public List<CardPrinted> getAiPlayables() {
        return aiPlayables;
    }

}
