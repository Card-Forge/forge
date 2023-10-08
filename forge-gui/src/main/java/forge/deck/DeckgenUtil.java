package forge.deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.generation.DeckGenerator2Color;
import forge.deck.generation.DeckGenerator3Color;
import forge.deck.generation.DeckGenerator5Color;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.generation.DeckGeneratorMonoColor;
import forge.deck.generation.IDeckGenPool;
import forge.deck.io.Archetype;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gamemodes.limited.ArchetypeDeckBuilder;
import forge.gamemodes.limited.CardThemedCommanderDeckBuilder;
import forge.gamemodes.limited.CardThemedConquestDeckBuilder;
import forge.gamemodes.limited.CardThemedDeckBuilder;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestEvent;
import forge.gamemodes.quest.QuestEventChallenge;
import forge.gamemodes.quest.QuestEventDuel;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.itemmanager.IItemManager;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.storage.IStorage;

/** 
 * Utility collection for various types of decks.
 * - Builders (builds or retrieves deck based on a selection)
 * - Randomizers (retrieves random deck of selected type)
 * - Color checker (see javadoc)
 * - Decklist display-er
 */
// TODO This class can be used for home menu constructed deck generation as well.
public class DeckgenUtil {
    private static List<DeckProxy> advPrecons = Lists.newArrayList(), advThemes = Lists.newArrayList(), geneticAI = Lists.newArrayList();

    public static Deck buildCardGenDeck(GameFormat format, boolean isForAI){
        try {
            List<String> keys      = new ArrayList<>(CardArchetypeLDAGenerator.ldaPools.get(format.getName()).keySet());
            String       randomKey = keys.get( MyRandom.getRandom().nextInt(keys.size()) );
            Predicate<PaperCard> cardFilter = Predicates.and(format.getFilterPrinted(),PaperCard.Predicates.name(randomKey));
            PaperCard keyCard = FModel.getMagicDb().getCommonCards().getAllCards(cardFilter).get(0);

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

    public static Deck buildPlanarConquestDeck(PaperCard card, GameFormat format, DeckFormat deckFormat){
        return buildPlanarConquestDeck(card, null, format, deckFormat, false);
    }

    public static Deck buildPlanarConquestCommanderDeck(PaperCard card, GameFormat format, DeckFormat deckFormat){
        Deck deck = buildPlanarConquestDeck(card, null, format, deckFormat, true);
        deck.getMain().removeAll(card);
        deck.getOrCreate(DeckSection.Commander).add(card);
        return deck;
    }

    public static Deck buildPlanarConquestCommanderDeck(PaperCard card, PaperCard secondKeycard, GameFormat format, DeckFormat deckFormat){
        Deck deck = buildPlanarConquestDeck(card, secondKeycard, format, deckFormat, true);
        deck.getMain().removeAll(card);
        deck.getOrCreate(DeckSection.Commander).add(card);
        return deck;
    }

    public static Deck buildPlanarConquestDeck(PaperCard card, PaperCard secondKeycard, GameFormat format, DeckFormat deckFormat, boolean forCommander){
        final boolean isForAI = true;
        Set<String> uniqueCards = new HashSet<>();
        List<PaperCard> selectedCards = new ArrayList<>();
        List<List<Pair<String, Double>>> cardArchetypes = CardArchetypeLDAGenerator.ldaPools.get(FModel.getFormats().getStandard().getName()).get(card.getName());
        for(List<Pair<String, Double>> archetype:cardArchetypes){
            for(Pair<String, Double> cardPair:archetype){
                String cardName = cardPair.getLeft();
                uniqueCards.add(cardName);
            }
        }
        for(String cardName:uniqueCards){
            selectedCards.add(StaticData.instance().getCommonCards().getUniqueByName(cardName));
        }

        //build deck from combined list
        CardThemedDeckBuilder dBuilder;

        if(forCommander){
            dBuilder = new CardThemedConquestDeckBuilder(card, selectedCards, format ,isForAI, deckFormat);
        }else{
            dBuilder = new CardThemedDeckBuilder(card,secondKeycard, selectedCards,format,isForAI, deckFormat);
        }

        Deck deck = dBuilder.buildDeck();
        return deck;
    }

    public static Deck buildCardGenDeck(PaperCard card, GameFormat format, boolean isForAI){
        return buildCardGenDeck(card, null, format, isForAI);
    }

    /**
     * Build a deck based on the chosen card.
     *
     * @param card
     * @param format
     * @param isForAI
     * @return
     */
    public static Deck buildCardGenDeck(PaperCard card, PaperCard secondKeycard, GameFormat format, boolean isForAI){
        return buildLDACardGenDeck(card, format, isForAI);
    }

    private static boolean isCrucialCard(PaperCard pc) {
        //avoid breaking decks, this should be deck/card hints in the future
        return pc.getName().contains("Urza") || pc.getName().contains("Yawgmoth") || pc.getName().equalsIgnoreCase("Living End");
    }
    /**
     * Build a deck based on the chosen card.
     *
     * @param card
     * @param format
     * @param isForAI
     * @return
     */
    public static Deck buildLDACardGenDeck(PaperCard card,GameFormat format, boolean isForAI){
        List<List<Pair<String, Double>>> preSelectedCardLists = CardArchetypeLDAGenerator.ldaPools.get(format.getName()).get(card.getName());
        List<Pair<String, Double>> preSelectedCardNames = preSelectedCardLists.get(MyRandom.getRandom().nextInt(preSelectedCardLists.size()));
        List<PaperCard> selectedCards = new ArrayList<>();
        for(Pair<String, Double> pair:preSelectedCardNames){
            String name = pair.getLeft();
            //remove any cards not valid in format
            PaperCard cardToAdd = Aggregates.random(StaticData.instance().getCommonCards().getAllCards(name, format.getFilterPrinted()));
            if (cardToAdd == null)
                continue;
            if(!cardToAdd.getName().equals(card.getName())) {
                selectedCards.add(cardToAdd);
            }
        }

        List<PaperCard> toRemove = new ArrayList<>();

        //randomly remove cards
        int removeCount=0;
        int i=0;
        for(PaperCard c:selectedCards){
            if(MyRandom.getRandom().nextInt(100)>70+(15-(i/selectedCards.size())*selectedCards.size()) && removeCount<4 //randomly remove some cards - more likely as distance increases
                    &&!isCrucialCard(c)){
                toRemove.add(c);
                removeCount++;
            }
            if(c.getName().equals(card.getName())){//may have been added in secondary list
                toRemove.add(c);
            }
            ++i;
        }
        selectedCards.removeAll(toRemove);
        //Add keycard
        List<PaperCard> playsetList = new ArrayList<>();
        int keyCardCount=4;
        if(card.getRules().getMainPart().getManaCost().getCMC()>7){
            keyCardCount=1+MyRandom.getRandom().nextInt(4);
        }else if(card.getRules().getMainPart().getManaCost().getCMC()>5){
            keyCardCount=2+MyRandom.getRandom().nextInt(3);
        }
        for(int j=0;j<keyCardCount;++j) {
            playsetList.add(card);
        }
        for (PaperCard c:selectedCards){
            for(int j=0;j<4;++j) {
                if(MyRandom.getRandom().nextInt(100)<90) {
                    playsetList.add(c);
                }
            }
        }

        //build deck from combined list
        CardThemedDeckBuilder dBuilder = new CardThemedDeckBuilder(card,null, playsetList,format,isForAI);
        Deck deck = dBuilder.buildDeck();
        if(deck.getMain().countAll()!=60){
            System.out.println(deck.getMain().countAll());
            System.out.println("Wrong card count "+deck.getMain().countAll());
            deck=buildLDACArchetypeDeck(format,isForAI);
        }
        if(deck.getMain().countAll(Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES))>27){
            System.out.println("Too many lands "+deck.getMain().countAll(Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES)));
            deck=buildLDACArchetypeDeck(format,isForAI);
        }
        while(deck.get(DeckSection.Sideboard).countAll()>15){
            deck.get(DeckSection.Sideboard).remove(deck.get(DeckSection.Sideboard).get(0));
        }
        return deck;
    }

    public static Deck buildLDACArchetypeDeck(GameFormat format, boolean isForAI){
        List<Archetype> keys = new ArrayList<>(CardArchetypeLDAGenerator.ldaArchetypes.get(format.getName()));
        Archetype randomKey = keys.get( MyRandom.getRandom().nextInt(keys.size()) );
        return buildLDACArchetypeDeck(randomKey,format,isForAI);
    }


    /**
     * Build a deck based on the chosen card.
     *
     * @param archetype
     * @param format
     * @param isForAI
     * @return
     */
    public static Deck buildLDACArchetypeDeck(Archetype archetype, GameFormat format, boolean isForAI){
        List<Pair<String, Double>> preSelectedCardNames = archetype.getCardProbabilities();
        PaperCard card = null;
        for(Pair<String, Double> pair : preSelectedCardNames){
            card = StaticData.instance().getCommonCards().getUniqueByName(pair.getLeft());
            if(card != null &&!card.getRules().getType().isLand()){
                break;
            }
        }
        List<PaperCard> selectedCards = new ArrayList<>();
        int cardCount=0;
        for(Pair<String, Double> pair:preSelectedCardNames){
            String name = pair.getLeft();
            //remove any cards not valid in format
            PaperCard cardToAdd = Aggregates.random(StaticData.instance().getCommonCards().getAllCards(name, format.getFilterPrinted()));
            if(cardToAdd != null && !cardToAdd.getName().equals(card.getName())) {
                selectedCards.add(cardToAdd);
                cardCount++;
            }
            if(cardCount>120){// no need to have more than this
                break;
            }
        }

        List<PaperCard> toRemove = new ArrayList<>();

        //randomly remove cards
        int removeCount=0;
        int i=0;
        for(PaperCard c:selectedCards){
            if( i > 4 && MyRandom.getRandom().nextInt(100)>70+(15-(i/selectedCards.size())*selectedCards.size()) && removeCount<4 //randomly remove some cards - more likely as distance increases
                    &&!isCrucialCard(c)){
                toRemove.add(c);
                removeCount++;
            }
            if(c.getName().equals(card.getName())){//may have been added in secondary list
                toRemove.add(c);
            }
            ++i;
        }
        selectedCards.removeAll(toRemove);
        //Add keycard
        List<PaperCard> playsetList = new ArrayList<>();
        int keyCardCount=4;
        if(card.getRules().getMainPart().getManaCost().getCMC()>7){
            keyCardCount=1+MyRandom.getRandom().nextInt(4);
        }else if(card.getRules().getMainPart().getManaCost().getCMC()>5){
            keyCardCount=2+MyRandom.getRandom().nextInt(3);
        }
        for(int j=0;j<keyCardCount;++j) {
            playsetList.add(card);
        }
        List<String> restrictedCardsAdded = new ArrayList<>();
        for (PaperCard c:selectedCards){
            if (format.getRestrictedCards().contains(c.getName())&&!restrictedCardsAdded.contains(c.getName())){
                playsetList.add(c);
                restrictedCardsAdded.add(c.getName());
                continue;
            }
            for(int j=0;j<4;++j) {
                if(MyRandom.getRandom().nextInt(100)<90) {
                    playsetList.add(c);
                }
            }
        }

        //build deck from combined list
        ArchetypeDeckBuilder dBuilder = new ArchetypeDeckBuilder(archetype, card, playsetList,format,isForAI);
        Deck deck = dBuilder.buildDeck();
        if(deck.getMain().countAll()!=60){
            System.out.println(deck.getMain().countAll());
            System.out.println("Wrong card count "+deck.getMain().countAll());
            deck=buildLDACArchetypeDeck(format,isForAI);
        }
        if(deck.getMain().countAll(Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES))>27){
            System.out.println("Too many lands "+deck.getMain().countAll(Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES)));
            deck=buildLDACArchetypeDeck(format,isForAI);
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
        final List<String> selection = new ArrayList<>();

        // A simulated selection of "random 1" will trigger the AI selection process.
        for (int i = 0; i < count; i++) { selection.add("Random"); }
        return DeckgenUtil.buildColorDeck(selection, formatFilter, forAi);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomColorDeck(boolean forAi) {
        final int[] colorCount = new int[] {1, 2, 3, 5};
        final int count = colorCount[MyRandom.getRandom().nextInt(colorCount.length)];
        final List<String> selection = new ArrayList<>();

        // A simulated selection of "random 1" will trigger the AI selection process.
        for (int i = 0; i < count; i++) { selection.add("Random"); }
        return DeckgenUtil.buildColorDeck(selection, null, forAi);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomCustomDeck() {
        final IStorage<Deck> allDecks = FModel.getDecks().getConstructed();
        // in case user has not created any yet
        if (allDecks.size() == 0) { return null; }
        final int rand = (int) (Math.floor(MyRandom.getRandom().nextDouble() * allDecks.size()));
        final String name = allDecks.getItemNames().toArray(new String[0])[rand];
        return allDecks.get(name);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getCommanderDeck() {
        final IStorage<Deck> allDecks = FModel.getDecks().getCommander();
        // in case user has not created any yet
        if (allDecks.size() == 0) { return null; }
        final int rand = (int) (Math.floor(MyRandom.getRandom().nextDouble() * allDecks.size()));
        final String name = allDecks.getItemNames().toArray(new String[0])[rand];
        return allDecks.get(name);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomOrPreconOrThemeDeck(String colors, boolean forAi, boolean isTheme, boolean useGeneticAI) {
        final List<String> selection = new ArrayList<>();
        Deck deck = null;
        if (advPrecons.isEmpty()) {
            advPrecons.addAll(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()));
        }
        if (advThemes.isEmpty()) {
            advThemes.addAll(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()));
            advThemes.addAll(DeckProxy.getNonEasyQuestDuelDecks());
        }
        if (geneticAI.isEmpty()) {
            geneticAI.addAll(DeckProxy.getAllGeneticAIDecks());
        }
        if (!colors.isEmpty()) {
            for (char c : colors.toLowerCase().toCharArray()) {
                switch (c) {
                    case 'b': selection.add("black");
                    case 'r': selection.add("red");
                    case 'g': selection.add("green");
                    case 'u': selection.add("blue");
                    case 'w': selection.add("white");
                }
            }
        }
        try {
            if (useGeneticAI) {
                if (!selection.isEmpty())
                    deck = Aggregates.random(Iterables.filter(geneticAI, deckProxy -> deckProxy.getColorIdentity().sharesColorWith(ColorSet.fromNames(colors.toCharArray())))).getDeck();
                else
                    deck = Aggregates.random(geneticAI).getDeck();

            } else {
                if (!selection.isEmpty() && selection.size() < 4) {
                    Predicate<DeckProxy> pred = Predicates.and(deckProxy -> deckProxy.getMainSize() <= 60, deckProxy -> deckProxy.getColorIdentity().hasAllColors(ColorSet.fromNames(colors.toCharArray()).getColor()));
                    if (isTheme)
                        deck = Aggregates.random(Iterables.filter(advThemes, pred)).getDeck();
                    else
                        deck = Aggregates.random(Iterables.filter(advPrecons, pred)).getDeck();
                } else {
                    if (isTheme)
                        deck = Aggregates.random(Iterables.filter(advThemes, deckProxy -> deckProxy.getMainSize() <= 60)).getDeck();
                    else
                        deck = Aggregates.random(Iterables.filter(advPrecons, deckProxy -> deckProxy.getMainSize() <= 60)).getDeck();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (deck != null) {
            return deck;
        }
        return getRandomColorDeck(forAi);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomPreconDeck() {
        final List<DeckProxy> allDecks = DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons());
        final int rand = (int) (Math.floor(MyRandom.getRandom().nextDouble() * allDecks.size()));
        return allDecks.get(rand).getDeck();
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomCommanderPreconDeck() {
        final Iterable<DeckProxy> allDecks = DeckProxy.getAllCommanderPreconDecks();
        return Aggregates.random(allDecks).getDeck();
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomThemeDeck() {
        final List<DeckProxy> allDecks = DeckProxy.getAllThemeDecks();
        final int rand = (int) (Math.floor(MyRandom.getRandom().nextDouble() * allDecks.size()));
        return allDecks.get(rand).getDeck();
    }

    public static Deck getRandomQuestDeck() {
        final List<Deck> allQuestDecks = new ArrayList<>();
        QuestController qCtrl = FModel.getQuest();

        for (final QuestEvent e : qCtrl.getDuelsManager().getAllDuels()) {
            allQuestDecks.add(e.getEventDeck());
        }

        for (final QuestEvent e : qCtrl.getChallenges()) {
            allQuestDecks.add(e.getEventDeck());
        }

        final int rand = (int) (Math.floor(MyRandom.getRandom().nextDouble() * allQuestDecks.size()));
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
        List<PaperCard> allSchemes = new ArrayList<>();
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
        List<PaperCard> allPlanars = new ArrayList<>();
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

    /** Generate a 2-5-color Commander deck. */
    public static Deck generateCommanderDeck(boolean forAi, GameType gameType) {
        final Deck deck;
        IDeckGenPool cardDb = FModel.getMagicDb().getCommonCards();
        PaperCard commander;
        ColorSet colorID;

        // Get random multicolor Legendary creature
        final DeckFormat format = gameType.getDeckFormat();
        Predicate<CardRules> canPlay = forAi ? DeckGeneratorBase.AI_CAN_PLAY : CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS;
        @SuppressWarnings("unchecked")
        Iterable<PaperCard> legends = cardDb.getAllCards(Predicates.and(format.isLegalCardPredicate(), format.isLegalCommanderPredicate(),
                Predicates.compose(canPlay, PaperCard.FN_GET_RULES)));

        commander = Aggregates.random(legends);
        return generateRandomCommanderDeck(commander, format, forAi, false);
    }

    /** Generate a ramdom Commander deck. */
    public static Deck generateRandomCommanderDeck(PaperCard commander, DeckFormat format, boolean forAi, boolean isCardGen) {
        final Deck deck;
        IDeckGenPool cardDb;
        DeckGeneratorBase gen = null;
        PaperCard selectedPartner = null;
        List<PaperCard> preSelectedCards = new ArrayList<>();
        if(isCardGen){
            if(format.equals(DeckFormat.Brawl)){//TODO: replace with actual Brawl based data
                Set<String> uniqueCards = new HashSet<>();
                List<List<Pair<String, Double>>> cardArchetypes = CardArchetypeLDAGenerator.ldaPools.get(FModel.getFormats().getStandard().getName()).get(commander.getName());
                for(List<Pair<String, Double>> archetype:cardArchetypes){
                    for(Pair<String, Double> cardPair:archetype){
                        String cardName = cardPair.getLeft();
                        uniqueCards.add(cardName);
                    }
                }
                for(String card:uniqueCards){
                    PaperCard paperCard = StaticData.instance().getCommonCards().getUniqueByName(card);
                    if(format.isLegalCard(paperCard)) {
                        preSelectedCards.add(paperCard);
                    }
                }
            }else {
                String matrixKey = (format.equals(DeckFormat.TinyLeaders) ? DeckFormat.Commander : format).toString(); //use Commander for Tiny Leaders
                List<Map.Entry<PaperCard, Integer>> potentialCards = new ArrayList<>(CardRelationMatrixGenerator.cardPools.get(matrixKey).get(commander.getName()));
                Collections.shuffle(potentialCards, MyRandom.getRandom());
                for(Map.Entry<PaperCard,Integer> pair:potentialCards){
                    if(format.isLegalCard(pair.getKey())) {
                        preSelectedCards.add(pair.getKey());
                    }
                }
            }

            if (format.equals(DeckFormat.Oathbreaker)) {
                //check for signature spells
                List<PaperCard> signatureSpells = new ArrayList<>();
                for (PaperCard c : preSelectedCards) {
                    if (c.getRules().canBeSignatureSpell()) {
                        signatureSpells.add(c);
                    }
                }

                if (signatureSpells.size() > 0) { //pass signature spell as partner for simplicity
                    selectedPartner = signatureSpells.get(MyRandom.getRandom().nextInt(signatureSpells.size()));
                    preSelectedCards.removeAll(StaticData.instance().getCommonCards().getAllCards(selectedPartner.getName()));
                }
            }
            else if (commander.getRules().canBePartnerCommander()) {
                //check for partner commanders
                List<PaperCard> partners = new ArrayList<>();
                for (PaperCard c : preSelectedCards) {
                    if (c.getRules().canBePartnerCommander()) {
                        if (commander.getRules().hasKeyword("Partner") && commander.getRules().getPartnerWith().isEmpty() && c.getRules().hasKeyword("Partner") && c.getRules().getPartnerWith().isEmpty())
                            partners.add(c);
                        else if (commander.getRules().getPartnerWith().equals(c.getName()) && c.getRules().getPartnerWith().equals(commander.getName()))
                            partners.add(c);
                        else if (commander.getRules().hasKeyword("Friends forever") && c.getRules().hasKeyword("Friends forever"))
                            partners.add(c);
                        else if (commander.getRules().hasKeyword("Choose a Background") && c.getRules().canBeBackground())
                            partners.add(c);
                        else if (c.getRules().hasKeyword("Choose a Background") && commander.getRules().canBeBackground())
                            partners.add(c);
                    }
                }

                if (partners.size() > 0) {
                    selectedPartner = partners.get(MyRandom.getRandom().nextInt(partners.size()));
                    preSelectedCards.removeAll(StaticData.instance().getCommonCards().getAllCards(selectedPartner.getName()));
                }
            }
            //randomly remove cards
            int removeCount=0;
            int i=0;
            List<PaperCard> toRemove = new ArrayList<>();
            for(PaperCard c:preSelectedCards){
                if(!format.isLegalCard(c)){
                    toRemove.add(c);
                    removeCount++;
                }
                if(preSelectedCards.size()<75){
                    break;
                }
                if(MyRandom.getRandom().nextInt(100)>60+(15-(i/preSelectedCards.size())*preSelectedCards.size()) && removeCount<4 //randomly remove some cards - more likely as distance increases
                        &&!c.getName().contains("Urza")&&!c.getName().contains("Wastes")){ //avoid breaking Tron decks
                    toRemove.add(c);
                    removeCount++;
                }
                ++i;
            }
            preSelectedCards.removeAll(toRemove);
            preSelectedCards.removeAll(StaticData.instance().getCommonCards().getAllCards(commander.getName()));
            gen = new CardThemedCommanderDeckBuilder(commander, selectedPartner, preSelectedCards, forAi, format);
        }else{
            cardDb = FModel.getMagicDb().getCommonCards();
            //shuffle first 400 random cards
            Iterable<PaperCard> colorList = Iterables.filter(format.getCardPool(cardDb).getAllCards(),
                    Predicates.and(format.isLegalCardPredicate(),Predicates.compose(Predicates.or(
                            new CardThemedDeckBuilder.MatchColorIdentity(commander.getRules().getColorIdentity()),
                            DeckGeneratorBase.COLORLESS_CARDS), PaperCard.FN_GET_RULES)));
            switch (format) {
            case Brawl: //for Brawl - add additional filterprinted rule to remove old reprints for a consistent look
                colorList = Iterables.filter(colorList,FModel.getFormats().getStandard().getFilterPrinted());
                break;
            case Oathbreaker:
                //check for signature spells
                List<PaperCard> signatureSpells = new ArrayList<>();
                for (PaperCard c : colorList) {
                    if (c.getRules().canBeSignatureSpell()) {
                        signatureSpells.add(c);
                    }
                }

                if (signatureSpells.size() > 0) { //pass signature spell as partner for simplicity
                    selectedPartner = signatureSpells.get(MyRandom.getRandom().nextInt(signatureSpells.size()));
                }
                break;
            default:
                if (commander.getRules().canBePartnerCommander()) {
                    //check for partner commanders
                    List<PaperCard> partners = new ArrayList<>();
                    for (PaperCard c : colorList) {
                        if (c.getRules().canBePartnerCommander()) {
                            partners.add(c);
                        }
                    }

                    if (partners.size() > 0) {
                        selectedPartner = partners.get(MyRandom.getRandom().nextInt(partners.size()));
                    }
                }
                break;
            }
            List<PaperCard> cardList = Lists.newArrayList(colorList);
            Collections.shuffle(cardList, MyRandom.getRandom());
            int shortlistlength=400;
            if(cardList.size()<shortlistlength){
                shortlistlength=cardList.size();
            }
            List<PaperCard> shortList = cardList.subList(0, shortlistlength);
            shortList.remove(commander);
            shortList.removeAll(StaticData.instance().getCommonCards().getAllCards(commander.getName()));
            if (selectedPartner != null) {
                shortList.remove(selectedPartner);
                shortList.removeAll(StaticData.instance().getCommonCards().getAllCards(selectedPartner.getName()));
            }
            gen = new CardThemedCommanderDeckBuilder(commander, selectedPartner, shortList, forAi, format);
        }

        gen.setSingleton(true);
        gen.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        CardPool cards = gen.getDeck(format.getMainRange().getMaximum(), forAi);

        // After generating card lists, build deck.
        if(selectedPartner!=null){
            deck = new Deck("Generated " + format.toString() + " deck (" + commander.getName() +
                    "--" + selectedPartner.getName() + ")");
        }else{
            deck = new Deck("Generated " + format.toString() + " deck (" + commander.getName() + ")");
        }
        deck.setDirectory("generated/commander");
        deck.getMain().addAll(cards);
        deck.getOrCreate(DeckSection.Commander).add(commander);
        if (selectedPartner!=null){
            deck.get(DeckSection.Commander).add(selectedPartner);
        }

        return deck;
    }

    public static Map<ManaCostShard, Integer> suggestBasicLandCount(Deck d) {
        int W=0, U=0, R=0, B=0, G=0, total=0;
        List<PaperCard> cards = d.getOrCreate(DeckSection.Main).toFlatList();
        HashMap<ManaCostShard, Integer> suggestionMap = new HashMap<>();

        // determine how many additional lands we need, but don't take lands already in deck into consideration,
        // or we risk incorrectly determining the target deck size
        int numLands = Iterables.size(Iterables.filter(cards, Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES)));
        int sizeNoLands = cards.size() - numLands;

        // attempt to determine if building for sealed, constructed or EDH
        int targetDeckSize = sizeNoLands < 30 ? 40
                : sizeNoLands > 60 ? 100 : 60;

        int numLandsToAdd = targetDeckSize - cards.size();

        if (numLandsToAdd == 0) {
            // already at target deck size, do nothing
            suggestionMap.put(ManaCostShard.WHITE, 0);
            suggestionMap.put(ManaCostShard.BLUE, 0);
            suggestionMap.put(ManaCostShard.RED, 0);
            suggestionMap.put(ManaCostShard.BLACK, 0);
            suggestionMap.put(ManaCostShard.GREEN, 0);
            return suggestionMap;
        }

        for (PaperCard c : d.getMain().toFlatList()) {
            ManaCost m = c.getRules().getManaCost();
            W += m.getShardCount(ManaCostShard.WHITE);
            U += m.getShardCount(ManaCostShard.BLUE);
            R += m.getShardCount(ManaCostShard.RED);
            B += m.getShardCount(ManaCostShard.BLACK);
            G += m.getShardCount(ManaCostShard.GREEN);
        }
        total = W + U + R + B + G;

        int whiteSources = Math.max(0, Math.round(numLandsToAdd * ((float)W / (float)total)));
        if (W > 0) { whiteSources = Math.max(1, whiteSources); }
        numLandsToAdd -= whiteSources;
        total -= W;
        int blueSources = Math.max(0, Math.round(numLandsToAdd * ((float)U / (float)total)));
        if (U > 0) { blueSources = Math.max(1, blueSources); }
        numLandsToAdd -= blueSources;
        total -= U;
        int blackSources = Math.max(0, Math.round(numLandsToAdd * ((float)B / (float)total)));
        if (B > 0) { blackSources = Math.max(1, blackSources); }
        numLandsToAdd -= blackSources;
        total -= B;
        int redSources = Math.max(0, Math.round(numLandsToAdd * ((float)R / (float)total)));
        if (R > 0) { redSources = Math.max(1, redSources); }
        numLandsToAdd -= redSources;
        total -= R;
        int greenSources = Math.max(0, Math.round(numLandsToAdd * ((float)G / (float)total)));
        if (G > 0) { greenSources = Math.max(1, greenSources); }
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

        suggestionMap.put(ManaCostShard.WHITE, whiteSources);
        suggestionMap.put(ManaCostShard.BLUE, blueSources);
        suggestionMap.put(ManaCostShard.RED, redSources);
        suggestionMap.put(ManaCostShard.BLACK, blackSources);
        suggestionMap.put(ManaCostShard.GREEN, greenSources);

        return suggestionMap;
    }
}
