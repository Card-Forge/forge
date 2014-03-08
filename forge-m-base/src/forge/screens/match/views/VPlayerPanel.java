package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.utils.Utils;

public class VPlayerPanel extends FContainer {
    private static final FSkinFont LIFE_FONT = FSkinFont.get(16);
    private static final FSkinFont INFO_FONT = FSkinFont.get(12);
    private static final FSkinColor INFO_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    private final RegisteredPlayer player;
    private final VPhases phases;
    private final VField field;
    private final VAvatar avatar;
    private final List<InfoLabel> infoLabels = new ArrayList<InfoLabel>();
    private final List<VZoneDisplay> zones = new ArrayList<VZoneDisplay>();
    private VZoneDisplay selectedZone;

    public VPlayerPanel(RegisteredPlayer player0) {
        player = player0;
        phases = add(new VPhases());
        field = add(new VField());
        avatar = add(new VAvatar(player.getPlayer().getAvatarIndex()));
        infoLabels.add(add(new LifeLabel()));
        addZoneDisplay(ZoneType.Hand, FSkinImage.HAND);
        addZoneDisplay(ZoneType.Graveyard, FSkinImage.GRAVEYARD);
        addZoneDisplay(ZoneType.Library, FSkinImage.LIBRARY);
        addZoneDisplay(ZoneType.Exile, FSkinImage.EXILE);

        setSelectedZone(ZoneType.Hand);
    }

    public void addZoneDisplay(ZoneType zoneType, FSkinImage tabIcon) {
        VZoneDisplay zone = add(new VZoneDisplay(zoneType));
        zone.setVisible(false);
        zones.add(zone);
        infoLabels.add(add(new ZoneInfoTab(tabIcon, zone)));
    }

    public ZoneType getSelectedZone() {
        if (selectedZone != null) {
            return selectedZone.getZoneType();
        }
        return null;
    }

    public void setSelectedZone(ZoneType zoneType) {
        for (VZoneDisplay zone : zones) {
            if (zone.getZoneType() == zoneType) {
                if (selectedZone != null) {
                    selectedZone.setVisible(false);
                }
                selectedZone = zone;
                selectedZone.setVisible(true);
                return;
            }
        }
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
        float infoLabelWidth;
        float infoLabelHeight = VAvatar.HEIGHT - VPhases.HEIGHT;
        for (InfoLabel infoLabel : infoLabels) {
            infoLabelWidth = infoLabel.getPreferredWidth();
            infoLabel.setBounds(x, y, infoLabelWidth, infoLabelHeight);
            x += infoLabelWidth;
        }

        field.setBounds(0, 0, width, y - zoneHeight);

        if (isFlipped()) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }
    }

    private abstract class InfoLabel extends FDisplayObject {
        protected static final float PADDING = 2;
        protected String value;
        public abstract float getPreferredWidth();
    }
    
    private class LifeLabel extends InfoLabel {
        private LifeLabel() {
            value = "20";
        }

        @Override
        public float getPreferredWidth() {
            return Utils.AVG_FINGER_WIDTH * 0.75f;
        }

        @Override
        public void draw(Graphics g) {
            g.drawText(value, LIFE_FONT, INFO_FORE_COLOR, PADDING, 0, getWidth(), getHeight(), false, HAlignment.LEFT, true);
        }
    }

    private class ZoneInfoTab extends InfoLabel {
        private final FSkinImage icon;
        private final VZoneDisplay zoneToOpen;

        private ZoneInfoTab(FSkinImage icon0, VZoneDisplay zoneToOpen0) {
            icon = icon0;
            zoneToOpen = zoneToOpen0;
            value = "99";
        }

        @Override
        public float getPreferredWidth() {
            return Utils.AVG_FINGER_WIDTH * 1.15f;
        }

        @Override
        public void draw(Graphics g) {
            float padding = 2;
            float h = getHeight() * 0.7f;
            float w = h;
            float x = padding;
            float y = (getHeight() - h) / 2;
            g.drawImage(icon, x, y, w, h);

            x += w * 1.05f;
            g.drawText(value, INFO_FONT, INFO_FORE_COLOR, x, padding, getWidth() - x, getHeight(), false, HAlignment.LEFT, true);

            if (selectedZone == zoneToOpen) {
                w = getWidth();
                h = getHeight();
                g.drawLine(1, MatchScreen.BORDER_COLOR, 0, 0, 0, h);
                g.drawLine(1, MatchScreen.BORDER_COLOR, 0, h, w, h);
                g.drawLine(1, MatchScreen.BORDER_COLOR, w, h, w, 0);
            }
        }
    }
}
