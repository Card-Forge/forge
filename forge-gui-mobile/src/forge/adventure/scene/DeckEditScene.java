package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Stage;
import forge.screens.FScreen;

/**
 * DeckEditScene
 * scene class that contains the Deck editor
 */
public class DeckEditScene extends ForgeScene {

    AdventureDeckEditor screen;
    Stage stage;

    private DeckEditScene() {

    }

    private static DeckEditScene object;

    public static DeckEditScene getInstance() {
        if(object==null)
            object=new DeckEditScene();
        return object;
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
        return screen==null?screen = new AdventureDeckEditor(false):screen;
    }

}
