package forge.screens.planarconquest;

import forge.Forge;
import forge.model.FModel;
import forge.planarconquest.ConquestBattle;
import forge.planarconquest.ConquestEvent;
import forge.planarconquest.ConquestLocation;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class ConquestEventScreen extends LaunchScreen {
    protected static final float PADDING = FOptionPane.PADDING;

    private final ConquestEvent event;
    private final ConquestLocation location;
    private final Callback<ConquestBattle> callback;
    private ConquestBattle battle;
    private int tier = 1; //TODO: Support picking tier

    public ConquestEventScreen(ConquestEvent event0, ConquestLocation location0, Callback<ConquestBattle> callback0) {
        super(event0.getName());
        event = event0;
        location = location0;
        callback = callback0;
    }

    @Override
    public void onActivate() {
        if (battle == null) { return; }

        //when returning to this screen from launched event, close it immediately and call callback
        Forge.back();
        callback.run(battle);
        battle = null;
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        
    }

    @Override
    protected void startMatch() {
        if (battle != null) { return; } //avoid starting battle more than once
        battle = event.createBattle(location, tier);

        LoadingOverlay.show("Starting battle...", new Runnable() {
            @Override
            public void run() {
                FModel.getConquest().startBattle(battle);
            }
        });
    }
}
