package forge.screens.quest;

import java.io.File;

import forge.Forge;
import forge.assets.FSkinImage;
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
            Forge.openScreen(duelsScreen);
        }
    });
    private static final FMenuItem challengesItem = new FMenuItem("Challenges", FSkinImage.QUEST_HEART, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(challengesScreen);
        }
    });
    private static final FMenuItem tournamentsItem = new FMenuItem("Tournaments", FSkinImage.PACK, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(tournamentsScreen);
        }
    });
    private static final FMenuItem decksItem = new FMenuItem("Quest Decks", FSkinImage.DECKLIST, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(decksScreen);
        }
    });
    private static final FMenuItem spellShopItem = new FMenuItem("Spell Shop", FSkinImage.QUEST_BOOK, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(spellShopScreen);
        }
    });
    private static final FMenuItem bazaarItem = new FMenuItem("Bazaar", FSkinImage.QUEST_BOTTLES, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(bazaarScreen);
        }
    });
    private static final FMenuItem statsItem = new FMenuItem("Statistics", FSkinImage.MULTI, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(statsScreen);
        }
    });
    private static final FMenuItem unlockSetsItem = new FMenuItem("Unlock Sets", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            ThreadUtil.invokeInGameThread(new Runnable() { //invoke in background thread so prompts can work
                @Override
                public void run() {
                    QuestUtil.chooseAndUnlockEdition();
                    updateCurrentQuestScreen();
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
                    updateCurrentQuestScreen();
                }
            });
        }
    });

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
                public void run() {
                    FModel.getQuest().load(QuestDataIO.loadData(data));
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
                        if (reason == LaunchReason.LoadQuest) {
                            Forge.back();
                            if (Forge.onHomeScreen()) { //open duels screen if Load Quest screen was opening direct from home screen
                                Forge.openScreen(duelsScreen);
                            }
                        }
                        else {
                            Forge.back();
                            if (Forge.getCurrentScreen() instanceof LoadQuestScreen) {
                                Forge.back(); //remove LoadQuestScreen from screen stack
                            }
                            if (Forge.onHomeScreen()) { //open duels screen if New Quest screen was opening direct from home screen
                                Forge.openScreen(duelsScreen);
                            }
                            if (Forge.getCurrentScreen() != decksScreen) {
                                Forge.openScreen(decksScreen); //open deck screen for new quest
                            }
                        }
                    }
                }
            });
            return;
        }

        //if current quest can't be loaded, open New Quest or Load Quest screen based on whether a quest exists
        if (dirQuests.exists() && dirQuests.isDirectory() && dirQuests.list().length > 0) {
            Forge.openScreen(new LoadQuestScreen());
        }
        else {
            Forge.openScreen(new NewQuestScreen());
        }
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        addItem(duelsItem); duelsItem.setSelected(currentScreen == duelsScreen);
        addItem(challengesItem); challengesItem.setSelected(currentScreen == challengesScreen);
        //addItem(tournamentsItem); tournamentsItem.setSelected(currentScreen == tournamentsScreen);
        addItem(decksItem); decksItem.setSelected(currentScreen == decksScreen);
        addItem(spellShopItem); spellShopItem.setSelected(currentScreen == spellShopScreen);
        addItem(bazaarItem); bazaarItem.setSelected(currentScreen == bazaarScreen);
        addItem(statsItem); statsItem.setSelected(currentScreen == statsScreen);
        addItem(unlockSetsItem);
        addItem(travelItem);
        addItem(new FMenuItem("New Quest", FSkinImage.NEW, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new NewQuestScreen());
            }
        }));
        addItem(new FMenuItem("Load Quest", FSkinImage.OPEN, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new LoadQuestScreen());
            }
        }));
        addItem(new FMenuItem("Preferences", FSkinImage.SETTINGS, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(prefsScreen);
            }
        }));
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
    public ICheckBox getCbCharm() {
        return statsScreen.getCbCharm();
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
