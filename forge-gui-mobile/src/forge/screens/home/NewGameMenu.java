package forge.screens.home;

import forge.Forge;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.constructed.ConstructedScreen;
import forge.screens.gauntlet.NewGauntletScreen;
import forge.screens.home.puzzle.PuzzleScreen;
import forge.screens.limited.NewDraftScreen;
import forge.screens.limited.NewSealedScreen;
import forge.screens.planarconquest.NewConquestScreen;
import forge.screens.quest.NewQuestScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class NewGameMenu extends FPopupMenu {

    public enum NewGameScreen {
        Constructed(Forge.getLocalizer().getMessageorUseDefault("lblConstructed", "Constructed"), FSkinImage.MENU_CONSTRUCTED, ConstructedScreen.class),
        BoosterDraft(Forge.getLocalizer().getMessageorUseDefault("lblBoosterDraft", "Booster Draft"), FSkinImage.MENU_DRAFT, NewDraftScreen.class),
        SealedDeck(Forge.getLocalizer().getMessageorUseDefault("lblSealedDeck", "Sealed Deck"), FSkinImage.MENU_SEALED, NewSealedScreen.class),
        QuestMode(Forge.getLocalizer().getMessageorUseDefault("lblQuestMode", "Quest Mode"), FSkinImage.QUEST_ZEP, NewQuestScreen.class),
        PuzzleMode(Forge.getLocalizer().getMessageorUseDefault("lblPuzzleMode", "Puzzle Mode"), FSkinImage.MENU_PUZZLE, PuzzleScreen.class),
        PlanarConquest(Forge.getLocalizer().getMessageorUseDefault("lblPlanarConquest", "Planar Conquest"), FSkinImage.MENU_GALAXY, NewConquestScreen.class),
        Gauntlet(Forge.getLocalizer().getMessageorUseDefault("lblGauntlet", "Gauntlet"), FSkinImage.MENU_GAUNTLET, NewGauntletScreen.class);

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
                    screen = screenClass.getConstructor().newInstance();
                    screen.setHeaderCaption(Forge.getLocalizer().getMessageorUseDefault("lblNewGame", "New Game") + " - " + item.getText());
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
            if (screen != null)
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
            if (NewGameScreen.Constructed != null) { //if this is null it means newgamescreen is not initialized
                preferredScreen = NewGameScreen.Constructed;
                prefs.setPref(FPref.NEW_GAME_SCREEN, preferredScreen.name());
                prefs.save();
            }
        }
    }

    public static NewGameScreen getPreferredScreen() {
        if (preferredScreen == null) //shouldn't happen but we return constructed as default
            return NewGameScreen.Constructed;
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
