package forge.screens;

import java.util.ArrayList;
import java.util.List;

import forge.screens.FScreen;
import forge.screens.match.FControl;
import forge.Forge.Graphics;
import forge.assets.FSkinImage;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.toolbox.FDisplayObject;

public abstract class LaunchScreen extends FScreen {
    private final StartButton btnStart;

    public LaunchScreen(String headerCaption) {
        super(true, headerCaption, true);
        btnStart = add(new StartButton());
    }

    @Override
    protected final void doLayout(float startY, float width, float height) {
        float imageWidth = FSkinImage.BTN_START_UP.getWidth();
        float imageHeight = FSkinImage.BTN_START_UP.getHeight();
        float padding = imageHeight * 0.1f;

        btnStart.setBounds((width - imageWidth) / 2, height - imageHeight - padding, imageWidth, imageHeight);

        doLayoutAboveBtnStart(startY, width, height - imageHeight - 2 * padding);
    }

    protected abstract void doLayoutAboveBtnStart(float startY, float width, float height);
    protected abstract boolean buildLaunchParams(LaunchParams launchParams);

    protected class LaunchParams {
        public GameType gameType;
        public final List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        public final List<GameType> appliedVariants = new ArrayList<GameType>();
    }

    private class StartButton extends FDisplayObject {
        private boolean pressed;
        private boolean creatingMatch;

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
            if (count == 1 && !creatingMatch) {
                creatingMatch = true; //ensure user doesn't create multiple matches by tapping multiple times

                LaunchParams launchParams = new LaunchParams();
                if (buildLaunchParams(launchParams)) {
                    if (launchParams.gameType == null) {
                        throw new RuntimeException("Must set launchParams.gameType");
                    }
                    if (launchParams.players.isEmpty()) {
                        throw new RuntimeException("Must add at least one player to launchParams.players");
                    }

                    boolean useRandomFoil = false; //Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
                    for (RegisteredPlayer rp : launchParams.players) {
                        rp.setRandomFoil(useRandomFoil);
                    }

                    GameRules rules = new GameRules(launchParams.gameType);
                    if (!launchParams.appliedVariants.isEmpty()) {
                        rules.setAppliedVariants(launchParams.appliedVariants);
                    }
                    rules.setPlayForAnte(false); //Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE));
                    rules.setManaBurn(false); //Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN));
                    rules.canCloneUseTargetsImage = false; //Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);

                    FControl.startGame(new Match(rules, launchParams.players));
                }

                creatingMatch = false;
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            g.drawImage(pressed ? FSkinImage.BTN_START_DOWN : FSkinImage.BTN_START_UP,
                    0, 0, getWidth(), getHeight());
        }
    }
}
