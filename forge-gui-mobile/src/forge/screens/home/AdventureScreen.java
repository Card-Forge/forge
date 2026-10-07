package forge.screens.home;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.screens.LaunchScreen;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.Utils;

import java.util.function.Consumer;

public class AdventureScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);
    private boolean loaded = false;
    private final FTextArea lblDesc = new FTextArea(false, Forge.getLocalizer().getMessage("lblAdventureDescription"), Forge.getAssets().getGifAnimation());
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
        Forge.getAssets().playGifAnimation();
        Forge.startContinuousRendering();
        super.onActivate();
    }

    @Override
    public void onSwitchAway(Consumer<Boolean> canSwitchCallback) {
        Forge.getAssets().stopGifAnimation();
        Forge.stopContinuousRendering();
        super.onSwitchAway(canSwitchCallback);
    }

    @Override
    protected void startMatch() {
        Forge.isMobileAdventureMode = true; //set early for the transition logo
        Forge.switchToAdventure();
    }
}
