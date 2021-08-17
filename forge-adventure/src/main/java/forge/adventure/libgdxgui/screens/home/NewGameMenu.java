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
import forge.adventure.libgdxgui.screens.constructed.ConstructedScreen;
import forge.adventure.libgdxgui.screens.gauntlet.NewGauntletScreen;
import forge.adventure.libgdxgui.screens.home.puzzle.PuzzleScreen;
import forge.adventure.libgdxgui.screens.limited.NewDraftScreen;
import forge.adventure.libgdxgui.screens.limited.NewSealedScreen;
import forge.adventure.libgdxgui.screens.planarconquest.NewConquestScreen;
import forge.adventure.libgdxgui.screens.quest.NewQuestScreen;
import forge.adventure.libgdxgui.toolbox.FEvent;
import forge.adventure.libgdxgui.toolbox.FEvent.FEventHandler;
import forge.util.Localizer;

public class NewGameMenu extends FPopupMenu {
    final static Localizer localizer = Localizer.getInstance();

    public enum NewGameScreen {
        Constructed(localizer.getMessageorUseDefault("lblConstructed", "Constructed"), FSkinImage.MENU_CONSTRUCTED, ConstructedScreen.class),
        BoosterDraft(localizer.getMessageorUseDefault("lblBoosterDraft", "Booster Draft"), FSkinImage.MENU_DRAFT, NewDraftScreen.class),
        SealedDeck(localizer.getMessageorUseDefault("lblSealedDeck", "Sealed Deck"), FSkinImage.MENU_SEALED, NewSealedScreen.class),
        QuestMode(localizer.getMessageorUseDefault("lblQuestMode", "Quest Mode"), FSkinImage.QUEST_ZEP, NewQuestScreen.class),
        PuzzleMode(localizer.getMessageorUseDefault("lblPuzzleMode", "Puzzle Mode"), FSkinImage.MENU_PUZZLE, PuzzleScreen.class),
        PlanarConquest(localizer.getMessageorUseDefault("lblPlanarConquest", "Planar Conquest"), FSkinImage.MENU_GALAXY, NewConquestScreen.class),
        Gauntlet(localizer.getMessageorUseDefault("lblGauntlet", "Gauntlet"), FSkinImage.MENU_GAUNTLET, NewGauntletScreen.class);

        private final FMenuItem item;
        private final Class<? extends FScreen> screenClass;
        private FScreen screen;

        NewGameScreen(final String caption0, final FImage icon0, final Class<? extends FScreen> screenClass0) {
            screenClass = screenClass0;
            item = new FMenuItem(caption0, icon0, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    open(true); //remove current screen from chain
                    setPreferredScreen(NewGameScreen.this);
                }
            });
        }
        
        private void initializeScreen() {
            if (screen == null) { //don't initialize screen until it's opened the first time
                try {
                    screen = screenClass.newInstance();
                    screen.setHeaderCaption(localizer.getMessageorUseDefault("lblNewGame", "New Game") + " - " + item.getText());
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
    private static final NewGameMenu menu = new NewGameMenu();
    private static NewGameScreen preferredScreen;

    static {
        try {
            preferredScreen = NewGameScreen.valueOf(prefs.getPref(FPref.NEW_GAME_SCREEN));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            preferredScreen = NewGameScreen.Constructed;
            prefs.setPref(FPref.NEW_GAME_SCREEN, preferredScreen.name());
            prefs.save();
        }
    }

    public static NewGameScreen getPreferredScreen() {
        return preferredScreen;
    }
    public static void setPreferredScreen(NewGameScreen preferredScreen0) {
        if (preferredScreen == preferredScreen0) { return; }
        preferredScreen = preferredScreen0;
        prefs.setPref(FPref.NEW_GAME_SCREEN, preferredScreen.name());
        prefs.save();
    }

    public static NewGameMenu getMenu() {
        return menu;
    }

    private NewGameMenu() {
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        for (NewGameScreen ngs : NewGameScreen.values()) {
            addItem(ngs.item);
            ngs.item.setSelected(currentScreen == ngs.screen);
        }
    }
}
