package forge.adventure.scene;

import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;
import forge.item.PaperCard;
import forge.screens.FScreen;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class ShopScene extends ForgeScene {
    private static ShopScene object;
    private PointOfInterestChanges changes;

    public static ShopScene instance() {
        if (object == null)
            object = new ShopScene();
        return object;
    }

    AdventureDeckEditor screen;

    private ShopScene() {

    }

    @Override
    public void dispose() {
    }


    @Override
    public void enter() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        screen = null;
        getScreen();
        screen.refresh();
        super.enter();
        doAutosell();
    }

    @Override
    public FScreen getScreen() {
        return screen == null ? screen = new AdventureDeckEditor(true, null) : screen;
    }

    public void doAutosell() {
        boolean promptToConfirmSale = true; // TODO: config option
        if (promptToConfirmSale) {
            int profit = 0;
            int cards = 0;
            for (PaperCard cardToSell : Current.player().autoSellCards.toFlatList()) {
                cards++;
                profit += getCardPrice(cardToSell);
            }
            confirmAutosell(profit, cards);
        }
    }

    private void confirmAutosell(int profit, int cards) {
        FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblSellAllConfirm", cards, profit), Forge.getLocalizer().getMessage("lblAutoSell"), Forge.getLocalizer().getMessage("lblSell"), Forge.getLocalizer().getMessage("lblCancel"), false, new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    AdventurePlayer.current().doAutosell();
                    if (screen != null)
                        screen.refresh();
                }
            }
        });
    }

    public void loadChanges(PointOfInterestChanges changes) {
        AdventurePlayer.current().loadChanges(changes);
        this.changes = changes;
    }

    public int getCardPrice(PaperCard pc) {
        return AdventurePlayer.current().cardSellPrice(pc);
    }
}
