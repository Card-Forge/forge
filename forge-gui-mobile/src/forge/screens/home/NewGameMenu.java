package forge.screens.home;

import forge.Forge;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.constructed.ConstructedScreen;
import forge.screens.gauntlet.NewGauntletScreen;
import forge.screens.limited.NewDraftScreen;
import forge.screens.limited.NewSealedScreen;
import forge.screens.planarconquest.NewConquestScreen;
import forge.screens.quest.NewQuestScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class NewGameMenu extends FPopupMenu {
    private enum NewGameScreen {
        Constructed("Constructed", ConstructedScreen.class),
        BoosterDraft("Booster Draft", NewDraftScreen.class),
        SealedDeck("Sealed Deck", NewSealedScreen.class),
        QuestMode("Quest Mode", NewQuestScreen.class),
        PlanarConquest("Planar Conquest", NewConquestScreen.class),
        Gauntlet("Gauntlet", NewGauntletScreen.class);
 
        private final FMenuItem item;
        private final Class<? extends FScreen> screenClass;
        private FScreen screen;

        private NewGameScreen(final String caption0, final Class<? extends FScreen> screenClass0) {
            screenClass = screenClass0;
            item = new FMenuItem(caption0, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    open();
                }
            });
        }

        private void open() {
            if (screen == null) { //don't initialize screen until it's opened the first time
                try {
                    screen = screenClass.newInstance();
                    screen.setHeaderCaption("New Game - " + item.getText());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            Forge.openScreen(screen);
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
        }
    }

    public static void openPreferredScreen() {
        preferredScreen.open();
    }
    public static void setPreferredScreen(FScreen screen) {
        for (NewGameScreen ngs : NewGameScreen.values()) {
            if (ngs.screen == screen) {
                preferredScreen = ngs;
                prefs.setPref(FPref.NEW_GAME_SCREEN, ngs.name());
                prefs.save();
                return;
            }
        }
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
