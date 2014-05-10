package forge.screens;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Input.Keys;

import forge.screens.FScreen;
import forge.screens.match.FControl;
import forge.Forge.Graphics;
import forge.assets.FSkinImage;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public abstract class LaunchScreen extends FScreen {
    private final StartButton btnStart;
    private boolean creatingMatch;

    public LaunchScreen(String headerCaption) {
        super(true, headerCaption, true);
        btnStart = add(new StartButton());
    }

    @Override
    protected final void doLayout(float startY, float width, float height) {
        float baseImageHeight = FSkinImage.BTN_START_UP.getHeight();
        float imageHeight = FSkinImage.BTN_START_UP.getNearestHQHeight(Utils.AVG_FINGER_HEIGHT);
        float imageWidth = FSkinImage.BTN_START_UP.getWidth() * imageHeight / baseImageHeight;
        float padding = imageHeight * 0.025f;

        btnStart.setBounds((width - imageWidth) / 2, height - imageHeight - padding, imageWidth, imageHeight);

        doLayoutAboveBtnStart(startY, width, height - imageHeight - 2 * padding);
    }

    protected abstract void doLayoutAboveBtnStart(float startY, float width, float height);
    protected abstract boolean buildLaunchParams(LaunchParams launchParams);

    protected class LaunchParams {
        public GameType gameType;
        public final List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        public final Set<GameType> appliedVariants = new HashSet<GameType>();
    }

    private void startMatch() {
        if (creatingMatch) { return; }
        creatingMatch = true; //ensure user doesn't create multiple matches by tapping multiple times

        LaunchParams launchParams = new LaunchParams();
        if (buildLaunchParams(launchParams)) {
            if (launchParams.gameType == null) {
                throw new RuntimeException("Must set launchParams.gameType");
            }
            if (launchParams.players.isEmpty()) {
                throw new RuntimeException("Must add at least one player to launchParams.players");
            }
            
            FControl.startMatch(launchParams.gameType, launchParams.appliedVariants, launchParams.players);
        }

        creatingMatch = false;
    }

    private class StartButton extends FDisplayObject {
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
