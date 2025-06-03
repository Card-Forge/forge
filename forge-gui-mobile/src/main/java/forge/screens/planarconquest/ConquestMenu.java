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
import forge.screens.home.HomeScreen;
import forge.screens.home.LoadGameMenu.LoadGameScreen;

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

    private static final FMenuItem multiverseItem = new FMenuItem(Forge.getLocalizer().getMessage("lblTheMultiverse"), FSkinImage.MULTIVERSE, event -> setCurrentScreen(multiverseScreen));
    private static final FMenuItem aetherItem = new FMenuItem(Forge.getLocalizer().getMessage("lblTheAether"), FSkinImage.AETHER_SHARD, event -> setCurrentScreen(aetherScreen));
    private static final FMenuItem commandersItem = new FMenuItem(Forge.getLocalizer().getMessage("lblCommanders"), FSkinImage.COMMANDER, event -> setCurrentScreen(commandersScreen));
    private static final FMenuItem planeswalkersItem = new FMenuItem(Forge.getLocalizer().getMessage("lblPlaneswalkers"), FSkinImage.PLANESWALKER, event -> setCurrentScreen(planeswalkersScreen));
    private static final FMenuItem collectionItem = new FMenuItem(Forge.getLocalizer().getMessage("lblCollection"), FSkinImage.SPELLBOOK, event -> setCurrentScreen(collectionScreen));
    private static final FMenuItem statsItem = new FMenuItem(Forge.getLocalizer().getMessage("lblStatistics"), FSkinImage.MENU_STATS, event -> setCurrentScreen(statsScreen));
    private static final FMenuItem planeswalkItem = new FMenuItem(Forge.getLocalizer().getMessage("lblPlaneswalk"), FSkinImage.PW_BADGE_COMMON, event -> setCurrentScreen(planeswalkScreen));
    private static final FMenuItem prefsItem = new FMenuItem(Forge.getLocalizer().getMessage("Preferences"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, event -> setCurrentScreen(prefsScreen));

    private static void setCurrentScreen(FScreen screen0) {
        //make it so pressing Back from any screen besides Multiverse screen always goes to Multiverse screen
        //and make it so Multiverse screen always goes back to screen that launched Planar Conquest
        Forge.openScreen(screen0, Forge.getCurrentScreen() != multiverseScreen);
    }

    static {
        //the first time planarconquest mode is launched, add button for it if in Landscape mode
        if (Forge.isLandscapeMode()) {
            HomeScreen.instance.addButtonForMode("-"+Forge.getLocalizer().getMessage("lblPlanarConquest"), event -> launchPlanarConquest(LaunchReason.StartPlanarConquest));
        }
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
        Forge.lastButtonIndex = 7;
        LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingCurrentConquest"), true, () -> {
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
