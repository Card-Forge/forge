package forge.screens.planarconquest;

import forge.Forge;
import forge.model.FModel;
import forge.planarconquest.ConquestEvent;
import forge.planarconquest.ConquestController.GameRunner;
import forge.planarconquest.ConquestEvent.IConquestEventLauncher;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FOptionPane;
import forge.util.ThreadUtil;

public class ConquestEventScreen extends LaunchScreen implements IConquestEventLauncher {
    protected static final float PADDING = FOptionPane.PADDING;

    private final ConquestEvent event;
    private boolean launchedEvent;

    public ConquestEventScreen(ConquestEvent event0) {
        super(event0.getEventName());
        event = event0;
    }

    @Override
    public void onActivate() {
        if (launchedEvent) {
            //when returning to this screen from launched event, close it immediately
            Forge.back();
        }
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        
    }

    @Override
    protected void startMatch() {
        if (launchedEvent) { return; } //avoid launching event more than once

        launchedEvent = true;
        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                FModel.getConquest().launchEvent(ConquestEventScreen.this, FModel.getConquest().getModel().getSelectedCommander(), event);
            }
        });
    }

    @Override
    public void startGame(final GameRunner gameRunner) {
        LoadingOverlay.show("Loading new game...", new Runnable() {
            @Override
            public void run() {
                gameRunner.finishStartingGame();
            }
        });
    }
}
