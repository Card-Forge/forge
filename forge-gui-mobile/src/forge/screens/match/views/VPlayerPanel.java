package forge.screens.match.views;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinImageInterface;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.localinstance.skin.FSkinProp;
import forge.menu.FMenuBar;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import forge.util.Utils;
import org.apache.commons.text.WordUtils;

public class VPlayerPanel extends FContainer {
    private static final FSkinFont LIFE_FONT = FSkinFont.get(18);
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

    //how long the zone display takes to slide in when a zone tab is tapped in landscape mode
    private static final float SLIDE_DURATION = 0.2f;

    /**
     * Zones to include in the extra zones dropdown.
     */
    private static final EnumSet<ZoneType> EXTRA_ZONES = EnumSet.of(ZoneType.Sideboard, ZoneType.PlanarDeck,
            ZoneType.SchemeDeck, ZoneType.ContraptionDeck, ZoneType.AttractionDeck, ZoneType.StickerSheets,
            ZoneType.Junkyard, ZoneType.Ante);

    private final PlayerView player;
    private final VPhaseIndicator phaseIndicator;
    private final VField field;
    private final VAvatar avatar;
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
    private static final EnumSet<ZoneType> COMMON_ZONES =
            EnumSet.of(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile);

    private float tabColTop, tabColHeight, commonTabWidth;
    private boolean slideFieldToo, slideClosing, forceClose;
    private float slideProgress = 1f;
    private TabSlideAnimation slideAnimation;
    private final CommandZoneDisplay commandZone;
    private final InfoTabCommand commandTab;
    private boolean commandOpen;              //landscape only, stays true while empty
    private float commandProgress = 0f;       //0 hidden, 1 fully slid out
    private float commandVisibleWidth = 0f;
    private CommandSlideAnimation commandAnimation;
    private boolean commandUserClosed; //closed by hand, stays closed until the zone empties

    public VPlayerPanel(PlayerView player0, boolean showHand, int playerCount) {
        player = player0;
        phaseIndicator = add(new VPhaseIndicator());

        if (playerCount > 2) {
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
        commandZone = add(new CommandZoneDisplay(player));
        commandZone.setVisible(false);
        commandTab = add(new InfoTabCommand(commandZone));
        tabs.add(commandTab);

        addZoneDisplay(ZoneType.Exile);
        extraTab = add(new InfoTabExtra());
        tabs.add(extraTab);

        if (showHand) {
            setSelectedZone(ZoneType.Hand);
        }
        // fix z-index
        bringTabsToFront();
    }

    private void syncCommandZone() {
        if (!Forge.isLandscapeMode()) { return; }
        if (commandZone.getCount() == 0) {
            commandUserClosed = false; //it opens by itself again next time it has cards
            setCommandOpen(false);
        } else if (!commandOpen && !commandUserClosed) {
            setCommandOpen(true);
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

    public static FSkinImageInterface iconFromZone(ZoneType zoneType) {
        return FSkin.getImages().get(FSkinProp.iconFromZone(zoneType, Forge.hdbuttons));
    }

    public Iterable<InfoTab> getTabs() {
        return tabs;
    }

    public InfoTab getSelectedTab() {
        return selectedTab;
    }

    public void resetZoneTabs() {
        for (InfoTab tab : tabs)
            tab.reset();
    }

    public void setSelectedZone(ZoneType zoneType) {
        if (zoneTabs.containsKey(zoneType))
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

        final boolean wasClosed = this.selectedTab == null;
        if (tab == null && !wasClosed && !forceClose && startSlideOut()) {
            return; //animation calls back here with forceClose when it finishes
        }
        hideSelectedTab();

        this.selectedTab = tab;
        selected = tabs.indexOf(tab);

        if (this.selectedTab != null) {
            this.selectedTab.setDisplayVisible(true);
            startSlideAnimation(wasClosed);
        }

        if (MatchController.getView() != null) {
            MatchController.getView().revalidate();
        }
    }

    private void startSlideAnimation(boolean fieldToo) {
        stopSlideAnimation();
        if (!Forge.isLandscapeMode() || getWidth() <= 0) {
            slideProgress = 1f;
            slideFieldToo = false;
            return;
        }
        slideFieldToo = fieldToo;
        slideProgress = 0f; //must be set before the revalidate that follows
        slideAnimation = new TabSlideAnimation(false);
        slideAnimation.start();
    }

    private boolean startSlideOut() {
        if (!Forge.isLandscapeMode() || getWidth() <= 0) { return false; }
        if (slideClosing) { return true; }
        stopSlideAnimation();
        slideClosing = true;
        slideFieldToo = true; //field widens back as the display leaves
        slideProgress = 1f;
        slideAnimation = new TabSlideAnimation(true);
        slideAnimation.start();
        return true;
    }

    private void stopSlideAnimation() {
        TabSlideAnimation old = slideAnimation;
        slideAnimation = null; //so the old animation's onEnd does nothing
        slideClosing = false;
        if (old != null) { old.stop(); }
    }

    private class TabSlideAnimation extends ForgeAnimation {
        private final boolean closing;
        private float elapsed;

        TabSlideAnimation(boolean closing0) { closing = closing0; }

        @Override
        protected boolean advance(float dt) {
            elapsed += dt;
            float t = Math.min(elapsed / SLIDE_DURATION, 1f);
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);
            slideProgress = closing ? 1f - eased : eased;
            if (selectedTab != null && Forge.isLandscapeMode()) {
                updateTabLayout(initW, initH);
            }
            return t < 1f;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (slideAnimation != this) { return; } //replaced by a newer animation
            slideAnimation = null;
            slideProgress = 1f;
            slideFieldToo = false;
            if (closing) {
                slideClosing = false;
                forceClose = true;
                try { setSelectedTab(null); } finally { forceClose = false; }
            } else if (!endingAll && selectedTab != null && Forge.isLandscapeMode()) {
                updateTabLayout(initW, initH);
            }
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
        if (!change) {
            selected++;
            if (selected >= 0 && selected < tabs.size() && tabs.get(selected) == commandTab) { selected++; }
        } else
            hideSelectedTab();
        int numTabs = tabs.size();
        int numExtraTabs = extraTab.displayAreas.size();
        if (selected < 0 || selected >= numTabs + numExtraTabs)
            selected = 0;
        if (selected >= numTabs) {
            extraTab.setActiveZoneByIndex(selected - numTabs);
            setSelectedTab(extraTab);
        } else
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

    private void bringTabsToFront() {
        for (InfoTab tab : tabs) {
            remove(tab);
            add(tab);
        }
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

    public void updateLife() {
        lblLife.update();
    }

    public void updateShards() {
        lblLife.updateShards();
    }

    public void updateManaPool() {
        tabManaPool.update();
    }

    private void onCommandZoneUpdated() {
        syncCommandZone();
        if (Forge.isLandscapeMode() && initW > 0) {
            layoutTabsLandscape(); //tab appears or disappears as cards come and go
            updateTabLayout(initW, initH); //width follows the card count
        } else {
            revalidate();
        }
    }

    public VZoneDisplay getOpenCommandDisplay() {
        return commandOpen && Forge.isLandscapeMode() ? commandZone : null;
    }

    /** @return screen x of the left-most open zone display, or Float.MAX_VALUE if none */
    public float getOpenDisplayScreenLeft() {
        float left = Float.MAX_VALUE;
        if (selectedTab != null && selectedTab.isVisible() && selectedTab.getDisplayArea() != null) {
            left = Math.min(left, selectedTab.getDisplayArea().screenPos.x);
        }
        if (commandOpen && commandProgress > 0f && Forge.isLandscapeMode()) {
            left = Math.min(left, commandZone.screenPos.x);
        }
        return left;
    }

    @SuppressWarnings("incomplete-switch")
    public void updateZone(ZoneType zoneType) {
        if (zoneType == ZoneType.Battlefield) {
            field.update(true);
        } else if (zoneType == ZoneType.Command) {
            commandTab.update();
            onCommandZoneUpdated();
        } else {
            if (zoneTabs.containsKey(zoneType)) {
                zoneTabs.get(zoneType).update();
            } else if (EXTRA_ZONES.contains(zoneType)) {
                extraTab.update(zoneType);
            }
            //update flashback zone when graveyard, library, exile, or stack zones updated
            switch (zoneType) {
                case Graveyard, Library, Exile, Stack -> zoneTabs.get(ZoneType.Flashback).update();
            }
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        if (Forge.isLandscapeMode()) {
            doLandscapeLayout(width, height);
            return;
        }
        if (commandOpen) { //landscape open state doesn't carry over
            stopCommandAnimation();
            commandOpen = false;
            commandProgress = 0f;
            commandZone.setVisible(false);
        }
        //layout for bottom panel by default
        float x = avatarHeight;
        float w = width - avatarHeight;
        float indicatorScale = 1f;
        if (avatarHeight < VAvatar.HEIGHT) {
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

        List<InfoTab> shown = new ArrayList<>();
        for (InfoTab tab : tabs) {
            boolean show = tab.isTabShown();
            tab.setVisible(show);
            if (show) { shown.add(tab); }
        }
        float infoTabWidth = (getWidth() - x) / shown.size();
        for (InfoTab tab : shown) {
            tab.setBounds(x, y, infoTabWidth, infoLabelHeight);
            x += infoTabWidth;
        }

        if (selectedTab != null) {
            y -= displayAreaHeight;
        }

        //command zone size first, so the field's second row leaves room for it
        int cmdCount = commandZone.getCount();
        float cmdHeight = y / 2;
        float cmdWidth = Math.min(cmdCount, 2) * commandZone.getCardWidth(cmdHeight);
        field.setCommandZoneWidth(cmdCount > 0 ? cmdWidth + 1 : 0);
        field.setBounds(0, 0, width, y); //the only field.setBounds, and it must be before the flip

        if (isFlipped()) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }

        //command zone is positioned after the flip, so it gets its flipped position directly
        commandZone.setVisible(cmdCount > 0);
        if (cmdCount > 0) {
            float top = isFlipped() ? height - y : y - cmdHeight;
            commandZone.setBounds(width - cmdWidth, top, cmdWidth, cmdHeight);
        }

        //this is used for landscape so set this to 0
        field.setFieldModifier(0);
    }

    private float initW, initH;

    private void doLandscapeLayout(float width, float height) {
        if (selectedTab == commandTab) { hideSelectedTab(); } //portrait selection doesn't carry over
        initW = width;
        initH = height;
        syncCommandZone();
        float x = 0;
        float y = 0;
        avatar.setPosition(x, y);
        y += avatar.getHeight();

        lblLife.setBounds(x, Forge.altPlayerLayout ? 0 : y, avatar.getWidth(), Forge.altPlayerLayout ? INFO_FONT.getLineHeight() : LIFE_FONT.getLineHeight());
        if (Forge.altPlayerLayout) {
            if (adjustHeight > 2)
                y += INFO_FONT.getLineHeight() / 2;
        } else
            y += lblLife.getHeight();

        tabColTop = y;
        tabColHeight = height - y;
        layoutTabsLandscape();
        updateTabLayout(width, height);
    }

    //position the zone tabs in a column, offset by the current tab scroll position
    private void layoutTabsLandscape() {
        if (tabs.isEmpty()) { return; }
        float tabWidth = avatar.getWidth();
        List<InfoTab> left = new ArrayList<>(), right = new ArrayList<>();
        for (InfoTab tab : tabs) {
            boolean show = tab.isTabShown();
            tab.setVisible(show);
            if (!show) { continue; }
            (tab.isCommonZone() ? right : left).add(tab);
        }
        commonTabWidth = right.isEmpty() ? 0 : tabWidth;
        layoutColumn(left, 0, tabColTop, tabWidth, tabColHeight);
        layoutColumn(right, initW - tabWidth, 0, tabWidth, initH);
    }

    private void layoutColumn(List<InfoTab> column, float x, float top, float w, float h) {
        if (column.isEmpty()) { return; }
        float tabHeight = h / column.size();
        float y = top;
        for (InfoTab tab : column) {
            tab.setBounds(x, y, w, tabHeight);
            y += tabHeight;
        }
    }

    /** @return the open zone display (selected tab or command zone) containing the screen point, or null */
    public VDisplayArea getOpenDisplayAt(float screenX, float screenY) {
        if (selectedTab != null && selectedTab.isVisible()) {
            VDisplayArea d = selectedTab.getDisplayArea();
            if (d != null && d.isVisible() && d.screenPos.contains(screenX, screenY)) { return d; }
        }
        if (commandOpen && commandZone.isVisible() && commandZone.screenPos.contains(screenX, screenY)) {
            return commandZone;
        }
        return null;
    }

    private void updateTabLayout(float width, float height) {
        float x = avatar.getRight();
        phaseIndicator.resetFont();
        phaseIndicator.setBounds(x, 0, avatar.getWidth() * 0.6f, height);
        x += phaseIndicator.getWidth();

        float fieldWidth = width - x - commonTabWidth;
        float displayAreaWidth = height / FCardPanel.ASPECT_RATIO;
        if (selectedTab != null) {
            fieldWidth -= displayAreaWidth * (slideFieldToo ? slideProgress : 1f);
        }

        //command zone: half height strip at the right end of the field, on the outer edge of the panel
        float cmdHeight = height / 2;
        float cmdWidth = Math.max(1, Math.min(commandZone.getCount(), 2)) * commandZone.getCardWidth(cmdHeight);
        commandVisibleWidth = cmdWidth * commandProgress;
        commandZone.setBounds(x + fieldWidth - commandVisibleWidth, isFlipped() ? 0 : height - cmdHeight, cmdWidth, cmdHeight);
        field.setCommandZoneWidth(commandVisibleWidth > 0 ? commandVisibleWidth + 1 : 0);
        field.setBounds(x, 0, fieldWidth, height);

        float displayLeft = width - commonTabWidth - displayAreaWidth + (1f - slideProgress) * displayAreaWidth;
        for (InfoTab tab : tabs) {
            tab.setDisplayBounds(displayLeft, 0, displayAreaWidth, height);
        }
        field.setFieldModifier(0);
    }

    @Override
    public void draw(Graphics g) {
        if (slideProgress < 1f && Forge.isLandscapeMode()) {
            //keep the zone display from drawing outside this panel while it slides in
            g.startClip(0, 0, getWidth(), getHeight());
            try {
                super.draw(g);
            } finally {
                g.endClip();
            }
        } else {
            super.draw(g);
        }
    }

    @Override
    protected void drawOverlay(Graphics g) {
        super.drawOverlay(g);
    }

    @Override
    public void drawBackground(Graphics g) {
        if (Forge.isLandscapeMode()) {
            FSkinColor tabBg = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2);
            if (commonTabWidth > 0) {
                g.fillRect(tabBg, getWidth() - commonTabWidth, 0, commonTabWidth, getHeight());
            }
            float colTop = avatar.getHeight();
            g.fillRect(tabBg, 0, colTop, avatar.getWidth(), getHeight() - colTop);
        } else {
            g.fillRect(Color.BLACK, 0, avatar.getTop(), getWidth(), avatarHeight);
        }
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
            } else {
                y = isFlipped() ? top + 1 : bottom;
                //don't know why infotab gets null here, either way don't crash the gui..
                float left = infoTab == null ? 0 : infoTab.getLeft();
                float right = infoTab == null ? 0 : infoTab.getRight();
                //leave gap at selected zone tab
                g.drawLine(1, MatchScreen.getBorderColor(), x, y, left, y);
                g.drawLine(1, MatchScreen.getBorderColor(), right, y, w, y);
            }
        }
        if (Forge.isLandscapeMode() && commandVisibleWidth > 0 && commandZone.isVisible()) {
            float cx = commandZone.getLeft();
            float cy = commandZone.getTop();
            float ch = commandZone.getHeight();
            g.fillRect(getDisplayAreaBackColor(), cx, cy, commandVisibleWidth, ch);
            g.drawLine(1, MatchScreen.getBorderColor(), cx, cy, cx, cy + ch);
            float edgeY = isFlipped() ? cy + ch : cy; //inner edge, away from the panel's outer side
            g.drawLine(1, MatchScreen.getBorderColor(), cx, edgeY, cx + commandVisibleWidth, edgeY);
        }
    }

    public Iterable<FScrollPane> getAllScrollPanes() {
        //Used to catalog scroll positions before resizing UI.
        ArrayList<FScrollPane> out = new ArrayList<>();
        out.add(field.getRow1());
        out.add(field.getRow2());
        out.add(commandZone);
        for (InfoTabZone tab : zoneTabs.values())
            out.add(tab.displayArea);
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
            int delta = player.getLife() - life;
            player.setAvatarLifeDifference(player.getAvatarLifeDifference() + delta);
            if (delta != 0) {
                life = player.getLife();
                lifeStr = String.valueOf(life);
            }

            delta = player.getCounters(CounterEnumType.POISON) - poisonCounters;
            if (delta != 0) {
                //TODO: Show animation on avatar for gaining poison counters
                poisonCounters = player.getCounters(CounterEnumType.POISON);
            }

            energyCounters = player.getCounters(CounterEnumType.ENERGY);
            experienceCounters = player.getCounters(CounterEnumType.EXPERIENCE);
            ticketCounters = player.getCounters(CounterEnumType.TICKET);
            radCounters = player.getCounters(CounterEnumType.RAD);
            manaShards = player.getNumManaShards();
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
            if (Forge.altPlayerLayout && Forge.isLandscapeMode()) {
                if (poisonCounters == 0 && energyCounters == 0 && experienceCounters == 0 && ticketCounters == 0 && radCounters == 0 && manaShards == 0) {
                    g.fillRect(Color.DARK_GRAY, 0, 0, INFO2_FONT.getBounds(lifeStr).width + 1, INFO2_FONT.getBounds(lifeStr).height + 1);
                    g.drawText(lifeStr, INFO2_FONT, getInfoForeColor().getColor(), 0, 0, getWidth(), getHeight(), false, Align.left, false);
                } else {
                    float halfHeight = getHeight() / divider;
                    float textStart = halfHeight + Utils.scale(1);
                    float textWidth = getWidth() - textStart;
                    int mod = 1;
                    g.fillRect(Color.DARK_GRAY, 0, 0, INFO_FONT.getBounds(lifeStr).width + halfHeight + 1, INFO_FONT.getBounds(lifeStr).height + 1);
                    g.drawImage(FSkinImage.QUEST_LIFE, 0, 0, halfHeight, halfHeight);
                    g.drawText(lifeStr, INFO_FONT, getInfoForeColor().getColor(), textStart, 0, textWidth, halfHeight, false, Align.left, false);
                    if (poisonCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, halfHeight + 2, INFO_FONT.getBounds(String.valueOf(poisonCounters)).width + halfHeight + 1, INFO_FONT.getBounds(String.valueOf(poisonCounters)).height + 1);
                        g.drawImage(FSkinImage.POISON, 0, halfHeight + 2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(poisonCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, halfHeight + 2, textWidth, halfHeight, false, Align.left, false);
                        mod += 1;
                    }
                    if (energyCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight * mod) + 2, INFO_FONT.getBounds(String.valueOf(energyCounters)).width + halfHeight + 1, INFO_FONT.getBounds(String.valueOf(energyCounters)).height + 1);
                        g.drawImage(FSkinImage.ENERGY, 0, (halfHeight * mod) + 2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(energyCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight * mod) + 2, textWidth, halfHeight, false, Align.left, false);
                        mod += 1;
                    }
                    if (experienceCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight * mod) + 2, INFO_FONT.getBounds(String.valueOf(experienceCounters)).width + halfHeight + 1, INFO_FONT.getBounds(String.valueOf(experienceCounters)).height + 1);
                        g.drawImage(FSkinImage.COMMANDER, 0, (halfHeight * mod) + 2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(experienceCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight * mod) + 2, textWidth, halfHeight, false, Align.left, false);
                        mod += 1;
                    }
                    if (radCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight * mod) + 2, INFO_FONT.getBounds(String.valueOf(radCounters)).width + halfHeight + 1, INFO_FONT.getBounds(String.valueOf(radCounters)).height + 1);
                        g.drawImage(FSkinImage.RAD, 0, (halfHeight * mod) + 2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(radCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight * mod) + 2, textWidth, halfHeight, false, Align.left, false);
                        mod += 1;
                    }
                    if (ticketCounters > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight * mod) + 2, INFO_FONT.getBounds(String.valueOf(ticketCounters)).width + halfHeight + 1, INFO_FONT.getBounds(String.valueOf(ticketCounters)).height + 1);
                        g.drawImage(FSkinImage.TICKET, 0, (halfHeight * mod) + 2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(ticketCounters), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight * mod) + 2, textWidth, halfHeight, false, Align.left, false);
                        mod += 1;
                    }
                    if (manaShards > 0) {
                        g.fillRect(Color.DARK_GRAY, 0, (halfHeight * mod) + 2, INFO_FONT.getBounds(String.valueOf(manaShards)).width + halfHeight + 1, INFO_FONT.getBounds(String.valueOf(manaShards)).height + 1);
                        g.drawImage(FSkinImage.AETHER_SHARD, 0, (halfHeight * mod) + 2, halfHeight, halfHeight);
                        g.drawText(String.valueOf(manaShards), INFO_FONT, getInfoForeColor().getColor(), textStart, (halfHeight * mod) + 2, textWidth, halfHeight, false, Align.left, false);
                        mod += 1;
                    }
                    adjustHeight = (mod > 2) && (avatar.getHeight() < halfHeight * mod) ? mod : 1;
                }
            } else {
                if (poisonCounters == 0 && energyCounters == 0 && manaShards == 0) {
                    g.drawText(lifeStr, LIFE_FONT, getInfoForeColor(), 0, 0, getWidth(), getHeight(), false, Align.center, true);
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
                    } else {
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
        protected boolean isCommonZone() { return false; }
        protected boolean isTabShown() { return true; }

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

        protected boolean isSelected() {
            return selectedTab == this;
        }

        protected FSkinColor getSelectedBackgroundColor() {
            return getDisplayAreaBackColor();
        }

        @Override
        public void draw(Graphics g) {
            if (Forge.isLandscapeMode() && isCommonZone()) {
                g.fillRect(FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2), 0, 0, getWidth(), getHeight());
            }
            float x, y, w, h;
            boolean drawOverlay = MatchController.getView().selectedPlayerPanel().getPlayer() == player && Forge.hasGamepad();
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
                float mod = isHovered() ? w / 8f : 0;
                g.drawImage(icon, x - mod / 2, y - mod / 2, w + mod, h + mod);
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
                float mod = isHovered() ? w / 8f : 0;
                g.drawImage(icon, x - mod / 2, y - mod / 2, w + mod, h + mod);

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
        public void reset() {
        } //Mana Display does not get cleared.
    }

    public class InfoTabCommand extends InfoTabSingleDisplay {
        private InfoTabCommand(VDisplayArea display) {
            super(FSkinImage.COMMAND, display);
        }

        @Override
        protected boolean isSelected() {
            return Forge.isLandscapeMode() ? commandOpen : selectedTab == this;
        }

        @Override
        protected boolean isTabShown() {
            return Forge.isLandscapeMode() && (isSelected() || displayArea.getCount() > 0 || commandProgress > 0f);
        }

        @Override
        public void setDisplayBounds(float x, float y, float width, float height) {
            if (!Forge.isLandscapeMode()) { //landscape lays this display out separately
                super.setDisplayBounds(x, y, width, height);
            }
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (Forge.isLandscapeMode()) {
                boolean open = !commandOpen;
                commandUserClosed = !open;
                setCommandOpen(open);
                return true;
            }
            return super.tap(x, y, count);
        }

        @Override
        public void reset() {
            displayArea.clear();
        }
    }

    private void setCommandOpen(boolean open) {
        if (commandOpen == open) { return; }
        commandOpen = open;
        stopCommandAnimation();
        if (open) { commandZone.setVisible(true); }
        if (!Forge.isLandscapeMode() || getWidth() <= 0) {
            commandProgress = open ? 1f : 0f;
            commandZone.setVisible(open);
            return;
        }
        commandAnimation = new CommandSlideAnimation(open);
        commandAnimation.start();
    }

    private void stopCommandAnimation() {
        CommandSlideAnimation old = commandAnimation;
        commandAnimation = null; //so the old animation's onEnd does nothing
        if (old != null) { old.stop(); }
    }

    private class CommandSlideAnimation extends ForgeAnimation {
        private final boolean opening;
        private final float startProgress;
        private float elapsed;

        CommandSlideAnimation(boolean opening0) {
            opening = opening0;
            startProgress = commandProgress; //so reversing mid-slide doesn't jump
        }

        @Override
        protected boolean advance(float dt) {
            elapsed += dt;
            float t = Math.min(elapsed / SLIDE_DURATION, 1f);
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);
            commandProgress = startProgress + ((opening ? 1f : 0f) - startProgress) * eased;
            if (Forge.isLandscapeMode()) {
                updateTabLayout(initW, initH);
            }
            return t < 1f;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (commandAnimation != this) { return; } //replaced by a newer animation
            commandAnimation = null;
            commandProgress = opening ? 1f : 0f;
            if (!opening) { commandZone.setVisible(false); }
            if (Forge.isLandscapeMode()) {
                layoutTabsLandscape();
                updateTabLayout(initW, initH);
            }
        }
    }

    /**
     * Player panel tab for a single typical card zone, such as the hand.
     */
    public class InfoTabZone extends InfoTabSingleDisplay {
        public final ZoneType zoneType;
        @Override
        protected boolean isCommonZone() { return COMMON_ZONES.contains(zoneType); }

        private InfoTabZone(VDisplayArea displayArea, ZoneType zoneType) {
            //super(zoneType == ZoneType.Command ? FSkinImage.COMMANDER : iconFromZone(zoneType), displayArea);
            super(iconFromZone(zoneType), displayArea);
            this.zoneType = zoneType;
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
            for (ZoneType zoneType : EXTRA_ZONES) {
                if (player.getCards(zoneType).isEmpty())
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
            if (this.displayAreas.containsKey(zone))
                return;
            VZoneDisplay display = VPlayerPanel.this.add(new VZoneDisplay(player, zone));
            bringTabsToFront();
            this.displayAreas.put(zone, display);
            this.hasCardsInExtraZone = true;
            if (zone == ZoneType.AttractionDeck || zone == ZoneType.ContraptionDeck)
                createZoneIfMissing(ZoneType.Junkyard); //If the game uses one, it uses both.
        }

        public void setActiveZone(ZoneType zone) {
            if (this.activeZone == zone)
                return;
            createZoneIfMissing(zone);
            getDisplayArea().setVisible(false);
            this.activeZone = zone;
            if (isSelected())
                getDisplayArea().setVisible(true);
            updateTab();
        }

        public void setActiveZoneByIndex(int index) {
            List<ZoneType> keyList = List.copyOf(displayAreas.keySet());
            setActiveZone(keyList.get(index % keyList.size()));
        }

        private void updateTab() {
            if (!hasCardsInExtraZone)
                this.value = "";
            else if (!getDisplayArea().isVisible())
                this.value = "+";
            else
                this.value = String.valueOf(displayAreas.get(this.activeZone).getCount());
            if (getDisplayArea().isVisible())
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
            if (!visible)
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
            if (!displayAreas.containsKey(zoneType)) {
                if (!EXTRA_ZONES.contains(zoneType))
                    return;
                if (player.getCards(zoneType).isEmpty())
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
        public boolean tap(float x, float y, int count) {
            if (this.displayAreas.isEmpty())
                return false;
            if (count >= 2) {
                onClickZone(this.activeZone);
                return true;
            }
            FPopupMenu menu = new FPopupMenu() {
                @Override
                protected void buildMenu() {
                    for (ZoneType zone : displayAreas.keySet()) {
                        String label = WordUtils.capitalize(zone.getTranslatedName());
                        addItem(new FMenuItem(label, iconFromZone(zone), (e) -> onClickZone(zone)));
                    }
                }
            };
            menu.show(this, this.getWidth(), 0);
            return true;
        }

        public void onClickZone(ZoneType zone) {
            if (activeZone == zone && this.isSelected()) {
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
            if (!Forge.isLandscapeMode() && getCount() != oldCount) {
                setVisible(getCount() > 0);
                VPlayerPanel.this.revalidate();
            }
        }

        @Override
        protected boolean layoutVerticallyForLandscapeMode() {
            return false; //single row that scrolls left/right
        }

        @Override
        public void draw(Graphics g) {
            if (Forge.isLandscapeMode() && commandVisibleWidth < 1f) {
                return;
            }
            if (commandProgress < 1f && Forge.isLandscapeMode()) {
                //only the part that has slid out from the field edge is visible
                g.startClip(0, 0, commandVisibleWidth, getHeight());
                try {
                    super.draw(g);
                } finally {
                    g.endClip();
                }
            } else {
                super.draw(g);
            }
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (MatchController.getView().selectedPlayerPanel() == this && !((FMenuBar) MatchController.getView().getHeader()).isShowingMenu(true)) {
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
