package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import forge.Adventure;
import forge.card.CardZoom;
import forge.screens.FScreen;
import forge.screens.RewardScreen;

import java.util.List;

public class ViewRewardsScene extends ForgeScene {
    RewardScreen screen;
    private static List<?> list;
    private static int index;
    private static ViewRewardsScene object;

    public static ViewRewardsScene getInstance(List<?> list0, int index0) {
        if (object == null)
            object = new ViewRewardsScene();
        // always set new list and index
        list = list0;
        index = index0;
        return object;
    }

    @Override
    public void enter() {
        Adventure.getInstance().renderTransitionScreen = false;

        // init screen
        getScreen();

        // access the image and swap if needed
        RewardScene scene = RewardScene.instance();
        TextureRegion currentBackground = scene.getUIBackground();

        if (RewardScene.Type.Shop.equals(scene.type)) {
            if (scene.marketBackgroundImg != null && scene.marketBackgroundImg.getDrawable() instanceof TextureRegionDrawable drawable) {
                currentBackground = drawable.getRegion();
            }
        }

        screen.setBackground(currentBackground);

        super.enter();
        CardZoom.show(list, index, null, true);
    }

    @Override
    public boolean leave() {
        Adventure.getInstance().renderTransitionScreen = true;
        return super.leave();
    }

    @Override
    public FScreen getScreen() {
        if (screen == null) {
            screen = new RewardScreen("", null);
        }
        return screen;
    }
}
