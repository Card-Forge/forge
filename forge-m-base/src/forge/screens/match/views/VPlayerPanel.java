package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;

import forge.Forge.Graphics;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;

public class VPlayerPanel extends FContainer {
    private final RegisteredPlayer player;
    private final VPhases phases;
    private final VField field;
    private final VAvatar avatar;
    private final List<StatLabel> statLabels = new ArrayList<StatLabel>();
    private final List<VZoneDisplay> zones = new ArrayList<VZoneDisplay>();

    public VPlayerPanel(RegisteredPlayer player0) {
        player = player0;
        phases = add(new VPhases());
        field = add(new VField());
        avatar = add(new VAvatar(player.getPlayer().getAvatarIndex()));
        addZoneDisplay(ZoneType.Hand);
        addZoneDisplay(ZoneType.Library);
        addZoneDisplay(ZoneType.Graveyard);
        addZoneDisplay(ZoneType.Exile);
    }

    public void addZoneDisplay(ZoneType zoneType) {
        zones.add(add(new VZoneDisplay(zoneType)));
        statLabels.add(add(new StatLabel()));
    }

    public boolean isFlipped() {
        return field.isFlipped();
    }
    public void setFlipped(boolean flipped0) {
        field.setFlipped(flipped0);
    }

    public VField getField() {
        return field;
    }

    @Override
    protected void doLayout(float width, float height) {
        //layout for bottom panel by default
        float x = VAvatar.WIDTH;
        phases.setBounds(x, height - VPhases.HEIGHT, width - VAvatar.WIDTH, VPhases.HEIGHT);

        float y = height - VAvatar.HEIGHT;
        float zoneHeight = y / 3;
        y -= zoneHeight;
        for (VZoneDisplay zone : zones) {
            zone.setBounds(0, y, width, zoneHeight);
        }

        y = height - VAvatar.HEIGHT;
        avatar.setPosition(0, y);
        float statLabelSize = VAvatar.HEIGHT - VPhases.HEIGHT;
        for (StatLabel statLabel : statLabels) {
            statLabel.setBounds(x, y, statLabelSize, statLabelSize);
            x += statLabelSize;
        }

        field.setBounds(0, 0, width, y - zoneHeight);

        if (isFlipped()) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }
    }

    private class StatLabel extends FDisplayObject {
        private StatLabel() {

        }

        @Override
        public void draw(Graphics g) {
            float x = 1;
            float w = getWidth() - 2;
            float h = getHeight() - 1;
            g.fillRect(Color.ORANGE, x, 0, w, h);
        }
    }
}
