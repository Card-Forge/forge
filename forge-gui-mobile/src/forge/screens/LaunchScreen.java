package forge.screens;

import com.badlogic.gdx.Input.Keys;

import forge.Graphics;
import forge.assets.FSkinImage;
import forge.menu.FPopupMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

public abstract class LaunchScreen extends FScreen {
    private static final float MAX_START_BUTTON_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;
    private static final float START_BUTTON_RATIO = FSkinImage.BTN_START_UP.getWidth() / FSkinImage.BTN_START_UP.getHeight();
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
                startMatch();
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            g.drawImage(pressed ? FSkinImage.BTN_START_DOWN : FSkinImage.BTN_START_UP,
                    0, 0, getWidth(), getHeight());
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
