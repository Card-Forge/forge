package forge.screens;

import com.badlogic.gdx.Input.Keys;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.menu.FPopupMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

public abstract class LaunchScreen extends FScreen {
    private static final float MAX_START_BUTTON_HEIGHT = 1.75f * Utils.AVG_FINGER_HEIGHT;
    private float START_BUTTON_RATIO = 0.f;
    private static final float PADDING = FOptionPane.PADDING;

    protected final StartButton btnStart = add(new StartButton());

    public LaunchScreen(String headerCaption) {
        super(headerCaption);
    }
    public LaunchScreen(String headerCaption, FPopupMenu menu) {
        super(headerCaption, menu);
    }

    @Override
    protected final void doLayout(float startY, float width, float height) {
        if (Forge.hdstart)
            START_BUTTON_RATIO = FSkinImage.HDBTN_START_UP.getWidth() / FSkinImage.HDBTN_START_UP.getHeight();
        else
            START_BUTTON_RATIO = FSkinImage.BTN_START_UP.getWidth() / FSkinImage.BTN_START_UP.getHeight();

        float buttonWidth = width - 2 * PADDING;
        float buttonHeight = buttonWidth / START_BUTTON_RATIO;
        if (buttonHeight > MAX_START_BUTTON_HEIGHT) {
            buttonHeight = MAX_START_BUTTON_HEIGHT;
            buttonWidth = buttonHeight * START_BUTTON_RATIO;
        }
        btnStart.setBounds((width - buttonWidth) / 2, height - buttonHeight - PADDING, buttonWidth, buttonHeight);

        doLayoutAboveBtnStart(startY, width, height - buttonHeight - 2 * PADDING);
    }

    protected abstract void doLayoutAboveBtnStart(float startY, float width, float height);
    protected abstract void startMatch();

    protected class StartButton extends FDisplayObject {
        private boolean pressed;

        /**
         * Instantiates a new FButton.
         */
        public StartButton() {
        }

        @Override
        public final boolean press(float x, float y) {
            pressed = true;
            return true;
        }

        @Override
        public final boolean release(float x, float y) {
            pressed = false;
            return true;
        }

        @Override
        public final boolean tap(float x, float y, int count) {
            if (count == 1) {
                btnStart.setEnabled(false);
                startMatch();
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            if (Forge.hdstart)
                g.drawImage(pressed ? FSkinImage.HDBTN_START_DOWN :
                        isHovered() ? FSkinImage.HDBTN_START_OVER : FSkinImage.HDBTN_START_UP,
                        isHovered() ? -2 : 0, 0, getWidth(), getHeight());
            else
                g.drawImage(pressed ? FSkinImage.BTN_START_DOWN :
                        isHovered() ? FSkinImage.BTN_START_OVER : FSkinImage.BTN_START_UP, 0, 0, getWidth(), getHeight());
            //its must be enabled or you can't start any game modes
            if (!Forge.isLoadingaMatch()) {
                if(!btnStart.isEnabled())
                    btnStart.setEnabled(true);
            }
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
        case Keys.SPACE:
            startMatch(); //start match on Enter or Space
            return true;
        }
        return super.keyDown(keyCode);
    }
}
