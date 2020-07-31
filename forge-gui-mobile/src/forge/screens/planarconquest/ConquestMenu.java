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
import forge.util.Localizer;

public class ConquestMenu extends FPopupMenu {
    private static final ConquestMenu conquestMenu = new ConquestMenu();
    private static final ConquestMultiverseScreen multiverseScreen = new ConquestMultiverseScreen();
    private static final ConquestAEtherScreen aetherScreen = new ConquestAEtherScreen();
    private static final ConquestCommandersScreen commandersScreen = new ConquestCommandersScreen();
    private static final ConquestPlaneswalkersScreen planeswalkersScreen = new ConquestPlaneswalkersScreen();
    private static final ConquestCollectionScreen collectionScreen = new ConquestCollectionScreen();
    private static final ConquestPlaneswalkScreen planeswalkScreen = new ConquestPlaneswalkScreen();
    private static final ConquestStatsScreen statsScreen = new ConquestStatsScreen();
    private static final ConquestPrefsScreen prefsScreen = new ConquestPrefsScreen();

    private static final FMenuItem multiverseItem = new FMenuItem(Localizer.getInstance().getMessage("lblTheMultiverse"), FSkinImage.MULTIVERSE, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(multiverseScreen);
        }
    });
    private static final FMenuItem aetherItem = new FMenuItem(Localizer.getInstance().getMessage("lblTheAether"), FSkinImage.AETHER_SHARD, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(aetherScreen);
        }
    });
    private static final FMenuItem commandersItem = new FMenuItem(Localizer.getInstance().getMessage("lblCommanders"), FSkinImage.COMMANDER, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(commandersScreen);
        }
    });
    private static final FMenuItem planeswalkersItem = new FMenuItem(Localizer.getInstance().getMessage("lblPlaneswalkers"), FSkinImage.PLANESWALKER, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(planeswalkersScreen);
        }
    });
    private static final FMenuItem collectionItem = new FMenuItem(Localizer.getInstance().getMessage("lblCollection"), FSkinImage.SPELLBOOK, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(collectionScreen);
        }
    });
    private static final FMenuItem statsItem = new FMenuItem(Localizer.getInstance().getMessage("lblStatistics"), FSkinImage.MENU_STATS, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(statsScreen);
        }
    });
    private static final FMenuItem planeswalkItem = new FMenuItem(Localizer.getInstance().getMessage("lblPlaneswalk"), FSkinImage.PW_BADGE_COMMON, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(planeswalkScreen);
        }
    });
    private static final FMenuItem prefsItem = new FMenuItem(Localizer.getInstance().getMessage("Preferences"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            setCurrentScreen(prefsScreen);
        }
    });

    private static void setCurrentScreen(FScreen screen0) {
        //make it so pressing Back from any screen besides Multiverse screen always goes to Multiverse screen
        //and make it so Multiverse screen always goes back to screen that launched Planar Conquest
        Forge.openScreen(screen0, Forge.getCurrentScreen() != multiverseScreen);
    }

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
        LoadingOverlay.show(Localizer.getInstance().getMessage("lblLoadingCurrentConquest"), new Runnable() {
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
        addItem(statsItem); statsItem.setSelected(currentScreen == statsScreen);
        addItem(prefsItem); prefsItem.setSelected(currentScreen == prefsScreen);
    }
}
