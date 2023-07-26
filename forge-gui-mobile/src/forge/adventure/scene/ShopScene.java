package forge.adventure.scene;

import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;
import forge.item.PaperCard;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class ShopScene extends ForgeScene {
    private static ShopScene object;
    private PointOfInterestChanges changes;

    public static ShopScene instance() {
        if(object==null)
            object=new ShopScene();
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
        return screen==null?screen = new AdventureDeckEditor(true, null):screen;
    }

    public void doAutosell() {
        boolean promptToConfirmSale = false; //Todo: config option
        if (promptToConfirmSale) {
            int profit = 0;
            int cards = 0;
            for (PaperCard cardToSell: Current.player().autoSellCards.toFlatList()) {
                cards++;
                profit += AdventurePlayer.current().cardSellPrice(cardToSell);
            }
            if (!confirmAutosell(profit, cards, changes.getTownPriceModifier())) {
                return;
            }
        }
        AdventurePlayer.current().doAutosell();
    }

    private boolean confirmAutosell(int profit, int cards, float townPriceModifier) {
        return true;
    }

    public void loadChanges(PointOfInterestChanges changes) {
        AdventurePlayer.current().loadChanges(changes);
        this.changes = changes;
    }
}
