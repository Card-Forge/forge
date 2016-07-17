package forge.limited;

import java.util.ArrayList;
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

import forge.card.CardAiHints;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.DeckHints;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.MyRandom;

/**
 * Limited format deck.
 */
public class LimitedDeckBuilder extends DeckGeneratorBase {
    @Override
    protected final float getLandPercentage() {
        return 0.44f;
    }
    @Override
    protected final float getCreaturePercentage() {
        return 0.33f;
    }
    @Override
    protected final float getSpellPercentage() {
        return 0.23f;
    }

    private final int numSpellsNeeded = 22;
    private int landsNeeded = 18;

    private final DeckColors deckColors;
    private Predicate<CardRules> hasColor;
    private final List<PaperCard> availableList;
    private final List<PaperCard> aiPlayables;
    private final List<PaperCard> deckList = new ArrayList<PaperCard>();
    private final List<String> setsWithBasicLands = new ArrayList<String>();
    // Views for aiPlayable

    private Iterable<PaperCard> colorList;
    private Iterable<PaperCard> onColorCreatures;
    private Iterable<PaperCard> onColorNonCreatures;

    private static final boolean logToConsole = false;

    /**
     *
     * Constructor.
     *
     * @param dList
     *            Cards to build the deck from.
     * @param pClrs
     *            Chosen colors.
     */
    public LimitedDeckBuilder(final List<PaperCard> dList, final DeckColors pClrs) {
        super(FModel.getMagicDb().getCommonCards(), DeckFormat.Limited);
        this.availableList = dList;
        this.deckColors = pClrs;
        this.colors = pClrs.getChosenColors();

        // removeUnplayables();
        final Iterable<PaperCard> playables = Iterables.filter(availableList,
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES));
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
    public LimitedDeckBuilder(final List<PaperCard> list) {
        this(list, new DeckColors());
    }

    @Override
    public CardPool getDeck(final int size, final boolean forAi) {
        return buildDeck().getMain();
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     *
     * @return the new Deck.
     */
    public Deck buildDeck() {
        return buildDeck(null);
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     *
     * @param landSetCode
     *             the set to take basic lands from (pass 'null' for random).
     * @return the new Deck.
     */
    @SuppressWarnings("unused")
    public Deck buildDeck(final String landSetCode) {
        // 1. Prepare
        hasColor = Predicates.or(new MatchColorIdentity(colors), COLORLESS_CARDS);
        colorList = Iterables.filter(aiPlayables, Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
        onColorCreatures = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES));
        onColorNonCreatures = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, PaperCard.FN_GET_RULES));
        // Guava iterables do not copy the collection contents, instead they act
        // as filters and iterate over _source_ collection each time. So even if
        // aiPlayable has changed, there is no need to create a new iterable.

        // 2. Add any planeswalkers
        final Iterable<PaperCard> onColorWalkers = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_PLANESWALKER, PaperCard.FN_GET_RULES));
        final List<PaperCard> walkers = Lists.newArrayList(onColorWalkers);
        deckList.addAll(walkers);
        aiPlayables.removeAll(walkers);

        if (walkers.size() > 0 && logToConsole) {
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
        final Iterable<PaperCard> nonLands = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
        if (deckList.size() == numSpellsNeeded && getAverageCMC(deckList) < 3) {
            final List<Pair<Double, PaperCard>> list = rankCards(nonLands);
            if (!list.isEmpty()) {
                final PaperCard c = list.get(0).getValue();
                deckList.add(c);
                getAiPlayables().remove(c);
                landsNeeded--;
                if (logToConsole) {
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
        final int[] clrCnts = calculateLandNeeds();
        if (landsNeeded > 0) {
            addLands(clrCnts, landSetCode);
        }

        fixDeckSize(clrCnts, landSetCode);

        if (deckList.size() == 40) {
            final Deck result = new Deck(generateName());
            result.getMain().add(deckList);
            final CardPool cp = result.getOrCreate(DeckSection.Sideboard);
            cp.add(aiPlayables);
            cp.add(availableList);
            if (logToConsole) {
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
        for (final PaperCard c : deckList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PLAYABLE");
        for (final PaperCard c : availableList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getRules().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PICKED");
        for (final PaperCard c : getAiPlayables()) {
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
     * @param landSetCode
     *            the set to take basic lands from (pass 'null' for random).
     */
    private void fixDeckSize(final int[] clrCnts, final String landSetCode) {
        while (deckList.size() > 40) {
            if (logToConsole) {
                System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            }
            final PaperCard c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            getAiPlayables().add(c);
            if (logToConsole) {
                System.out.println(" - Removed " + c.getName() + " randomly.");
            }
        }

        while (deckList.size() < 40) {
            if (logToConsole) {
                System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            }
            if (getAiPlayables().size() > 1) {
                final PaperCard c = getAiPlayables().get(MyRandom.getRandom().nextInt(getAiPlayables().size() - 1));
                deckList.add(c);
                getAiPlayables().remove(c);
                if (logToConsole) {
                    System.out.println(" - Added " + c.getName() + " randomly.");
                }
            } else if (getAiPlayables().size() == 1) {
                final PaperCard c = getAiPlayables().get(0);
                deckList.add(c);
                getAiPlayables().remove(c);
                if (logToConsole) {
                    System.out.println(" - Added " + c.getName() + " randomly.");
                }
            } else {
                // if no playable cards remain fill up with basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i] > 0) {
                        final PaperCard cp = getBasicLand(i, landSetCode);
                        deckList.add(cp);
                        if (logToConsole) {
                            System.out.println(" - Added " + cp.getName() + " as last resort.");
                        }
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
        final Set<String> sets = new HashSet<String>();
        for (final PaperCard cp : aiPlayables) {
            final CardEdition ee = FModel.getMagicDb().getEditions().get(cp.getEdition());
            if( !sets.contains(cp.getEdition()) && CardEdition.Predicates.hasBasicLands.apply(ee)) {
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
     * @param landSetCode
     *             the set to take basic lands from (pass 'null' for random).
     */
    private void addLands(final int[] clrCnts, final String landSetCode) {
        // basic lands that are available in the deck
        final Iterable<PaperCard> basicLands = Iterables.filter(aiPlayables, Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));
        final Set<PaperCard> snowLands = new HashSet<PaperCard>();

        // total of all ClrCnts
        int totalColor = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i];
        }
        if (totalColor == 0) {
            throw new RuntimeException("Add Lands to empty deck list!");
        }

        // do not update landsNeeded until after the loop, because the
        // calculation involves landsNeeded
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i] > 0) {
                // calculate number of lands for each color
                final float p = (float) clrCnts[i] / (float) totalColor;
                int nLand = Math.round(landsNeeded * p); // desired truncation to int
                if (logToConsole) {
                    System.out.printf("Basics[%s]: %d/%d = %f%% = %d cards%n", MagicColor.Constant.BASIC_LANDS.get(i), clrCnts[i], totalColor, 100*p, nLand);
                }

                // if appropriate snow-covered lands are available, add them
                for (final PaperCard cp : basicLands) {
                    if (cp.getName().equals(MagicColor.Constant.SNOW_LANDS.get(i))) {
                        snowLands.add(cp);
                        nLand--;
                    }
                }

                for (int j = 0; j < nLand; j++) {
                    deckList.add(getBasicLand(i, landSetCode));
                }
            }
        }

        deckList.addAll(snowLands);
        aiPlayables.removeAll(snowLands);
    }

    /**
     * Get basic land.
     *
     * @param basicLand
     * @param landSetCode
     *             the set to take basic lands from (pass 'null' for random).
     * @return card
     */
    private PaperCard getBasicLand(final int basicLand, final String landSetCode) {
        String set;
        if (landSetCode == null) {
            if (setsWithBasicLands.size() > 1) {
                set = setsWithBasicLands.get(MyRandom.getRandom().nextInt(setsWithBasicLands.size() - 1));
            } else {
                set = setsWithBasicLands.get(0);
            }
        } else {
            set = landSetCode;
        }
        return FModel.getMagicDb().getCommonCards().getCard(MagicColor.Constant.BASIC_LANDS.get(basicLand), set);
    }

    /**
     * Attempt to optimize basic land counts according to color representation.
     * Only consider colors that are supposed to be in the deck. It's not worth
     * putting one land in for that random off-color card we had to stick in at
     * the end...
     *
     * @return CCnt
     */
    private int[] calculateLandNeeds() {
        final int[] clrCnts = { 0,0,0,0,0 };

        // count each card color using mana costs
        for (final PaperCard cp : deckList) {
            final ManaCost mc = cp.getRules().getManaCost();

            // count each mana symbol in the mana cost
            for (final ManaCostShard shard : mc) {
                for ( int i = 0 ; i < MagicColor.WUBRG.length; i++ ) {
                    final byte c = MagicColor.WUBRG[i];
                    if ( shard.canBePaidWithManaOfColor(c) && colors.hasAnyColor(c)) {
                        clrCnts[i]++;
                    }
                }
            }
        }
        return clrCnts;
    }

    /**
     * Add non-basic lands to the deck.
     */
    private void addNonBasicLands() {
        final List<String> inverseDuals = getInverseDualLandList();
        final Iterable<PaperCard> lands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, PaperCard.FN_GET_RULES));
        final List<Pair<Double, PaperCard>> ranked = rankCards(lands);
        for (final Pair<Double, PaperCard> bean : ranked) {
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
                    if (logToConsole) {
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
            final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
            List<Pair<Double, PaperCard>> ranked = rankCards(others);
            for (final Pair<Double, PaperCard> bean : ranked) {
                // Want a card that has just one "off" color.
                final ColorSet off = colors.getOffColors(bean.getValue().getRules().getColor());
                if (off.isMonoColor()) {
                    colors = ColorSet.fromMask(colors.getColor() | off.getColor());
                    break;
                }
            }

            hasColor = Predicates.or(new DeckGeneratorBase.MatchColorIdentity(colors),
                    DeckGeneratorBase.COLORLESS_CARDS);
            final Iterable<PaperCard> threeColorList = Iterables.filter(aiPlayables,
                    Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
            ranked = rankCards(threeColorList);
            for (final Pair<Double, PaperCard> bean : ranked) {
                if (nCards > 0) {
                    deckList.add(bean.getValue());
                    aiPlayables.remove(bean.getValue());
                    nCards--;
                    if (logToConsole) {
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
        final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
        final List<Pair<Double, PaperCard>> ranked = rankCards(others);
        for (final Pair<Double, PaperCard> bean : ranked) {
            if (nCards > 0) {
                deckList.add(bean.getValue());
                aiPlayables.remove(bean.getValue());
                nCards--;
                if (logToConsole) {
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
    private void addNonCreatures(final List<Pair<Double, PaperCard>> nonCreatures, int num) {
        for (final Pair<Double, PaperCard> bean : nonCreatures) {
            if (num > 0) {
                final PaperCard cardToAdd = bean.getValue();
                deckList.add(cardToAdd);
                num--;
                getAiPlayables().remove(cardToAdd);
                if (logToConsole) {
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
    private int addDeckHintsCards(final PaperCard cardToAdd, int num) {
        // cards with DeckHints will try to grab additional cards from the pool
        final DeckHints hints = cardToAdd.getRules().getAiHints().getDeckHints();
        if (hints != null && hints.getType() != DeckHints.Type.NONE) {
            final Iterable<PaperCard> onColor = Iterables.filter(aiPlayables, Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
            final List<PaperCard> comboCards = hints.filter(onColor);
            if (logToConsole) {
                System.out.println("Found " + comboCards.size() + " cards for " + cardToAdd.getName());
            }
            for (final Pair<Double, PaperCard> comboBean : rankCards(comboCards)) {
                if (num > 0) {
                    // This is not exactly right, because the
                    // rankedComboCards could include creatures and
                    // non-creatures.
                    // This code could add too many of one or the other.
                    final PaperCard combo = comboBean.getValue();
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
        for (final ListIterator<PaperCard> it = deckList.listIterator(); it.hasNext();) {
            final PaperCard card = it.next();
            final CardAiHints ai = card.getRules().getAiHints();
            if (ai.getRemRandomDecks()) {
                final List<PaperCard> comboCards = new ArrayList<PaperCard>();
                if (ai.getDeckNeeds() != null
                        && ai.getDeckNeeds().getType() != DeckHints.Type.NONE) {
                    final DeckHints needs = ai.getDeckNeeds();
                    comboCards.addAll(needs.filter(deckList));
                }
                if (ai.getDeckHints() != null
                        && ai.getDeckHints().getType() != DeckHints.Type.NONE) {
                    final DeckHints hints = ai.getDeckHints();
                    comboCards.addAll(hints.filter(deckList));
                }
                if (comboCards.isEmpty()) {
                    if (logToConsole) {
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
                    if (logToConsole) {
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
    private void addCreatures(final List<Pair<Double, PaperCard>> creatures, int num) {
        for (final Pair<Double, PaperCard> bean : creatures) {
            if (num > 0) {
                final PaperCard c = bean.getValue();
                deckList.add(c);
                num--;
                getAiPlayables().remove(c);
                if (logToConsole) {
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
    private void addManaCurveCreatures(final List<Pair<Double, PaperCard>> creatures, int num) {
        final Map<Integer, Integer> creatureCosts = new HashMap<Integer, Integer>();
        for (int i = 1; i < 7; i++) {
            creatureCosts.put(i, 0);
        }
        final Predicate<PaperCard> filter = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE,
                PaperCard.FN_GET_RULES);
        for (final IPaperCard creature : Iterables.filter(deckList, filter)) {
            int cmc = creature.getRules().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
        }

        for (final Pair<Double, PaperCard> bean : creatures) {
            final PaperCard c = bean.getValue();
            int cmc = c.getRules().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            final Integer currentAtCmc = creatureCosts.get(cmc);
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
                if (logToConsole) {
                    System.out.println("Creature[" + num + "]:" + c.getName() + " (" + c.getRules().getManaCost() + ")");
                }
                num = addDeckHintsCards(c, num);
            } else {
                if (logToConsole) {
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
    protected List<Pair<Double, PaperCard>> rankCards(final Iterable<PaperCard> cards) {
        final List<Pair<Double, PaperCard>> ranked = new ArrayList<Pair<Double, PaperCard>>();
        for (final PaperCard card : cards) {
            Double rkg;
            String customRankings = IBoosterDraft.CUSTOM_RANKINGS_FILE[0];

            if (customRankings != null) {
                rkg = DraftRankCache.getCustomRanking(customRankings, card.getName());
                if (rkg == null) {
                    // try the default rankings if custom rankings contain no entry
                    rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
                }
            } else {
                rkg = DraftRankCache.getRanking(card.getName(), card.getEdition());
            }

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
    private static double getAverageCMC(final List<PaperCard> cards) {
        double sum = 0.0;
        for (final IPaperCard cardPrinted : cards) {
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
    public void setColors(final ColorSet colors) {
        this.colors = colors;
    }

    /**
     * @return the aiPlayables
     */
    public List<PaperCard> getAiPlayables() {
        return aiPlayables;
    }

}
