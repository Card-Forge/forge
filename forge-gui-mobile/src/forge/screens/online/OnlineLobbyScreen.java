package forge.screens.online;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.assets.FSkinFont;
import forge.screens.LaunchScreen;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.Utils;

public class OnlineLobbyScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            "Coming soon..."));

    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu());

        lblDesc.setAlignment(HAlignment.CENTER);
        lblDesc.setCenterVertically(true);
        lblDesc.setFont(FSkinFont.get(20));
        lblDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);

        btnStart.setEnabled(false);
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
    protected void startMatch() {
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        return false;
    }
}
