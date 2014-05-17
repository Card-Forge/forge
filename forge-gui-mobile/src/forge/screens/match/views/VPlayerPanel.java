package forge.screens.match.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.screens.match.FControl;
import forge.screens.match.MatchScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class VPlayerPanel extends FContainer {
    private static final FSkinFont LIFE_FONT = FSkinFont.get(18);
    private static final FSkinFont INFO_FONT = FSkinFont.get(12);
    private static final FSkinColor INFO_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor DISPLAY_AREA_BACK_COLOR = FSkinColor.get(Colors.CLR_INACTIVE).alphaColor(0.5f);
    private static final float INFO_TAB_PADDING_X = Utils.scaleX(2);
    private static final float INFO_TAB_PADDING_Y = Utils.scaleY(2);

    private final Player player;
    private final VPhaseIndicator phaseIndicator;
    private final VField field;
    private final VAvatar avatar;
    private final LifeLabel lblLife;
    private final InfoTab tabManaPool;
    private final InfoTab tabFlashbackZone;
    private final Map<ZoneType, InfoTab> zoneTabs = new HashMap<ZoneType, InfoTab>();
    private final List<InfoTab> tabs = new ArrayList<InfoTab>();
    private InfoTab selectedTab;

    public VPlayerPanel(Player player0) {
        player = player0;
        phaseIndicator = add(new VPhaseIndicator());
        field = add(new VField(player));
        avatar = add(new VAvatar(player));
        lblLife = add(new LifeLabel());
        addZoneDisplay(ZoneType.Hand, FSkinImage.HAND);
        addZoneDisplay(ZoneType.Graveyard, FSkinImage.GRAVEYARD);
        addZoneDisplay(ZoneType.Library, FSkinImage.LIBRARY);

        VFlashbackZone flashbackZone = add(new VFlashbackZone(player0));
        tabFlashbackZone = add(new InfoTab(FSkinImage.FLASHBACK, flashbackZone));
        tabs.add(tabFlashbackZone);

        VManaPool manaPool = add(new VManaPool(player));
        tabManaPool = add(new InfoTab(FSkinImage.MANA_X, manaPool));
        tabs.add(tabManaPool);

        addZoneDisplay(ZoneType.Exile, FSkinImage.EXILE);
        addZoneDisplay(ZoneType.Command, FSkinImage.PLANESWALKER);
    }

    public Player getPlayer() {
        return player;
    }

    public void addZoneDisplay(ZoneType zoneType, FSkinImage tabIcon) {
        VZoneDisplay zoneDisplay = add(new VZoneDisplay(player, zoneType));
        InfoTab zoneTab = add(new InfoTab(tabIcon, zoneDisplay));
        zoneTabs.put(zoneType, zoneTab);
        tabs.add(zoneTab);
    }

    public Iterable<InfoTab> getTabs() {
        return tabs;
    }

    public InfoTab getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedZone(ZoneType zoneType) {
        setSelectedTab(zoneTabs.get(zoneType));
    }

    private void setSelectedTab(InfoTab selectedTab0) {
        if (selectedTab == selectedTab0) {
            return;
        }

        if (selectedTab != null) {
            selectedTab.displayArea.setVisible(false);
        }

        selectedTab = selectedTab0;

        if (selectedTab != null) {
            selectedTab.displayArea.setVisible(true);
        }

        if (FControl.getView() != null) { //must revalidate entire screen so panel heights updated
            FControl.getView().revalidate();
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

    public VPhaseIndicator getPhaseIndicator() {
        return phaseIndicator;
    }

    public VAvatar getAvatar() {
        return avatar;
    }

    public void updateLife() {
        lblLife.update();
    }

    public void updateManaPool() {
        tabManaPool.update();
    }

    public void updateZone(ZoneType zoneType) {
        if (zoneType == ZoneType.Battlefield) {
            field.update();
        }
        else {
            InfoTab zoneTab = zoneTabs.get(zoneType);
            if (zoneTab != null) { //TODO: Support Ante somehow
                zoneTab.update();
            }
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        //layout for bottom panel by default
        float x = VAvatar.WIDTH;
        float w = width - VAvatar.WIDTH;
        float h = phaseIndicator.getPreferredHeight(w);
        phaseIndicator.setBounds(x, height - h, w, h);

        float y = height - VAvatar.HEIGHT;
        float displayAreaHeight = y / 3;
        y -= displayAreaHeight;
        for (InfoTab tab : tabs) {
            tab.displayArea.setBounds(0, y, width, displayAreaHeight);
        }

        y = height - VAvatar.HEIGHT;
        avatar.setPosition(0, y);

        float lifeLabelWidth = LIFE_FONT.getFont().getBounds("99").width * 1.2f; //make just wide enough for 2-digit life totals
        float infoLabelHeight = VAvatar.HEIGHT - phaseIndicator.getHeight();
        lblLife.setBounds(x, y, lifeLabelWidth, infoLabelHeight);
        x += lifeLabelWidth;

        float infoTabWidth = (getWidth() - x) / tabs.size();
        for (InfoTab tab : tabs) {
            tab.setBounds(x, y, infoTabWidth, infoLabelHeight);
            x += infoTabWidth;
        }

        if (selectedTab != null) {
            y -= displayAreaHeight;
        }
        field.setBounds(0, 0, width, y);

        if (isFlipped()) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }
    }

    @Override
    public void drawBackground(Graphics g) {
        if (selectedTab != null) { //draw background and border for selected zone if needed 
            float w = getWidth();
            VDisplayArea selectedDisplayArea = selectedTab.displayArea;
            g.fillRect(DISPLAY_AREA_BACK_COLOR, 0, selectedDisplayArea.getTop(), w, selectedDisplayArea.getHeight());

            float y = isFlipped() ? selectedDisplayArea.getTop() + 1 : selectedDisplayArea.getBottom();
            //leave gap at selected zone tab
            g.drawLine(1, MatchScreen.BORDER_COLOR, 0, y, selectedTab.getLeft(), y);
            g.drawLine(1, MatchScreen.BORDER_COLOR, selectedTab.getRight(), y, w, y);
        }
    }

    private class LifeLabel extends FDisplayObject {
        private int life = 20;
        private String lifeStr = String.valueOf(life);

        private LifeLabel() {
        }

        private void update() {
            int delta = player.getLife() - life;
            if (delta == 0) { return; }

            if (delta < 0) {
                //TODO: Show animation on avatar for life loss
                if (player.getLobbyPlayer() == FControl.getGuiPlayer()) {
                    //when gui player loses life, vibrate device for a length of time based on amount of life lost
                    Gdx.input.vibrate(Math.min(delta * -100, 2000)); //never vibrate more than two seconds regardless of life lost
                }
            }
            life = player.getLife();
            lifeStr = String.valueOf(life);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            FControl.getInputProxy().selectPlayer(player, null); //treat tapping on life the same as tapping on the avatar
            return true;
        }

        @Override
        public void draw(Graphics g) {
            g.drawText(lifeStr, LIFE_FONT, INFO_FORE_COLOR, 0, 0, getWidth(), getHeight(), false, HAlignment.CENTER, true);
        }
    }

    public class InfoTab extends FDisplayObject {
        private String value = "0";
        private final FSkinImage icon;
        private final VDisplayArea displayArea;

        private InfoTab(FSkinImage icon0, VDisplayArea displayArea0) {
            icon = icon0;
            displayArea = displayArea0;
        }

        public VDisplayArea getDisplayArea() {
            return displayArea;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (selectedTab == this) {
                setSelectedTab(null);
            }
            else {
                setSelectedTab(this);
            }
            return true;
        }

        public void update() {
            displayArea.update();
            value = String.valueOf(displayArea.getCount());
        }

        @Override
        public void draw(Graphics g) {
            float x, y, w, h;

            if (selectedTab == this) {
                y = 0;
                w = getWidth();
                h = getHeight();
                float yAcross;
                if (isFlipped()) {
                    y += INFO_TAB_PADDING_Y;
                    yAcross = y;
                    y--;
                    h++;
                }
                else {
                    h -= INFO_TAB_PADDING_Y;
                    yAcross = h;
                    y--;
                    h += 2;
                }
                g.fillRect(DISPLAY_AREA_BACK_COLOR, 0, isFlipped() ? INFO_TAB_PADDING_Y : 0, w, getHeight() - INFO_TAB_PADDING_Y);
                g.startClip(-1, y, w + 2, yAcross - y); //use clip to ensure all corners connect
                g.drawLine(1, MatchScreen.BORDER_COLOR, 0, yAcross, w, yAcross);
                g.drawLine(1, MatchScreen.BORDER_COLOR, 0, y, 0, h);
                g.drawLine(1, MatchScreen.BORDER_COLOR, w, y, w, h);
                g.endClip();
            }

            //show image left of text if wider than tall
            if (getWidth() > getHeight()) {
                float maxImageWidth = getWidth() - INFO_FONT.getFont().getBounds("0").width - 3 * INFO_TAB_PADDING_X;
                w = icon.getNearestHQWidth(maxImageWidth);
                if (w > maxImageWidth) {
                    w /= 2;
                }
                h = icon.getHeight() * w / icon.getWidth();
                x = INFO_TAB_PADDING_X + (maxImageWidth - w) / 2;
                y = (getHeight() - h) / 2;
                g.drawImage(icon, x, y, w, h);

                x += w + INFO_TAB_PADDING_X;
                g.drawText(value, INFO_FONT, INFO_FORE_COLOR, x, 0, getWidth() - x + 1, getHeight(), false, HAlignment.LEFT, true);
            }
            else { //show image above text if taller than wide
                float maxImageWidth = getWidth() - 2 * INFO_TAB_PADDING_X;
                w = icon.getNearestHQWidth(maxImageWidth);
                if (w > maxImageWidth) {
                    w /= 2;
                }
                h = icon.getHeight() * w / icon.getWidth();
                x = (getWidth() - w) / 2;
                y = INFO_TAB_PADDING_Y;
                g.drawImage(icon, x, y, w, h);

                y += h + INFO_TAB_PADDING_Y;
                g.drawText(value, INFO_FONT, INFO_FORE_COLOR, 0, y, getWidth(), getHeight() - y + 1, false, HAlignment.CENTER, false);
            }
        }
    }
}
