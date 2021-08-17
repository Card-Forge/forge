package forge.adventure.libgdxgui.screens.home;

import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.assets.FImage;
import forge.adventure.libgdxgui.assets.FSkinImage;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.adventure.libgdxgui.menu.FMenuItem;
import forge.adventure.libgdxgui.menu.FPopupMenu;
import forge.model.FModel;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.screens.gauntlet.LoadGauntletScreen;
import forge.adventure.libgdxgui.screens.limited.LoadDraftScreen;
import forge.adventure.libgdxgui.screens.limited.LoadSealedScreen;
import forge.adventure.libgdxgui.screens.planarconquest.LoadConquestScreen;
import forge.adventure.libgdxgui.screens.quest.LoadQuestScreen;
import forge.adventure.libgdxgui.toolbox.FEvent;
import forge.adventure.libgdxgui.toolbox.FEvent.FEventHandler;
import forge.util.Localizer;

public class LoadGameMenu extends FPopupMenu {
    public enum LoadGameScreen {
        BoosterDraft("lblBoosterDraft", FSkinImage.MENU_DRAFT, LoadDraftScreen.class),
        SealedDeck("lblSealedDeck", FSkinImage.MENU_SEALED, LoadSealedScreen.class),
        QuestMode("lblQuestMode", FSkinImage.QUEST_ZEP, LoadQuestScreen.class),
        PlanarConquest("lblPlanarConquest", FSkinImage.MENU_GALAXY, LoadConquestScreen.class),
        Gauntlet("lblGauntlet", FSkinImage.MENU_GAUNTLET, LoadGauntletScreen.class);

        private final FMenuItem item;
        private final Class<? extends FScreen> screenClass;
        private FScreen screen;

        LoadGameScreen(final String caption0, final FImage icon0, final Class<? extends FScreen> screenClass0) {
            screenClass = screenClass0;
            item = new FMenuItem(Localizer.getInstance().getMessage(caption0), icon0, new FEventHandler() {
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
                    screen.setHeaderCaption(Localizer.getInstance().getMessage("lblLoadGame") + " - " + item.getText());
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
