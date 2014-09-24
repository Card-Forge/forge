package forge.screens;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Input.Keys;

import forge.match.MatchUtil;
import forge.menu.FPopupMenu;
import forge.screens.FScreen;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

public abstract class LaunchScreen extends FScreen {
    private static final float MAX_START_BUTTON_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;
    private static final float START_BUTTON_RATIO = FSkinImage.BTN_START_UP.getWidth() / FSkinImage.BTN_START_UP.getHeight();
    private static final float PADDING = FOptionPane.PADDING;

    protected final StartButton btnStart = add(new StartButton());
    protected boolean creatingMatch;

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
    protected abstract boolean buildLaunchParams(LaunchParams launchParams);

    protected class LaunchParams {
        public GameType gameType;
        public final List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        public final Set<GameType> appliedVariants = new HashSet<GameType>();
    }

    protected void startMatch() {
        if (creatingMatch) { return; }
        creatingMatch = true; //ensure user doesn't create multiple matches by tapping multiple times

        LoadingOverlay.show("Loading new game...", new Runnable() {
            @Override
            public void run() {
                LaunchParams launchParams = new LaunchParams();
                if (buildLaunchParams(launchParams)) {
                    if (launchParams.gameType == null) {
                        throw new RuntimeException("Must set launchParams.gameType");
                    }
                    if (launchParams.players.isEmpty()) {
                        throw new RuntimeException("Must add at least one player to launchParams.players");
                    }

                    MatchUtil.startMatch(launchParams.gameType, launchParams.appliedVariants, launchParams.players);
                }
                creatingMatch = false;
            }
        });
    }

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
