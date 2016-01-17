package forge.screens.home;

import forge.Forge;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.gauntlet.LoadGauntletScreen;
import forge.screens.limited.LoadDraftScreen;
import forge.screens.limited.LoadSealedScreen;
import forge.screens.planarconquest.LoadConquestScreen;
import forge.screens.quest.LoadQuestScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class LoadGameMenu extends FPopupMenu {
    public enum LoadGameScreen {
        BoosterDraft("Booster Draft", FSkinImage.HAND, LoadDraftScreen.class),
        SealedDeck("Sealed Deck", FSkinImage.PACK, LoadSealedScreen.class),
        QuestMode("Quest Mode", FSkinImage.QUEST_ZEP, LoadQuestScreen.class),
        PlanarConquest("Planar Conquest", FSkinImage.MULTIVERSE, LoadConquestScreen.class),
        Gauntlet("Gauntlet", FSkinImage.ALPHASTRIKE, LoadGauntletScreen.class);
 
        private final FMenuItem item;
        private final Class<? extends FScreen> screenClass;
        private FScreen screen;

        private LoadGameScreen(final String caption0, final FImage icon0, final Class<? extends FScreen> screenClass0) {
            screenClass = screenClass0;
            item = new FMenuItem(caption0, icon0, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    Forge.back(); //remove current screen from chain
                    open();
                    setPreferredScreen(LoadGameScreen.this);
                }
            });
        }

        private void initializeScreen() {
            if (screen == null) { //don't initialize screen until it's opened the first time
                try {
                    screen = screenClass.newInstance();
                    screen.setHeaderCaption("Load Game - " + item.getText());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        public void open() {
            initializeScreen();
            Forge.openScreen(screen);
        }

        public void setAsBackScreen(boolean replace) {
            initializeScreen();
            Forge.setBackScreen(screen, replace);
        }
    }

    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final LoadGameMenu menu = new LoadGameMenu();
    private static LoadGameScreen preferredScreen;

    static {
        try {
            preferredScreen = LoadGameScreen.valueOf(prefs.getPref(FPref.LOAD_GAME_SCREEN));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            preferredScreen = LoadGameScreen.BoosterDraft;
            prefs.setPref(FPref.LOAD_GAME_SCREEN, preferredScreen.name());
            prefs.save();
        }
    }

    public static LoadGameScreen getPreferredScreen() {
        return preferredScreen;
    }
    public static void setPreferredScreen(LoadGameScreen preferredScreen0) {
        if (preferredScreen == preferredScreen0) { return; }
        preferredScreen = preferredScreen0;
        prefs.setPref(FPref.LOAD_GAME_SCREEN, preferredScreen.name());
        prefs.save();
    }

    public static LoadGameMenu getMenu() {
        return menu;
    }

    private LoadGameMenu() {
        //LoadGameScreen.PlanarConquest.item.setVisible(false);
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        for (LoadGameScreen lgs : LoadGameScreen.values()) {
            addItem(lgs.item);
            lgs.item.setSelected(currentScreen == lgs.screen);
        }
    }
}
