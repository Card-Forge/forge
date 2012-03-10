package forge.quest.data;


import java.util.ArrayList;
import java.util.List;

import forge.Singletons;
import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.item.PreconDeck;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.IStorage;
import forge.util.IStorageView;
import forge.util.Predicate;
import forge.util.StorageView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestController {

    private QuestData model;
    // gadgets
    
    // Utility class to access cards, has access to private fields
    // Moved some methods there that otherwise would make this class even more
    // complex
    private transient QuestUtilCards myCards;

    private transient QuestEvent currentEvent;

    transient IStorage<Deck> decks;

    // acquired
    // since
    // last
    // game-win/loss
    
    /** The available challenges. */
    private List<Integer> availableChallenges = new ArrayList<Integer>();

    /** The available quests. */
    private List<Integer> availableQuests = null;

    // This is used by shop. Had no idea where else to place this
    private static transient IStorageView<PreconDeck> preconManager =
            new StorageView<PreconDeck>(new PreconReader(ForgeProps.getFile(NewConstants.Quest.PRECONS)));

    /** The Constant RANK_TITLES. */
    public static final String[] RANK_TITLES = new String[] { "Level 0 - Confused Wizard", "Level 1 - Mana Mage",
            "Level 2 - Death by Megrim", "Level 3 - Shattered the Competition", "Level 4 - Black Knighted",
            "Level 5 - Shockingly Good", "Level 6 - Regressed into Timmy", "Level 7 - Loves Blue Control",
            "Level 8 - Immobilized by Fear", "Level 9 - Lands = Friends", "Level 10 - Forging new paths",
            "Level 11 - Infect-o-tron", "Level 12 - Great Balls of Fire", "Level 13 - Artifact Schmartifact",
            "Level 14 - Mike Mulligan's The Name", "Level 15 - Fresh Air: Good For The Health",
            "Level 16 - In It For The Love", "Level 17 - Sticks, Stones, Bones", "Level 18 - Credits For Breakfast",
            "Level 19 - Millasaurus", "Level 20 - One-turn Wonder", "Teaching Gandalf a Lesson",
            "What Do You Do With The Other Hand?", "Freelance Sorcerer, Works Weekends",
            "Should We Hire Commentators?", "Saltblasted For Your Talent", "Serra Angel Is Your Girlfriend", };

    // Cards - class uses data from here
    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public QuestUtilCards getCards() {
        return this.myCards;
    }

    /**
     * Gets the my decks.
     * 
     * @return the myDecks
     */
    public IStorage<Deck> getMyDecks() {
        return decks;
    }

    public QuestEvent getCurrentEvent() {
        return currentEvent;
    }
    public void setCurrentEvent(QuestEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    /**
     * Gets the precons.
     *
     * @return QuestPreconManager
     */
    public static IStorageView<PreconDeck> getPrecons() {
        return preconManager;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param selectedQuest
     */
    public void load(QuestData selectedQuest) {
        model = selectedQuest;
        // These are helper classes that hold no data.
        this.decks = model == null ? null : new QuestDeckMap(model.getAssets().myDecks);
        this.myCards = model == null ? null : new QuestUtilCards(this);
        currentEvent = null;
        
        QuestEventManager.INSTANCE.randomizeOpponents();
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void save() {
        if ( model != null )
            model.saveData();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isLoaded() {
        return false;
    }

    /**
     * Clear available challenges.
     */
    public void clearAvailableChallenges() {
        this.availableChallenges.clear();
    }

    /**
     * Gets the available challenges.
     * 
     * @return the available challenges
     */
    public List<Integer> getAvailableChallenges() {
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        if (this.availableQuests != null) {
            this.availableChallenges = this.availableQuests;
            this.availableQuests = null;
        }
    
        return this.availableChallenges != null ? new ArrayList<Integer>(this.availableChallenges) : null;
    }

    /**
     * Sets the available challenges.
     * 
     * @param list
     *            the new available challenges
     */
    public void setAvailableChallenges(final List<Integer> list) {
        this.availableChallenges = list;
    }

    /**
     * New game.
     * 
     * @param diff
     *            the diff
     * @param mode
     *            the mode
     * @param startPool
     *            the start type
     */
    public void newGame(final String name, final int diff, final QuestMode mode, final QuestStartPool startPool, final String preconName) {
        
        load(new QuestData(name, diff, mode));
        
        final Predicate<CardPrinted> filter;
        switch (startPool) {
            case PRECON:
                myCards.addPreconDeck(preconManager.get(preconName));
                return;
    
            case STANDARD:
                filter = Singletons.getModel().getFormats().getStandard().getFilterPrinted();
                break;
    
            default: //Unrestricted
                filter = CardPrinted.Predicates.Presets.IS_TRUE;
                break;
        }
    
        this.getAssets().setCredits(Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.STARTING_CREDITS, diff));
        this.myCards.setupNewGameCardPool(filter, diff);
    }

    /**
     * Gets the rank.
     * 
     * @return the rank
     */
    public String getRank() {
        int level = model.getAchievements().getLevel();
        if (level >= RANK_TITLES.length) {
            level = RANK_TITLES.length - 1;
        }
        return RANK_TITLES[level];
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public QuestAssets getAssets() {
        return model == null ? null : model.getAssets();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public String getName() {
        return model == null ? null : model.getName();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public QuestAchievements getAchievements() {
        return model == null ? null : model.getAchievements();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public QuestMode getMode() {
        return model.getMode();
    }
}
