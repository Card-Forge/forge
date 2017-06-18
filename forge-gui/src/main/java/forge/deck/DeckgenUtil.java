package forge.deck;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import com.google.common.collect.Lists;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.generation.*;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.itemmanager.IItemManager;
import forge.limited.CardThemedDeckBuilder;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestEventDuel;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.gui.SOptionPane;
import forge.util.storage.IStorage;

import java.util.*;

/** 
 * Utility collection for various types of decks.
 * - Builders (builds or retrieves deck based on a selection)
 * - Randomizers (retrieves random deck of selected type)
 * - Color checker (see javadoc)
 * - Decklist display-er
 */
// TODO This class can be used for home menu constructed deck generation as well.
public class DeckgenUtil {

    public static Deck buildCardGenDeck(GameFormat format, boolean isForAI){
        Random random    = new Random();
        List<String> keys      = new ArrayList<>(CardRelationMatrixGenerator.cardPools.get(format).keySet());
        String       randomKey = keys.get( random.nextInt(keys.size()) );
        Predicate<PaperCard> cardFilter = Predicates.and(format.getFilterPrinted(),PaperCard.Predicates.name(randomKey));
        PaperCard keyCard = FModel.getMagicDb().getCommonCards().getAllCards(cardFilter).get(0);
        try {
            return buildCardGenDeck(keyCard,format,isForAI);
        }catch (Exception e){
            e.printStackTrace();
            return buildCardGenDeck(format,isForAI);
        }
    }

    public static Deck buildCardGenDeck(String cardName, GameFormat format, boolean isForAI){
        try {
            Predicate<PaperCard> cardFilter = Predicates.and(format.getFilterPrinted(),PaperCard.Predicates.name(cardName));
            return buildCardGenDeck(FModel.getMagicDb().getCommonCards().getAllCards(cardFilter).get(0),format,isForAI);
        }catch (Exception e){
            e.printStackTrace();
            return buildCardGenDeck(format,isForAI);
        }
    }

    /**
     * Take two lists of cards with counts of each and combine the second into the first by adding a mean normalized fraction
     * of the count in the second list to the first list.
     * @param cards1
     * @param cards2
     */
    public static void combineDistances(List<Map.Entry<PaperCard,Integer>> cards1,List<Map.Entry<PaperCard,Integer>> cards2){
        Float secondListWeighting=0.4f;
        Integer maxDistance=0;
        for (Map.Entry<PaperCard,Integer> pair1:cards1){
            maxDistance=maxDistance+pair1.getValue();
        }
        maxDistance=maxDistance/cards1.size();
        Integer maxDistance2=0;
        for (Map.Entry<PaperCard,Integer> pair2:cards2){
            maxDistance2=maxDistance2+pair2.getValue();
        }
        maxDistance2=maxDistance2/cards2.size();
        for (Map.Entry<PaperCard,Integer> pair2:cards2){
            boolean isCardPresent=false;
            for (Map.Entry<PaperCard,Integer> pair1:cards1){
                if (pair1.getKey().equals(pair2.getKey())){
                    pair1.setValue(pair1.getValue()+new Float((pair2.getValue()*secondListWeighting*maxDistance/maxDistance2)).intValue());
                    isCardPresent=true;
                    break;
                }
            }
            if(!isCardPresent){
                Map.Entry<PaperCard,Integer> newEntry=new AbstractMap.SimpleEntry<PaperCard, Integer>(pair2.getKey(),new Float((pair2.getValue()*0.4*maxDistance/maxDistance2)).intValue());
                cards1.add(newEntry);
            }
        }
    }

    public static class CardDistanceComparator implements Comparator<Map.Entry<PaperCard,Integer>>
    {
        @Override
        public int compare(Map.Entry<PaperCard,Integer> index1, Map.Entry<PaperCard,Integer> index2)
        {
            return index1.getValue().compareTo(index2.getValue());
        }
    }

    /**
     * Build a deck based on the chosen card.
     *
     * @param card
     * @param format
     * @param isForAI
     * @return
     */
    public static Deck buildCardGenDeck(PaperCard card, GameFormat format, boolean isForAI){
        List<Map.Entry<PaperCard,Integer>> potentialCards = new ArrayList<>();
        potentialCards.addAll(CardRelationMatrixGenerator.cardPools.get(format).get(card.getName()));
        Collections.sort(potentialCards,new CardDistanceComparator());
        Collections.reverse(potentialCards);
        //get second keycard
        Random r = new Random();
        List<PaperCard> preSelectedCards = new ArrayList<>();
        for(Map.Entry<PaperCard,Integer> pair:potentialCards){
            preSelectedCards.add(pair.getKey());
        }
        //filter out land cards and if for AI non-playable cards as potential second key cards
        Iterable<PaperCard> preSelectedNonLandCards;
        if(isForAI){
            preSelectedNonLandCards=Iterables.filter(preSelectedCards,Predicates.and(
                    Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES),
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES)));
        }else{
            preSelectedNonLandCards=Iterables.filter(preSelectedCards,
                    Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES));
        }
        preSelectedCards= Lists.newArrayList(preSelectedNonLandCards);

        //choose a second card randomly from the top 8 cards if possible
        int randMax=8;
        if(preSelectedCards.size()<randMax){
            randMax=preSelectedCards.size();
        }
        PaperCard secondKeycard = preSelectedCards.get(r.nextInt(randMax));
        List<Map.Entry<PaperCard,Integer>> potentialSecondCards = CardRelationMatrixGenerator.cardPools.get(format).get(secondKeycard.getName());

        //combine card distances from second key card and re-sort
        if(potentialSecondCards !=null && potentialSecondCards.size()>0) {
            combineDistances(potentialCards, potentialSecondCards);
            Collections.sort(potentialCards, new CardDistanceComparator());
            Collections.reverse(potentialCards);
        }

        List<PaperCard> selectedCards = new ArrayList<>();
        for(Map.Entry<PaperCard,Integer> pair:potentialCards){
            selectedCards.add(pair.getKey());
        }
        List<PaperCard> toRemove = new ArrayList<>();

        //randomly remove cards
        int removeCount=0;
        int i=0;
        for(PaperCard c:selectedCards){
            if(r.nextInt(100)>70+(15-(i/selectedCards.size())*selectedCards.size()) && removeCount<4 //randomly remove some cards - more likely as distance increases
                    &&!c.getName().contains("Urza")){ //avoid breaking Tron decks
                toRemove.add(c);
                removeCount++;
            }
            if(c.getName().equals(card.getName())){//may have been added in secondary list
                toRemove.add(c);
            }
            if(c.getName().equals(secondKeycard.getName())){//remove so we can add correct amount
                toRemove.add(c);
            }
            ++i;
        }
        selectedCards.removeAll(toRemove);
        //Add keycard
        List<PaperCard> playsetList = new ArrayList<>();
        int keyCardCount=4;
        if(card.getRules().getMainPart().getManaCost().getCMC()>7){
            keyCardCount=1+r.nextInt(4);
        }else if(card.getRules().getMainPart().getManaCost().getCMC()>5){
            keyCardCount=2+r.nextInt(3);
        }
        for(int j=0;j<keyCardCount;++j) {
            playsetList.add(card);
        }
        //Add 2nd keycard
        int keyCard2Count=4;
        if(card.getRules().getMainPart().getManaCost().getCMC()>7){
            keyCard2Count=1+r.nextInt(4);
        }else if(card.getRules().getMainPart().getManaCost().getCMC()>5){
            keyCard2Count=2+r.nextInt(3);
        }
        for(int j=0;j<keyCard2Count;++j) {
            playsetList.add(secondKeycard);
        }
        for (PaperCard c:selectedCards){
            for(int j=0;j<4;++j) {
                if(r.nextInt(100)<90) {
                    playsetList.add(c);
                }
            }
        }

        //build deck from combined list
        CardThemedDeckBuilder dBuilder = new CardThemedDeckBuilder(card,secondKeycard, playsetList,format,isForAI);
        Deck deck = dBuilder.buildDeck();
        if(deck.getMain().countAll()!=60){
            System.out.println(deck.getMain().countAll());
            System.out.println("Wrong card count "+deck.getMain().countAll());
            deck=buildCardGenDeck(format,isForAI);
        }
        if(deck.getMain().countAll(Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES))>27){
            System.out.println("Too many lands "+deck.getMain().countAll(Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES)));
            deck=buildCardGenDeck(format,isForAI);
        }
        while(deck.get(DeckSection.Sideboard).countAll()>15){
            deck.get(DeckSection.Sideboard).remove(deck.get(DeckSection.Sideboard).get(0));
        }
        return deck;
    }

    /**
     * @param selection {@link java.lang.String} array
     * @return {@link forge.deck.Deck}
     */

    public static Deck buildColorDeck(List<String> selection, Predicate<PaperCard> formatFilter, boolean forAi) {
        try {
            final Deck deck;
            String deckName = null;

            DeckGeneratorBase gen = null;
            CardDb cardDb = FModel.getMagicDb().getCommonCards();
            if (formatFilter == null){
                if (selection.size() == 1) {
                    gen = new DeckGeneratorMonoColor(cardDb, DeckFormat.Constructed,selection.get(0));
                }
                else if (selection.size() == 2) {
                    gen = new DeckGenerator2Color(cardDb, DeckFormat.Constructed,selection.get(0), selection.get(1));
                }
                else if (selection.size() == 3) {
                    gen = new DeckGenerator3Color(cardDb, DeckFormat.Constructed,selection.get(0), selection.get(1), selection.get(2));
                }
                else {
                    gen = new DeckGenerator5Color(cardDb, DeckFormat.Constructed);
                    deckName = "5 colors";
                }
            }else {
                if (selection.size() == 1) {
                    gen = new DeckGeneratorMonoColor(cardDb, DeckFormat.Constructed, formatFilter, selection.get(0));
                } else if (selection.size() == 2) {
                    gen = new DeckGenerator2Color(cardDb, DeckFormat.Constructed, formatFilter, selection.get(0), selection.get(1));
                } else if (selection.size() == 3) {
                    gen = new DeckGenerator3Color(cardDb, DeckFormat.Constructed, formatFilter, selection.get(0), selection.get(1), selection.get(2));
                } else {
                    gen = new DeckGenerator5Color(cardDb, DeckFormat.Constructed, formatFilter);
                    deckName = "5 colors";
                }
            }
            gen.setSingleton(FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
            gen.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
            final CardPool cards = gen.getDeck(60, forAi);

            if (null == deckName) {
                deckName = Lang.joinHomogenous(Arrays.asList(selection));
            }

            // After generating card lists, build deck.
            deck = new Deck("Random deck : " + deckName);
            deck.setDirectory("generated/color");
            deck.getMain().addAll(cards);
            return deck;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return buildColorDeck(selection, formatFilter, forAi); //try again if previous color deck couldn't be generated
    }

    public static QuestEvent getQuestEvent(final String name) {
        QuestController qCtrl = FModel.getQuest();
        for (QuestEventChallenge challenge : qCtrl.getChallenges()) {
            if (challenge.getTitle().equals(name)) {
                return challenge;
            }
        }

        QuestEventDuel duel = Iterables.find(qCtrl.getDuelsManager().getAllDuels(), new Predicate<QuestEventDuel>() {
            @Override public boolean apply(QuestEventDuel in) { return in.getName().equals(name); }
        });
        return duel;
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomColorDeck(Predicate<PaperCard> formatFilter, boolean forAi) {
        final int[] colorCount = new int[] {1, 2, 3};
        final int count = colorCount[MyRandom.getRandom().nextInt(colorCount.length)];
        final List<String> selection = new ArrayList<String>();

        // A simulated selection of "random 1" will trigger the AI selection process.
        for (int i = 0; i < count; i++) { selection.add("Random"); }
        return DeckgenUtil.buildColorDeck(selection, formatFilter, forAi);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomColorDeck(boolean forAi) {
        final int[] colorCount = new int[] {1, 2, 3, 5};
        final int count = colorCount[MyRandom.getRandom().nextInt(colorCount.length)];
        final List<String> selection = new ArrayList<String>();

        // A simulated selection of "random 1" will trigger the AI selection process.
        for (int i = 0; i < count; i++) { selection.add("Random"); }
        return DeckgenUtil.buildColorDeck(selection, null, forAi);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomCustomDeck() {
        final IStorage<Deck> allDecks = FModel.getDecks().getConstructed();
        final int rand = (int) (Math.floor(Math.random() * allDecks.size()));
        final String name = allDecks.getItemNames().toArray(new String[0])[rand];
        return allDecks.get(name);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomPreconDeck() {
        final List<DeckProxy> allDecks = DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons());
        final int rand = (int) (Math.floor(Math.random() * allDecks.size()));
        return allDecks.get(rand).getDeck();
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomThemeDeck() {
        final List<DeckProxy> allDecks = DeckProxy.getAllThemeDecks();
        final int rand = (int) (Math.floor(Math.random() * allDecks.size()));
        return allDecks.get(rand).getDeck();
    }

    public static Deck getRandomQuestDeck() {
        final List<Deck> allQuestDecks = new ArrayList<Deck>();
        QuestController qCtrl = FModel.getQuest();

        for (final QuestEvent e : qCtrl.getDuelsManager().getAllDuels()) {
            allQuestDecks.add(e.getEventDeck());
        }

        for (final QuestEvent e : qCtrl.getChallenges()) {
            allQuestDecks.add(e.getEventDeck());
        }

        final int rand = (int) (Math.floor(Math.random() * allQuestDecks.size()));
        return allQuestDecks.get(rand);
    }

    public static void randomSelectColors(final IItemManager<DeckProxy> deckManager) {
        final int size = deckManager.getItemCount();
        if (size == 0) { return; }

        int nColors = MyRandom.getRandom().nextInt(3) + 1;
        Integer[] indices = new Integer[nColors];
        for (int i = 0; i < nColors; i++) {
            int next = MyRandom.getRandom().nextInt(size);

            boolean isUnique = true;
            for (int j = 0; j < i; j++) {
                if (indices[j] == next) {
                    isUnique = false;
                    break;
                }
            }
            if (isUnique) {
                indices[i] = next;
            }
            else {
                i--; // try over with this number
            }
        }
        deckManager.setSelectedIndices(indices);
    }

    public static void randomSelect(final IItemManager<DeckProxy> deckManager) {
        final int size = deckManager.getItemCount();
        if (size == 0) { return; }

        deckManager.setSelectedIndex(MyRandom.getRandom().nextInt(size));
    }

    /** 
     * Checks lengths of selected values for color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param colors0 String[]
     * @return boolean
     */
    public static boolean colorCheck(final List<String> colors0) {
        boolean result = true;

        if (colors0.size() == 4) {
            SOptionPane.showMessageDialog(
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for this deck.",
                    "Generate deck: 4 colors", SOptionPane.ERROR_ICON);
            result = false;
        }
        else if (colors0.size() > 5) {
            SOptionPane.showMessageDialog(
                    "Generate deck: maximum five colors!",
                    "Generate deck: too many colors", SOptionPane.ERROR_ICON);
            result = false;
        }
        return result;
    }

    public static Deck generateSchemeDeck() {
        Deck deck = new Deck("");
        deck.putSection(DeckSection.Schemes, generateSchemePool());
        return deck;
    }

    public static CardPool generateSchemePool() {
        CardPool schemes = new CardPool();
        List<PaperCard> allSchemes = new ArrayList<PaperCard>();
        for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            if (c.getRules().getType().isScheme()) {
                allSchemes.add(c);
            }
        }

        int schemesToAdd = 20;
        int attemptsLeft = 100; // to avoid endless loop
        while (schemesToAdd > 0 && attemptsLeft > 0) {
            PaperCard cp = Aggregates.random(allSchemes);
            int appearances = schemes.count(cp) + 1;
            if (appearances < 2) {
                schemes.add(cp);
                schemesToAdd--;
            }
            else {
                attemptsLeft--;
            }
        }

        return schemes;
    }

    public static Deck generatePlanarDeck() {
        Deck deck = new Deck("");
        deck.putSection(DeckSection.Planes, generatePlanarPool());
        return deck;
    }

    public static CardPool generatePlanarPool() {
        CardPool res = new CardPool();
        List<PaperCard> allPlanars = new ArrayList<PaperCard>();
        for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            if (c.getRules().getType().isPlane() || c.getRules().getType().isPhenomenon()) {
                allPlanars.add(c);
            }
        }

        int phenoms = 0;
        int targetsize = MyRandom.getRandom().nextInt(allPlanars.size()-10)+10;
        while (true) {
            PaperCard rndPlane = Aggregates.random(allPlanars);
            allPlanars.remove(rndPlane);

            if (rndPlane.getRules().getType().isPhenomenon() && phenoms < 2) {
                res.add(rndPlane);
                phenoms++;
            }
            else if (rndPlane.getRules().getType().isPlane()) {
                res.add(rndPlane);
            }

            if (allPlanars.isEmpty() || res.countAll() == targetsize) {
                break;
            }
        }

        return res;
    }

    /** Generate a 2-color Commander deck. */
    public static Deck generateCommanderDeck(boolean forAi, GameType gameType) {
        final Deck deck;
        IDeckGenPool cardDb = FModel.getMagicDb().getCommonCards();
        PaperCard commander;
        ColorSet colorID;

        // Get random multicolor Legendary creature
        final DeckFormat format = gameType.getDeckFormat();
        Predicate<CardRules> canPlay = forAi ? DeckGeneratorBase.AI_CAN_PLAY : DeckGeneratorBase.HUMAN_CAN_PLAY;
        @SuppressWarnings("unchecked")
        Iterable<PaperCard> legends = cardDb.getAllCards(Predicates.compose(Predicates.and(
                new Predicate<CardRules>() {
                    @Override
                    public boolean apply(CardRules rules) {
                        return format.isLegalCommander(rules);
                    }
                },
                CardRulesPredicates.Presets.IS_MULTICOLOR,
                canPlay), PaperCard.FN_GET_RULES));

        do {
            commander = Aggregates.random(legends);
            colorID = commander.getRules().getColorIdentity();
        } while (colorID.countColors() != 2);

        List<String> comColors = new ArrayList<String>(2);
        if (colorID.hasWhite()) { comColors.add("White"); }
        if (colorID.hasBlue())  { comColors.add("Blue"); }
        if (colorID.hasBlack()) { comColors.add("Black"); }
        if (colorID.hasRed())   { comColors.add("Red"); }
        if (colorID.hasGreen()) { comColors.add("Green"); }

        DeckGeneratorBase gen = null;
        gen = new DeckGenerator2Color(cardDb, format, comColors.get(0), comColors.get(1));
        gen.setSingleton(true);
        gen.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        CardPool cards = gen.getDeck(gameType.getDeckFormat().getMainRange().getMaximum(), forAi);

        // After generating card lists, build deck.
        deck = new Deck("Generated " + gameType.toString() + " deck (" + commander.getName() + ")");
        deck.setDirectory("generated/commander");
        deck.getMain().addAll(cards);
        deck.getOrCreate(DeckSection.Commander).add(commander);

        return deck;
    }

    public static Map<ManaCostShard, Integer> suggestBasicLandCount(Deck d) {
        int W=0, U=0, R=0, B=0, G=0, total=0;
        List<PaperCard> cards = d.getOrCreate(DeckSection.Main).toFlatList();

        // attempt to determine if building for sealed, constructed or EDH
        int targetDeckSize = cards.size() < 30 ? 40 
                : cards.size() > 60 ? 100 : 60; 

        int numLandsToAdd = targetDeckSize - cards.size();

        for (PaperCard c : d.getMain().toFlatList()) {
            ManaCost m = c.getRules().getManaCost();
            W += m.getShardCount(ManaCostShard.WHITE);
            U += m.getShardCount(ManaCostShard.BLUE);
            R += m.getShardCount(ManaCostShard.RED);
            B += m.getShardCount(ManaCostShard.BLACK);
            G += m.getShardCount(ManaCostShard.GREEN);
        }
        total = W + U + R + B + G;

        int whiteSources = Math.round(numLandsToAdd * ((float)W / (float)total));
        numLandsToAdd -= whiteSources;
        total -= W;
        int blueSources = Math.round(numLandsToAdd * ((float)U / (float)total));
        numLandsToAdd -= blueSources;
        total -= U;
        int blackSources = Math.round(numLandsToAdd * ((float)B / (float)total));
        numLandsToAdd -= blackSources;
        total -= B;
        int redSources = Math.round(numLandsToAdd * ((float)R / (float)total));
        numLandsToAdd -= redSources;
        total -= R;
        int greenSources = Math.round(numLandsToAdd * ((float)G / (float)total));
        numLandsToAdd -= greenSources;
        total -= G;
        
        // in case of a rounding error, add the remaining lands to one of the relevant colors
        if (numLandsToAdd > 0) {
            if (whiteSources > 0) { whiteSources += numLandsToAdd; }
            else if (blueSources > 0) { blueSources += numLandsToAdd; }
            else if (blackSources > 0) { blackSources += numLandsToAdd; }
            else if (redSources > 0) { redSources += numLandsToAdd; }
            else if (greenSources > 0) { greenSources += numLandsToAdd; }
        }

        HashMap<ManaCostShard, Integer> suggestionMap = new HashMap<>();
        suggestionMap.put(ManaCostShard.WHITE, whiteSources);
        suggestionMap.put(ManaCostShard.BLUE, blueSources);
        suggestionMap.put(ManaCostShard.RED, redSources);
        suggestionMap.put(ManaCostShard.BLACK, blackSources);
        suggestionMap.put(ManaCostShard.GREEN, greenSources);

        return suggestionMap;
    }
}
