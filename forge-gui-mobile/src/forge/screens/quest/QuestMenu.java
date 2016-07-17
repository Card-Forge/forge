package forge.screens.quest;

import java.io.File;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor.DeckController;
import forge.deck.FDeckEditor.EditorType;
import forge.interfaces.IButton;
import forge.interfaces.ICheckBox;
import forge.interfaces.IComboBox;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.quest.IVQuestStats;
import forge.quest.QuestUtil;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.HomeScreen;
import forge.screens.home.LoadGameMenu.LoadGameScreen;
import forge.screens.home.NewGameMenu.NewGameScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;

public class QuestMenu extends FPopupMenu implements IVQuestStats {
    private static final QuestMenu questMenu = new QuestMenu();
    private static final QuestBazaarScreen bazaarScreen = new QuestBazaarScreen();
    private static final QuestChallengesScreen challengesScreen = new QuestChallengesScreen();
    private static final QuestDecksScreen decksScreen = new QuestDecksScreen();
    private static final QuestDuelsScreen duelsScreen = new QuestDuelsScreen();
    private static final QuestPrefsScreen prefsScreen = new QuestPrefsScreen();
    private static final QuestSpellShopScreen spellShopScreen = new QuestSpellShopScreen();
    private static final QuestStatsScreen statsScreen = new QuestStatsScreen();
    private static final QuestTournamentsScreen tournamentsScreen = new QuestTournamentsScreen();

    private static final FMenuItem duelsItem = new FMenuItem("Duels", FSkinImage.QUEST_GEAR, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(duelsScreen);
        }
    });
    private static final FMenuItem challengesItem = new FMenuItem("Challenges", FSkinImage.QUEST_HEART, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(challengesScreen);
        }
    });
    private static final FMenuItem tournamentsItem = new FMenuItem("Tournaments", FSkinImage.PACK, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(tournamentsScreen);
        }
    });
    private static final FMenuItem decksItem = new FMenuItem("Quest Decks", FSkinImage.DECKLIST, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(decksScreen);
        }
    });
    private static final FMenuItem spellShopItem = new FMenuItem("Spell Shop", FSkinImage.QUEST_BOOK, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(spellShopScreen);
        }
    });
    private static final FMenuItem bazaarItem = new FMenuItem("Bazaar", FSkinImage.QUEST_BOTTLES, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(bazaarScreen);
        }
    });
    private static final FMenuItem statsItem = new FMenuItem("Statistics", FSkinImage.MULTI, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(statsScreen);
        }
    });
    private static final FMenuItem unlockSetsItem = new FMenuItem("Unlock Sets", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            ThreadUtil.invokeInGameThread(new Runnable() { //invoke in background thread so prompts can work
                @Override
                public void run() {
                    QuestUtil.chooseAndUnlockEdition();
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            updateCurrentQuestScreen();
                        }
                    });
                }
            });
        }
    });
    private static final FMenuItem travelItem = new FMenuItem("Travel", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            ThreadUtil.invokeInGameThread(new Runnable() { //invoke in background thread so prompts can work
                @Override
                public void run() {
                    QuestUtil.travelWorld();
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            updateCurrentQuestScreen();
                        }
                    });
                }
            });
        }
    });
    private static final FMenuItem prefsItem = new FMenuItem("Preferences", FSkinImage.SETTINGS, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(prefsScreen);
        }
    });

    static {
        statsScreen.addTournamentResultsLabels(tournamentsScreen);
    }

    private static void setCurrentScreen(FScreen screen0) {
        //make it so pressing Back from any screen besides Duels screen always goes to Duels screen
        //and make it so Duels screen always goes back to screen that launched Quest mode
        Forge.openScreen(screen0, Forge.getCurrentScreen() != duelsScreen);
    }

    private static void updateCurrentQuestScreen() {
        if (duelsItem.isSelected()) {
            duelsScreen.update();
        }
        else if (challengesItem.isSelected()) {
            challengesScreen.update();
        }
        else if (tournamentsItem.isSelected()) {
            tournamentsScreen.update();
        }
        else if (decksItem.isSelected()) {
            decksScreen.refreshDecks();
        }
        else if (spellShopItem.isSelected()) {
            spellShopScreen.update();
        }
        else if (statsItem.isSelected()) {
            statsScreen.update();
        }
    }

    static {
        //the first time quest mode is launched, add button for it if in Landscape mode
        if (Forge.isLandscapeMode()) {
            HomeScreen.instance.addButtonForMode("Quest Mode", new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    launchQuestMode(LaunchReason.StartQuestMode);
                }
            });
        }
    }

    public static QuestMenu getMenu() {
        return questMenu;
    }

    private QuestMenu() {
    }

    public enum LaunchReason {
        StartQuestMode,
        LoadQuest,
        NewQuest
    }

    public static void launchQuestMode(final LaunchReason reason) {
        //attempt to load current quest
        final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
        final String questname = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath(), questname);
        if (data.exists()) {
            LoadingOverlay.show("Loading current quest...", new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    FModel.getQuest().load(QuestDataIO.loadData(data));
                    ((DeckController<Deck>)EditorType.Quest.getController()).setRootFolder(FModel.getQuest().getMyDecks());
                    ((DeckController<DeckGroup>)EditorType.QuestDraft.getController()).setRootFolder(FModel.getQuest().getDraftDecks());
                    if (reason == LaunchReason.StartQuestMode) {
                        if (QuestUtil.getCurrentDeck() == null) {
                            Forge.openScreen(decksScreen); //if quest doesn't have a deck specified, open decks screen by default
                        }
                        else {
                            Forge.openScreen(duelsScreen); //TODO: Consider opening most recent quest view
                        }
                    }
                    else {
                        duelsScreen.update();
                        challengesScreen.update();
                        tournamentsScreen.update();
                        decksScreen.refreshDecks();
                        Forge.openScreen(duelsScreen);
                        if (reason == LaunchReason.NewQuest) {
                            LoadGameScreen.QuestMode.setAsBackScreen(true);
                        }
                    }
                }
            });
            return;
        }

        //if current quest can't be loaded, open New Quest or Load Quest screen based on whether a quest exists
        if (dirQuests.exists() && dirQuests.isDirectory() && dirQuests.list().length > 0) {
            LoadGameScreen.QuestMode.open();
        }
        else {
            NewGameScreen.QuestMode.open();
        }
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        addItem(duelsItem); duelsItem.setSelected(currentScreen == duelsScreen);
        addItem(challengesItem); challengesItem.setSelected(currentScreen == challengesScreen);
        addItem(tournamentsItem); tournamentsItem.setSelected(currentScreen == tournamentsScreen);
        addItem(decksItem); decksItem.setSelected(currentScreen == decksScreen);
        addItem(spellShopItem); spellShopItem.setSelected(currentScreen == spellShopScreen);
        addItem(bazaarItem); bazaarItem.setSelected(currentScreen == bazaarScreen);
        addItem(statsItem); statsItem.setSelected(currentScreen == statsScreen);
        addItem(unlockSetsItem);
        addItem(travelItem);
        addItem(prefsItem); prefsItem.setSelected(currentScreen == prefsScreen);
    }

    @Override
    public IButton getBtnRandomOpponent() {
        return duelsScreen.getBtnRandomOpponent();
    }

    @Override
    public IButton getBtnBazaar() {
        return bazaarItem;
    }

    @Override
    public IButton getBtnSpellShop() {
        return spellShopItem;
    }

    @Override
    public IButton getBtnUnlock() {
        return unlockSetsItem;
    }

    @Override
    public IButton getBtnTravel() {
        return travelItem;
    }

    @Override
    public IButton getLblCredits() {
        return statsScreen.getLblCredits();
    }

    @Override
    public IButton getLblLife() {
        return statsScreen.getLblLife();
    }

    @Override
    public IButton getLblWorld() {
        return statsScreen.getLblWorld();
    }

    @Override
    public IButton getLblWins() {
        return statsScreen.getLblWins();
    }

    @Override
    public IButton getLblLosses() {
        return statsScreen.getLblLosses();
    }

    @Override
    public IButton getLblNextChallengeInWins() {
        return Forge.getCurrentScreen() == challengesScreen ? challengesScreen.getLblNextChallengeInWins() : duelsScreen.getLblNextChallengeInWins();
    }

    @Override
    public IButton getLblCurrentDeck() {
        return Forge.getCurrentScreen() == challengesScreen ? challengesScreen.getLblCurrentDeck() : duelsScreen.getLblCurrentDeck();
    }

    @Override
    public IButton getLblWinStreak() {
        return statsScreen.getLblWinStreak();
    }

    @Override
    public IComboBox<String> getCbxPet() {
        return statsScreen.getCbxPet();
    }

    @Override
    public ICheckBox getCbPlant() {
        return statsScreen.getCbPlant();
    }

    @Override
    public IComboBox<String> getCbxMatchLength() {
        return statsScreen.getCbxMatchLength();
    }

    @Override
    public IButton getLblZep() {
        return statsScreen.getLblZep();
    }

    @Override
    public boolean isChallengesView() {
        return Forge.getCurrentScreen() == challengesScreen || Forge.getCurrentScreen() == statsScreen; //treat stats screen as challenges view so Zeppelin shows up 
    }

    @Override
    public boolean allowHtml() {
        return false;
    }

    public static void showSpellShop() {
        Forge.openScreen(spellShopScreen);
    }

    public static void showBazaar() {
        Forge.openScreen(bazaarScreen);
    }
}
