package forge.screens.planarconquest;

import java.io.File;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.FDeckEditor.DeckController;
import forge.deck.FDeckEditor.EditorType;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.planarconquest.ConquestDataIO;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.properties.ForgeConstants;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class ConquestMenu extends FPopupMenu {
    private static final ConquestMenu conquestMenu = new ConquestMenu();
    private static final ConquestMapScreen mapScreen = new ConquestMapScreen();
    private static final ConquestPrefsScreen prefsScreen = new ConquestPrefsScreen();

    private static final FMenuItem mapItem = new FMenuItem("Command Center", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(mapScreen);
        }
    });

    public static ConquestMenu getMenu() {
        return conquestMenu;
    }

    private ConquestMenu() {
    }

    public enum LaunchReason {
        StartPlanarConquest,
        LoadConquest,
        NewConquest
    }

    public static void launchPlanarConquest(final LaunchReason reason) {
        //attempt to load current quest
        final File dirConquests = new File(ForgeConstants.CONQUEST_SAVE_DIR);
        final String questname = FModel.getConquestPreferences().getPref(CQPref.CURRENT_CONQUEST);
        final File data = new File(dirConquests.getPath(), questname);
        if (data.exists() || ConquestDataIO.TEST_MODE) {
            LoadingOverlay.show("Loading current conquest...", new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    FModel.getConquest().load(ConquestDataIO.loadData(data));
                    ((DeckController<Deck>)EditorType.PlanarConquest.getController()).setRootFolder(FModel.getConquest().getDecks());
                    if (reason == LaunchReason.StartPlanarConquest) {
                        Forge.openScreen(mapScreen);
                    }
                    else {
                        mapScreen.update();
                        if (reason == LaunchReason.LoadConquest) {
                            Forge.back();
                            if (Forge.onHomeScreen()) { //open map screen if Load Conquest screen was opening direct from home screen
                                Forge.openScreen(mapScreen);
                            }
                        }
                        else {
                            Forge.back();
                            if (Forge.getCurrentScreen() instanceof LoadConquestScreen) {
                                Forge.back(); //remove LoadConquestScreen from screen stack
                            }
                            if (Forge.onHomeScreen()) { //open map screen if New Conquest screen was opening direct from home screen
                                Forge.openScreen(mapScreen);
                            }
                        }
                    }
                }
            });
            return;
        }

        //if current quest can't be loaded, open New Conquest or Load Conquest screen based on whether a quest exists
        if (dirConquests.exists() && dirConquests.isDirectory() && dirConquests.list().length > 0) {
            Forge.openScreen(new LoadConquestScreen());
        }
        else {
            Forge.openScreen(new NewConquestScreen());
        }
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        addItem(mapItem); mapItem.setSelected(currentScreen == mapScreen);
        addItem(new FMenuItem("New Conquest", FSkinImage.NEW, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new NewConquestScreen());
            }
        }));
        addItem(new FMenuItem("Load Conquest", FSkinImage.OPEN, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new LoadConquestScreen());
            }
        }));
        addItem(new FMenuItem("Preferences", FSkinImage.SETTINGS, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(prefsScreen);
            }
        }));
    }
}
