package forge.adventure.scene;

import forge.adventure.stage.GameHUD;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class ShopScene extends ForgeScene {
    private static ShopScene object;

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
        getScreen();
        screen.refresh();
        super.enter();

    } 
    @Override
    public FScreen getScreen() {
        return screen==null?screen = new AdventureDeckEditor(true):screen;
    }

}
