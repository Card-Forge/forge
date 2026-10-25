package forge.screens.match.views;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
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
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
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
    private static final FSkinFont INFO2_FONT = FSkinFont.get(11);

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

    //alternate landscape layout: the open card zone is a box in the panel's outer corner with the command zone above it
    private static final float ALT_ZONE_HEIGHT_RATIO = 0.5f;  //box height as a fraction of panel height
    private static final float ALT_MANA_LABEL_RATIO = 0.6f;     //width of one mana icon in the alternate layout, as a fraction of the box height
    private static final float ALT_ZONE_MAX_WIDTH_RATIO = 0.45f; //the box grows with its cards up to this fraction of the space between the tab columns
    private static int altZoneLayoutCache = -1;

    //compact landscape layout: a control column on the left (avatar, life, the phase button and a scroller showing three zone tabs), card zones sliding up from the panel's outer edge and the command zone sliding in from the right
    private static final int COMPACT_MAX_VISIBLE_TABS = 3;         //zone tabs shown at once, the others scroll into place
    private static final float COMPACT_MIN_CELL_RATIO = 0.5f;      //the avatar shrinks until three tabs are at least this fraction of the column width tall (or COMPACT_MIN_CELL)
    private static final float COMPACT_MIN_CELL = Utils.scale(28); //smallest comfortable tab height
    private static final float COMPACT_MAX_AVATAR_RATIO = 0.20f;  //the avatar is at most this fraction of the panel height
    private static final float COMPACT_MIN_AVATAR = Utils.scale(28); //the avatar never shrinks below this, however short the panel is
    private static final float COMPACT_COLUMN_WIDTH_RATIO = 1.25f; //control column width as a multiple of the avatar width, wide enough for 3 digit tab values
    private static final FSkinFont COMPACT_VALUE_FONT = INFO2_FONT; //tab values, larger than the portrait ones
    private static final float COMPACT_STRIP_RATIO = 1f / 3f;      //card zone and command zone height as a fraction of panel height: with the field shrunk to the other two thirds its two rows are exactly as tall
    private static final float COMPACT_STOPS_WIDTH_RATIO = 0.75f;  //phase stop column width as a fraction of the avatar width
    private static final float COMPACT_SNAP_SPEED = 14f;           //how quickly the tab scroller settles on a tab
    private static final float COMPACT_FLING_WINDOW_MS = 300f;     //a fling this soon after a drag belongs to the tab scroller
    private static final float COMPACT_SCROLL_GAP = Utils.scale(2); //clear space between the scroll bar and the tabs
    private static final float COMPACT_PANEL_MARGIN = 0;  //gap between the rounded control panel and the screen edge, the field and the other panel
    private static final Color COMPACT_GLASS = new Color(30 / 255f, 35 / 255f, 40 / 255f, 0.62f);
    private static final Color COMPACT_ZONE_OVER = new Color(30 / 255f, 35 / 255f, 40 / 255f, 0.80f);
    private static final Color COMPACT_ZONE = new Color(30 / 255f, 35 / 255f, 40 / 255f, 0.62f);
    private static final Color COMPACT_GLASS_CHIP = new Color(1f, 1f, 1f, 0.10f);
    private static final Color COMPACT_GLASS_EDGE = new Color(1f, 1f, 1f, 0.10f);
    private static final float COMPACT_PANEL_RADIUS = Utils.scale(8);  //corner radius of the control panel
    private static final float COMPACT_PANEL_PAD_TOP = Utils.scale(5); //space above the avatar inside the panel
    private static int compactLayoutCache = -1; //0 off, 1 controls on the left, 2 on the right, 3 alternating: bottom player right, the others left
    private static final float COMPACT_ZONE_INSET = Utils.scale(5);   //room between an open zone's cards and the rounded box around them
    private static final float COMPACT_BOX_RADIUS = Utils.scale(4);
    private static final float COMPACT_ZONE_GAP = Utils.scale(3);    //space between the card zone box and the command zone box
    private static final Color COMPACT_BOX_EDGE = new Color(1f, 1f, 1f, 0.26f);
    private static final int COMPACT_DOUBLE_ROWS = 4;                 //tabs per column in the double tab layout

    /** @return true if the compact landscape layout is in use (it needs landscape mode and the "Use Compact Layout" setting) */
    public static boolean isCompactLayout() {
        if (!Forge.isLandscapeMode()) { return false; }
        if (compactLayoutCache < 0) { reloadCompactLayout(); }
        return compactLayoutCache > 0;
    }

    /** @return true if this panel's controls are on the right side of the screen in the compact layout */
    public boolean isCompactRight() {
        if (!isCompactLayout()) { return false; }
        return compactLayoutCache == 2 || (compactLayoutCache == 3 && !isFlipped());
    }

    private static int menuHiddenCache = -1;

    /** @return true when the compact layout keeps the top menu bar hidden until its menu button is pressed */
    public static boolean isMenuHidden() {
        if (!isCompactLayout()) { return false; }
        if (menuHiddenCache < 0) { reloadMenuHidden(); }
        return menuHiddenCache > 0;
    }

    public static void reloadMenuHidden() {
        menuHiddenCache = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_HIDE_MENU) ? 1 : 0;
    }

    private static int unifiedPhaseCache = -1;
    private static final float COMPACT_PHASE_MAX_RATIO = 0.12f; //a panel's own phase button is never taller than this share of a short panel

    /** @return true when one phase label next to the stack menu replaces the per panel phase labels (compact layout only) */
    public static boolean isUnifiedPhase() {
        if (!isCompactLayout()) { return false; }
        if (unifiedPhaseCache < 0) { reloadUnifiedPhase(); }
        return unifiedPhaseCache > 0;
    }

    /** Call when the Unified Phase Label setting changes. */
    public static void reloadUnifiedPhase() {
        unifiedPhaseCache = FModel.getPreferences().getPrefBoolean(FPref.UI_UNIFIED_PHASE_LABEL) ? 1 : 0;
    }

    private static int doubleTabsCache = -1;

    /** @return true when the compact layout shows its zone tabs as two columns, all at once, instead of one scrolling column */
    public static boolean isDoubleTabs() {
        if (!isCompactLayout()) { return false; }
        if (doubleTabsCache < 0) { reloadDoubleTabs(); }
        return doubleTabsCache > 0;
    }

    /** Call when the Double Zone Tabs setting changes. */
    public static void reloadDoubleTabs() {
        doubleTabsCache = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_DOUBLE_TABS) ? 1 : 0;
    }

    private static int zoneOverlayCache = -1;

    /** @return true when zone tabs open over the battlefield instead of shrinking it (the Compact Zone Tabs setting) */
    public static boolean isZoneOverlay() {
        if (zoneOverlayCache < 0) { reloadZoneOverlay(); }
        return zoneOverlayCache > 0;
    }

    public static void reloadZoneOverlay() {
        zoneOverlayCache = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_ZONE_TABS) ? 1 : 0;
    }

    private static int frostedCache = -1;

    /** @return true when the frosted look is in use: the Frosted Style setting, or the compact layout which always has it */
    public static boolean isFrosted() {
        if (isCompactLayout()) { return true; }
        if (frostedCache < 0) { reloadFrosted(); }
        return frostedCache > 0;
    }

    /** Call when the Frosted Style setting changes. */
    public static void reloadFrosted() {
        frostedCache = FModel.getPreferences().getPrefBoolean(FPref.UI_FROSTED_STYLE) ? 1 : 0;
    }

    private static boolean isCompactAlternate() {
        return isCompactLayout() && compactLayoutCache == 3;
    }

    /** Call when the setting changes. The setting is Off, Left, Right or Alternate; the old on/off value is read as Left/Off. */
    public static void reloadCompactLayout() {
        String mode = FModel.getPreferences().getPref(FPref.UI_COMPACT_LANDSCAPE_LAYOUT);
        if ("true".equalsIgnoreCase(mode)) { mode = "Left"; }
        else if ("false".equalsIgnoreCase(mode)) { mode = "Off"; }
        if ("Right".equalsIgnoreCase(mode)) { compactLayoutCache = 2; }
        else if ("Alternate".equalsIgnoreCase(mode)) { compactLayoutCache = 3; }
        else if ("Off".equalsIgnoreCase(mode)) { compactLayoutCache = 0; }
        else { compactLayoutCache = 1; }
    }

    //left edge of the control column: the panel's left edge, or its right edge in the right hand layout
    private float getColumnX() {
        return isCompactRight() && initW > 0 ? initW - tabColWidth : 0f;
    }

    //the background image is drawn behind the whole panel, so it must not follow the field, which moves as zones open
    public float getBackgroundLeft() {
        if (!isCompactLayout()) { return field.getLeft(); }
        if (isCompactAlternate()) { return 0f; } //the columns of both players sit on different sides
        return isCompactRight() ? 0f : tabColWidth;
    }

    public float getBackgroundRightInset() {
        if (isCompactAlternate()) { return 0f; }
        return isCompactRight() ? tabColWidth : 0f;
    }

    public float getBackgroundHeight() {
        return isCompactLayout() ? getHeight() : field.getHeight();
    }

    /** @return true if the open card zone is a horizontal box instead of a side column (alternate and compact layouts) */
    public static boolean isAltZoneLayout() {
        if (!Forge.isLandscapeMode()) { return false; }
        if (isCompactLayout()) { return true; }
        if (altZoneLayoutCache < 0) { reloadAltZoneLayout(); }
        return altZoneLayoutCache == 1;
    }

    /** Call when the setting changes. */
    public static void reloadAltZoneLayout() {
        altZoneLayoutCache = FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_PLAYERZONETABS) ? 1 : 0;
    }

    /** @return true if this display is the card zone box of the alternate layout (the command zone never is) */
    public boolean isStripDisplay(VDisplayArea display) {
        return isAltZoneLayout() && isBoxDisplay(display) && display != commandZone;
    }

    //displays that live in the corner box of the alternate layout: card zones and the mana pool
    private static boolean isBoxDisplay(VDisplayArea display) {
        return display instanceof VCardDisplayArea || display instanceof VManaPool;
    }

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

    private final Rectangle flightArea = new Rectangle();
    private float tabColTop, tabColHeight, commonTabWidth;
    private boolean slideFieldToo, slideClosing, forceClose;
    private float slideProgress = 1f;
    private TabSlideAnimation slideAnimation;
    private final CommandZoneDisplay commandZone;
    private final InfoTabCommand commandTab;
    private boolean commandOpen;              //landscape only, stays true while empty
    private float commandProgress = 0f;       //0 hidden, 1 fully slid out
    private float commandVisibleWidth = 0f;
    private float commandBoxVisibleWidth = 0f, commandBoxLeft = 0f; //compact layout: the command zone's rounded box, which is wider than its cards by the inset
    private CommandSlideAnimation commandAnimation;
    private boolean commandUserClosed; //closed by hand, stays closed until the zone empties
    private static final float TAB_ICON_RATIO = 0.6f;        //icon box as a fraction of tab height, the rest is top/bottom padding
    private static final float TAB_COLUMN_PAD = Utils.scale(4);
    private float tabIconSize;
    private static final float TAB_FILL_MAX_RATIO = 2.4f; //tabs share the column height but never grow past this fraction of the tab width
    private static final float TAB_MAX_HEIGHT_RATIO = 0.9f; //tab height as a fraction of tab width
    private static final float TAB_MIN_ICON = Utils.scale(16);
    private float tabColWidth;
    private float tabValueWidth;
    private final List<InfoTab> leftTabs = new ArrayList<>(), rightTabs = new ArrayList<>(), shownTabs = new ArrayList<>();

    //compact layout: the tab scroller and the phase button
    private final CompactTabStrip tabStrip;
    private final CompactPhaseButton phaseButton;
    private final List<InfoTab> compactOrder = new ArrayList<>(), compactTabs = new ArrayList<>();
    private float compactScroll, compactScrollTarget; //in tabs, 0 shows the first three tabs
    private float compactCell, compactViewTop, compactViewHeight;
    private float compactSingleWidth; //width of one control column: the double tab layout is two cells wide, the reveal tab keeps this width
    private final List<InfoTab> compactGrid = new ArrayList<>(); //double tab layout: the tabs in grid order, two per row
    private int compactVisible = COMPACT_MAX_VISIBLE_TABS; //tabs shown at once
    private float compactPhaseTop, compactPhaseHeight, compactAvatarSize = -1f, compactAvatarNatural; //phase button under the life total, avatar fitted to the panel
    private CompactScrollAnimation compactScrollAnimation;
    private final Color compactHintColor = new Color();
    private boolean commandUserOpened; //compact layout: opened by hand while no card zone was open, stays open until closed by hand

    public VPlayerPanel(PlayerView player0, boolean showHand, int playerCount) {
        player = player0;
        phaseIndicator = add(new VPhaseIndicator(player0));

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
        tabStrip = add(new CompactTabStrip());
        phaseButton = add(new CompactPhaseButton());
        phaseIndicator.setOnOpenWidthChanged(this::onPhaseStopsResized);
        phaseIndicator.setOnRight(this::isCompactRight);
        // fix z-index
        bringTabsToFront();
        //the compact layout opens the phase stops over the field, so the indicator has to draw above it
        remove(phaseIndicator);
        add(phaseIndicator);
    }

    private void syncCommandZone() {
        if (!Forge.isLandscapeMode()) { return; }
        if (isCompactLayout()) {
            syncCompactCommandZone();
            return;
        }
        if (commandZone.getCount() == 0) {
            commandUserClosed = false; //it opens by itself again next time it has cards
            setCommandOpen(false);
        } else if (!commandOpen && !commandUserClosed) {
            setCommandOpen(true);
        }
    }

    //compact layout: the command zone slides in beside the card zone whenever one is open (unless closed by hand), and can
    //be opened on its own by hand; either way it only shows while it has cards
    private void syncCompactCommandZone() {
        final boolean cardZoneOpen = selectedTab != null && !slideClosing;
        if (!cardZoneOpen) { commandUserClosed = false; } //it follows the next card zone that opens again
        if (commandZone.getCount() == 0) {
            commandUserOpened = false;
            commandUserClosed = false;
            setCommandOpen(false);
            return;
        }
        setCommandOpen(commandUserOpened || (cardZoneOpen && !commandUserClosed));
    }

    private void toggleCompactCommandZone() {
        if (commandOpen) {
            commandUserOpened = false;
            commandUserClosed = true;
        } else {
            commandUserOpened = true;
            commandUserClosed = false;
        }
        syncCommandZone();
    }

    //the phase stops column pushes the field and the open zones aside while it is open
    private void onPhaseStopsResized() {
        if (isCompactLayout() && initW > 0) { updateAnimatedLayout(initW, initH); }
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
        if (tab != null) { scrollCompactToTab(tab); }

        if (this.selectedTab != null) {
            this.selectedTab.setDisplayVisible(true);
            startSlideAnimation(wasClosed);
        }
        if (isCompactLayout()) { syncCommandZone(); }

        if (MatchController.getView() != null) {
            MatchController.getView().revalidate();
        }
    }

    private void startSlideAnimation(boolean fieldToo) {
        stopSlideAnimation();
        boxWidthCur = -1f; //the card zone box snaps to the size of the newly selected zone
        if (!Forge.isLandscapeMode() || getWidth() <= 0) {
            slideProgress = 1f;
            slideFieldToo = false;
            return;
        }
        if (isCompactLayout() && !fieldToo) { //switching zones while one is open: the contents swap, the band stays put
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
        if (isCompactLayout()) { syncCommandZone(); } //the command zone leaves together with the card zone
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
                updateAnimatedLayout(initW, initH);
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
                updateAnimatedLayout(initW, initH);
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
        if (isCompactLayout()) { syncCommandZone(); }
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
        if (tabStrip != null) { //draws the scroll indicators over the tabs
            remove(tabStrip);
            add(tabStrip);
        }
    }

    @Override
    public void setRotate180(boolean b0) {
        //only rotate certain parts of panel
        avatar.setRotate180(b0);
        lblLife.setRotate180(b0);
        phaseIndicator.setRotate180(b0);
        phaseButton.setRotate180(b0);
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
            updateAnimatedLayout(initW, initH); //width follows the card count
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
        if (isCompactLayout()) { return left; } //zones sit in the band along the outer edge and never take space from the field
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
            refreshAltBox();
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
        tabStrip.setVisible(false); //compact landscape controls
        phaseButton.setVisible(false);
        phaseIndicator.syncCompactVisibility();
        restoreAvatarSize();
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

        List<InfoTab> shown = shownTabs;
        shown.clear();
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
        field.setContentAlignment(0f);
        field.setFieldModifier(0);
    }

    private float initW, initH;

    private void doLandscapeLayout(float width, float height) {
        if (selectedTab == commandTab) { hideSelectedTab(); } //portrait selection doesn't carry over
        initW = width;
        initH = height;
        syncCommandZone();
        //tab columns keep a minimum width so the icon and a 2-digit count always fit, even with the half size avatars of 3-4 player games
        if (isCompactLayout()) { //wider column and bigger values so 2 and 3 digit counts stay readable
            tabValueWidth = COMPACT_VALUE_FONT.getBounds("000").width;
            fitCompactAvatar(height);
            compactSingleWidth = Math.max(Math.round(avatar.getWidth() * COMPACT_COLUMN_WIDTH_RATIO), getCompactMinColumnWidth());
            //double tabs: two narrow columns side by side, the counts are badges so each only needs room for its icon
            tabColWidth = isDoubleTabs() ? 2 * getCompactDoubleCell() : compactSingleWidth;
        } else {
            restoreAvatarSize();
            tabValueWidth = INFO_FONT.getBounds("00").width;
            tabColWidth = Math.max(avatar.getWidth(), TAB_MIN_ICON + tabValueWidth + 3 * INFO_TAB_PADDING_X);
        }

        final float x = getColumnX();
        float y = isCompactLayout() ? COMPACT_PANEL_PAD_TOP : 0f;
        avatar.setPosition(x + (tabColWidth - avatar.getWidth()) / 2, y);
        y += avatar.getHeight();

        lblLife.setBounds(x, Forge.altPlayerLayout ? 0 : y, tabColWidth, Forge.altPlayerLayout ? INFO_FONT.getLineHeight() : LIFE_FONT.getLineHeight());
        if (Forge.altPlayerLayout) {
            if (adjustHeight > 2)
                y += INFO_FONT.getLineHeight() / 2;
        } else
            y += lblLife.getHeight();

        final boolean compact = isCompactLayout();
        final boolean phaseButtonShown = compact && !isUnifiedPhase();
        if (compact) { //the phase button sits right under the life total (unless the unified label replaces it), the tab scroller takes the rest
            compactPhaseHeight = getCompactPhaseHeight();
            compactPhaseTop = y + (phaseButtonShown ? TAB_COLUMN_PAD : 0f);
            y = compactPhaseTop + compactPhaseHeight;
        }
        tabColTop = y;
        tabColHeight = height - y;
        tabStrip.setVisible(compact && !isDoubleTabs()); //no scroller, so no scroll bar, when every tab is showing
        phaseButton.setVisible(phaseButtonShown);
        layoutTabsLandscape();
        updateTabLayout(width, height);
    }

    //the compact layout shrinks the avatar on short panels; give it its size back when that layout isn't in use
    private void restoreAvatarSize() {
        if (compactAvatarSize < 0) { return; }
        avatar.setSize(compactAvatarNatural, compactAvatarNatural);
        compactAvatarSize = -1f;
    }

    private float getCompactMinColumnWidth() {
        return TAB_MIN_ICON + tabValueWidth + 3 * INFO_TAB_PADDING_X + getCompactGutter();
    }

    private float getCompactPhaseHeight() {
        if (isUnifiedPhase()) { return 0f; } //the unified label next to the stack menu replaces the panel button
        //a tad shorter than before, and never more than a slice of a short panel, so the zone tabs keep their room in 3-4 player games
        final float wanted = Math.max(Utils.scale(14), INFO2_FONT.getLineHeight());
        final float cap = initH > 0 ? Math.max(Utils.scale(12), initH * COMPACT_PHASE_MAX_RATIO) : wanted;
        return Math.round(Math.min(wanted, cap));
    }

    //vertical room the panel's phase button takes including its gap, nothing when the unified label replaces it
    private float getCompactPhaseSpace() {
        final float h = getCompactPhaseHeight();
        return h > 0 ? TAB_COLUMN_PAD + h : 0f;
    }

    //Shrinks the avatar on short panels (long phones, where a finger sized avatar is huge) so at least three zone tabs
    //of a usable size fit in the column under the avatar, the life total and the phase button.
    private void fitCompactAvatar(float height) {
        if (compactAvatarSize < 0 || Math.abs(avatar.getWidth() - compactAvatarSize) > 0.5f) {
            compactAvatarNatural = Math.min(avatar.getWidth(), VAvatar.HEIGHT); //set from outside (game size changes)
        }
        final float lifeHeight = Forge.altPlayerLayout ? (adjustHeight > 2 ? INFO_FONT.getLineHeight() / 2 : 0) : LIFE_FONT.getLineHeight();
        final boolean doubleTabs = isDoubleTabs();
        final float minCell = doubleTabs ? COMPACT_MIN_CELL : Math.max(COMPACT_MIN_CELL, getCompactMinColumnWidth() * COMPACT_MIN_CELL_RATIO);
        final float budget = height - COMPACT_PANEL_PAD_TOP - lifeHeight - getCompactPhaseSpace() - 2 * TAB_COLUMN_PAD - (doubleTabs ? COMPACT_DOUBLE_ROWS : COMPACT_MAX_VISIBLE_TABS) * minCell;
        final float limit = Math.min(compactAvatarNatural * 0.8f, Math.min(budget, height * COMPACT_MAX_AVATAR_RATIO));
        final float size = Math.round(Math.max(Math.min(COMPACT_MIN_AVATAR, compactAvatarNatural), limit));
        if (Math.abs(avatar.getWidth() - size) > 0.5f || avatar.getHeight() != avatar.getWidth()) {
            avatar.setSize(size, size);
        }
        compactAvatarSize = avatar.getWidth();
    }

    private void layoutTabsLandscape() {
        if (tabs.isEmpty()) { return; }
        if (isCompactLayout()) {
            layoutCompactColumn();
            return;
        }
        float tabWidth = tabColWidth;
        List<InfoTab> left = leftTabs, right = rightTabs;
        left.clear();
        right.clear();
        for (InfoTab tab : tabs) {
            boolean show = tab.isTabShown();
            tab.setVisible(show);
            if (!show) { continue; }
            (tab.isCommonZone() ? right : left).add(tab);
        }
        commonTabWidth = right.isEmpty() ? 0 : tabWidth;

        //one tab height for both columns so every tab and icon matches
        if (tabValueWidth <= 0) { tabValueWidth = INFO_FONT.getBounds("00").width; }
        float valueW = tabValueWidth;
        //each column's tabs share all of its height, with a small gap between them, so they are big enough for a finger
        final float fillGap = Utils.scale(3);
        final float maxCell = tabWidth * TAB_FILL_MAX_RATIO;
        float cellLeft = left.isEmpty() ? 0 : Math.min(maxCell, (tabColHeight - 2 * TAB_COLUMN_PAD - fillGap * (left.size() - 1)) / left.size());
        float cellRight = right.isEmpty() ? 0 : Math.min(maxCell, (initH - 2 * TAB_COLUMN_PAD - fillGap * (right.size() - 1)) / right.size());
        cellLeft = Math.max(cellLeft, left.isEmpty() ? 0 : 1f);
        cellRight = Math.max(cellRight, right.isEmpty() ? 0 : 1f);
        final float cell = left.isEmpty() ? cellRight : (right.isEmpty() ? cellLeft : Math.min(cellLeft, cellRight)); //icons match across columns
        tabIconSize = Math.max(1f, Math.min(cell * TAB_ICON_RATIO, tabWidth - valueW - 3 * INFO_TAB_PADDING_X));

        layoutColumn(left, 0, tabColTop, tabWidth, tabColHeight, cellLeft, fillGap);
        layoutColumn(right, initW - tabWidth, 0, tabWidth, initH, cellRight, fillGap);
    }

    //the order the compact layout's tab scroller shows the tabs in: hand and library first, as in the tabs shown at rest
    private List<InfoTab> getCompactOrder() {
        if (compactOrder.isEmpty()) {
            for (ZoneType zoneType : new ZoneType[] { ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Flashback }) {
                InfoTab zoneTab = zoneTabs.get(zoneType);
                if (zoneTab != null) { compactOrder.add(zoneTab); }
            }
            compactOrder.add(tabManaPool);
            compactOrder.add(commandTab);
            compactOrder.add(extraTab);
        }
        return compactOrder;
    }

    private int getCompactVisibleCount() {
        return Math.max(1, Math.min(compactVisible, compactTabs.size()));
    }

    private float getCompactMaxScroll() {
        return Math.max(0, compactTabs.size() - getCompactVisibleCount());
    }

    //width of one of the two tab columns of the double tab layout
    private float getCompactDoubleCell() {
        return Math.max(Math.round(avatar.getWidth() * 0.9f), Math.round(TAB_MIN_ICON * 2f));
    }

    //Double tab layout: every zone tab at once in two columns, in the order of the desktop version, so nothing scrolls:
    //  hand       | library
    //  graveyard  | exile
    //  flashback  | command
    //  extra      | mana pool
    //The grid is read left to right whichever side the controls are on, and a tab that isn't shown leaves its cell empty
    //so every other tab stays where the desktop puts it.
    private void layoutDoubleColumns() {
        compactTabs.clear();
        compactViewHeight = 0f; //nothing is clipped to a scroller
        final List<InfoTab> grid = getCompactGridOrder();
        for (InfoTab tab : grid) {
            if (tab != null && !tab.isTabShown()) { tab.setVisible(false); }
        }
        if (tabColWidth <= 0 || initH <= 0) { return; }
        final float cellW = tabColWidth / 2;
        final float top = Math.round(tabColTop + TAB_COLUMN_PAD);
        final float cellH = Math.max(1f, (float) Math.floor(Math.min((tabColHeight - 2 * TAB_COLUMN_PAD) / COMPACT_DOUBLE_ROWS, cellW * 1.2f)));
        final float x = getColumnX();
        for (int i = 0; i < grid.size(); i++) {
            final InfoTab tab = grid.get(i);
            if (tab == null || !tab.isTabShown()) { continue; }
            final int col = i % 2, row = i / 2;
            tab.setBounds(x + col * cellW, top + row * cellH, cellW, cellH);
            tab.setVisible(true);
        }
        phaseButton.setBounds(x + TAB_COLUMN_PAD, compactPhaseTop, tabColWidth - 2 * TAB_COLUMN_PAD, compactPhaseHeight);
    }

    //the desktop's tab grid, row by row: the extra zones tab and the mana pool are always the last row
    private List<InfoTab> getCompactGridOrder() {
        if (compactGrid.isEmpty()) {
            for (ZoneType zoneType : new ZoneType[] { ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Flashback }) {
                compactGrid.add(zoneTabs.get(zoneType));
            }
            compactGrid.add(commandTab);
            compactGrid.add(extraTab);
            compactGrid.add(tabManaPool);
        }
        return compactGrid;
    }

    //compact layout: avatar and life on top, the phase button under them and the three tab scroller below
    private void layoutCompactColumn() {
        commonTabWidth = 0f; //no right hand tab column
        if (isDoubleTabs()) {
            layoutDoubleColumns();
            return;
        }
        final List<InfoTab> shown = compactTabs;
        shown.clear();
        for (InfoTab tab : getCompactOrder()) {
            if (tab.isTabShown()) { shown.add(tab); } else { tab.setVisible(false); }
        }
        if (shown.isEmpty() || tabColWidth <= 0 || initH <= 0) { return; }
        if (tabValueWidth <= 0) { tabValueWidth = INFO_FONT.getBounds("00").width; }

        //three tabs, always: the avatar was fitted so they stay a usable size
        final float available = tabColHeight - 2 * TAB_COLUMN_PAD;
        compactVisible = COMPACT_MAX_VISIBLE_TABS;
        final int visible = getCompactVisibleCount();
        float cell = available / COMPACT_MAX_VISIBLE_TABS;
        cell = Math.max(1f, (float) Math.floor(Math.min(cell, tabColWidth * TAB_MAX_HEIGHT_RATIO)));
        compactCell = cell;
        tabIconSize = Math.max(1f, Math.min(cell * TAB_ICON_RATIO, tabColWidth - getCompactGutter() - tabValueWidth - 3 * INFO_TAB_PADDING_X));

        compactViewTop = Math.round(tabColTop + TAB_COLUMN_PAD);
        compactViewHeight = cell * visible;
        tabStrip.setBounds(getColumnX(), compactViewTop, tabColWidth, compactViewHeight);

        phaseButton.setBounds(getColumnX() + TAB_COLUMN_PAD, compactPhaseTop, tabColWidth - 2 * TAB_COLUMN_PAD, compactPhaseHeight);

        final float max = getCompactMaxScroll();
        compactScroll = Math.max(0f, Math.min(compactScroll, max));
        compactScrollTarget = Math.max(0f, Math.min(compactScrollTarget, max));
        placeCompactTabs();
    }

    //room kept free on the column's left edge for the scroll bar, with a small gap before the tabs
    private float getCompactGutter() {
        return Math.max(2f, Utils.scale(3)) + Utils.scale(1) + COMPACT_SCROLL_GAP;
    }

    //tabs sit one cell apart, shifted by the scroll position; only tabs inside the viewport are visible (and take touches)
    private void placeCompactTabs() {
        final float viewBottom = compactViewTop + compactViewHeight;
        for (int i = 0; i < compactTabs.size(); i++) {
            final InfoTab tab = compactTabs.get(i);
            final float y = Math.round(compactViewTop + (i - compactScroll) * compactCell);
            tab.setBounds(getColumnX() + getCompactGutter(), y, tabColWidth - getCompactGutter(), compactCell);
            tab.setVisible(y + compactCell > compactViewTop + 0.5f && y < viewBottom - 0.5f);
        }
    }

    private void setCompactScroll(float scroll) {
        compactScroll = Math.max(0f, Math.min(scroll, getCompactMaxScroll()));
        placeCompactTabs();
        Gdx.graphics.requestRendering();
    }

    //snaps to a whole tab, so the viewport always holds complete tabs when at rest
    private void setCompactScrollTarget(float target) {
        compactScrollTarget = Math.max(0f, Math.min(Math.round(target), getCompactMaxScroll()));
        if (compactScrollAnimation == null && compactScroll != compactScrollTarget) {
            compactScrollAnimation = new CompactScrollAnimation();
            compactScrollAnimation.start();
        }
    }

    private void stopCompactScrollAnimation() {
        CompactScrollAnimation old = compactScrollAnimation;
        compactScrollAnimation = null; //so the old animation's onEnd does nothing
        if (old != null) { old.stop(); }
    }

    //a tab that was selected from elsewhere (gamepad, a card moving zones) must not stay out of sight
    private void scrollCompactToTab(InfoTab tab) {
        if (!isCompactLayout()) { return; }
        final int idx = compactTabs.indexOf(tab);
        if (idx < 0) { return; }
        float target = compactScrollTarget;
        if (idx < target) {
            target = idx;
        } else if (idx > target + getCompactVisibleCount() - 1) {
            target = idx - getCompactVisibleCount() + 1;
        }
        setCompactScrollTarget(target);
    }

    private class CompactScrollAnimation extends ForgeAnimation {
        @Override
        protected boolean advance(float dt) {
            final float diff = compactScrollTarget - compactScroll;
            if (Math.abs(diff) < 0.01f) {
                compactScroll = compactScrollTarget;
                placeCompactTabs();
                return false;
            }
            compactScroll += diff * Math.min(1f, dt * COMPACT_SNAP_SPEED);
            placeCompactTabs();
            return true;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (compactScrollAnimation != this) { return; } //replaced by a newer animation
            compactScrollAnimation = null;
            compactScroll = compactScrollTarget;
            placeCompactTabs();
        }
    }

    /**
     * Transparent touch surface over the compact layout's tab viewport. Taps still reach the tabs themselves; a vertical
     * drag scrolls the tabs and lets go on a whole tab.
     */
    private class CompactTabStrip extends FDisplayObject {
        private boolean panning;
        private long lastPanMillis;

        //a scroll bar on the column's right edge plus arrows when more tabs wait above or below the two shown
        @Override
        public void draw(Graphics g) {
            final float max = getCompactMaxScroll();
            if (max <= 0 || !isCompactLayout()) { return; }
            final float w = getWidth(), h = getHeight();
            //theme colours: the active colour for the thumb and arrows over a track in the border colour
            final Color c = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_ACTIVE : Colors.CLR_ACTIVE).getColor();
            final Color track = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_INACTIVE : Colors.CLR_INACTIVE).getColor();

            final float barW = Math.max(2f, Utils.scale(3));
            final float barX = Utils.scale(1); //on the left edge of the column, clear of the tabs
            final float thumbH = Math.max(Utils.scale(8), h * getCompactVisibleCount() / compactTabs.size());
            final float thumbY = (h - thumbH) * Math.max(0f, Math.min(compactScroll / max, 1f));
            compactHintColor.set(track.r, track.g, track.b, 0.8f);
            g.fillRoundRect(compactHintColor, barX, 0, barW, h, barW / 2);
            compactHintColor.set(c.r, c.g, c.b, 1f);
            g.fillRoundRect(compactHintColor, barX, thumbY, barW, thumbH, barW / 2);

            //no arrows: the scroll bar already shows that more tabs wait above or below
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
            if (!isCompactLayout() || compactCell <= 0 || getCompactMaxScroll() <= 0) { return false; } //nothing to scroll
            if (!panning && Math.abs(deltaX) > Math.abs(deltaY)) { return false; } //a sideways drag isn't a scroll
            panning = true;
            lastPanMillis = System.currentTimeMillis();
            stopCompactScrollAnimation();
            setCompactScroll(compactScroll - deltaY / compactCell); //the tabs follow the finger
            return true;
        }

        @Override
        public boolean panStop(float x, float y) {
            if (!panning) { return false; }
            panning = false;
            setCompactScrollTarget(compactScroll);
            return true;
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            if (!isCompactLayout() || System.currentTimeMillis() - lastPanMillis > COMPACT_FLING_WINDOW_MS) { return false; }
            if (Math.abs(velocityY) < Math.abs(velocityX)) { return false; }
            //a flick moves on by one whole tab in its direction instead of coasting
            if (velocityY < 0) {
                setCompactScrollTarget((float) Math.floor(compactScroll + 0.001f) + 1);
            } else {
                setCompactScrollTarget((float) Math.ceil(compactScroll - 0.001f) - 1);
            }
            return true;
        }
    }

    /** Compact layout: names the current phase, and opens the vertical phase stops beside the avatar when pressed. */
    private class CompactPhaseButton extends FDisplayObject {
        @Override
        public boolean tap(float x, float y, int count) {
            if (!isCompactLayout()) { return false; }
            phaseIndicator.toggleStops();
            return true;
        }

        @Override
        public void draw(Graphics g) {
            final boolean rotated = getRotate180();
            if (rotated) { g.startRotateTransform(getWidth() / 2, getHeight() / 2, 180); }
            try {
                phaseIndicator.drawPhaseButton(g, getWidth(), getHeight());
            } finally {
                if (rotated) { g.endTransform(); }
            }
        }
    }

    private void layoutColumn(List<InfoTab> column, float x, float top, float w, float h, float cell, float gap) {
        if (column.isEmpty()) { return; }
        int n = column.size();
        float y = top + (h - (cell * n + gap * (n - 1))) / 2;
        for (InfoTab tab : column) {
            tab.setBounds(x, Math.round(y), w, cell);
            y += cell + gap;
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
        phaseIndicator.resetFont();
        if (isCompactLayout()) { //the stops column opens over the left edge of the field
            final float stopsWidth = Math.round(Math.max(avatar.getWidth() * COMPACT_STOPS_WIDTH_RATIO, tabColWidth * 0.55f));
            //opens on the inner side of the control column
            phaseIndicator.setBounds(isCompactRight() ? getColumnX() - stopsWidth : tabColWidth, 0, stopsWidth, height);
        } else {
            phaseIndicator.setBounds(tabColWidth, 0, avatar.getWidth() * 0.6f, height);
        }
        phaseIndicator.syncCompactVisibility();
        updateAnimatedLayout(width, height);
    }

    //only what moves during slide animations: field, command zone and zone displays
    private void updateAnimatedLayout(float width, float height) {
        if (isCompactLayout()) {
            updateCompactAnimatedLayout(width, height);
            return;
        }
        if (isAltZoneLayout()) {
            updateAltAnimatedLayout(width, height);
            return;
        }
        float x = tabColWidth + avatar.getWidth() * 0.6f; //where the phase indicator ends

        float fieldWidth = width - x - commonTabWidth;
        float displayAreaWidth = height / FCardPanel.ASPECT_RATIO;
        //the field changes size once when a zone opens or closes, not every frame of the slide: cards re-flowing into new
        //rows while the display slides was the flicker. With compact zone tabs it keeps its size and the display covers it.
        final float shrunkWidth = selectedTab == null ? fieldWidth : fieldWidth - displayAreaWidth;
        if (!isZoneOverlay()) { fieldWidth = shrunkWidth; }

        //command zone: half height strip at the right end of the field, on the outer edge of the panel
        float cmdHeight = height / 2;
        float cmdWidth = Math.max(1, Math.min(commandZone.getCount(), 2)) * commandZone.getCardWidth(cmdHeight);
        commandVisibleWidth = cmdWidth * commandProgress;
        commandZone.setBounds(x + shrunkWidth - commandVisibleWidth, isFlipped() ? 0 : height - cmdHeight, cmdWidth, cmdHeight);
        field.setCommandZoneWidth(commandVisibleWidth > 0 ? commandVisibleWidth + 1 : 0);
        field.setBounds(x, 0, fieldWidth, height);
        field.setEndPadding(isZoneOverlay() && selectedTab != null ? displayAreaWidth : 0f);

        float displayLeft = width - commonTabWidth - displayAreaWidth + (1f - slideProgress) * displayAreaWidth;
        for (InfoTab tab : tabs) {
            tab.setDisplayBounds(displayLeft, 0, displayAreaWidth, height);
        }
        field.setContentAlignment(0f);
        field.setFieldModifier(0);
    }

    //alternate landscape layout: the card zone is a box in the outer corner (half the panel high, right aligned),
    //the command zone sits above it (below it for the flipped player) and the field keeps the rest
    private void updateAltAnimatedLayout(float width, float height) {
        final float x = tabColWidth + avatar.getWidth() * 0.6f; //where the phase indicator ends
        final float regionWidth = width - x - commonTabWidth;   //space between the phase indicator and the common tabs
        final boolean box = selectedTab != null && isBoxDisplay(selectedTab.getDisplayArea());
        final float shown = selectedTab == null ? 0f : 1f; //the field resizes once, not every frame of the slide
        final boolean overlay = isZoneOverlay();
        final float displayAreaWidth = height / FCardPanel.ASPECT_RATIO;

        final float boxHeight = Math.round(height * ALT_ZONE_HEIGHT_RATIO);
        final float boxWidth = getBoxWidth(regionWidth * ALT_ZONE_MAX_WIDTH_RATIO, boxHeight);
        final float boxVisible = box ? boxWidth * shown : 0f;
        //anything else (the mana pool) keeps the full height side column
        final float sideWidth = selectedTab == null || box ? 0f : displayAreaWidth * shown;

        float cmdWidth = Math.max(1, Math.min(commandZone.getCount(), 2)) * commandZone.getCardWidth(boxHeight);
        commandVisibleWidth = cmdWidth * commandProgress;
        final float fieldRight = x + regionWidth - sideWidth;
        commandZone.setBounds(fieldRight - commandVisibleWidth, isFlipped() ? height - boxHeight : 0, cmdWidth, boxHeight);
        final float cmdReserve = commandVisibleWidth > 0 ? commandVisibleWidth + 1 : 0;
        if (overlay) { //compact zone tabs: the field keeps its whole area, zones open over it
            field.setBounds(x, 0, regionWidth, height);
            field.setCommandZoneWidth(0);
        } else {
            field.setBounds(x, 0, fieldRight - x - cmdReserve, height);
            field.setCommandZoneWidth(Math.max(0f, boxVisible - cmdReserve)); //only the outer row is shortened by the box
        }
        field.setEndPadding(overlay ? Math.max(box ? boxWidth : sideWidth > 0 ? displayAreaWidth : 0f, commandProgress > 0 ? cmdWidth : 0f) : 0f);

        final float boxTop = isFlipped() ? 0 : height - boxHeight;
        final float boxLeft = x + regionWidth - boxWidth * slideProgress;
        final float sideLeft = width - commonTabWidth - displayAreaWidth + (1f - slideProgress) * displayAreaWidth;
        for (InfoTab tab : tabs) {
            if (isBoxDisplay(tab.getDisplayArea())) {
                tab.setDisplayBounds(boxLeft, boxTop, boxWidth, boxHeight);
            } else {
                tab.setDisplayBounds(sideLeft, 0, displayAreaWidth, height);
            }
        }
        field.setContentAlignment(0f);
        field.setFieldModifier(0);
    }

    //compact layout: the card zone and the command zone share a band along the panel's outer edge, a third of the panel high.
    //The field shrinks to the rest, so its two rows are as tall as the zone cards and nothing is ever covered. The card zone
    //slides up from the edge and runs up to the command zone, which slides in from the right; the phase stops column, while
    //open, pushes everything aside.
    private void updateCompactAnimatedLayout(float width, float height) {
        final boolean right = isCompactRight();
        final float x = right ? 0f : tabColWidth + phaseIndicator.getOpenWidth(); //left edge of what the controls leave free
        final float regionWidth = Math.max(1f, right ? width - tabColWidth - phaseIndicator.getOpenWidth() : width - x);
        final boolean flipped = isFlipped();
        if (isZoneOverlay()) { //compact zone tabs: the card zone is a box in the right hand corner, as in the alternate zone layout
            updateCompactOverlayLayout(width, height, x, regionWidth);
            return;
        }
        final float stripHeight = Math.round(height * COMPACT_STRIP_RATIO);
        final float stripTop = flipped ? 0 : height - stripHeight;
        final float boxReveal = selectedTab == null ? 0f : slideProgress;
        final float bandHeight = stripHeight * Math.max(boxReveal, commandProgress);
        final float fieldBand = isZoneOverlay() ? 0f : bandHeight; //compact zone tabs: the band covers the field instead of shrinking it

        //the command zone is boxed exactly like the card zone: same band, same inset, so cards match in height and the boxes never overlap
        final float inset = COMPACT_ZONE_INSET;
        final float cmdCardsWidth = Math.max(1, Math.min(commandZone.getCount(), 2)) * commandZone.getCardWidth(stripHeight - 2 * inset);
        final float cmdBoxWidth = cmdCardsWidth + 2 * inset;
        commandVisibleWidth = cmdCardsWidth * commandProgress; //the part of the cards slid out
        commandBoxVisibleWidth = cmdBoxWidth * commandProgress;
        commandBoxLeft = right ? x : width - commandBoxVisibleWidth;
        //slides in from the screen edge facing away from the controls
        commandZone.setBounds(right ? x + inset - (cmdCardsWidth - commandVisibleWidth) : width - commandBoxVisibleWidth + inset, stripTop + inset, cmdCardsWidth, stripHeight - 2 * inset);

        final float gap = commandBoxVisibleWidth > 0 ? COMPACT_ZONE_GAP : 0f; //air between the two boxes
        final float boxWidth = Math.max(1f, regionWidth - commandBoxVisibleWidth - gap); //expands up to the command zone box
        final float boxX = right ? x + commandBoxVisibleWidth + gap : x;
        final float hidden = (1f - slideProgress) * stripHeight * (flipped ? -1f : 1f); //beyond the edge while sliding
        for (InfoTab tab : tabs) {
            tab.setDisplayBounds(boxX + inset, stripTop + hidden + inset, Math.max(1f, boxWidth - 2 * inset), stripHeight - 2 * inset);
        }

        field.setCommandZoneWidth(0);
        field.setEndPadding(0f);
        if (isCompactAlternate()) {
            //the field keeps the middle of the screen: it leaves room for a column on both sides so its cards stay centered
            final float stops = phaseIndicator.getOpenWidth();
            final float fieldX = right ? tabColWidth : tabColWidth + stops;
            final float fieldW = Math.max(1f, width - 2 * tabColWidth - stops);
            field.setContentAlignment(0.5f);
            field.setBounds(fieldX, flipped ? fieldBand : 0, fieldW, height - fieldBand);
        } else {
            field.setContentAlignment(right ? 1f : 0f); //cards sit against the controls
            field.setBounds(x, flipped ? fieldBand : 0, regionWidth, height - fieldBand);
        }
        field.setFieldModifier(0);
    }

    //compact zone tabs: like the alternate layout's corner box, half the panel high and right aligned, with the command zone
    //at the other end of the panel, while the field keeps its whole area and the box covers what is under it
    private void updateCompactOverlayLayout(float width, float height, float x, float regionWidth) {
        final boolean flipped = isFlipped();
        final float boxHeight = Math.round(height * ALT_ZONE_HEIGHT_RATIO);
        final float inset = COMPACT_ZONE_INSET; //the cards are a little smaller than the box, so the rounded frame drawn around them has room
        final float boxWidth = getBoxWidth(regionWidth * ALT_ZONE_MAX_WIDTH_RATIO, boxHeight - 2 * inset) + 2 * inset;
        final float boxTop = flipped ? 0 : height - boxHeight;
        final float boxLeft = x + regionWidth - boxWidth * slideProgress; //slides in from the right edge

        final float cmdCardsWidth = Math.max(1, Math.min(commandZone.getCount(), 2)) * commandZone.getCardWidth(boxHeight - 2 * inset);
        final float cmdWidth = cmdCardsWidth + 2 * inset; //the outer box, as wide as the card zone box is for the same cards
        commandVisibleWidth = cmdCardsWidth * commandProgress;
        commandBoxVisibleWidth = cmdWidth * commandProgress;
        commandBoxLeft = x + regionWidth - commandBoxVisibleWidth;
        commandZone.setBounds(commandBoxLeft + inset, (flipped ? height - boxHeight : 0) + inset, cmdCardsWidth, boxHeight - 2 * inset);

        for (InfoTab tab : tabs) {
            tab.setDisplayBounds(boxLeft + inset, boxTop + inset, boxWidth - 2 * inset, boxHeight - 2 * inset);
        }
        field.setCommandZoneWidth(0);
        if (isCompactAlternate()) {
            final float stops = phaseIndicator.getOpenWidth();
            final boolean rightSide = isCompactRight();
            final float fieldX = rightSide ? tabColWidth : tabColWidth + stops;
            final float fieldW = Math.max(1f, width - 2 * tabColWidth - stops);
            field.setContentAlignment(0.5f);
            field.setBounds(fieldX, 0, fieldW, height);
        } else {
            field.setContentAlignment(isCompactRight() ? 1f : 0f);
            field.setBounds(x, 0, regionWidth, height);
        }
        final boolean covers = selectedTab != null && isBoxDisplay(selectedTab.getDisplayArea());
        final float cover = Math.max(covers ? boxWidth : 0f, commandProgress > 0 ? cmdWidth : 0f); //snapped, so the rows don't re-flow every animation frame
        field.setEndPadding(Math.max(0f, field.getLeft() + field.getWidth() - (x + regionWidth - cover)));
        field.setFieldModifier(0);
    }

    private float boxWidthCur = -1f, boxWidthTarget = -1f; //animated width of the alternate layout's card zone box
    private BoxWidthAnimation boxWidthAnimation;

    //the box is one card wide while empty and grows with its cards, up to a share of the panel width
    private float getBoxWidth(float maxWidth, float boxHeight) {
        float cardWidth = commandZone.getCardWidth(boxHeight);
        float target = cardWidth;
        VDisplayArea display = selectedTab == null ? null : selectedTab.getDisplayArea();
        if (display instanceof VManaPool pool) { //a fixed row of mana icons
            target = pool.getLabelCount() * Math.round(boxHeight * ALT_MANA_LABEL_RATIO);
        } else if (display instanceof VCardDisplayArea) {
            target = Math.max(cardWidth, display.getCount() * cardWidth);
        }
        target = (float) Math.ceil(Math.min(target, Math.max(cardWidth, maxWidth)));
        if (boxWidthCur < 0 || selectedTab == null) { //a newly opened zone snaps to its size
            boxWidthCur = boxWidthTarget = target;
        } else if (target != boxWidthTarget) {
            boxWidthTarget = target;
            if (boxWidthAnimation == null) {
                boxWidthAnimation = new BoxWidthAnimation();
                boxWidthAnimation.start();
            }
        }
        return Math.round(boxWidthCur);
    }

    //zone contents changed: the box follows the card count
    private void refreshAltBox() {
        if (selectedTab != null && Forge.isLandscapeMode() && initW > 0 && isAltZoneLayout()) {
            updateAnimatedLayout(initW, initH);
        }
    }

    private class BoxWidthAnimation extends ForgeAnimation {
        @Override
        protected boolean advance(float dt) {
            float diff = boxWidthTarget - boxWidthCur;
            if (Math.abs(diff) < 1f) {
                boxWidthCur = boxWidthTarget;
            } else {
                boxWidthCur += diff * Math.min(1f, dt * 16f);
            }
            if (selectedTab != null && Forge.isLandscapeMode() && initW > 0) {
                updateAnimatedLayout(initW, initH);
            }
            return boxWidthCur != boxWidthTarget;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (boxWidthAnimation == this) { boxWidthAnimation = null; }
            boxWidthCur = boxWidthTarget;
        }
    }

    public float getCommonTabWidth() { return commonTabWidth; }

    /** @return the width of one control column in the compact layout (the double tab layout is two of these wide), 0 before the first layout */
    public float getColumnWidth() { return compactSingleWidth > 0f && isCompactLayout() ? compactSingleWidth : tabColWidth; }

    /**
     * @return the screen rect cards can visibly fly through in landscape mode, which is the panel without its two tab
     * columns, or null in portrait mode or before the first layout
     */
    public Rectangle getFlightArea() {
        if (!Forge.isLandscapeMode() || screenPos.width <= 0 || tabColWidth <= 0) { return null; }
        float left = screenPos.x + tabColWidth;
        float right = screenPos.x + screenPos.width - commonTabWidth;
        if (isCompactRight()) { //controls on the right: the free area is everything left of them
            left = screenPos.x;
            right = screenPos.x + screenPos.width - tabColWidth;
        }
        if (right <= left) { return null; }
        flightArea.set(left, screenPos.y, right - left, screenPos.height);
        return flightArea;
    }

    @Override
    public void draw(Graphics g) {
        if (Forge.isLandscapeMode() && commonTabWidth > 0 && slideProgress < 1f) {
            //zone displays slide in and out from behind the right tab column, so don't let them draw over it
            g.startClip(0, 0, getWidth() - commonTabWidth, getHeight());
            try {
                super.draw(g);
            } finally {
                g.endClip();
            }
            drawCommonTabColumn(g);
        } else if (slideProgress < 1f && Forge.isLandscapeMode()) {
            //keep the zone display from drawing outside this panel while it slides
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

    //the right tab column and its tabs, drawn again on top after the clipped pass leaves them out
    private void drawCommonTabColumn(Graphics g) {
        FSkinColor tabBg = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2);
        if (!isFrosted()) { g.fillRect(tabBg, getWidth() - commonTabWidth, 0, commonTabWidth, getHeight()); }
        for (InfoTab tab : tabs) {
            if (tab.isVisible() && tab.isCommonZone()) {
                g.draw(tab);
            }
        }
    }

    @Override
    protected void drawOverlay(Graphics g) {
        super.drawOverlay(g);
    }

    static void fillOverlayZone(Graphics g, float x, float w, float h) {
        if (isFrosted()) { g.fillRect(COMPACT_ZONE_OVER, x, 0, w, h); }
        else { g.fillRect(getDisplayAreaBackColor(), x, 0, w, h); }
    }

    //frosted style: zone boxes and their edges are frosted glass like the panel, not solid theme colours
    private void fillZone(Graphics g, float x, float y, float w, float h) {
        if (isFrosted()) { g.fillRect(COMPACT_ZONE, x, y, w, h); }
        else { g.fillRect(getDisplayAreaBackColor(), x, y, w, h); }
    }

    private void zoneLine(Graphics g, float x1, float y1, float x2, float y2) {
        if (isFrosted()) { g.drawLine(1, COMPACT_GLASS_EDGE, x1, y1, x2, y2); }
        else { g.drawLine(1, MatchScreen.getBorderColor(), x1, y1, x2, y2); }
    }

    //compact layout: the open zone sits in a rounded frosted box, a light ring around dark glass
    private void drawZoneBox(Graphics g, float x, float y, float w, float h) {
        if (w <= 0f || h <= 0f) {
            return;
        }

        final float ring = Math.min(
                Math.max(1f, Utils.scale(1.5f)),
                Math.min(w, h) * 0.5f);
        final float radius = Math.min(COMPACT_BOX_RADIUS, Math.min(w, h) * 0.5f);
        final float innerW = w - 2f * ring;
        final float innerH = h - 2f * ring;

        g.fillRoundRect(COMPACT_BOX_EDGE, x, y, w, h, radius);

        if (innerW > 0f && innerH > 0f) {
            g.fillRoundRect(COMPACT_ZONE,
                    x + ring, y + ring, innerW, innerH,
                    Math.max(0f, radius - ring));
        }
    }

    @Override
    public void drawBackground(Graphics g) {
        if (Forge.isLandscapeMode()) {
            FSkinColor tabBg = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2);
            if (commonTabWidth > 0) {
                if (isFrosted()) { g.fillRect(COMPACT_GLASS, getWidth() - commonTabWidth, 0, commonTabWidth, getHeight()); }
                else { g.fillRect(tabBg, getWidth() - commonTabWidth, 0, commonTabWidth, getHeight()); }
            }
            if (isFrosted()) { //a rounded panel floating inside the column, so the background shows around it
                //frosted glass: one flat translucent slate rectangle across the whole column, so panels leave no gaps
                g.fillRect(COMPACT_GLASS, getColumnX(), 0, tabColWidth, getHeight());
                if (isDoubleTabs()) { //a faint line between the two tab columns
                    final float mid = getColumnX() + tabColWidth / 2;
                    g.drawLine(1, COMPACT_GLASS_EDGE, mid, tabColTop, mid, getHeight());
                }
            } else {
                g.fillRect(tabBg, getColumnX(), 0, tabColWidth, getHeight());
            }
        } else {
            g.fillRect(isFrosted() ? COMPACT_GLASS : Color.BLACK, 0, avatar.getTop(), getWidth(), avatarHeight);
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
            final boolean overlayBox = isZoneOverlay() && Forge.isLandscapeMode() && isStripDisplay(selectedDisplayArea);
            //compact layout: the open zone sits in a rounded box with room around its cards, so it reads apart from the battlefield
            final boolean compactBox = isCompactLayout() && isStripDisplay(selectedDisplayArea);
            if (compactBox) {
                drawZoneBox(g, x - COMPACT_ZONE_INSET, top - COMPACT_ZONE_INSET, w + 2 * COMPACT_ZONE_INSET, h + 2 * COMPACT_ZONE_INSET);
            } else if (!overlayBox) {
                fillZone(g, x, top, w, h); //an overlay box is painted by the zone itself, card-sized
            }

            if (overlayBox || compactBox) {
                //no frame lines: the compact box has its own ring, and the full overlay box would extend past the cards
            } else if (Forge.isLandscapeMode()) {
                if (isStripDisplay(selectedDisplayArea)) { //corner box: border on its left and field facing edges
                    float edgeY = isFlipped() ? bottom : top;
                    zoneLine(g, x, top, x, bottom);
                    zoneLine(g, x, edgeY, x + w, edgeY);
                    if (isCompactLayout()) { //the compact box floats beside the field, so it is closed on its right too
                        zoneLine(g, x + w, top, x + w, bottom);
                    }
                } else {
                    zoneLine(g, x, top, x, bottom);
                }
            } else {
                y = isFlipped() ? top + 1 : bottom;
                //don't know why infotab gets null here, either way don't crash the gui..
                float left = infoTab == null ? 0 : infoTab.getLeft();
                float right = infoTab == null ? 0 : infoTab.getRight();
                //leave gap at selected zone tab
                zoneLine(g, x, y, left, y);
                zoneLine(g, right, y, w, y);
            }
        }
        if (Forge.isLandscapeMode() && commandVisibleWidth > 0 && commandZone.isVisible()) {
            if (isCompactLayout()) { //the same rounded box as the card zone, its height and inset taken from the layout
                final float inset = COMPACT_ZONE_INSET;
                drawZoneBox(g, commandBoxLeft, commandZone.getTop() - inset, commandBoxVisibleWidth, commandZone.getHeight() + 2 * inset);
            } else {
                //the visible part of the zone: its left side normally, its right side when it slides in from the left edge
                float cx = isCompactRight() ? commandZone.getLeft() + commandZone.getWidth() - commandVisibleWidth : commandZone.getLeft();
                float cy = commandZone.getTop();
                float ch = commandZone.getHeight();
                fillZone(g, cx, cy, commandVisibleWidth, ch);
                float sideX = isCompactRight() ? cx + commandVisibleWidth : cx; //the edge facing the field
                zoneLine(g, sideX, cy, sideX, cy + ch);
                //edge facing the field (the alternate layout puts the command zone on the inner side)
                float edgeY = isFlipped() != isAltZoneLayout() ? cy + ch : cy;
                zoneLine(g, cx, edgeY, cx + commandVisibleWidth, edgeY);
            }
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

        //landscape: every icon is fitted into the same square box at the same x, so tabs line up
        private void drawLandscapeContent(Graphics g) {
            FSkinImageInterface icon = getIcon();
            boolean rotated = lblLife.getRotate180();
            float box = tabIconSize > 0 ? tabIconSize : Math.min(getWidth(), getHeight()) * 0.6f;
            float gap = INFO_TAB_PADDING_X * 2;
            float valueW = tabValueWidth;
            float x0 = Math.max(INFO_TAB_PADDING_X, (getWidth() - (box + gap + valueW)) / 2);
            float cy = getHeight() / 2;

            //fit the icon inside the box, keeping its proportions
            float iw = box, ih = box;
            float ratio = (float) icon.getWidth() / icon.getHeight();
            if (ratio > 1) { ih = box / ratio; } else { iw = box * ratio; }
            float ix = x0 + (box - iw) / 2;
            float iy = cy - ih / 2;
            float grow = isHovered() ? box / 8f : 0;

            if (rotated) { g.startRotateTransform(ix + iw / 2, cy, 180); }
            g.drawImage(icon, ix - grow / 2, iy - grow / 2, iw + grow, ih + grow);
            if (rotated) { g.endTransform(); }

            float tx = x0 + box + gap;
            float tw = getWidth() - tx + 1;
            int align = Align.left;
            if (rotated) {
                g.startRotateTransform(tx + tw / 2, cy, 180);
                align = Align.right;
            }
            g.drawText(value, isCompactLayout() ? COMPACT_VALUE_FONT : INFO_FONT, getInfoForeColor(), tx, 0, tw, getHeight(), false, align, true);
            if (rotated) { g.endTransform(); }
        }

        //double tabs: so two columns fit side by side
        private void drawDoubleContent(Graphics g) {
            final FSkinImageInterface icon = getIcon();
            final float w = getWidth();
            final float h = getHeight();
            final boolean hasValue = !value.isEmpty();
            final boolean rotated = lblLife.getRotate180();

            final float pad = INFO_TAB_PADDING_X;
            final float gap = INFO_TAB_PADDING_X;
            final float textW = hasValue ? COMPACT_VALUE_FONT.getBounds(value).width : 0f;
            final float iconBox = Math.max(1f,
                    Math.min(TAB_MIN_ICON, Math.min(h - 2 * pad, w * 0.42f)));

            float iw = iconBox;
            float ih = iconBox;
            final float ratio = (float) icon.getWidth() / icon.getHeight();

            if (ratio > 1f) {
                ih = iconBox / ratio;
            } else {
                iw = iconBox * ratio;
            }

            final float contentW = iw + (hasValue ? gap + textW : 0f);
            final float startX = Math.max(pad, (w - contentW) / 2f);
            final float iconY = (h - ih) / 2f;
            final float grow = isHovered() ? iconBox / 8f : 0f;

            if (rotated) {
                g.startRotateTransform(w / 2f, h / 2f, 180);
            }

            g.drawImage(icon,
                    startX - grow / 2f,
                    iconY - grow / 2f,
                    iw + grow,
                    ih + grow);

            if (hasValue) {
                final float tx = startX + iw + gap;
                g.drawText(value, COMPACT_VALUE_FONT, getInfoForeColor(),
                        tx, 0, Math.max(0f, w - tx - pad), h,
                        false, Align.left, true);
            }

            if (rotated) {
                g.endTransform();
            }
        }

        @Override
        public void draw(Graphics g) {
            //compact layout: tabs scroll under the edges of the viewport, so a tab part way out must not draw past it
            final boolean clip = isCompactLayout() && compactViewHeight > 0;
            if (clip) { g.startClip(0, compactViewTop - getTop(), getWidth(), compactViewHeight); }
            try {
                drawTab(g);
            } finally {
                if (clip) { g.endClip(); }
            }
        }

        private void drawTab(Graphics g) {
            if (Forge.isLandscapeMode() && isCommonZone() && !isFrosted()) {
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
                float fillY = Forge.isLandscapeMode() ? 0 : (isFlipped() ? INFO_TAB_PADDING_Y : 0);
                float fillH = Forge.isLandscapeMode() ? getHeight() : getHeight() - INFO_TAB_PADDING_Y;
                if (drawOverlay)
                    g.fillRect(FSkinColor.getStandardColor(50, 200, 150).alphaColor(0.3f), 0, fillY, w, fillH);
                //change the graveyard tab selection color to active phase color to indicate the player has delirium
                if (isCompactLayout()) { //a flat soft band across the panel, no rounding
                    final float pad = Utils.scale(3);
                    g.fillRect(COMPACT_GLASS_CHIP, pad, 0, w - 2 * pad, getHeight());
                } else if (isFrosted()) {
                    g.fillRect(COMPACT_GLASS_CHIP, 0, fillY, w, fillH);
                } else {
                    g.fillRect(this.getSelectedBackgroundColor(), 0, fillY, w, fillH);
                }
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
            if (Forge.isLandscapeMode()) {
                if (isDoubleTabs()) { drawDoubleContent(g); } else { drawLandscapeContent(g); }
                return;
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
            //return Forge.isLandscapeMode() && (isSelected() || displayArea.getCount() > 0 || commandProgress > 0f);
            // Keep the Commander zone tab available in compact landscape mode,
            // even when the zone is empty. The compact two-column layout should
            // expose the same zone controls consistently.
            return Forge.isLandscapeMode() && (isCompactLayout() || isSelected() || displayArea.getCount() > 0 || commandProgress > 0f);
        }

        @Override
        public void setDisplayBounds(float x, float y, float width, float height) {
            if (!Forge.isLandscapeMode()) { //landscape lays this display out separately
                super.setDisplayBounds(x, y, width, height);
            }
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (isCompactLayout()) {
                toggleCompactCommandZone();
                return true;
            }
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
                updateAnimatedLayout(initW, initH);
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
                updateAnimatedLayout(initW, initH);
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
                g.startClip(isCompactRight() ? getWidth() - commandVisibleWidth : 0, 0, commandVisibleWidth, getHeight());
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
