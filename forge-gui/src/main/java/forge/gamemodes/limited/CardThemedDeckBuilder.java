package forge.gamemodes.limited;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
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
import forge.deck.generation.DeckGenPool;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.generation.IDeckGenPool;
import forge.game.GameFormat;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.MyRandom;

/**
 * Limited format deck.
 */
public class CardThemedDeckBuilder extends DeckGeneratorBase {
    @Override
    protected final float getLandPercentage() {
        return 0.41f;
    }
    @Override
    protected final float getCreaturePercentage() {
        return 0.34f;
    }
    @Override
    protected final float getSpellPercentage() {
        return 0.25f;
    }

    protected int targetSize;
    protected int numSpellsNeeded;
    protected int numCreaturesToStart;
    protected int landsNeeded;

    protected PaperCard keyCard;
    protected PaperCard secondKeyCard;

    protected Predicate<CardRules> hasColor;
    protected List<PaperCard> availableList;
    protected List<PaperCard> aiPlayables;
    protected final List<PaperCard> deckList = new ArrayList<>();
    protected final List<String> setsWithBasicLands = new ArrayList<>();
    protected List<PaperCard> rankedColorList;

    // Views for aiPlayable
    protected Iterable<PaperCard> onColorCreaturesAndSpells;

    protected static final boolean logToConsole = false;
    protected static final boolean logColorsToConsole = false;

    protected Iterable<PaperCard> keyCards;
    protected Map<Integer,Integer> targetCMCs;


    public CardThemedDeckBuilder(IDeckGenPool pool, DeckFormat format){
        super(pool,format);
    }

    public CardThemedDeckBuilder(PaperCard keyCard0, PaperCard secondKeyCard0, final List<PaperCard> dList, GameFormat format, boolean isForAI){
        this(keyCard0,secondKeyCard0, dList, format, isForAI, DeckFormat.Constructed);
    }


    /**
     *
     * Constructor.
     *
     * @param dList
     *            Cards to build the deck from.
     */
    public CardThemedDeckBuilder(PaperCard keyCard0, PaperCard secondKeyCard0, final List<PaperCard> dList, GameFormat format, boolean isForAI, DeckFormat deckFormat) {
        super(new DeckGenPool(FModel.getMagicDb().getCommonCards().getUniqueCards()), deckFormat, format.getFilterPrinted());
        this.availableList = dList;
        keyCard=keyCard0;
        secondKeyCard=secondKeyCard0;
        // remove Unplayables
        if(isForAI) {
            final Iterable<PaperCard> playables = Iterables.filter(availableList,
                    Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES));
            this.aiPlayables = Lists.newArrayList(playables);
        }else{
            this.aiPlayables = Lists.newArrayList(availableList);
        }
        this.availableList.removeAll(aiPlayables);
        targetSize=deckFormat.getMainRange().getMinimum();
        FullDeckColors deckColors = new FullDeckColors();
        int cardCount=0;
        int colourCheckAmount = 30;
        if (targetSize < 60){
            colourCheckAmount = 10;//lower amount for planar decks
        }
        //get colours for first few cards
        for(PaperCard c:getAiPlayables()){
            if(c.getRules().getType().isLand()){
                continue;
            }
            if(deckColors.canChoseMoreColors()){
                deckColors.addColorsOf(c);
                cardCount++;
            }
            if(cardCount > colourCheckAmount){
                break;
            }
        }
        colors = deckColors.getChosenColors();

        if (logColorsToConsole) {
            System.out.println(keyCard.getName());
            System.out.println("Pre Colors: " + colors.toEnumSet().toString());
        }
        if(!colors.hasAllColors(keyCard.getRules().getColorIdentity().getColor())){
            colors = ColorSet.fromMask(colors.getColor() | keyCard.getRules().getColorIdentity().getColor());
        }
        if(secondKeyCard!=null) {
            if (!colors.hasAllColors(secondKeyCard.getRules().getColorIdentity().getColor())) {
                colors = ColorSet.fromMask(colors.getColor() | secondKeyCard.getRules().getColorIdentity().getColor());
            }
        }
        numSpellsNeeded = ((Double)Math.floor(targetSize*(getCreaturePercentage()+getSpellPercentage()))).intValue();
        numCreaturesToStart = ((Double)Math.ceil(targetSize*(getCreaturePercentage()))).intValue();
        landsNeeded = ((Double)Math.ceil(targetSize*(getLandPercentage()))).intValue();
        if (logColorsToConsole) {
            System.out.println(keyCard.getName());
            System.out.println("Pre Colors: " + colors.toEnumSet().toString());
        }
        findBasicLandSets();
    }


    @Override
    public CardPool getDeck(final int size, final boolean forAi) {
        return buildDeck().getMain();
    }

    protected void updateColors(){
        //update colors
        FullDeckColors finalDeckColors = new FullDeckColors();
        for(PaperCard c:deckList){
            if(finalDeckColors.canChoseMoreColors()){
                finalDeckColors.addColorsOf(c);
            }
        }
        colors = finalDeckColors.getChosenColors();
        if (logColorsToConsole) {
            System.out.println("Final Colors: " + colors.toEnumSet().toString());
        }
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
        hasColor = Predicates.or(new MatchColorIdentity(colors), COLORLESS_CARDS);
        if (logColorsToConsole) {
            System.out.println(keyCard.getName());
            System.out.println("Colors: " + colors.toEnumSet().toString());
        }
        Iterable<PaperCard> colorList = Iterables.filter(aiPlayables,
                Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
        rankedColorList = Lists.newArrayList(colorList);
        onColorCreaturesAndSpells = Iterables.filter(rankedColorList,
                Predicates.compose(Predicates.or(CardRulesPredicates.Presets.IS_CREATURE,
                        CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL), PaperCard.FN_GET_RULES));

        // Guava iterables do not copy the collection contents, instead they act
        // as filters and iterate over _source_ collection each time. So even if
        // aiPlayable has changed, there is no need to create a new iterable.

        generateTargetCMCs();

        // 2. Add keycards

        addKeyCards();

        // 3. Add creatures, trying to follow mana curve

        numSpellsNeeded = numSpellsNeeded - deckList.size(); //subtract keycard count
        addManaCurveCards(onColorCreaturesAndSpells, numSpellsNeeded, "Creatures and Spells");
        if (logToConsole) {
            System.out.println("Post Creatures and Spells : " + deckList.size());
        }

        // 4.If we couldn't get enough, try to fill up with on-color cards
        addCards(onColorCreaturesAndSpells, numSpellsNeeded - deckList.size());
        if (logToConsole) {
            System.out.println("Post more creatures and spells : " + deckList.size());
        }

        // 5. If there are still on-color cards, and the average cmc is low, add extras.
        double avCMC = getAverageCMC(deckList);
        //calculated required lands based on https://www.channelfireball.com/articles/how-many-lands-do-you-need-to-consistently-hit-your-land-drops/
        float baseLandParameter = 16f;
        //reduce landcount if filtered hands enabled
        if(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.FILTERED_HANDS)){
            baseLandParameter--;
        }
        landsNeeded = Double.valueOf((baseLandParameter + 3.14f * avCMC) * targetSize/60f).intValue();
        if (logToConsole) {
            System.out.println("Required lands from linear regression : " + avCMC + " cmc, needed:  " + landsNeeded);
        }
        numSpellsNeeded = targetSize-landsNeeded;
        int extraCardsToAdd = numSpellsNeeded - deckList.size();
        if (logToConsole) {
            System.out.println("Extra cards to add : " + extraCardsToAdd);
        }
        if(extraCardsToAdd>0){
            for(int i=0; i<extraCardsToAdd; ++i){
                addLowCMCCard();
            }
        }

        if (logToConsole) {
            System.out.println("Post lowcoc : " + deckList.size());
        }

        // 6. If not enough cards yet, try to add a third color,
        // to try and avoid adding purely random cards.
        addThirdColorCards(numSpellsNeeded - deckList.size());
        if (logColorsToConsole) {
            System.out.println("Post 3rd colour : " + deckList.size());
            System.out.println("Colors: " + colors.toEnumSet().toString());
        }

        // 7. If there are still less than 22 non-land cards add off-color
        // cards. This should be avoided.
        int stillNeeds = numSpellsNeeded - deckList.size();
        if (logToConsole) {
            System.out.println("Still Needs? : " + stillNeeds);
        }
        if (stillNeeds > 0)
            addCards(onColorCreaturesAndSpells, stillNeeds);
        stillNeeds = numSpellsNeeded - deckList.size();
        if (stillNeeds > 0)
            extendPlaysets(stillNeeds);
        stillNeeds = numSpellsNeeded - deckList.size();
        if (stillNeeds > 0)
            addRandomCards(stillNeeds);

        if (logToConsole) {
            System.out.println("Post Randoms : " + deckList.size());
        }

        updateColors();

        addLandKeyCards();

        // 8. Add non-basic lands
        List<String> duals = getDualLandList();
        addNonBasicLands();
        if (logToConsole) {
            System.out.println("Post Nonbasic lands : " + deckList.size());
        }

        checkEvolvingWilds();

        // 9. Fill up with basic lands.
        final int[] clrCnts = calculateLandNeeds();

        // Add dual lands
        if (clrCnts.length>1) {

            for (String s : duals) {
                this.cardCounts.put(s, 0);
            }
        }

        if (logToConsole) {
            System.out.println("Lands needed : " + landsNeeded);
        }
        if (landsNeeded > 0) {
            addLands(clrCnts);
        }
        if (logToConsole) {
            System.out.println("Post Lands : " + deckList.size());
        }
        addWastesIfRequired();
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

    //Extend to playsets for non land cards to fill out deck for when no other suitable cards are available
    protected void extendPlaysets(int numSpellsNeeded){
        Map<PaperCard,Integer> currentCounts = new HashMap<>();
        List<PaperCard> cardsToAdd = new ArrayList<>();
        int i=0;
        for(PaperCard card: deckList){
            if(card.getRules().getType().isLand()){
                continue;
            }
            if(currentCounts.containsKey(card)){
                currentCounts.put(card, currentCounts.get(card) + 1);
            }else{
                currentCounts.put(card, 1);
            }
        }
        for(PaperCard card: currentCounts.keySet()){
            if(currentCounts.get(card)==2 || currentCounts.get(card)==3){
                cardsToAdd.add(card);
                ++i;
                if(i >= numSpellsNeeded ){
                    break;
                }
            }
        }
        deckList.addAll(cardsToAdd);
        aiPlayables.removeAll(cardsToAdd);
        rankedColorList.removeAll(cardsToAdd);
    }

    protected void generateTargetCMCs(){
        targetCMCs = new HashMap<>();
        targetCMCs.put(1,Math.round((MyRandom.getRandom().nextInt(16)+2)*targetSize/60));//10
        targetCMCs.put(2,Math.round((MyRandom.getRandom().nextInt(20)+6)*targetSize/60));//16
        targetCMCs.put(3,Math.round((MyRandom.getRandom().nextInt(10)+8)*targetSize/60));//13
        targetCMCs.put(4,Math.round((MyRandom.getRandom().nextInt(8)+6)*targetSize/60));//8
        targetCMCs.put(5,Math.round((MyRandom.getRandom().nextInt(8)+6)*targetSize/60));//7
        targetCMCs.put(6,Math.round((MyRandom.getRandom().nextInt(8)+6)*targetSize/60));//4

        while(sumMapValues(targetCMCs) < numSpellsNeeded){
            int randomKey = MyRandom.getRandom().nextInt(6)+1;
            targetCMCs.put(randomKey,targetCMCs.get(randomKey) + 1);
        }
    }

    private int sumMapValues(Map<Integer, Integer> integerMap){
        int sum = 0;
        for (float f : integerMap.values()) {
            sum += f;
        }
        return sum;
    }

    protected void addKeyCards(){
        // Add the first keycard if not land
        if(!keyCard.getRules().getMainPart().getType().isLand()) {
            keyCards = Iterables.filter(aiPlayables,PaperCard.Predicates.name(keyCard.getName()));
            final List<PaperCard> keyCardList = Lists.newArrayList(keyCards);
            deckList.addAll(keyCardList);
            aiPlayables.removeAll(keyCardList);
            rankedColorList.removeAll(keyCardList);
        }
        // Add the second keycard if not land
        if(secondKeyCard!=null && !secondKeyCard.getRules().getMainPart().getType().isLand()) {
            Iterable<PaperCard> secondKeyCards = Iterables.filter(aiPlayables,PaperCard.Predicates.name(secondKeyCard.getName()));
            final List<PaperCard> keyCardList = Lists.newArrayList(secondKeyCards);
            deckList.addAll(keyCardList);
            aiPlayables.removeAll(keyCardList);
            rankedColorList.removeAll(keyCardList);
        }
    }

    protected void addLandKeyCards(){
        // Add the deck card
        if(keyCard.getRules().getMainPart().getType().isLand()) {
            keyCards = Iterables.filter(aiPlayables,PaperCard.Predicates.name(keyCard.getName()));
            final List<PaperCard> keyCardList = Lists.newArrayList(keyCards);
            deckList.addAll(keyCardList);
            aiPlayables.removeAll(keyCardList);
            rankedColorList.removeAll(keyCardList);
            landsNeeded--;
        }
        // Add the deck card
        if(secondKeyCard!=null && secondKeyCard.getRules().getMainPart().getType().isLand()) {
            Iterable<PaperCard> secondKeyCards = Iterables.filter(aiPlayables,PaperCard.Predicates.name(secondKeyCard.getName()));
            final List<PaperCard> keyCardList = Lists.newArrayList(secondKeyCards);
            deckList.addAll(keyCardList);
            aiPlayables.removeAll(keyCardList);
            rankedColorList.removeAll(keyCardList);
            landsNeeded--;
        }
    }

    public static class MatchColorIdentity implements Predicate<CardRules> {
        private final ColorSet allowedColor;

        public MatchColorIdentity(ColorSet color) {
            allowedColor = color;
        }

        @Override
        public boolean apply(CardRules subject) {
            return ((allowedColor.containsAllColorsFrom(subject.getColorIdentity().getColor())));
        }
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

    /**
     * Add a third color to the deck.
     *
     * @param num
     *           number to add
     */
    protected void addThirdColorCards(int num) {
        if (num > 0) {
            final Iterable<PaperCard> others = Iterables.filter(aiPlayables,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
            // We haven't yet ranked the off-color cards.
            // Compare them to the cards already in the deckList.
            //List<PaperCard> rankedOthers = CardRanker.rankCardsInPack(others, deckList, colors, true);
            List<PaperCard> toAdd = new ArrayList<>();
            for (final PaperCard card : others) {
                // Want a card that has just one "off" color.
                final ColorSet off = colors.getOffColors(card.getRules().getColor());
                if (off.isMonoColor()) {
                    colors = ColorSet.fromMask(colors.getColor() | off.getColor());
                    break;
                }
            }

            hasColor = Predicates.and(CardRulesPredicates.Presets.IS_NON_LAND,Predicates.or(new MatchColorIdentity(colors),
                    DeckGeneratorBase.COLORLESS_CARDS));
            final Iterable<PaperCard> threeColorList = Iterables.filter(aiPlayables,
                    Predicates.compose(hasColor, PaperCard.FN_GET_RULES));
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
        }
    }

    protected void addLowCMCCard(){
        final Iterable<PaperCard> nonLands = Iterables.filter(rankedColorList,
                Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
        final PaperCard card = Iterables.getFirst(nonLands, null);
        if (card != null) {
            deckList.add(card);
            aiPlayables.remove(card);
            rankedColorList.remove(card);

            //landsNeeded--;
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
    protected String generateName() {
        if(secondKeyCard!=null ) {
            return keyCard.getName() + " - " + secondKeyCard.getName() + " based deck";
        }else{
            return keyCard.getName() + " based deck";
        }
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

    private Set<String> getDeckListNames(){
        Set<String> deckListNames = new HashSet<>();
        for(PaperCard card:deckList){
            deckListNames.add(card.getName());
        }
        return deckListNames;
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

        int stillNeeds = targetSize - deckList.size();
        if (stillNeeds > 0)
            addCards(onColorCreaturesAndSpells, stillNeeds);
        stillNeeds = targetSize - deckList.size();
        if (stillNeeds > 0)
            extendPlaysets(stillNeeds);
        stillNeeds = targetSize - deckList.size();
        if (stillNeeds == 0)
            return;

        Predicate<PaperCard> possibleFromFullPool = new Predicate<PaperCard>() {
            final Set<String> deckListNames = getDeckListNames();
            @Override
            public boolean apply(PaperCard card) {
                return format.isLegalCard(card)
                        && card.getRules().getColorIdentity().hasNoColorsExcept(colors)
                        && !deckListNames.contains(card.getName())
                        && !card.getRules().getAiHints().getRemAIDecks()
                        && !card.getRules().getAiHints().getRemRandomDecks()
                        && !card.getRules().getMainPart().getType().isLand();
            }
        };
        List<PaperCard> possibleList = Lists.newArrayList(pool.getAllCards(possibleFromFullPool));
        //ensure we do not add more keycards in case they are commanders
        if (keyCard != null) {
            possibleList.removeAll(StaticData.instance().getCommonCards().getAllCards(keyCard.getName()));
        }
        if (secondKeyCard != null) {
            possibleList.removeAll(StaticData.instance().getCommonCards().getAllCards(secondKeyCard.getName()));
        }
        //Iterator<PaperCard> iRandomPool = CardRanker.rankCardsInDeck(possibleList.subList(0, targetSize <= possibleList.size() ? targetSize : possibleList.size())).iterator();
        Collections.shuffle(possibleList);
        Iterator<PaperCard> iRandomPool = possibleList.iterator();
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
    protected void findBasicLandSets() {
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

        // total of all ClrCnts
        int totalColor = 0;
        int numColors = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i];
            if (clrCnts[i] > 0) {
                numColors++;
            }
        }
        // add one of each land required first so that any rounding errors do not remove the only land of a colour
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i] > 0) {
                float p = (float) clrCnts[i] / (float) totalColor;
                int nLand = Math.round(landsNeeded * p); // desired truncation to int
                if(nLand >0) {
                    deckList.add(getBasicLand(i));
                    landsNeeded--;
                }
            }
        }

        // do not update landsNeeded until after the loop, because the
        // calculation involves landsNeeded
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i] > 0) {
                // calculate remaining number of lands for each color
                float p = (float) clrCnts[i] / (float) totalColor;
                int nLand = Math.round(landsNeeded * p); // desired truncation to int
                if (logToConsole) {
                    System.out.printf("Basics[%s]: %d/%d = %f%% = %d cards%n", MagicColor.Constant.BASIC_LANDS.get(i), clrCnts[i], totalColor, 100*p, nLand + 1);
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
    }

    /**
     * Get basic land.
     *
     * @param basicLand
     *             the set to take basic lands from (pass 'null' for random).
     * @return card
     */
    protected PaperCard getBasicLand(final int basicLand) {
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
        PaperCard waste = FModel.getMagicDb().getCommonCards().getUniqueByName("Wastes");
        if(colors.isColorless()&& keyCard.getRules().getColorIdentity().isColorless()
                && format.isLegalCard(waste)) {
            while (landsNeeded > 0) {
                deckList.add(waste);
                landsNeeded--;
                aiPlayables.remove(waste);
                rankedColorList.remove(waste);
            }
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
        //Brawl allows colourless commanders to have any number of one basic land to fill out the deck..
        if (format.equals(DeckFormat.Brawl) && keyCard.getRules().getColorIdentity().isColorless()){
            clrCnts[MyRandom.getRandom().nextInt(5)] = 1;
            return clrCnts;
        }
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
        //check all colors have at least one count for each color in colors
        for ( int i = 0 ; i < MagicColor.WUBRG.length; i++ ) {
            final byte c = MagicColor.WUBRG[i];

            if ( colors.hasAnyColor(c)) {
                if(clrCnts[i] == 0 ) {
                    clrCnts[i]++;
                }
            }
        }
        return clrCnts;
    }

    private boolean containsTronLands(Iterable<PaperCard> cards){
        for(PaperCard card : cards){
            if(card.getRules().getType().isLand() && (
                    card.getName().equals("Urza's Mine")
                    || card.getName().equals("Urza's Tower")
                    || card.getName().equals("Urza's Power Plant"))){
                return true;
            }
        }
        return false;
    }

    /**
     * Add non-basic lands to the deck.
     */
    private void addNonBasicLands() {
        Iterable<PaperCard> lands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, PaperCard.FN_GET_RULES));
        List<PaperCard> landsToAdd = new ArrayList<>();
        int minBasics;//Keep a minimum number of basics to ensure playable decks
        if(colors.isColorless()) {
            minBasics = 0;
        }if(containsTronLands(lands)){
            minBasics=Math.round((MyRandom.getRandom().nextInt(5)+3)*((float) targetSize) / 60);
        }else if(colors.isMonoColor()){
            minBasics=Math.round((MyRandom.getRandom().nextInt(15)+9)*((float) targetSize) / 60);
        }else{
            minBasics=Math.round((MyRandom.getRandom().nextInt(8)+6)*((float) targetSize) / 60);
        }

        lands = Iterables.filter(aiPlayables,
                Predicates.compose(CardRulesPredicates.Presets.IS_NONBASIC_LAND, PaperCard.FN_GET_RULES));

        for (final PaperCard card : lands) {
            if (landsNeeded > minBasics) {
                // Use only lands that are within our colors
                if (card.getRules().getDeckbuildingColors().hasNoColorsExcept(colors)) {
                    landsToAdd.add(card);
                    landsNeeded--;
                } else if (logToConsole) {
                    System.out.println("Excluding NonBasicLand: " + card.getName());
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
        final Set<String> deckListNames = getDeckListNames();
        Predicate<PaperCard> possibleFromFullPool = new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                return format.isLegalCard(card)
                        && card.getRules().getColorIdentity().hasNoColorsExcept(colors)
                        && !deckListNames.contains(card.getName())
                        &&!card.getRules().getAiHints().getRemAIDecks()
                        &&!card.getRules().getAiHints().getRemRandomDecks()
                        &&!card.getRules().getMainPart().getType().isLand();
            }
        };
        List<PaperCard> possibleList = Lists.newArrayList(pool.getAllCards(possibleFromFullPool));
        //ensure we do not add more keycards in case they are commanders
        if (keyCard != null) {
            possibleList.removeAll(StaticData.instance().getCommonCards().getAllCards(keyCard.getName()));
        }
        if (secondKeyCard != null) {
            possibleList.removeAll(StaticData.instance().getCommonCards().getAllCards(secondKeyCard.getName()));
        }
        Collections.shuffle(possibleList);
        //addManaCurveCards(CardRanker.rankCardsInDeck(possibleList.subList(0, targetSize*3 <= possibleList.size() ? targetSize*3 : possibleList.size())),
                //num, "Random Card");
        addManaCurveCards(possibleList, num, "Random Card");
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
            if (num > 0) {
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

        final Map<Integer, Integer> creatureCosts = new HashMap<>();
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
