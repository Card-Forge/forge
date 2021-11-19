package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Stage;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class ShopScene extends ForgeScene {

    AdventureDeckEditor screen;
    Stage stage;

    public ShopScene() {

    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }



    @Override
    public void enter() {
        getScreen();
        screen.refresh();
        super.enter();

    } 
    @Override
    public FScreen getScreen() {
        return screen==null?screen = new AdventureDeckEditor(true):screen;
    }

}
