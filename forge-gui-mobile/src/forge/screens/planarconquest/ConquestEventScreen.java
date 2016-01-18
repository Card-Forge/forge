package forge.screens.planarconquest;

import forge.Forge;
import forge.model.FModel;
import forge.planarconquest.ConquestEvent;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class ConquestEventScreen extends LaunchScreen {
    protected static final float PADDING = FOptionPane.PADDING;

    private final ConquestEvent event;
    private final Callback<ConquestEvent> callback;
    private boolean launchedEvent;

    public ConquestEventScreen(ConquestEvent event0, Callback<ConquestEvent> callback0) {
        super(event0.getEventName());
        event = event0;
        callback = callback0;
    }

    @Override
    public void onActivate() {
        if (launchedEvent) {
            //when returning to this screen from launched event, close it immediately and call callback
            Forge.back();
            callback.run(event);
            launchedEvent = false;
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
                FModel.getConquest().launchEvent(event);
            }
        });
    }
}
