package forge.screens;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Input.Keys;

import forge.FThreads;
import forge.screens.FScreen;
import forge.screens.match.FControl;
import forge.screens.match.MatchLoader;
import forge.Forge.Graphics;
import forge.assets.FSkinImage;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.toolbox.FDisplayObject;
import forge.util.ThreadUtil;
import forge.util.Utils;

public abstract class LaunchScreen extends FScreen {
    private static final float IMAGE_HEIGHT = FSkinImage.BTN_START_UP.getNearestHQHeight(Utils.AVG_FINGER_HEIGHT * 2);
    private static final float IMAGE_WIDTH = FSkinImage.BTN_START_UP.getWidth() * IMAGE_HEIGHT / FSkinImage.BTN_START_UP.getHeight();
    private static final float PADDING = IMAGE_HEIGHT * 0.025f;

    protected final StartButton btnStart;

    public LaunchScreen(String headerCaption) {
        super(headerCaption);
        btnStart = add(new StartButton());
    }

    @Override
    protected final void doLayout(float startY, float width, float height) {
        btnStart.setBounds((width - IMAGE_WIDTH) / 2, height - IMAGE_HEIGHT - PADDING, IMAGE_WIDTH, IMAGE_HEIGHT);
        doLayoutAboveBtnStart(startY, width, height - IMAGE_HEIGHT - 2 * PADDING);
    }

    protected abstract void doLayoutAboveBtnStart(float startY, float width, float height);
    protected abstract boolean buildLaunchParams(LaunchParams launchParams);

    protected class LaunchParams {
        public GameType gameType;
        public final List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        public final Set<GameType> appliedVariants = new HashSet<GameType>();
    }

    private void startMatch() {
        final LaunchParams launchParams = new LaunchParams();
        if (!buildLaunchParams(launchParams)) { return; }

        if (launchParams.gameType == null) {
            throw new RuntimeException("Must set launchParams.gameType");
        }
        if (launchParams.players.isEmpty()) {
            throw new RuntimeException("Must add at least one player to launchParams.players");
        }

        final MatchLoader loader = new MatchLoader();
        loader.show(); //show loading overlay then delay starting game so UI can respond
        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        FControl.startMatch(launchParams.gameType, launchParams.appliedVariants, launchParams.players);
                        loader.hide();
                    }
                });
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
