package forge.screens.match.views;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinImageInterface;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FMenuBar;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import forge.util.Utils;
import forge.util.collect.FCollectionView;
import org.apache.commons.text.WordUtils;

public class VPlayerPanel extends FContainer {
    private static final FSkinFont LIFE_FONT = FSkinFont.get(18);
    private static final FSkinFont LIFE_FONT_ALT = FSkinFont.get(22);
    private static final FSkinFont INFO_FONT = FSkinFont.get(12);
    private static final FSkinFont INFO2_FONT = FSkinFont.get(14);
    private static FSkinColor getInfoForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }
    private static FSkinColor getDisplayAreaBackColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_INACTIVE).alphaColor(0.5f);
        return FSkinColor.get(Colors.CLR_INACTIVE).alphaColor(0.5f);
    }
    private static FSkinColor getDeliriumHighlight() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_PHASE_ACTIVE_ENABLED).alphaColor(0.5f);
        return FSkinColor.get(Colors.CLR_PHASE_ACTIVE_ENABLED).alphaColor(0.5f);
    }
    private static final float INFO_TAB_PADDING_X = Utils.scale(2);
    private static final float INFO_TAB_PADDING_Y = Utils.scale(2);

    /**
     * Zones to include in the extra zones dropdown.
     */
    private static final EnumSet<ZoneType> EXTRA_ZONES = EnumSet.of(ZoneType.Sideboard, ZoneType.PlanarDeck,
            ZoneType.SchemeDeck, ZoneType.ContraptionDeck, ZoneType.AttractionDeck, ZoneType.Junkyard, ZoneType.Ante);

    private final PlayerView player;
    private final VPhaseIndicator phaseIndicator;
    private final VField field;
    private final VAvatar avatar;
    private final VZoneDisplay commandZone;
    private final LifeLabel lblLife;
    private final InfoTab tabManaPool;
    private final Map<ZoneType, InfoTabZone> zoneTabs = new HashMap<>();
    private final InfoTabExtra extraTab;
    private final List<InfoTab> tabs = new ArrayList<>();
    private InfoTab selectedTab;
    private VField.FieldRow selectedRow;
    private float avatarHeight = VAvatar.HEIGHT;
    private float displayAreaHeightFactor = 1.0f;
    private boolean forMultiPlayer = false;
    public int adjustHeight = 1;
    private int selected = 0;
    private boolean isBottomPlayer = false;
    public VPlayerPanel(PlayerView player0, boolean showHand, int playerCount) {
        player = player0;
        phaseIndicator = add(new VPhaseIndicator());

        if(playerCount > 2){
            forMultiPlayer = true;
            avatarHeight *= 0.5f;
            //displayAreaHeightFactor *= 0.7f;
        }
        field = add(new VField(player));
        selectedRow = field.getRow1();
        avatar = add(new VAvatar(player, avatarHeight));
        lblLife = add(new LifeLabel());
        addZoneDisplay(ZoneType.Hand);
        addZoneDisplay(ZoneType.Graveyard);
        addZoneDisplay(ZoneType.Library);
        addZoneDisplay(ZoneType.Flashback);

        VManaPool manaPool = add(new VManaPool(player));
        tabManaPool = add(new InfoTabSingleDisplay(FSkinImage.HDMANAPOOL, manaPool));
        tabs.add(tabManaPool);

        addZoneDisplay(ZoneType.Exile);
        extraTab = add(new InfoTabExtra());
        tabs.add(extraTab);

        commandZone = add(new CommandZoneDisplay(player));

        if (showHand) {
            setSelectedZone(ZoneType.Hand);
        }
    }

    public PlayerView getPlayer() {
        return player;
    }

    public void setBottomPlayer(boolean val) {
        isBottomPlayer = val;
    }

    public void addZoneDisplay(ZoneType zoneType) {
        VZoneDisplay zoneDisplay = add(new VZoneDisplay(player, zoneType));
        InfoTabZone zoneTab = add(new InfoTabZone(zoneDisplay, zoneType));
        zoneTabs.put(zoneType, zoneTab);
        tabs.add(zoneTab);
    }

    public static FSkinImage iconFromZone(ZoneType zoneType) {
        switch (zoneType) {
            case Hand: return Forge.hdbuttons ? FSkinImage.HDHAND : FSkinImage.HAND;
            case Library: return Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.LIBRARY;
            case Graveyard: return Forge.hdbuttons ? FSkinImage.HDGRAVEYARD : FSkinImage.GRAVEYARD;
            case Exile: return Forge.hdbuttons ? FSkinImage.HDEXILE : FSkinImage.EXILE;
            case Sideboard: return Forge.hdbuttons ? FSkinImage.HDSIDEBOARD :FSkinImage.SIDEBOARD;
            case Flashback: return Forge.hdbuttons ? FSkinImage.HDFLASHBACK :FSkinImage.FLASHBACK;
            case Command: return FSkinImage.COMMAND;
            case PlanarDeck: return FSkinImage.PLANAR;
            case SchemeDeck: return FSkinImage.SCHEME;
            case AttractionDeck: return FSkinImage.ATTRACTION;
            case ContraptionDeck: return FSkinImage.CONTRAPTION;
            case Ante: return FSkinImage.ANTE;
            case Junkyard: return FSkinImage.JUNKYARD;
            default: return FSkinImage.HDLIBRARY;
        }
    }

    public Iterable<InfoTab> getTabs() {
        return tabs;
    }

    public InfoTab getSelectedTab() {
        return selectedTab;
    }

    public void resetZoneTabs() {
        for(InfoTab tab : tabs)
            tab.reset();
    }

    public void setSelectedZone(ZoneType zoneType) {
        if(zoneTabs.containsKey(zoneType))
            setSelectedTab(zoneTabs.get(zoneType));
        else {
            extraTab.setActiveZone(zoneType);
            setSelectedTab(extraTab);
        }
    }

    public void hideSelectedTab() {
        if (selectedTab != null) {
            selectedTab.setDisplayVisible(false);
            selectedTab = null;
        }
    }

    public void setSelectedTab(InfoTab tab) {
        if (this.selectedTab == tab) {
            return;
        }

        hideSelectedTab();

        this.selectedTab = tab;
        selected = tabs.indexOf(tab);

        if (this.selectedTab != null) {
            this.selectedTab.setDisplayVisible(true);
        }

        if (MatchController.getView() != null) { //must revalidate entire screen so panel heights updated
            MatchController.getView().revalidate();
        }
    }
    public void setNextSelectedTab(boolean change) {
        if (change) {
            if (selectedTab != null) {
                selectedTab.setDisplayVisible(false);
                selectedTab = null;
            }
            if (MatchController.getView() != null) { //must revalidate entire screen so panel heights updated
                MatchController.getView().revalidate();
            }
        }
        if (!change)
            selected++;
        else
            hideSelectedTab();
        int numTabs = tabs.size();
        int numExtraTabs = extraTab.displayAreas.size();
        if (selected < 0 || selected >= numTabs + numExtraTabs)
            selected = 0;
        if(selected >= numTabs) {
            extraTab.setActiveZoneByIndex(selected - numTabs);
            setSelectedTab(extraTab);
        }
        else
            setSelectedTab(tabs.get(selected));
    }
    public void closeSelectedTab() {
        if (selectedTab != null) {
            selectedTab.setDisplayVisible(false);
            selectedTab = null;
        }
        if (MatchController.getView() != null) { //must revalidate entire screen so panel heights updated
            MatchController.getView().revalidate();
        }
        selected--;
        if (selected < -1)
            selected = -1;
    }

    public InfoTab getManaPoolTab() {
        return tabManaPool;
    }

    public boolean isFlipped() {
        return field.isFlipped();
    }
    public void setFlipped(boolean flipped0) {
        field.setFlipped(flipped0);
    }

    @Override
    public void setRotate180(boolean b0) {
        //only rotate certain parts of panel
        avatar.setRotate180(b0);
        lblLife.setRotate180(b0);
        phaseIndicator.setRotate180(b0);
        for (InfoTab tab : tabs) {
            tab.setRotate180(b0);
        }
        field.getRow1().setRotate180(b0);
        field.getRow2().setRotate180(b0);
    }

    public VField getField() {
        return field;
    }
    public VField.FieldRow getSelectedRow() {
        return selectedRow;
    }
    public void switchRow() {
        if (selectedRow == field.getRow1())
            selectedRow = field.getRow2();
        else
            selectedRow = field.getRow1();
    }

    public VPhaseIndicator getPhaseIndicator() {
        return phaseIndicator;
    }

    public VAvatar getAvatar() {
        return avatar;
    }

    public VZoneDisplay getCommandZone() {
        return commandZone;
    }

    public void updateLife() {
        lblLife.update();
    }

    public void updateShards() {
        lblLife.updateShards();
    }

    public void updateManaPool() {
        tabManaPool.update();
    }

    public void updateZone(ZoneType zoneType) {
        if (zoneType == ZoneType.Battlefield ) {
            field.update(true);
        }
        else if (zoneType == ZoneType.Command) {
            commandZone.update();
        }
        else {
            if(zoneTabs.containsKey(zoneType))
                zoneTabs.get(zoneType).update();
            else if(EXTRA_ZONES.contains(zoneType)) {
                extraTab.update(zoneType);
            }

            //update flashback zone when graveyard, library, exile, or stack zones updated
            switch (zoneType) {
            case Graveyard:
            case Library:
            case Exile:
            case Stack:
                zoneTabs.get(ZoneType.Flashback).update();
                break;
            default:
                break;
            }
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        if (Forge.isLandscapeMode()) {
            doLandscapeLayout(width, height);
            return;
        }

        //layout for bottom panel by default
        float x = avatarHeight;
        float w = width - avatarHeight;
        float indicatorScale = 1f;
        if(avatarHeight<VAvatar.HEIGHT){
            indicatorScale = 0.6f;
        }
        float h = phaseIndicator.getPreferredHeight(w) * indicatorScale;
        phaseIndicator.setBounds(x, height - h, w, h);

        float y = height - avatarHeight;
        float displayAreaHeight = displayAreaHeightFactor * y / 3;
        y -= displayAreaHeight;
        for (InfoTab tab : tabs) {
            tab.setDisplayBounds(0, y, width, displayAreaHeight);
        }

        y = height - avatarHeight;
        avatar.setPosition(0, y);

        float lifeLabelWidth = LIFE_FONT.getBounds("99").width * 1.2f * indicatorScale; //make just wide enough for 2-digit life totals
        float infoLabelHeight = avatarHeight - phaseIndicator.getHeight();
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

        //account for command zone if needed
        int commandZoneCount = commandZone.getCount();
        if (commandZoneCount > 0) {
            float commandZoneHeight = y / 2;
            float commandZoneWidth = Math.min(commandZoneCount, 2) * commandZone.getCardWidth(commandZoneHeight);
            commandZone.setBounds(width - commandZoneWidth, y - commandZoneHeight, commandZoneWidth, commandZoneHeight);

            field.setCommandZoneWidth(commandZoneWidth + 1); //ensure second row of field accounts for width of command zone and its border
        }
        else {
            field.setCommandZoneWidth(0);
        }

        field.setBounds(0, 0, width, y);

        if (isFlipped()) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }

        //this is used for landscape so set this to 0
        field.setFieldModifier(0);
    }

    private float initW, initH, commandZoneWidth, commandZoneCount, avatarWidth, prefWidth;
    private final float mod = 2.4f;
    private void doLandscapeLayout(float width, float height) {
        initW = width;
        initH = height;
        float x = 0;
        float y = 0;
        float yAlt = 0;
        avatarWidth = Forge.altZoneTabs ? avatar.getWidth() : 0;
        avatar.setPosition(x, y);
        y += avatar.getHeight();

        lblLife.setBounds(x, (Forge.altPlayerLayout && !Forge.altZoneTabs) ? 0 : y, avatar.getWidth(), (Forge.altPlayerLayout && !Forge.altZoneTabs) ? INFO_FONT.getLineHeight() : Forge.altZoneTabs ? LIFE_FONT_ALT.getLineHeight() : LIFE_FONT.getLineHeight());
        if (Forge.altPlayerLayout && !Forge.altZoneTabs) {
            if (adjustHeight > 2)
                y += INFO_FONT.getLineHeight()/2;
        } else
            y += lblLife.getHeight();

        float infoTabWidth = avatar.getWidth();
        int tabSize = !Forge.altZoneTabs ? tabs.size() : tabs.size() - 4;
        float infoTabHeight = (height - y) / tabSize;
        float infoTabHeightAlt = (height - yAlt) / 4;

        for (InfoTab tab : tabs) {
            if (!Forge.altZoneTabs) {
                tab.setBounds(x, y, infoTabWidth, infoTabHeight);
                y += infoTabHeight;
            } else {
                if (!tab.isAlignedRightForAltDisplay()) {
                    tab.setBounds(x, y, infoTabWidth, infoTabHeight);
                    y += infoTabHeight;
                } else {
                    tab.setBounds(x+width-avatarWidth, yAlt, avatarWidth, infoTabHeightAlt);
                    yAlt += infoTabHeightAlt;
                }
            }
        }
        x = avatar.getRight();
        phaseIndicator.resetFont();
        phaseIndicator.setBounds(x, 0, avatar.getWidth() * 0.6f, height);
        x += phaseIndicator.getWidth();

        float fieldWidth = width - x - avatarWidth;
        float displayAreaWidth = height / FCardPanel.ASPECT_RATIO;
        if (selectedTab != null) {
            fieldWidth -= displayAreaWidth;
        }

        //account for command zone if needed
        commandZoneWidth = 0f;
        commandZoneCount = commandZone.getCount();
        if (commandZoneCount > 0) {
            float commandZoneHeight = height / 2;
            float minCommandCards = Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode) ? 5 : 2;
            commandZoneWidth = Math.min(commandZoneCount, minCommandCards) * commandZone.getCardWidth(commandZoneHeight);
            float x2 = x + fieldWidth - commandZoneWidth;
            float y2 = height - commandZoneHeight;
            if (Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode)) {
                x2 = width - avatarWidth - commandZoneWidth;
                y2 = 0;
            }
            commandZone.setBounds(x2, y2, commandZoneWidth, commandZoneHeight);
            if (isFlipped()) { //flip across x-axis if needed
                commandZone.setTop(height - commandZone.getBottom());
            }

            field.setCommandZoneWidth(commandZoneWidth + 1); //ensure second row of field accounts for width of command zone and its border
        } else {
            field.setCommandZoneWidth(0);
        }
        prefWidth = width / mod;
        if (Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode)) {
            field.setBounds(x, 0, width - (avatarWidth / 16f), height);
            updateFieldDisplayArea(width);
        } else
            field.setBounds(x, 0, fieldWidth, height);

        x = width - displayAreaWidth-avatarWidth;
        for (InfoTab tab : tabs) {
            if (Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode)) {
                updateTabDisplayArea(tab, width, height);
            } else {
                tab.setDisplayBounds(x, 0, displayAreaWidth, height);
            }
        }

        if (!Forge.altZoneTabs) {
            field.setFieldModifier(0);
        } else {
            if (!"Horizontal".equalsIgnoreCase(Forge.altZoneTabMode))
                field.setFieldModifier(avatarWidth / 16);
        }
    }

    private void updateFieldDisplayArea(float width) {
        field.getRow1().setWidth(width - (avatarWidth / 8f) - (commandZoneCount > 0 ? commandZoneWidth + 1 : 0));
        field.getRow2().setWidth(width - (avatarWidth / 8f) - (selectedTab == null ? 0 : selectedTab.getIdealWidth(prefWidth) + 1) - avatarWidth * mod);
    }

    private void updateTabDisplayArea(InfoTab tab, float width, float height) {
        float w = tab.getIdealWidth(prefWidth);
        float h = height / 2f;
        tab.setDisplayBounds(width - w - avatarWidth, isBottomPlayer ? h : 0, w, h);
    }

    @Override
    public void drawBackground(Graphics g) {
        float y;
        InfoTab infoTab = selectedTab;
        if (infoTab != null) { //draw background and border for selected zone if needed
            VDisplayArea selectedDisplayArea = infoTab.getDisplayArea();
            float x = selectedDisplayArea == null ? 0 : selectedDisplayArea.getLeft();
            float w = selectedDisplayArea == null ? 0 : selectedDisplayArea.getWidth();
            float top = selectedDisplayArea == null ? 0 : selectedDisplayArea.getTop();
            float h = selectedDisplayArea == null ? 0 : selectedDisplayArea.getHeight();
            float bottom = selectedDisplayArea == null ? 0 : selectedDisplayArea.getBottom();
            g.fillRect(getDisplayAreaBackColor(), x, top, w, h);

            if (Forge.isLandscapeMode()) {
                g.drawLine(1, MatchScreen.getBorderColor(), x, top, x, bottom);
            }
            else {
                y = isFlipped() ? top + 1 : bottom;
                //don't know why infotab gets null here, either way don't crash the gui..
                float left = infoTab == null ? 0 : infoTab.getLeft();
                float right = infoTab == null ? 0 : infoTab.getRight();
                //leave gap at selected zone tab
                g.drawLine(1, MatchScreen.getBorderColor(), x, y, left, y);
                g.drawLine(1, MatchScreen.getBorderColor(), right, y, w, y);
            }
        }
        if (commandZone != null && commandZone.isVisible()) { //draw border for command zone if needed
            float x = commandZone.getLeft();
            y = commandZone.getTop();
            g.drawLine(1, MatchScreen.getBorderColor(), x, y, x, y + commandZone.getHeight());
            if (isFlipped()) {
                y += commandZone.getHeight();
            }
            g.drawLine(1, MatchScreen.getBorderColor(), x, y, x + commandZone.getWidth(), y);
        }
    }

    public Iterable<FScrollPane> getAllScrollPanes() {
        //Used to catalog scroll positions before resizing UI.
        ArrayList<FScrollPane> out = new ArrayList<>();
        out.add(field.getRow1());
        out.add(field.getRow2());
        for(InfoTabZone tab : zoneTabs.values())
            out.add(tab.displayArea);
        out.add(commandZone);
        out.addAll(extraTab.displayAreas.values());
        return out;
    }

    private class LifeLabel extends FDisplayObject {
        private int life = player.getLife();
        private int poisonCounters = player.getCounters(CounterEnumType.POISON);
        private int energyCounters = player.getCounters(CounterEnumType.ENERGY);
        private int experienceCounters = player.getCounters(CounterEnumType.EXPERIENCE);
        private int ticketCounters = player.getCounters(CounterEnumType.TICKET);
        private int radCounters = player.getCounters(CounterEnumType.RAD);
        private int manaShards = player.getNumManaShards();
        private String lifeStr = String.valueOf(life);

        private LifeLabel() {
        }

        private void update() {
            int vibrateDuration = 0;
            int delta = player.getLife() - life;
            player.setAvatarLifeDifference(player.getAvatarLifeDifference()+delta);
            if (delta != 0) {
                if (delta < 0) {
                    vibrateDuration += delta * -100;
                }
                life = player.getLife();
                lifeStr = String.valueOf(life);
            }

            delta = player.getCounters(CounterEnumType.POISON) - poisonCounters;
            if (delta != 0) {
                if (delta > 0) {
                    //TODO: Show animation on avatar for gaining poison counters
                    vibrateDuration += delta * 200;
                }
                poisonCounters = player.getCounters(CounterEnumType.POISON);
            }

            energyCounters = player.getCounters(CounterEnumType.ENERGY);
            experienceCounters = player.getCounters(CounterEnumType.EXPERIENCE);
            ticketCounters = player.getCounters(CounterEnumType.TICKET);
            radCounters = player.getCounters(CounterEnumType.RAD);
            manaShards = player.getNumManaShards();

            //when gui player loses life, vibrate device for a length of time based on amount of life lost
            if (vibrateDuration > 0 && MatchController.instance.isLocalPlayer(player) &&
                    FModel.getPreferences().getPrefBoolean(FPref.UI_VIBRATE_ON_LIFE_LOSS)) {
                //never vibrate more than two seconds regardless of life lost or poison counters gained
                Gdx.input.vibrate(Math.min(vibrateDuration, 2000));
            }
        }
        private void updateShards() {
            manaShards = player.getNumManaShards();
        }

        @Override
        public boolean tap(float x, float y, int count) {
            MatchController.instance.getGameController().selectPlayer(player, null); //treat tapping on life the same as tapping on the avatar
            return true;
        }

        @Override
        public void draw(Graphics g) {
            adjustHeight = 1;
            float divider = Gdx.app.getGraphics().getHeight() > 900 ? 1.2f : 2f;
            if(Forge.altPlayerLayout && !Forge.altZoneTabs && Forge.isLandscapeMode()) {
                if (poisonCounters == 0 && energyCounters == 0 && experienceCounters == 0 && ticketCounters == 0 && radCounters == 0 && manaShards == 0) {
                    g.fillRect(Color.DARK_GRAY, 0, 0, INFO2_FONT.getBounds(lifeStr).width+1, INFO2_FONT.getBounds(lifeStr).height+1);
                    g.drawText(lifeStr, INFO2_FONT, getInfoForeColor().getColor(), 0, 0, getWidth(), getHeight(), false, Align.left, false);
                } else {
                    float halfHeight = getHeight() / divider;
                    float textStart = halfHeight + Utils.scale(1);
                    float textWidth = getWidth() - textStart;
                    int mod = 1;
                    g.fillRect(Color.DARK_GRAY, 0, 0, INFO_FONT.getBounds(lifeStr).width+halfHeight+1, INFO_FONT.getBounds(lifeStr).height+1);
                    g.drawImage(FSkinImage.QUEST_LIFE, 0, 0, halfHeight, halfHeight);
                    g.drawText(lifeStr, INFO_FONT, getInfoForeColor().getColor(), textStart, 0, textWidth, halfHeight, false, Align.left, false);
                    if (poisonCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, halfHeight+2, INFO_FONT.getBounds(String.valueOf(poisonCounters)).width+halfHeight+1, INFO_FONT.getBounds(String.valueOf(poisonCounters)).height+1);
                        g.drawImage(FSkinImage.POISON, 0, halfHeight+2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(poisonCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, halfHeight+2, textWidth, halfHeight, false, Align.left, false);
                        mod+=1;
                    }
                    if (energyCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight*mod)+2, INFO_FONT.getBounds(String.valueOf(energyCounters)).width+halfHeight+1, INFO_FONT.getBounds(String.valueOf(energyCounters)).height+1);
                        g.drawImage(FSkinImage.ENERGY, 0, (halfHeight*mod)+2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(energyCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight*mod)+2, textWidth, halfHeight, false, Align.left, false);
                        mod+=1;
                    }
                    if (experienceCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight*mod)+2, INFO_FONT.getBounds(String.valueOf(experienceCounters)).width+halfHeight+1, INFO_FONT.getBounds(String.valueOf(experienceCounters)).height+1);
                        g.drawImage(FSkinImage.COMMANDER, 0, (halfHeight*mod)+2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(experienceCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight*mod)+2, textWidth, halfHeight, false, Align.left, false);
                        mod+=1;
                    }
                    if (radCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight*mod)+2, INFO_FONT.getBounds(String.valueOf(radCounters)).width+halfHeight+1, INFO_FONT.getBounds(String.valueOf(radCounters)).height+1);
                        g.drawImage(FSkinImage.RAD, 0, (halfHeight*mod)+2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(radCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight*mod)+2, textWidth, halfHeight, false, Align.left, false);
                        mod+=1;
                    }
                    if (ticketCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight*mod)+2, INFO_FONT.getBounds(String.valueOf(ticketCounters)).width+halfHeight+1, INFO_FONT.getBounds(String.valueOf(ticketCounters)).height+1);
                        g.drawImage(FSkinImage.TICKET, 0, (halfHeight*mod)+2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(ticketCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight*mod)+2, textWidth, halfHeight, false, Align.left, false);
                        mod+=1;
                    }
                    if (manaShards > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight*mod)+2, INFO_FONT.getBounds(String.valueOf(manaShards)).width+halfHeight+1, INFO_FONT.getBounds(String.valueOf(manaShards)).height+1);
                        g.drawImage(FSkinImage.AETHER_SHARD, 0, (halfHeight*mod)+2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(manaShards), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight*mod)+2, textWidth, halfHeight, false, Align.left, false);
                        mod+=1;
                    }
                    adjustHeight = (mod > 2) && (avatar.getHeight() < halfHeight*mod)? mod : 1;
                }
            } else {
                if (poisonCounters == 0 && energyCounters == 0 && manaShards == 0) {
                    g.drawText(lifeStr, Forge.altZoneTabs ? LIFE_FONT_ALT : LIFE_FONT, getInfoForeColor(), 0, 0, getWidth(), getHeight(), false, Align.center, true);
                } else {
                    float halfHeight = getHeight() / 2;
                    float textStart = halfHeight + Utils.scale(1);
                    float textWidth = getWidth() - textStart;
                    g.drawImage(FSkinImage.QUEST_LIFE, 0, 0, halfHeight, halfHeight);
                    g.drawText(lifeStr, INFO_FONT, getInfoForeColor(), textStart, 0, textWidth, halfHeight, false, Align.center, true);
                    if (poisonCounters > 0) { //prioritize showing poison counters over energy counters
                        g.drawImage(FSkinImage.POISON, 0, halfHeight, halfHeight, halfHeight);
                        g.drawText(String.valueOf(poisonCounters), INFO_FONT, getInfoForeColor(), textStart, halfHeight, textWidth, halfHeight, false, Align.center, true);
                    } else if (energyCounters > 0) { //prioritize showing energy counters over mana shards
                        g.drawImage(FSkinImage.ENERGY, 0, halfHeight, halfHeight, halfHeight);
                        g.drawText(String.valueOf(energyCounters), INFO_FONT, getInfoForeColor(), textStart, halfHeight, textWidth, halfHeight, false, Align.center, true);
                    }
                    else {
                        g.drawImage(FSkinImage.MANASHARD, 0, halfHeight, halfHeight, halfHeight);
                        g.drawText(String.valueOf(manaShards), INFO_FONT, getInfoForeColor(), textStart, halfHeight, textWidth, halfHeight, false, Align.center, true);
                    }
                }
            }
        }
    }

    /**
     * A tab in the player panel, which toggles the visibility of the player's zones or mana pool.
     */
    public abstract class InfoTab extends FDisplayObject {
        protected String value = "0";
        protected FSkinImageInterface icon;

        protected InfoTab(FSkinImageInterface icon) {
            this.icon = icon;
        }

        public FSkinImageInterface getIcon() {
            return icon;
        }

        public abstract VDisplayArea getDisplayArea();
        public abstract void setDisplayVisible(boolean visible);
        public abstract void setDisplayBounds(float x, float y, float width, float height);
        public abstract void setRotate180(boolean rotate180);
        public abstract void update();
        public abstract void reset();
        public abstract float getIdealWidth(float pref);

        protected boolean isSelected() {
            return selectedTab == this;
        }
        protected FSkinColor getSelectedBackgroundColor() {
            return getDisplayAreaBackColor();
        }
        protected boolean isAlignedRightForAltDisplay() {
            return false;
        }

        @Override
        public void draw(Graphics g) {
            float x, y, w, h;
            boolean drawOverlay = MatchController.getView().selectedPlayerPanel().getPlayer() == player && Forge.hasGamepad();
            if (Forge.altZoneTabs && this.isAlignedRightForAltDisplay()) {
                //draw extra
                g.fillRect(FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2), 0, 0, getWidth(), getHeight());
                if (isSelected()) {
                    if (drawOverlay)
                        g.fillRect(FSkinColor.getStandardColor(50, 200, 150).alphaColor(0.3f), 0, isFlipped() ? INFO_TAB_PADDING_Y : 0, getWidth(), getHeight() - INFO_TAB_PADDING_Y);
                    g.fillRect(getDisplayAreaBackColor(), 0, isFlipped() ? INFO_TAB_PADDING_Y : 0, getWidth(), getHeight() - INFO_TAB_PADDING_Y);
                }
            }
            if (isSelected()) {
                y = 0;
                w = getWidth();
                h = getHeight();
                float yAcross;
                if (isFlipped()) {
                    y += INFO_TAB_PADDING_Y;
                    yAcross = y;
                    y--;
                    h++;
                } else {
                    h -= INFO_TAB_PADDING_Y;
                    yAcross = h;
                    y--;
                    h += 2;
                }
                if (drawOverlay)
                    g.fillRect(FSkinColor.getStandardColor(50, 200, 150).alphaColor(0.3f), 0, isFlipped() ? INFO_TAB_PADDING_Y : 0, w, getHeight() - INFO_TAB_PADDING_Y);
                //change the graveyard tab selection color to active phase color to indicate the player has delirium
                g.fillRect(this.getSelectedBackgroundColor(), 0, isFlipped() ? INFO_TAB_PADDING_Y : 0, w, getHeight() - INFO_TAB_PADDING_Y);
                if (!Forge.isLandscapeMode()) {
                    if (isFlipped()) { //use clip to ensure all corners connect
                        g.startClip(-1, y, w + 2, h);
                    } else {
                        g.startClip(-1, y, w + 2, yAcross - y);
                    }
                    if (forMultiPlayer) {
                        g.drawLine(1, MatchScreen.getBorderColor(), 0, yAcross, w, yAcross);
                        g.drawLine(1, MatchScreen.getBorderColor(), 0, y, 0, h);
                        g.drawLine(1, MatchScreen.getBorderColor(), w, y, w, h);
                    }
                    g.endClip();
                }
            }

            FSkinImageInterface icon = this.getIcon();

            //show image left of text if wider than tall
            if (getWidth() > getHeight()) {
                float maxImageWidth = getWidth() - INFO_FONT.getBounds("0").width - 3 * INFO_TAB_PADDING_X;
                w = icon.getNearestHQWidth(maxImageWidth);
                if (w > maxImageWidth) {
                    w /= 2;
                }
                h = icon.getHeight() * w / icon.getWidth();
                float maxImageHeight = getHeight() - 2 * INFO_TAB_PADDING_Y;
                if (h > maxImageHeight) {
                    h = icon.getNearestHQHeight(maxImageHeight);
                    if (h > maxImageWidth) {
                        h /= 2;
                    }
                    w = icon.getWidth() * h / icon.getHeight();
                }
                x = INFO_TAB_PADDING_X + (maxImageWidth - w) / 2;
                y = (getHeight() - h) / 2;
                if (lblLife.getRotate180()) {
                    g.startRotateTransform(x + w / 2, y + h / 2, 180);
                }
                float mod = isHovered() ? w/8f:0;
                g.drawImage(icon, x-mod/2, y-mod/2, w+mod, h+mod);
                if (lblLife.getRotate180()) {
                    g.endTransform();
                }

                x += w + INFO_TAB_PADDING_X;
                int alignX = Align.left;
                if (lblLife.getRotate180()) {
                    g.startRotateTransform(x + (getWidth() - x + 1) / 2, getHeight() / 2, 180);
                    alignX = Align.right;
                }
                g.drawText(value, INFO_FONT, getInfoForeColor(), x, 0, getWidth() - x + 1, getHeight(), false, alignX, true);
                if (lblLife.getRotate180()) {
                    g.endTransform();
                }
            } else { //show image above text if taller than wide
                if (lblLife.getRotate180()) {
                    g.startRotateTransform(getWidth() / 2, getHeight() / 2, 180);
                }
                float maxImageWidth = getWidth() - 2 * INFO_TAB_PADDING_X;
                w = icon.getNearestHQWidth(maxImageWidth);
                if (w > maxImageWidth) {
                    w /= 2;
                }
                h = icon.getHeight() * w / icon.getWidth();
                x = (getWidth() - w) / 2;
                y = INFO_TAB_PADDING_Y;
                float mod = isHovered() ? w/8f:0;
                g.drawImage(icon, x-mod/2, y-mod/2, w+mod, h+mod);

                y += h + INFO_TAB_PADDING_Y;
                g.drawText(value, INFO_FONT, getInfoForeColor(), 0, y, getWidth(), getHeight() - y + 1, false, Align.center, false);
                if (lblLife.getRotate180()) {
                    g.endTransform();
                }
            }
        }
    }

    /**
     * Player panel tab linked to a single display area - usually either the mana pool or a zone via InfoTabZone.
     */
    public class InfoTabSingleDisplay extends InfoTab {
        protected final VDisplayArea displayArea;

        private InfoTabSingleDisplay(FSkinImageInterface icon, VDisplayArea displayArea) {
            super(icon);
            this.displayArea = displayArea;
        }

        public VDisplayArea getDisplayArea() {
            return displayArea;
        }

        public void setDisplayVisible(boolean visible) {
            this.displayArea.setVisible(visible);
        }

        public void setDisplayBounds(float x, float y, float width, float height) {
            this.displayArea.setBounds(x, y, width, height);
        }

        public void setRotate180(boolean rotate180) {
            this.displayArea.setRotate180(rotate180);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (isSelected())
                setSelectedTab(null);
            else
                setSelectedTab(this);
            return true;
        }

        public void update() {
            displayArea.update();
            value = String.valueOf(displayArea.getCount());
        }

        @Override
        public void reset() {} //Mana Display does not get cleared.

        @Override
        public float getIdealWidth(float pref) {
            return pref;
        }
    }

    /**
     * Player panel tab for a single typical card zone, such as the hand.
     */
    public class InfoTabZone extends InfoTabSingleDisplay {
        public final ZoneType zoneType;

        private InfoTabZone(VDisplayArea displayArea, ZoneType zoneType) {
            super(iconFromZone(zoneType), displayArea);
            this.zoneType = zoneType;
        }

        private final EnumSet<ZoneType> altDisplayZones = EnumSet.of(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile);
        public boolean isAlignedRightForAltDisplay() {
            return altDisplayZones.contains(this.zoneType);
        }

        @Override
        protected FSkinColor getSelectedBackgroundColor() {
            if ((this.zoneType == ZoneType.Graveyard) && player.hasDelirium())
                return getDeliriumHighlight();
            return super.getSelectedBackgroundColor();
        }

        @Override
        public void reset() {
            displayArea.clear();
        }

        @Override
        public float getIdealWidth(float pref) {
            if (displayArea instanceof VCardDisplayArea vCardDisplayArea) {
                float cardWidth = vCardDisplayArea.getCardWidth(vCardDisplayArea.getHeight());
                float size = vCardDisplayArea.getCount();
                return Math.min(cardWidth * size, pref);
            }
            return pref;
        }

        @Override
        public void update() {
            super.update();
            if (selectedTab != null && Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode)) {
                updateFieldDisplayArea(initW);
                updateTabDisplayArea(selectedTab, initW, initH);
            }
        }
    }

    /**
     * Player panel tab that can contain several extra rarely-used zones. Displays a dropdown when clicked, letting the
     * player pick which one to show.
     */
    public class InfoTabExtra extends InfoTab {
        private static final FSkinImageInterface DEFAULT_ICON = FSkinImage.HDSTAR_OUTLINE;

        private final EnumMap<ZoneType, VDisplayArea> displayAreas;
        private ZoneType activeZone;
        private boolean hasCardsInExtraZone = false;

        private InfoTabExtra() {
            super(DEFAULT_ICON);
            this.displayAreas = new EnumMap<>(ZoneType.class);
            for(ZoneType zoneType : EXTRA_ZONES) {
                FCollectionView<CardView> cards = player.getCards(zoneType);
                if(cards == null || cards.isEmpty())
                    continue;
                createZoneIfMissing(zoneType);
                hasCardsInExtraZone = true;
            }
            VZoneDisplay sb = VPlayerPanel.this.add(new VZoneDisplay(player, ZoneType.Sideboard));
            this.displayAreas.put(ZoneType.Sideboard, sb);
            this.activeZone = ZoneType.Sideboard;
            this.updateTab();
        }

        public void createZoneIfMissing(ZoneType zone) {
            if(this.displayAreas.containsKey(zone))
                return;
            VZoneDisplay display = VPlayerPanel.this.add(new VZoneDisplay(player, zone));
            this.displayAreas.put(zone, display);
            this.hasCardsInExtraZone = true;
            if(zone == ZoneType.AttractionDeck || zone == ZoneType.ContraptionDeck)
                createZoneIfMissing(ZoneType.Junkyard); //If the game uses one, it uses both.
        }

        public void setActiveZone(ZoneType zone) {
            if(this.activeZone == zone)
                return;
            createZoneIfMissing(zone);
            getDisplayArea().setVisible(false);
            this.activeZone = zone;
            if(isSelected())
                getDisplayArea().setVisible(true);
            updateTab();
        }
        public void setActiveZoneByIndex(int index) {
            List<ZoneType> keyList = List.copyOf(displayAreas.keySet());
            setActiveZone(keyList.get(index % keyList.size()));
        }

        private void updateTab() {
            if(!hasCardsInExtraZone)
                this.value = "";
            else if(!getDisplayArea().isVisible())
                this.value = "+";
            else
                this.value = String.valueOf(displayAreas.get(this.activeZone).getCount());
            if(getDisplayArea().isVisible())
                this.icon = iconFromZone(this.activeZone);
            else
                this.icon = DEFAULT_ICON;
        }


        @Override
        public VDisplayArea getDisplayArea() {
            return displayAreas.get(activeZone);
        }

        @Override
        public void setDisplayVisible(boolean visible) {
            if(!visible)
                displayAreas.values().forEach(d -> d.setVisible(false));
            else
                getDisplayArea().setVisible(true);
            updateTab();
        }

        @Override
        public void setDisplayBounds(float x, float y, float width, float height) {
            displayAreas.values().forEach(d -> d.setBounds(x, y, width, height));
        }

        @Override
        public void setRotate180(boolean rotate180) {
            displayAreas.values().forEach(d -> d.setRotate180(rotate180));
        }

        @Override
        public void update() {
            displayAreas.values().forEach(VDisplayArea::update);
            updateTab();
        }
        public void update(ZoneType zoneType) {
            if(!displayAreas.containsKey(zoneType)) {
                if(!EXTRA_ZONES.contains(zoneType))
                    return;
                FCollectionView<CardView> cards = player.getCards(zoneType);
                if(cards == null || cards.isEmpty())
                    return;
                createZoneIfMissing(zoneType);
            }
            displayAreas.get(zoneType).update();
            updateTab();
        }

        @Override
        public void reset() {
            Iterator<Map.Entry<ZoneType, VDisplayArea>> iterator = displayAreas.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ZoneType, VDisplayArea> e = iterator.next();
                VDisplayArea display = e.getValue();
                display.clear();
                //Remove all zones besides sideboard, as this tab is in its initial state.
                //Seems appropriate when resetting, but this code path only gets visited from resetFields in
                //MatchScreen.java, which only gets called when initiating an online game. I'm not sure why zones are
                //specifically cleared at that point, but I'm not really able to test this part right now. Feel free to
                //uncomment and see what happens, or delete entirely if unnecessary.
                //if (e.getKey() == ZoneType.Sideboard)
                //    continue;
                //VPlayerPanel.this.remove(display);
                //iterator.remove();
            }
            activeZone = ZoneType.Sideboard;
        }

        @Override
        public float getIdealWidth(float pref) {
            return pref;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if(this.displayAreas.isEmpty())
                return false;
            if(count >= 2) {
                onClickZone(this.activeZone);
                return true;
            }
            FPopupMenu menu = new FPopupMenu() {
                @Override
                protected void buildMenu() {
                    for(ZoneType zone : displayAreas.keySet()) {
                        String label = WordUtils.capitalize(zone.getTranslatedName());
                        addItem(new FMenuItem(label, iconFromZone(zone), (e) -> onClickZone(zone)));
                    }
                }
            };
            menu.show(this, this.getWidth(), 0);
            return true;
        }

        public void onClickZone(ZoneType zone) {
            if(activeZone == zone && this.isSelected()) {
                setSelectedTab(null);
                return;
            }
            setActiveZone(zone);
            setSelectedTab(this);
        }
    }

    private class CommandZoneDisplay extends VZoneDisplay {
        private CommandZoneDisplay(PlayerView player0) {
            super(player0, ZoneType.Command);
        }

        @Override
        protected void refreshCardPanels(Iterable<CardView> model) {
            int oldCount = getCount();
            super.refreshCardPanels(model);
            int newCount = getCount();
            if (newCount != oldCount) {
                setVisible(newCount > 0);
                VPlayerPanel.this.revalidate(); //need to revalidated entire panel when command zone size changes
            }
        }

        @Override
        protected boolean layoutVerticallyForLandscapeMode() {
            return false;
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (MatchController.getView().selectedPlayerPanel() == this && !((FMenuBar)MatchController.getView().getHeader()).isShowingMenu(true)) {
            if (keyCode == Input.Keys.BUTTON_B) {
                MatchScreen.nullPotentialListener();
                closeSelectedTab();
                return true;
            }
            if (keyCode == Input.Keys.BUTTON_R1) {
                setNextSelectedTab(false);
                return true;
            }
        }
        return super.keyDown(keyCode);
    }
}
