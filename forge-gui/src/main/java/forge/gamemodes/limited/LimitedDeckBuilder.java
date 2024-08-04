package forge.gamemodes.limited;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardAiHints;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
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

    protected final DeckColors deckColors;
    protected Predicate<CardRules> hasColor;
    protected final List<PaperCard> availableList;
    protected final List<PaperCard> aiPlayables;
    protected final List<PaperCard> deckList = new ArrayList<>();
    protected final List<String> setsWithBasicLands = new ArrayList<>();
    protected List<PaperCard> rankedColorList;
    protected final List<PaperCard> draftedConspiracies;

    // Views for aiPlayable
    private Iterable<PaperCard> onColorCreatures;
    protected Iterable<PaperCard> onColorNonCreatures;

    protected static final boolean logToConsole = false;

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

        // remove Unplayables
        final Iterable<PaperCard> playables = Iterables.filter(availableList,
                Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_LIMITED_DECKS, PaperCard::getRules));
        this.aiPlayables = Lists.newArrayList(playables);
        this.availableList.removeAll(aiPlayables);

        // keep Conspiracies in a separate list
        final Iterable<PaperCard> conspiracies = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.coreType(true, "Conspiracy"), PaperCard::getRules));
        this.draftedConspiracies = Lists.newArrayList(conspiracies);
        this.aiPlayables.removeAll(draftedConspiracies);

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
        Iterable<PaperCard> colorList = Iterables.filter(aiPlayables,
                Predicates.compose(hasColor, PaperCard::getRules));
        rankedColorList = CardRanker.rankCardsInDeck(colorList);
        onColorCreatures = Iterables.filter(rankedColorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard::getRules));
        onColorNonCreatures = Iterables.filter(rankedColorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, PaperCard::getRules));
        // Guava iterables do not copy the collection contents, instead they act
        // as filters and iterate over _source_ collection each time. So even if
        // aiPlayable has changed, there is no need to create a new iterable.

        // 2. Add any planeswalkers
        final Iterable<PaperCard> onColorWalkers = Iterables.filter(colorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_PLANESWALKER, PaperCard::getRules));
        final List<PaperCard> walkers = Lists.newArrayList(onColorWalkers);
        deckList.addAll(walkers);
        aiPlayables.removeAll(walkers);
        rankedColorList.removeAll(walkers);

        if (walkers.size() > 0 && logToConsole) {
            System.out.println("Planeswalker: " + walkers.get(0).getName());
        }

        // 3. Add creatures, trying to follow mana curve
        addManaCurveCreatures(onColorCreatures, 16);

        // 4.Try to fill up to num needed with on-color non-creature cards
        addNonCreatures(onColorNonCreatures, numSpellsNeeded - deckList.size());

        // 5.If we couldn't get enough, try to fill up with on-color creature cards
        addCreatures(onColorCreatures, numSpellsNeeded - deckList.size());

        // 6. If there are still on-color cards, and the average cmc is low, add
        // an extra.
        if (deckList.size() == numSpellsNeeded && getAverageCMC(deckList) < 4) {
            final Iterable<PaperCard> nonLands = Iterables.filter(rankedColorList,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard::getRules));
            final PaperCard card = Iterables.getFirst(nonLands, null);
            if (card != null) {
                deckList.add(card);
                aiPlayables.remove(card);
                rankedColorList.remove(card);
                landsNeeded--;
                if (logToConsole) {
                    System.out.println("Low CMC: " + card.getName());
                }
            }
        }

        // 7. If not enough cards yet, try to add a third color,
        // to try and avoid adding purely random cards.
        addThirdColorCards(numSpellsNeeded - deckList.size());

        // 8. Add non-basic lands that were drafted.
        addNonBasicLands();

        // 9. Check for DeckNeeds cards.
        checkRemRandomDeckCards();

        // 10. Fill up with basic lands.
        final int[] clrCnts = calculateLandNeeds();
        if (landsNeeded > 0) {
            addLands(clrCnts, landSetCode);
        }

        fixDeckSize(clrCnts, landSetCode);

        if (deckList.size() == 40) {
            // add the main deck cards
            final Deck result = new Deck(generateName());
            result.getMain().add(deckList);
            // create the sideboard
            final CardPool cp = result.getOrCreate(DeckSection.Sideboard);
            cp.add(aiPlayables);
            cp.add(availableList);
            // add conspiracies, if any were drafted
            if (!draftedConspiracies.isEmpty()) {
                final CardPool cpConsp = result.getOrCreate(DeckSection.Conspiracy);
                cpConsp.add(draftedConspiracies);
            }
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
        for (final PaperCard c : aiPlayables) {
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
            aiPlayables.add(c);
            if (logToConsole) {
                System.out.println(" - Removed " + c.getName() + " randomly.");
            }
        }

        while (deckList.size() < 40) {
            if (logToConsole) {
                System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            }
            if (aiPlayables.size() > 1) {
                final PaperCard c = aiPlayables.get(MyRandom.getRandom().nextInt(aiPlayables.size() - 1));
                deckList.add(c);
                aiPlayables.remove(c);
                rankedColorList.remove(c);
                if (logToConsole) {
                    System.out.println(" - Added " + c.getName() + " randomly.");
                }
            } else if (aiPlayables.size() == 1) {
                final PaperCard c = aiPlayables.get(0);
                deckList.add(c);
                aiPlayables.remove(c);
                rankedColorList.remove(c);
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
        final Set<String> sets = new HashSet<>();
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
     *             counts of lands needed, by color
     * @param landSetCode
     *             the set to take basic lands from (pass 'null' for random).
     */
    private void addLands(final int[] clrCnts, final String landSetCode) {
        // basic lands that are available in the deck
        final Iterable<PaperCard> basicLands = Iterables.filter(aiPlayables, Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard::getRules));
        final Set<PaperCard> snowLands = new HashSet<>();

        // total of all ClrCnts
        int totalColor = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i];
        }
        if (totalColor == 0) {
        	// TODO: Technically this can happen in a colorless deck.
            throw new RuntimeException("Add Lands to empty deck list!");
        }
        
        // First, add 2 land of each required color. This prevents the AI from including
        // a splash card with no viable way to cast it.
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i] > 0) {
            	int nLand = 2;
            	System.out.printf("Basics[%s]: %d cards%n", MagicColor.Constant.BASIC_LANDS.get(i), nLand);
            	for (int j = 0; j < nLand; j++) {
                    deckList.add(getBasicLand(i, landSetCode));
                }
            }
        }
        
        for (int i = 0; i < 5; i++) {
        	int slotsRemaining = 40-deckList.size(); // How many to still distribute
            if (clrCnts[i] > 0) {
                // calculate proportion of mana symbols for each remaining color
                float p = (float) clrCnts[i] / (float) totalColor;

                // Rounding prefers WUBRG order, as a side effect.
                int nLand = Math.round(slotsRemaining * p); // Round up/down
                
                if (logToConsole) {
                    System.out.printf("Basics[%s]: %d/%d = %f%% = %d cards%n", MagicColor.Constant.BASIC_LANDS.get(i), clrCnts[i], totalColor, 100*p, nLand);
                }
                
                totalColor -= clrCnts[i];

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
        final Iterable<PaperCard> lands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, PaperCard::getRules));
        List<PaperCard> landsToAdd = new ArrayList<>();
        for (final PaperCard card : lands) {
            if (landsNeeded > 0) {
                // Use only lands that are within our colors
                // TODO: Use off-color fetchlands that get an on-color dual land
                if (card.getRules().getDeckbuildingColors().hasNoColorsExcept(colors)) {
                    landsToAdd.add(card);
                    landsNeeded--;
                    if (logToConsole) {
                        System.out.println("NonBasicLand[" + landsNeeded + "]:" + card.getName());
                    }
                }
            }
        }
        deckList.addAll(landsToAdd);
        aiPlayables.removeAll(landsToAdd);
    }

    /**
     * Add a third color to the deck.
     *
     * @param num
     *           number to add
     */
    private void addThirdColorCards(int num) {
        if (num > 0) {
            final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard::getRules));
            // We haven't yet ranked the off-color cards.
            // Compare them to the cards already in the deckList.
            List<PaperCard> rankedOthers = CardRanker.rankCardsInPack(others, deckList, colors, true);
            List<PaperCard> toAdd = new ArrayList<>();
            for (final PaperCard card : rankedOthers) {
                // Want a card that has just one "off" color.
                final ColorSet off = colors.getOffColors(card.getRules().getColor());
                if (off.isMonoColor()) {
                    colors = ColorSet.fromMask(colors.getColor() | off.getColor());
                    break;
                }
            }

            hasColor = Predicates.or(new DeckGeneratorBase.MatchColorIdentity(colors),
                    DeckGeneratorBase.COLORLESS_CARDS);
            final Iterable<PaperCard> threeColorList = Iterables.filter(rankedOthers,
                    Predicates.compose(hasColor, PaperCard::getRules));
            for (final PaperCard card : threeColorList) {
                if (num > 0) {
                    toAdd.add(card);
                    num--;
                    if (logToConsole) {
                        System.out.println("Third Color[" + num + "]:" + card.getName() + "("
                                + card.getRules().getManaCost() + ")");
                    }
                } else {
                    break;
                }
            }
            deckList.addAll(toAdd);
            aiPlayables.removeAll(toAdd);
            rankedColorList.removeAll(toAdd);
        }
    }

    /**
     * Add random cards to the deck.
     *
     * @param num
     *           number to add
     */
    private void addRandomCards(int num) {
        final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard::getRules));
        List <PaperCard> toAdd = new ArrayList<>();
        for (final PaperCard card : others) {
            if (num > 0) {
                toAdd.add(card);
                num--;
                if (logToConsole) {
                    System.out.println("Random[" + num + "]:" + card.getName() + "("
                            + card.getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
        deckList.addAll(toAdd);
        aiPlayables.removeAll(toAdd);
        rankedColorList.removeAll(toAdd);
    }

    /**
     * Add highest ranked non-creatures to the deck.
     *
     * @param nonCreatures
     *            cards to choose from
     * @param num
     *            number to add
     */
    private void addNonCreatures(final Iterable<PaperCard> nonCreatures, int num) {
        List<PaperCard> toAdd = new ArrayList<>();
        for (final PaperCard card : nonCreatures) {
            if (num > 0) {
                toAdd.add(card);
                num--;
                if (logToConsole) {
                    System.out.println("Others[" + num + "]:" + card.getName() + " ("
                            + card.getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
        deckList.addAll(toAdd);
        aiPlayables.removeAll(toAdd);
        rankedColorList.removeAll(toAdd);
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
                final List<PaperCard> comboCards = new ArrayList<>();
                if (ai.getDeckNeeds() != null && ai.getDeckNeeds().isValid()) {
                    Iterables.addAll(comboCards, ai.getDeckNeeds().filter(deckList));
                }
                if (ai.getDeckHints() != null && ai.getDeckHints().isValid()) {
                    Iterables.addAll(comboCards, ai.getDeckHints().filter(deckList));
                }
                if (comboCards.isEmpty()) {
                    if (logToConsole) {
                        System.out.println("No combo cards found for " + card.getName() + ", removing it.");
                    }
                    it.remove();
                    availableList.add(card);
                    if (card.getRules().getType().isCreature()) {
                        numCreatures++;
                    } else if (card.getRules().getType().isLand()) {
                    	// Do nothing, it will be replaced with basics later.
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
            addCreatures(onColorCreatures, numCreatures);
        }
        if (numOthers > 0) {
            addNonCreatures(onColorNonCreatures, numOthers);
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
     *            number to add
     */
    private void addCreatures(final Iterable<PaperCard> creatures, int num) {
        List<PaperCard> creaturesToAdd = new ArrayList<>();
        for (final PaperCard card : creatures) {
            if (num > 0) {
                creaturesToAdd.add(card);
                num--;
                if (logToConsole) {
                    System.out.println("Creature[" + num + "]:" + card.getName() + " (" + card.getRules().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
        deckList.addAll(creaturesToAdd);
        aiPlayables.removeAll(creaturesToAdd);
        rankedColorList.removeAll(creaturesToAdd);
    }

    /**
     * Add creatures to the deck, trying to follow some mana curve. Trying to
     * have generous limits at each cost, but perhaps still too strict. But
     * we're trying to prevent the AI from adding everything at a single cost.
     *
     * @param creatures
     *            cards to choose from
     * @param num
     *            number to add
     */
    private void addManaCurveCreatures(final Iterable<PaperCard> creatures, int num) {
        final Map<Integer, Integer> creatureCosts = new HashMap<>();
        for (int i = 1; i < 7; i++) {
            creatureCosts.put(i, 0);
        }
        final Predicate<PaperCard> filter = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE,
                PaperCard::getRules);
        for (final IPaperCard creature : Iterables.filter(deckList, filter)) {
            int cmc = creature.getRules().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
        }

        List<PaperCard> creaturesToAdd = new ArrayList<>();
        for (final PaperCard card : creatures) {
            int cmc = card.getRules().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            final Integer currentAtCmc = creatureCosts.get(cmc);
            boolean willAddCreature = false;
            if (cmc <= 1 && currentAtCmc < 2) {
                willAddCreature = true;
            } else if (cmc == 2 && currentAtCmc < 6) {
                willAddCreature = true;
            } else if (cmc == 3 && currentAtCmc < 7) {
                willAddCreature = true;
            } else if (cmc == 4 && currentAtCmc < 4) {
                willAddCreature = true;
            } else if (cmc == 5 && currentAtCmc < 3) {
                willAddCreature = true;
            } else if (cmc >= 6 && currentAtCmc < 2) {
                willAddCreature = true;
            }

            if (willAddCreature) {
                creaturesToAdd.add(card);
                num--;
                creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
                if (logToConsole) {
                    System.out.println("Creature[" + num + "]:" + card.getName() + " (" + card.getRules().getManaCost() + ")");
                }
            } else {
                if (logToConsole) {
                    System.out.println(card.getName() + " not added because CMC " + card.getRules().getManaCost().getCMC()
                            + " has " + currentAtCmc + " already.");
                }
            }
            if (num <= 0) {
                break;
            }
        }
        deckList.addAll(creaturesToAdd);
        aiPlayables.removeAll(creaturesToAdd);
        rankedColorList.removeAll(creaturesToAdd);
    }

    /**
     * Calculate average CMC.
     *
     * @param cards
     *            cards to choose from
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
