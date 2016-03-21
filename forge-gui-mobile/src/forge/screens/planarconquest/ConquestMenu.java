package forge.screens.planarconquest;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.FDeckEditor.DeckController;
import forge.deck.FDeckEditor.EditorType;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.LoadGameMenu.LoadGameScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class ConquestMenu extends FPopupMenu {
    private static final ConquestMenu conquestMenu = new ConquestMenu();
    private static final ConquestMultiverseScreen multiverseScreen = new ConquestMultiverseScreen();
    private static final ConquestAEtherScreen aetherScreen = new ConquestAEtherScreen();
    private static final ConquestCommandersScreen commandersScreen = new ConquestCommandersScreen();
    private static final ConquestPlaneswalkersScreen planeswalkersScreen = new ConquestPlaneswalkersScreen();
    private static final ConquestCollectionScreen collectionScreen = new ConquestCollectionScreen();
    private static final ConquestPlaneswalkScreen planeswalkScreen = new ConquestPlaneswalkScreen();
    private static final ConquestPrefsScreen prefsScreen = new ConquestPrefsScreen();

    private static final FMenuItem multiverseItem = new FMenuItem("The Multiverse", FSkinImage.MULTIVERSE, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(multiverseScreen);
        }
    });
    private static final FMenuItem aetherItem = new FMenuItem("The AEther", FSkinImage.AETHER_SHARD, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(aetherScreen);
        }
    });
    private static final FMenuItem commandersItem = new FMenuItem("Commanders", FSkinImage.COMMANDER, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(commandersScreen);
        }
    });
    private static final FMenuItem planeswalkersItem = new FMenuItem("Planeswalkers", FSkinImage.PLANESWALKER, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(planeswalkersScreen);
        }
    });
    private static final FMenuItem collectionItem = new FMenuItem("Collection", FSkinImage.SPELLBOOK, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(collectionScreen);
        }
    });
    private static final FMenuItem planeswalkItem = new FMenuItem("Planeswalk", FSkinImage.PW_BADGE_COMMON, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            Forge.openScreen(planeswalkScreen);
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
        LoadingOverlay.show("Loading current conquest...", new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                ((DeckController<Deck>)EditorType.PlanarConquest.getController()).setRootFolder(FModel.getConquest().getDecks());
                if (reason == LaunchReason.StartPlanarConquest) {
                    Forge.openScreen(multiverseScreen);
                }
                else {
                    multiverseScreen.update();
                    Forge.openScreen(multiverseScreen);
                    if (reason == LaunchReason.NewConquest) {
                        LoadGameScreen.PlanarConquest.setAsBackScreen(true);
                    }
                }
            }
        });
    }

    public static void selectCommander() {
        Forge.openScreen(commandersScreen);
    }

    @Override
    protected void buildMenu() {
        FScreen currentScreen = Forge.getCurrentScreen();
        addItem(multiverseItem); multiverseItem.setSelected(currentScreen == multiverseScreen);
        addItem(aetherItem); aetherItem.setSelected(currentScreen == aetherScreen);
        addItem(commandersItem); commandersItem.setSelected(currentScreen == commandersScreen);
        addItem(planeswalkersItem); planeswalkersItem.setSelected(currentScreen == planeswalkersScreen);
        addItem(collectionItem); collectionItem.setSelected(currentScreen == collectionScreen);
        addItem(planeswalkItem); planeswalkItem.setSelected(currentScreen == planeswalkScreen);
        addItem(prefsItem); prefsItem.setSelected(currentScreen == prefsScreen);
    }
}
