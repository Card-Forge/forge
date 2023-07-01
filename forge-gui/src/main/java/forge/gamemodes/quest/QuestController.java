/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.gamemodes.quest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

import forge.card.CardEdition;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.game.event.GameEvent;
import forge.game.event.GameEventMulligan;
import forge.gamemodes.quest.bazaar.QuestBazaarManager;
import forge.gamemodes.quest.bazaar.QuestItemType;
import forge.gamemodes.quest.bazaar.QuestPetStorage;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gamemodes.quest.data.GameFormatQuest;
import forge.gamemodes.quest.data.QuestAchievements;
import forge.gamemodes.quest.data.QuestAssets;
import forge.gamemodes.quest.data.QuestData;
import forge.gamemodes.quest.data.QuestPreferences.DifficultyPrefs;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gamemodes.quest.data.StarRating;
import forge.gamemodes.quest.io.QuestChallengeReader;
import forge.item.PreconDeck;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

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
    private QuestUtilCards myCards;

    private GameFormatQuest questFormat;

    private QuestEvent currentEvent;

    /** The decks. */
    private transient IStorage<Deck> decks;

    private QuestEventDuelManagerInterface duelManager = null;
    private IStorage<QuestEventChallenge> allChallenges = null;

    private QuestBazaarManager bazaar = null;

    private QuestPetStorage pets = null;

    // This is used by shop. Had no idea where else to place this
    private static transient IStorage<PreconDeck> preconManager = null;

    private transient IStorage<DeckGroup> draftDecks;


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

    /** */
    public static final int MAX_PET_SLOTS = 2;

    public QuestController() {
    }

    /**
     *
     * TODO: Write javadoc for this method.
     * @param slot &emsp; int
     * @param name &emsp; String
     */
    public void selectPet(Integer slot, String name) {
        if (this.model != null) {
                this.model.getPetSlots().put(slot, name);
        }
    }

    public void setMatchLength(String len) {
        if (this.model != null) {
            this.model.setMatchLength(Integer.parseInt(len));
        }
    }

    public int getMatchLength() {
        return this.model == null ? 3 : this.model.getMatchLength();
    }

    /**
     *
     * @param slot &emsp; int
     * @return String
     */
    public String getSelectedPet(Integer slot) {
        return this.model == null ? null : this.model.getPetSlots().get(slot);
    }

    // Cards - class uses data from here
    /**
     * Gets the cards.
     *
     * @return the cards
     */
    public QuestUtilCards getCards() {
        return this.myCards;
    }

    // Set the card's custom rating to N stars
    public void SetRating(String name, String edition, int n) {
        StarRating r = new StarRating();
        r.Name = name;
        r.Edition = edition;
        for (int i = 0; i < 6; i++) {
            r.rating = i;
            model.Ratings.remove(r);
        }

        r.rating = n;
        if (n != 0) {
            model.Ratings.add(r);
        }
    }

    /**
     * Gets the my decks.
     *
     * @return the myDecks
     */
    public IStorage<Deck> getMyDecks() {
        return this.decks;
    }

    public IStorage<DeckGroup> getDraftDecks() {
        if (draftDecks == null) {
            draftDecks = new QuestDeckGroupMap(new HashMap<>());
        }
        final QuestAchievements achievements = this.getAchievements();
        if (achievements != null && (achievements.getCurrentDraftIndex() == -1 || achievements.getCurrentDraft() == null)) {
            draftDecks.delete(QuestEventDraft.DECK_NAME);
        }
        return draftDecks;
    }

    /**
     * Gets the current format if any.
     *
     * @return GameFormatQuest, the game format (if persistent).
     */
    public GameFormatQuest getFormat() {

        return (getWorldFormat() == null ? this.questFormat : getWorldFormat());
    }

    /**
     * Gets the custom format for the main world, if any.
     */
    public GameFormatQuest getMainFormat() {
        return this.questFormat;
    }

    /**
     * Gets the current event.
     *
     * @return the current event
     */
    public QuestEvent getCurrentEvent() {
        return this.currentEvent;
    }

    /**
     * Sets the current event.
     *
     * @param currentEvent the new current event
     */
    public void setCurrentEvent(final QuestEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    public static IStorage<PreconDeck> getPrecons() {
        if (null == preconManager) {
            // read with a special class, that will fill sell rules as it processes each PreconDeck
            preconManager = new StorageBase<>("Quest shop decks", new PreconDeck.Reader(new File(ForgeConstants.QUEST_PRECON_DIR)) {
                @Override
                protected PreconDeck getPreconDeckFromSections(java.util.Map<String, java.util.List<String>> sections) {
                    PreconDeck result = super.getPreconDeckFromSections(sections);
                    preconDeals.put(result.getName(), new SellRules(sections.get("shop")));
                    return result;
                }
            });
        }
        return QuestController.preconManager;
    }
    private final static Map<String, SellRules> preconDeals = new TreeMap<>();
    public static SellRules getPreconDeals(PreconDeck deck) {
        return preconDeals.get(deck.getName());
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @param selectedQuest the selected quest
     */
    public void load(final QuestData selectedQuest) {
        this.model = selectedQuest;
        // These are helper classes that hold no data.
        this.decks = this.model == null ? null : this.model.getAssets().getDeckStorage();
        this.myCards = this.model == null ? null : new QuestUtilCards(this);
        this.questFormat = this.model == null ? null : this.model.getFormat();
        this.currentEvent = null;

        this.draftDecks = this.model == null ? null : this.model.getAssets().getDraftDeckStorage();

        this.resetDuelsManager();
        this.resetChallengesManager();
        this.getDuelsManager().randomizeOpponents();
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void save() {
        if (this.model != null) {
            this.model.saveData();
        }
    }

    /**
     * New game.
     *
     * @param name the name
     * @param difficulty
     *      the difficulty
     * @param mode the mode
     * @param formatPrizes
     *          prize boosters format
     * @param allowSetUnlocks
     *      allow unlocking of sets
     * @param startingCards
     *      the starting deck
     * @param formatStartingPool
     *      format used for the starting pool
     * @param startingWorld
     *      starting world
     * @param userPrefs
     *      user preferences
     */
    public void newGame(final String name, final int difficulty, final QuestMode mode,
            final GameFormat formatPrizes, final boolean allowSetUnlocks,
            final Deck startingCards, final GameFormat formatStartingPool,
            final String startingWorld, final StartingPoolPreferences userPrefs,
            DeckConstructionRules dcr) {

        this.load(new QuestData(name, difficulty, mode, formatPrizes, allowSetUnlocks, startingWorld, dcr)); // pass awards and unlocks here

        if (startingCards != null) {
            this.myCards.addDeck(startingCards);
        } else {
            this.myCards.setupNewGameCardPool(formatStartingPool, difficulty, userPrefs);
        }

        this.getAssets().setCredits(FModel.getQuestPreferences().getPrefInt(DifficultyPrefs.STARTING_CREDITS, difficulty));

        // Reset starting cards here.
        this.myCards.resetNewList();
    }

    /**
     * Gets the rank.
     *
     * @return the rank
     */
    public String getRank() {
        return getRank(getLevel());
    }

    public String getRank(int level) {
        if (level >= QuestController.RANK_TITLES.length) {
            level = QuestController.RANK_TITLES.length - 1;
        }
        return QuestController.RANK_TITLES[level];
    }

    public int getLevel() {
        return this.model.getAchievements().getLevel();
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @return the assets
     */
    public QuestAssets getAssets() {
        return this.model == null ? null : this.model.getAssets();
    }

    /**
     * Gets the QuestWorld, if any.
     *
     * @return QuestWorld or null, if using regular duels and challenges.
     */
    public QuestWorld getWorld() {
        return this.model == null || this.model.getWorldId() == null ? null : FModel.getWorlds().get(this.model.getWorldId());
    }

    /**
     * Sets a new QuestWorld.
     *
     * @param newWorld
     *      string, the new world id
     */
    public void setWorld(final QuestWorld newWorld) {
        if (this.model == null) {
            return;
        }

        this.model.setWorldId(newWorld == null ? null : newWorld.getName());
    }

    /**
     * Gets the QuestWorld Format, if any.
     *
     * @return GameFormatQuest or null.
     */
    public GameFormatQuest getWorldFormat() {
        if (this.model == null || this.model.getWorldId() == null) {
            return null;
        }

        final QuestWorld curQw = FModel.getWorlds().get(this.model.getWorldId());

        if (curQw == null) {
            return null;
        }

        return curQw.getFormat();
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @return the name
     */
    public String getName() {
        return this.model == null ? null : this.model.getName();
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @return the achievements
     */
    public QuestAchievements getAchievements() {
        return this.model == null ? null : this.model.getAchievements();
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @return the mode
     */
    public QuestMode getMode() {
        return this.model.getMode();
    }

    /**
     * Gets the bazaar.
     *
     * @return the bazaar
     */
    public final QuestBazaarManager getBazaar() {
        if (null == this.bazaar) {
            this.bazaar = new QuestBazaarManager(new File(ForgeConstants.BAZAAR_INDEX_FILE));
        }
        return this.bazaar;
    }

    /**
     * Gets the event manager.
     *
     * @return the event manager
     */
    public QuestEventDuelManagerInterface getDuelsManager() {
        if (this.duelManager == null) {
            resetDuelsManager();
        }
        return this.duelManager;
    }

    /**
     *
     * TODO: Write javadoc for this method.
     * @return QuestEventManager
     */
    public IStorage<QuestEventChallenge> getChallenges() {
        if (this.allChallenges == null) {
            resetChallengesManager();
        }
        return this.allChallenges;
    }

    /**
     *
     * Reset the duels manager.
     */
    public void resetDuelsManager() {

        QuestWorld world = getWorld();
        String path = ForgeConstants.DEFAULT_CHALLENGES_DIR;

        //Use a variant specialized duel manager if this is a variant quest
        if (FModel.getQuest() != null && FModel.getQuest().getDeckConstructionRules() != null) {
            switch(FModel.getQuest().getDeckConstructionRules()){
                case Default: break;
                case Commander: this.duelManager = new QuestEventCommanderDuelManager(); return;
            }
        }

        if (world != null) {
            if (world.getName().equals(QuestWorld.STANDARDWORLDNAME)) {
                this.duelManager = new QuestEventLDADuelManager(FModel.getFormats().getStandard());
                return;
            } else if (world.getName().equals(QuestWorld.PIONEERWORLDNAME)) {
                this.duelManager = new QuestEventLDADuelManager(FModel.getFormats().getPioneer());
                return;
            }else if (world.getName().equals(QuestWorld.MODERNWORLDNAME)) {
                this.duelManager = new QuestEventLDADuelManager(FModel.getFormats().getModern());
                return;
            }else if (world.isCustom()) {
                path = world.getDuelsDir() == null ? ForgeConstants.DEFAULT_DUELS_DIR : ForgeConstants.USER_QUEST_WORLD_DIR + world.getDuelsDir();
                this.duelManager = new QuestEventDuelManager(new File(path));
            } else {
                path = world.getDuelsDir() == null ? ForgeConstants.DEFAULT_DUELS_DIR : ForgeConstants.QUEST_WORLD_DIR + world.getDuelsDir();
                if(QuestWorld.MAINWORLDNAME.equals(world.getName())) {
                    this.duelManager = new MainWorldEventDuelManager(new File(path));                    
                } else {
                    this.duelManager = new QuestEventDuelManager(new File(path));                                
                }
            }
        } else {
            this.duelManager = new QuestEventDuelManager(new File(path));            
        }

    }

    public HashSet<StarRating> GetRating() {
        if (model == null) return null;
        return model.Ratings;
    }

    /**
     *
     * Reset the challenges manager.
     */
    public void resetChallengesManager() {

        QuestWorld world = getWorld();
        String path = ForgeConstants.DEFAULT_CHALLENGES_DIR;

        if (world != null) {

            if (world.getName().equals(QuestWorld.STANDARDWORLDNAME)) {
                QuestChallengeGenerator gen = new QuestChallengeGenerator(FModel.getFormats().getStandard());
                allChallenges = gen.generateChallenges();
                return;
            }else if (world.getName().equals(QuestWorld.STANDARDWORLDNAME)) {
                QuestChallengeGenerator gen = new QuestChallengeGenerator(FModel.getFormats().getModern());
                allChallenges = gen.generateChallenges();
                return;
            } else if (world.isCustom()) {
                path = world.getChallengesDir() == null ? ForgeConstants.DEFAULT_CHALLENGES_DIR : ForgeConstants.USER_QUEST_WORLD_DIR + world.getChallengesDir();
            } else {
                path = world.getChallengesDir() == null ? ForgeConstants.DEFAULT_CHALLENGES_DIR : ForgeConstants.QUEST_WORLD_DIR + world.getChallengesDir();
            }

        }

        this.allChallenges = new StorageBase<>("Quest Challenges", new QuestChallengeReader(new File(path)));

    }

    /**
     *
     * TODO: Write javadoc for this method.
     * @return QuestPetStorage
     */
    public QuestPetStorage getPetsStorage() {
        if (this.pets == null) {
            this.pets = new QuestPetStorage(new File(ForgeConstants.BAZAAR_INDEX_FILE));
        }

        return this.pets;
    }

    /**
     * Quest format has unlockable sets available at the moment.
     * @return int number of unlockable sets.
     */
    public int getUnlocksTokens() {
        if (this.questFormat == null || !this.questFormat.canUnlockSets()) {
            return 0;
        }

        final int wins = this.model.getAchievements().getWin();

        int cntLocked = this.questFormat.getLockedSets().size();
        int unlocksAvaliable = wins / FModel.getQuestPreferences().getPrefInt(QPref.WINS_UNLOCK_SET);
        int unlocksSpent = this.questFormat.getUnlocksUsed();

        return unlocksAvaliable > unlocksSpent ? Math.min(unlocksAvaliable - unlocksSpent, cntLocked) : 0;
    }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) { // Receives events only during quest games
        if (ev instanceof GameEventMulligan) {
            GameEventMulligan mev = (GameEventMulligan) ev;
            // First mulligan is free
            if (mev.player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()
                    && getAssets().hasItem(QuestItemType.SLEIGHT) && mev.player.getStats().getMulliganCount() < 7) {
                mev.player.drawCard();
            }
        }
    }


    public int getTurnsToUnlockChallenge() {
    	int turns = FModel.getQuestPreferences().getPrefInt(QPref.WINS_NEW_CHALLENGE);

        if (FModel.getQuest().getAssets().hasItem(QuestItemType.ZEPPELIN)) {
        	turns -= 2;
        }
        // User may have MAP and ZEPPELIN, so MAP must be tested second.
        else if (FModel.getQuest().getAssets().hasItem(QuestItemType.MAP)) {
        	turns -= 1;
        }

        return Math.max(turns, 1);
    }


    public final void regenerateChallenges() {
        final QuestAchievements achievements = model.getAchievements();
        final List<String> unlockedChallengeIds = new ArrayList<>();
        final List<String> availableChallengeIds = achievements.getCurrentChallenges();

        // clean up challenges potentially coming over from a different quest world
        List<String> nonExistentIds = new ArrayList<>();
        for (String cid : availableChallengeIds) {
            if (this.getChallenges().get(cid) == null) {
                System.out.println("Warning: removing a challenge that does not exist in the current quest world: " + cid);
                nonExistentIds.add(cid);
            }
        }
        availableChallengeIds.removeAll(nonExistentIds);

        int maxChallenges = achievements.getWin() / getTurnsToUnlockChallenge() - achievements.getChallengesPlayed();
        if (maxChallenges > 5) {
            maxChallenges = 5;
        }

        // Generate IDs as needed.
        if (achievements.getCurrentChallenges().size() < maxChallenges) {
            for (final QuestEventChallenge qc : allChallenges) {
                if (qc.getWinsReqd() > achievements.getWin()) {
                    continue;
                }
                if (!qc.isRepeatable() && achievements.getLockedChallenges().contains(qc.getId())) {
                    continue;
                }
                if (!availableChallengeIds.contains(qc.getId())) {
                    unlockedChallengeIds.add(qc.getId());
                }
            }

            Collections.shuffle(unlockedChallengeIds);

            maxChallenges = Math.min(maxChallenges, unlockedChallengeIds.size());

            for (int i = availableChallengeIds.size(); i < maxChallenges; i++) {
                availableChallengeIds.add(unlockedChallengeIds.get(i));
            }
        }

        achievements.setCurrentChallenges(availableChallengeIds);
        save();
    }

    public CardEdition getDefaultLandSet() {
        List<String> availableEditionCodes = questFormat != null ? questFormat.getAllowedSetCodes() : Lists.newArrayList(FModel.getMagicDb().getEditions().getItemNames());
        List<CardEdition> availableEditions = new ArrayList<>();

        for (String s : availableEditionCodes) {
            availableEditions.add(FModel.getMagicDb().getEditions().get(s));
        }

        CardEdition randomLandSet = CardEdition.Predicates.getRandomSetWithAllBasicLands(availableEditions);
        return randomLandSet == null ? FModel.getMagicDb().getEditions().get("ZEN") : randomLandSet;
    }

    public String getCurrentDeck() {
        return model.currentDeck;
    }

    public void setCurrentDeck(String s) {
        model.currentDeck = s;
    }

    public DeckConstructionRules getDeckConstructionRules(){
        if (model == null) {
            return null;
        }
        return model.deckConstructionRules;
    }
}
