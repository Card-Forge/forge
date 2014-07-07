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
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.screens.LoadingOverlay;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

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

    private static final FMenuItem unlockSetsItem = new FMenuItem("Unlock Sets", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
        }
    });
    private static final FMenuItem travelItem = new FMenuItem("Travel", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
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

    public static QuestMenu getMenu() {
        return questMenu;
    }

    private QuestMenu() {
    }

    public static void launchQuestMode() {
        //attempt to load current quest
        final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
        final String questname = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath(), questname);
        if (data.exists()) {
            LoadingOverlay.show("Loading current quest...", new Runnable() {
                @Override
                public void run() {
                    FModel.getQuest().load(QuestDataIO.loadData(data));
                    Forge.openScreen(duelsScreen); //TODO: Consider opening most recent quest view
                }
            });
            return;
        }

        //if current quest can't be loaded, open Load Quest screen
        Forge.openScreen(new LoadQuestScreen());
    }

    @Override
    protected void buildMenu() {
        addItem(new FMenuItem("Duels", FSkinImage.QUEST_GEAR, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(duelsScreen);
            }
        }));
        addItem(new FMenuItem("Challenges", FSkinImage.QUEST_HEART, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(challengesScreen);
            }
        }));
        addItem(new FMenuItem("Tournaments", FSkinImage.PACK, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(tournamentsScreen);
            }
        }));
        addItem(unlockSetsItem);
        addItem(travelItem);
        addItem(spellShopItem);
        addItem(bazaarItem);
        addItem(new FMenuItem("Statistics", FSkinImage.MULTI, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(statsScreen);
            }
        }));
        addItem(new FMenuItem("Change Deck", FSkinImage.DECKLIST, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(decksScreen);
            }
        }));
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
        return duelsScreen.getLblNextChallengeInWins();
    }

    @Override
    public IButton getLblCurrentDeck() {
        return duelsScreen.getLblCurrentDeck();
    }

    @Override
    public IButton getLblWinStreak() {
        return statsScreen.getLblWinStreak();
    }

    @Override
    public IComboBox<String> getCbxPet() {
        return duelsScreen.getCbxPet();
    }

    @Override
    public ICheckBox getCbPlant() {
        return duelsScreen.getCbPlant();
    }

    @Override
    public ICheckBox getCbCharm() {
        return duelsScreen.getCbCharm();
    }

    @Override
    public IButton getLblZep() {
        return duelsScreen.getLblZep();
    }

    @Override
    public boolean isChallengesView() {
        return Forge.getCurrentScreen() == challengesScreen;
    }
}
