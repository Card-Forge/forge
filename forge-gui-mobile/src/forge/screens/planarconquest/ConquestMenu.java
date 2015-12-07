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
import forge.screens.home.LoadGameMenu.LoadGameScreen;
import forge.screens.home.NewGameMenu.NewGameScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class ConquestMenu extends FPopupMenu {
    private static final ConquestMenu conquestMenu = new ConquestMenu();
    private static final ConquestMapScreen mapScreen = new ConquestMapScreen();
    private static final ConquestCommandersScreen commandersScreen = new ConquestCommandersScreen();
    private static final ConquestCollectionScreen collectionScreen = new ConquestCollectionScreen();
    private static final ConquestPrefsScreen prefsScreen = new ConquestPrefsScreen();

    private static final FMenuItem mapItem = new FMenuItem("Planar Map", FSkinImage.QUEST_MAP, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(mapScreen);
        }
    });
    private static final FMenuItem commandersItem = new FMenuItem("Commanders", FSkinImage.PLANESWALKER, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(commandersScreen);
        }
    });
    private static final FMenuItem collectionItem = new FMenuItem("Collection", FSkinImage.QUEST_BOX, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(collectionScreen);
        }
    });
    private static final FMenuItem prefsItem = new FMenuItem("Preferences", FSkinImage.SETTINGS, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(prefsScreen);
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
        if (data.exists()) {
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
                        Forge.openScreen(mapScreen);
                        if (reason == LaunchReason.NewConquest) {
                            LoadGameScreen.PlanarConquest.setAsBackScreen(true);
                        }
                    }
                }
            });
            return;
        }

        //if current quest can't be loaded, open New Conquest or Load Conquest screen based on whether a quest exists
        if (dirConquests.exists() && dirConquests.isDirectory() && dirConquests.list().length > 0) {
            LoadGameScreen.PlanarConquest.open();
        }
        else {
            NewGameScreen.PlanarConquest.open();
        }
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        addItem(mapItem); mapItem.setSelected(currentScreen == mapScreen);
        addItem(commandersItem); commandersItem.setSelected(currentScreen == commandersScreen);
        addItem(collectionItem); collectionItem.setSelected(currentScreen == collectionScreen);
        addItem(prefsItem); prefsItem.setSelected(currentScreen == prefsScreen);
    }
}
