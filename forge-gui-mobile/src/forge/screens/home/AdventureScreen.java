package forge.screens.home;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import forge.Forge;
import forge.animation.GifAnimation;
import forge.assets.FSkinFont;
import forge.localinstance.properties.ForgeConstants;
import forge.screens.LaunchScreen;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.Callback;
import forge.util.Utils;

public class AdventureScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);
    private boolean loaded = false;
    private static GifAnimation animation = null;
    private final FTextArea lblDesc = new FTextArea(false, Forge.getLocalizer().getMessage("lblAdventureDescription"), animation);
    public AdventureScreen() {
        super(null, NewGameMenu.getMenu());
        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.getInlineLabelColor());
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - PADDING;
        lblDesc.setBounds(x, y, w, h);
    }

    @Override
    public void onActivate() {
        if (!loaded) {
            loaded = true;
            add(lblDesc);
        }
        if (animation != null) {
            animation.start();
        }
        Forge.startContinuousRendering();
        super.onActivate();
    }

    @Override
    public void onSwitchAway(Callback<Boolean> canSwitchCallback) {
        if (animation != null) {
            animation.stop();
        }
        Forge.stopContinuousRendering();
        super.onSwitchAway(canSwitchCallback);
    }

    @Override
    protected void startMatch() {
        Forge.isMobileAdventureMode = true; //set early for the transition logo
        Forge.switchToAdventure();
    }
    public static void preload() {
        //keep low frame and under 1mb for performance
        String demo = ForgeConstants.EFFECTS_DIR+"demo.gif";
        if (Gdx.files.absolute(demo).exists())
            animation = new GifAnimation(demo, Animation.PlayMode.LOOP);
    }
    public static void dispose() {
        if (animation != null) {
            animation.dispose();
        }
        animation = null;
    }
}
