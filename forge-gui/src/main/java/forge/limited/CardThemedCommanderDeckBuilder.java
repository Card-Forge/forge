package forge.limited;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGenPool;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.MyRandom;

import java.util.*;

/**
 * Limited format deck.
 */
public class CardThemedCommanderDeckBuilder extends DeckGeneratorBase {
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

    protected int targetSize;
    protected int numSpellsNeeded;
    protected int numCreaturesToStart;
    protected int landsNeeded;

    protected final PaperCard commanderCard;
    protected final PaperCard partnerCard;

    protected Predicate<CardRules> hasColor;
    protected final List<PaperCard> availableList;
    protected final List<PaperCard> aiPlayables;
    protected final List<PaperCard> deckList = new ArrayList<>();
    protected final List<String> setsWithBasicLands = new ArrayList<>();
    protected List<PaperCard> rankedColorList;

    // Views for aiPlayable
    protected Iterable<PaperCard> onColorCreatures;
    protected Iterable<PaperCard> onColorNonCreatures;

    protected static final boolean logToConsole = false;
    protected static final boolean logColorsToConsole = false;


    /**
     *
     * Constructor.
     *
     * @param dList
     *            Cards to build the deck from.
     */
    public CardThemedCommanderDeckBuilder(PaperCard commanderCard0,PaperCard partner0, final List<PaperCard> dList, boolean isForAI, DeckFormat format) {
        super(FModel.getMagicDb().getCommonCards(), format);
        this.availableList = dList;
        commanderCard = commanderCard0;
        partnerCard = partner0;
        // remove Unplayables
        if(isForAI) {
            final Iterable<PaperCard> playables = Iterables.filter(availableList,
                    Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES));
            this.aiPlayables = Lists.newArrayList(playables);
        }else{
            this.aiPlayables = Lists.newArrayList(availableList);
        }
        this.availableList.removeAll(aiPlayables);
        targetSize=format.getMainRange().getMinimum();
        colors = commanderCard.getRules().getColorIdentity();
        colors = ColorSet.fromMask(colors.getColor() | commanderCard.getRules().getColorIdentity().getColor());
        if(partnerCard!=null) {
            colors = ColorSet.fromMask(colors.getColor() | partnerCard.getRules().getColorIdentity().getColor());
            targetSize--;
        }
        numSpellsNeeded = ((Double)Math.floor(targetSize*(getCreaturePercentage()+getSpellPercentage()))).intValue();
        numCreaturesToStart = ((Double)Math.ceil(targetSize*(getCreaturePercentage()))).intValue();
        landsNeeded = ((Double)Math.ceil(targetSize*(getLandPercentage()))).intValue();;
        if (logColorsToConsole) {
            System.out.println(commanderCard.getName());
            System.out.println("Pre Colors: " + colors.toEnumSet().toString());
        }
        findBasicLandSets();
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
    @SuppressWarnings("unused")
    public Deck buildDeck() {
        // 1. Prepare
        hasColor = Predicates.or(new MatchColorIdentity(colors), COLORLESS_CARDS);
        if (logColorsToConsole) {
            System.out.println(commanderCard.getName());
            System.out.println("Colors: " + colors.toEnumSet().toString());
        }
        Iterable<PaperCard> colorList = Iterables.filter(aiPlayables,
                Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
        rankedColorList = Lists.newArrayList(colorList);
        onColorCreatures = Iterables.filter(rankedColorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES));
        onColorNonCreatures = Iterables.filter(rankedColorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, PaperCard.FN_GET_RULES));
        // Guava iterables do not copy the collection contents, instead they act
        // as filters and iterate over _source_ collection each time. So even if
        // aiPlayable has changed, there is no need to create a new iterable.

        // 2. Add any planeswalkers  - removed - treat as non-creature

        // 3. Add creatures, trying to follow mana curve

        addManaCurveCards(onColorCreatures, numCreaturesToStart, "Creatures");
        if (logToConsole) {
            System.out.println("Post Creatures : " + deckList.size());
        }

        // 4.Try to fill up to num needed with on-color non-creature cards
        addManaCurveCards(onColorNonCreatures, numSpellsNeeded - deckList.size(), "Spells");
        if (logToConsole) {
            System.out.println("Post Spells : " + deckList.size());
        }

        // 5.If we couldn't get enough, try to fill up with on-color cards
        addCards(rankedColorList, numSpellsNeeded - deckList.size());
        if (logToConsole) {
            System.out.println("Post more creatures : " + deckList.size());
        }

        // 6. If there are still on-color cards, and the average cmc is low, add
        // extras.
        double avCMC=getAverageCMC(deckList);
        int maxCMC=getMaxCMC(deckList);
        if (deckList.size() == numSpellsNeeded && avCMC < 4) {
            addLowCMCCard();
            if(targetSize>60){
                addLowCMCCard();
            }
        }
        if (deckList.size() >= numSpellsNeeded && avCMC < 3 && maxCMC<6) {
            addLowCMCCard();
        }
        if (deckList.size() >= numSpellsNeeded && avCMC < 2.5 && maxCMC<5) {
            addLowCMCCard();
            if(targetSize>60){
                addLowCMCCard();
            }
        }
        if (logToConsole) {
            System.out.println("Post lowcoc : " + deckList.size());
        }

/*        // 7. If not enough cards yet, try to add a third color,
        // to try and avoid adding purely random cards.
        addThirdColorCards(numSpellsNeeded - deckList.size());
        if (logColorsToConsole) {
            System.out.println("Post 3rd colour : " + deckList.size());
            System.out.println("Colors: " + colors.toEnumSet().toString());
        }*/

        // 8. Check for DeckNeeds cards.
        //checkRemRandomDeckCards(); - no need

        // 9. If there are still less than 22 non-land cards add off-color
        // cards. This should be avoided.
        addRandomCards(numSpellsNeeded - deckList.size());
        if (logToConsole) {
            System.out.println("Post Randoms : " + deckList.size());
        }

        List<String> duals = getDualLandList();
        addNonBasicLands();
        if (logToConsole) {
            System.out.println("Post Nonbasic lands : " + deckList.size());
        }

        checkEvolvingWilds();

        // 11. Fill up with basic lands.
        final int[] clrCnts = calculateLandNeeds();

        // Add dual lands
        if (clrCnts.length>1) {

            for (String s : duals) {
                this.cardCounts.put(s, 0);
            }
        }


        if (landsNeeded > 0) {
            addLands(clrCnts);
        }
        if (logToConsole) {
            System.out.println("Post Lands : " + deckList.size());
        }
        if (commanderCard.getRules().getColorIdentity().isColorless()&&landsNeeded>0){
            // 10. Add non-basic lands that were drafted.
            addWastesIfRequired();
        }
        fixDeckSize();
        if (logToConsole) {
            System.out.println("Post Size fix : " + deckList.size());
        }

        //Create Deck
        final Deck result = new Deck(generateName());
        result.getMain().add(deckList);

        //Add remaining non-land colour matching cards to sideboard
        final CardPool cp = result.getOrCreate(DeckSection.Sideboard);
        Iterable<PaperCard> potentialSideboard = Iterables.filter(aiPlayables,
                Predicates.and(Predicates.compose(hasColor, PaperCard.FN_GET_RULES),
                        Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES)));
        int i=0;
        while(i<15 && potentialSideboard.iterator().hasNext()){
            PaperCard sbCard = potentialSideboard.iterator().next();
            cp.add(sbCard);
            aiPlayables.remove(sbCard);
            rankedColorList.remove(sbCard);


            ++i;
        }
        if (logToConsole) {
            debugFinalDeck();
        }
        return result;

    }

    /**
     * If evolving wilds is in the deck and there are fewer than 4 spaces for basic lands - remove evolving wilds
     */
    protected void checkEvolvingWilds(){
        List<PaperCard> evolvingWilds = Lists.newArrayList(Iterables.filter(deckList,PaperCard.Predicates.name("Evolving Wilds")));
        if((evolvingWilds.size()>0 && landsNeeded<4 ) || colors.countColors()<2){
            deckList.removeAll(evolvingWilds);
            landsNeeded=landsNeeded+evolvingWilds.size();
            aiPlayables.addAll(evolvingWilds);
        }
    }


    protected void addLowCMCCard(){
        final Iterable<PaperCard> nonLands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
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

    /**
     * Set the basic land pool
     * @param edition
     * @return
     */
    protected boolean setBasicLandPool(String edition){
        Predicate<PaperCard> isSetBasicLand;
        if (edition !=null){
            isSetBasicLand = Predicates.and(IPaperCard.Predicates.printedInSet(edition),
                    Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));
        }else{
            isSetBasicLand = Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES);
        }

        landPool = new DeckGenPool(format.getCardPool(fullCardDB).getAllCards(isSetBasicLand));
        return landPool.contains("Plains");
    }

    /**
     * Generate a descriptive name.
     *
     * @return name
     */
    private String generateName() {
        return commanderCard.getName() +" based commander deck";
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
     */
    private void fixDeckSize() {
        while (deckList.size() > targetSize) {
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
        if(deckList.size()==targetSize){
            return;
        }

        Predicate<PaperCard> possibleFromFullPool = new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                return format.isLegalCard(card)
                        &&!card.getRules().getManaCost().isPureGeneric()
                        && colors.containsAllColorsFrom(card.getRules().getColorIdentity().getColor())
                        && !deckList.contains(card)
                        &&!card.getRules().getAiHints().getRemAIDecks()
                        &&!card.getRules().getAiHints().getRemRandomDecks()
                        &&!card.getRules().getMainPart().getType().isLand();
            }
        };
        List<PaperCard> randomPool = Lists.newArrayList(pool.getAllCards(possibleFromFullPool));
        Collections.shuffle(randomPool,new Random());
        Iterator<PaperCard> iRandomPool=randomPool.iterator();
        while (deckList.size() < targetSize) {
            if (logToConsole) {
                System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            }
            PaperCard randomCard = iRandomPool.next();
            deckList.add(randomCard);
            if (logToConsole) {
                System.out.println(" - Added " + randomCard.getName() + " randomly.");
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
            setsWithBasicLands.add("BFZ");
        }
    }

    /**
     * Add lands to fulfill the given color counts.
     *
     * @param clrCnts
     *             counts of lands needed, by color
     */
    private void addLands(final int[] clrCnts) {
        // basic lands that are available in the deck
        final Iterable<PaperCard> basicLands = Iterables.filter(aiPlayables, Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES));
        final Set<PaperCard> snowLands = new HashSet<PaperCard>();

        // total of all ClrCnts
        int totalColor = 0;
        int numColors = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i];
            if (clrCnts[i] > 0) {
                numColors++;
            }
        }
        /*if (totalColor == 0) {
            for (int j = 0; j < nLand; j++) {
                deckList.add(getBasicLand(i));
            }
        }*/

        // do not update landsNeeded until after the loop, because the
        // calculation involves landsNeeded
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i] > 0) {
                // calculate number of lands for each color
                float p = (float) clrCnts[i] / (float) totalColor;
                if (numColors == 2) {
                    // In the normal two-color case, constrain to within 40% and 60% so that the AI
                    // doesn't put too few lands of the lesser color, risking getting screwed on that color.
                    // Don't do this for the odd case where a third color had to be added to the deck.
                    p = Math.min(Math.max(p, 0.4f), 0.6f);
                }
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
                    deckList.add(getBasicLand(i));
                }
            }
        }

        // A common problem at this point is that p in the above loop was exactly 1/2,
        // and nLand rounded up for both colors, so that one too many lands was added.
        // So if the deck size is > 60, remove the last land added.
        // Otherwise, the fixDeckSize() method would remove random cards.
        while (deckList.size() > targetSize) {
            deckList.remove(deckList.size() - 1);
        }

        deckList.addAll(snowLands);
        aiPlayables.removeAll(snowLands);
        rankedColorList.remove(snowLands);
    }

    /**
     * Get basic land.
     *
     * @param basicLand
     *             the set to take basic lands from (pass 'null' for random).
     * @return card
     */
    private PaperCard getBasicLand(final int basicLand) {
        String set;
            if (setsWithBasicLands.size() > 1) {
                set = setsWithBasicLands.get(MyRandom.getRandom().nextInt(setsWithBasicLands.size() - 1));
            } else {
                set = setsWithBasicLands.get(0);
            }
        return FModel.getMagicDb().getCommonCards().getCard(MagicColor.Constant.BASIC_LANDS.get(basicLand), set);
    }

    /**
     * Only adds wastes if present in the card pool but if present adds them all
     */
    private void addWastesIfRequired(){
        Iterable<PaperCard> wastes = Iterables.filter(aiPlayables,PaperCard.Predicates.name("Wastes"));
        if(wastes.iterator().hasNext()){
            PaperCard waste = wastes.iterator().next();
            while(landsNeeded>0) {
                deckList.add(waste);
                landsNeeded--;
            }
            aiPlayables.remove(waste);
            rankedColorList.remove(waste);
        }
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
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, PaperCard.FN_GET_RULES));
        List<PaperCard> landsToAdd = new ArrayList<>();
        int minBasics;//Keep a minimum number of basics to ensure playable decks
        if(colors.isMonoColor()){
            minBasics=Math.round((r.nextInt(15)+6)*targetSize/60);
        }else{
            minBasics=Math.round((r.nextInt(8)+6)*targetSize/60);
        }


        for (final PaperCard card : lands) {
            if (landsNeeded > minBasics) {
                // Throw out any dual-lands for the wrong colors. Assume
                // everything else is either
                // (a) dual-land of the correct two colors, or
                // (b) a land that generates colorless mana and has some other
                // beneficial effect.
                if (!card.getRules().getColorIdentity().isColorless() && card.getRules().getColorIdentity().getSharedColors(colors).countColors()==0){
                    //skip as does not match colours
                    if (logToConsole) {
                        System.out.println("Excluding NonBasicLand: " + card.getName());
                    }
                    continue;
                }
                if (!inverseDLands.contains(card.getName())&&!dLands.contains(card.getName())&&r.nextInt(100)<90) {
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
        rankedColorList.removeAll(landsToAdd);
    }


    /**
     * Add random cards to the deck.
     *
     * @param num
     *           number to add
     */
    private void addRandomCards(int num) {
        Predicate<PaperCard> possibleFromFullPool = new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                return format.isLegalCard(card)
                        &&!card.getRules().getManaCost().isPureGeneric()
                        && colors.containsAllColorsFrom(card.getRules().getColorIdentity().getColor())
                        && !deckList.contains(card)
                        &&!card.getRules().getAiHints().getRemAIDecks()
                        &&!card.getRules().getAiHints().getRemRandomDecks()
                        &&!card.getRules().getMainPart().getType().isLand();
            }
        };
        List<PaperCard> randomPool = Lists.newArrayList(pool.getAllCards(possibleFromFullPool));
        Collections.shuffle(randomPool,new Random());
        Iterator<PaperCard> iRandomPool=randomPool.iterator();
        for(int i=0;i<num;++i){
            PaperCard randomCard=iRandomPool.next();
            deckList.add(randomCard);
            if(logToConsole) {
                System.out.println("Random Card[" + i + "]:" + randomCard.getName() + " (" + randomCard.getRules().getManaCost() + ")");
            }
        }
    }

    /**
     * Add creatures to the deck.
     *
     * @param cards
     *            cards to choose from
     * @param num
     *            number to add
     */
    private void addCards(final Iterable<PaperCard> cards, int num) {
        List<PaperCard> cardsToAdd = new ArrayList<>();
        for (final PaperCard card : cards) {
            if(card.getRules().getMainPart().getType().isLand()){
                continue;
            }
            if (num +1 > 0) {
                cardsToAdd.add(card);
                if (logToConsole) {
                    System.out.println("Extra needed[" + num + "]:" + card.getName() + " (" + card.getRules().getManaCost() + ")");
                }
                num--;
            } else {
                break;
            }
        }
        deckList.addAll(cardsToAdd);
        aiPlayables.removeAll(cardsToAdd);
        rankedColorList.removeAll(cardsToAdd);
    }

    /**
     * Add cards to the deck, trying to follow some mana curve. Trying to
     * have generous limits at each cost, but perhaps still too strict. But
     * we're trying to prevent the AI from adding everything at a single cost.
     *
     * @param creatures
     *            cards to choose from
     * @param num
     *            number to add
     */
    private void addManaCurveCards(final Iterable<PaperCard> creatures, int num, String nameForLog) {
/*        // Add the deck card
        if(commanderCard.getRules().getMainPart().getType().isCreature()) {
            keyCards = Iterables.filter(aiPlayables,PaperCard.Predicates.name(commanderCard.getName()));
            final List<PaperCard> keyCardList = Lists.newArrayList(keyCards);
            deckList.addAll(keyCardList);
            aiPlayables.removeAll(keyCardList);
            rankedColorList.removeAll(keyCardList);
        }*/
        final Map<Integer,Integer> targetCMCs = new HashMap<>();
        targetCMCs.put(1,Math.round((r.nextInt(4)+2)*targetSize/60));//2
        targetCMCs.put(2,Math.round((r.nextInt(5)+5)*targetSize/60));//6
        targetCMCs.put(3,Math.round((r.nextInt(5)+6)*targetSize/60));//7
        targetCMCs.put(4,Math.round((r.nextInt(3)+3)*targetSize/60));//4
        targetCMCs.put(5,Math.round((r.nextInt(3)+3)*targetSize/60));//3
        targetCMCs.put(6,Math.round((r.nextInt(3)+1)*targetSize/60));//2


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
            if (cmc <= 1 && currentAtCmc < targetCMCs.get(1)) {
                willAddCreature = true;
            } else if (cmc == 2 && currentAtCmc < targetCMCs.get(2)) {
                willAddCreature = true;
            } else if (cmc == 3 && currentAtCmc < targetCMCs.get(3)) {
                willAddCreature = true;
            } else if (cmc == 4 && currentAtCmc < targetCMCs.get(4)) {
                willAddCreature = true;
            } else if (cmc == 5 && currentAtCmc < targetCMCs.get(5)) {
                willAddCreature = true;
            } else if (cmc >= 6 && currentAtCmc < targetCMCs.get(6)) {
                willAddCreature = true;
            }

            if (willAddCreature) {
                creaturesToAdd.add(card);
                num--;
                creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
                if (logToConsole) {
                    System.out.println(nameForLog+"[" + num + "]:" + card.getName() + " (" + card.getRules().getManaCost() + ")");
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
     * Calculate max CMC.
     *
     * @param cards
     *            cards to choose from
     * @return the average
     */
    private static int getMaxCMC(final List<PaperCard> cards) {
        int max = 0;
        for (final IPaperCard cardPrinted : cards) {
            if(cardPrinted.getRules().getManaCost().getCMC()>max) {
                max = cardPrinted.getRules().getManaCost().getCMC();
            }
        }
        return max;
    }

    /**
     * @return the colors
     */
    public ColorSet getColors() {
        return colors;
    }

    /**
     * @param colors0
     *            the colors to set
     */
    public void setColors(final ColorSet colors0) {
        colors = colors0;
    }

    /**
     * @return the aiPlayables
     */
    public List<PaperCard> getAiPlayables() {
        return aiPlayables;
    }

}
