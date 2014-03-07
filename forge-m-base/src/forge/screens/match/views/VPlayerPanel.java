package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.game.player.RegisteredPlayer;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;

public class VPlayerPanel extends FContainer {
    private final RegisteredPlayer player;
    private final VPhases phases;
    private final VField field;
    private final VAvatar avatar;
    private final List<VZoneDisplay> zones = new ArrayList<VZoneDisplay>();

    public VPlayerPanel(RegisteredPlayer player0) {
        player = player0;
        phases = add(new VPhases());
        field = add(new VField());
        avatar = add(new VAvatar(player.getPlayer().getAvatarIndex()));
    }

    public boolean isFlipped() {
        return field.isFlipped();
    }
    public void setFlipped(boolean flipped0) {
        field.setFlipped(flipped0);
    }

    @Override
    protected void doLayout(float width, float height) {
        //layout for bottom panel by default
        field.setBounds(0, 0, width, height - VAvatar.HEIGHT);
        phases.setBounds(VAvatar.WIDTH, height - VPhases.HEIGHT, width - VAvatar.WIDTH, VPhases.HEIGHT);
        avatar.setPosition(0, height - VAvatar.HEIGHT);

        if (isFlipped()) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }
    }
}
