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
import forge.util.Localizer;

public class LoadGameMenu extends FPopupMenu {
    final static Localizer localizer = Localizer.getInstance();

    public enum LoadGameScreen {
        BoosterDraft(localizer.getMessage("lblBoosterDraft"), FSkinImage.HAND, LoadDraftScreen.class),
        SealedDeck(localizer.getMessage("lblSealedDeck"), FSkinImage.PACK, LoadSealedScreen.class),
        QuestMode(localizer.getMessage("lblQuestMode"), FSkinImage.QUEST_ZEP, LoadQuestScreen.class),
        PlanarConquest(localizer.getMessage("lblPlanarConquest"), FSkinImage.MULTIVERSE, LoadConquestScreen.class),
        Gauntlet(localizer.getMessage("lblGauntlet"), FSkinImage.ALPHASTRIKE, LoadGauntletScreen.class);
 
        private final FMenuItem item;
        private final Class<? extends FScreen> screenClass;
        private FScreen screen;

        LoadGameScreen(final String caption0, final FImage icon0, final Class<? extends FScreen> screenClass0) {
            screenClass = screenClass0;
            item = new FMenuItem(caption0, icon0, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    open(true); //remove current screen from chain
                    setPreferredScreen(LoadGameScreen.this);
                }
            });
        }

        private void initializeScreen() {
            if (screen == null) { //don't initialize screen until it's opened the first time
                try {
                    screen = screenClass.newInstance();
                    screen.setHeaderCaption(localizer.getMessage("lblLoadGame") + " - " + item.getText());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        public void open() {
            open(false);
        }
        private void open(boolean replaceBackScreen) {
            initializeScreen();
            Forge.openScreen(screen, replaceBackScreen);
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
