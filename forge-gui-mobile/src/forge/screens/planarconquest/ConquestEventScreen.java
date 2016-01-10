package forge.screens.planarconquest;

import forge.Forge;
import forge.model.FModel;
import forge.planarconquest.ConquestEvent;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FOptionPane;

public class ConquestEventScreen extends LaunchScreen {
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

        LoadingOverlay.show("Starting battle...", new Runnable() {
            @Override
            public void run() {
                FModel.getConquest().launchEvent(FModel.getConquest().getModel().getSelectedCommander(), event);
            }
        });
    }
}
