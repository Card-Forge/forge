package forge.screens.match;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;
import com.google.common.collect.Maps;

import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.GuiBase;
import forge.animation.AbilityEffect;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.interfaces.IGameController;
import forge.menu.FDropDown;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuBar;
import forge.menu.FMenuItem;
import forge.menu.FMenuTab;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.screens.match.views.VDevMenu;
import forge.screens.match.views.VGameMenu;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VManaPool;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPlayerPanel.InfoTab;
import forge.screens.match.views.VPlayers;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FCardPanel;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FScrollPane;
import forge.util.Callback;

public class MatchScreen extends FScreen {
    public static FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);

    private final Map<PlayerView, VPlayerPanel> playerPanels = Maps.newHashMap();
    private List<VPlayerPanel> playerPanelsList;
    private final VGameMenu gameMenu;
    private final VPlayers players;
    private final VLog log;
    private final VStack stack;
    private final VDevMenu devMenu;
    private final FieldScroller scroller;
    private final VPrompt bottomPlayerPrompt, topPlayerPrompt;
    private VPlayerPanel bottomPlayerPanel, topPlayerPanel;
    private AbilityEffect activeEffect;

    public MatchScreen(List<VPlayerPanel> playerPanels0) {
        super(new FMenuBar());

        scroller = add(new FieldScroller());

        for (VPlayerPanel playerPanel : playerPanels0) {
            playerPanels.put(playerPanel.getPlayer(), scroller.add(playerPanel));
            playerPanel.setFlipped(true);
        }
        bottomPlayerPanel = playerPanels0.get(0);
        bottomPlayerPanel.setFlipped(false);
        topPlayerPanel = playerPanels0.get(1);
        playerPanelsList = playerPanels0;
        //reorder list so bottom player is at the end of the list ensuring top to bottom turn order
        playerPanelsList.remove(bottomPlayerPanel);
        playerPanelsList.add(bottomPlayerPanel);


        bottomPlayerPrompt = add(new VPrompt("", "",
                new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        getGameController().selectButtonOk();
                    }
                },
                new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        getGameController().selectButtonCancel();
                    }
                }));

        if (MatchController.instance.getLocalPlayerCount() <= 1 || MatchController.instance.hotSeatMode()) {
            topPlayerPrompt = null;
        }
        else { //show top prompt if multiple human players and not playing in Hot Seat mode
            topPlayerPrompt = add(new VPrompt("", "",
                    new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            getGameController().selectButtonOk();
                        }
                    },
                    new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            getGameController().selectButtonCancel();
                        }
                    }));
            topPlayerPrompt.setRotate180(true);
            topPlayerPanel.setRotate180(true);
            getHeader().setRotate90(true);
        }

        gameMenu = new VGameMenu();
        gameMenu.setDropDownContainer(this);
        players = new VPlayers();
        players.setDropDownContainer(this);
        log = new VLog();
        log.setDropDownContainer(this);
        devMenu = new VDevMenu();
        devMenu.setDropDownContainer(this);
        stack = new VStack();
        stack.setDropDownContainer(this);

        FMenuBar menuBar = (FMenuBar)getHeader();
        if (topPlayerPrompt == null) {
            menuBar.addTab("Game", gameMenu);
            menuBar.addTab("Players (" + playerPanels.size() + ")", players);
            menuBar.addTab("Log", log);
            menuBar.addTab("Dev", devMenu);
            menuBar.addTab("Stack (0)", stack);
        }
        else {
            menuBar.addTab("\u2022 \u2022 \u2022", new PlayerSpecificMenu(true));
            stack.setRotate90(true);
            menuBar.addTab("Stack (0)", stack);
            menuBar.addTab("\u2022 \u2022 \u2022", new PlayerSpecificMenu(false));

            //create fake menu tabs for other drop downs so they can be positioned as needed
            gameMenu.setMenuTab(new HiddenMenuTab(gameMenu));
            players.setMenuTab(new HiddenMenuTab(players));
            log.setMenuTab(new HiddenMenuTab(log));
            devMenu.setMenuTab(new HiddenMenuTab(devMenu));
        }
    }

    private boolean is4Player(){
        return playerPanels.keySet().size() == 4;
    }

    private boolean is3Player(){
        return playerPanels.keySet().size() == 3;
    }

    private IGameController getGameController() {
        return MatchController.instance.getGameController();
    }

    private class HiddenMenuTab extends FMenuTab {
        private HiddenMenuTab(FDropDown dropDown0) {
            super(null, null, dropDown0, -1);
            setVisible(false);
        }
        @Override
        public void setText(String text0) {
            //avoid trying to set text for this tab
        }
    }

    private class PlayerSpecificMenu extends FDropDownMenu {
        private PlayerSpecificMenu(boolean forTopPlayer) {
            setRotate180(forTopPlayer);
        }

        @Override
        protected void updateSizeAndPosition() {
            Rectangle menuTabPos = getMenuTab().screenPos;
            FScreen screen = Forge.getCurrentScreen();
            float maxWidth = screen.getWidth() - menuTabPos.width;
            float maxHeight = screen.getHeight() / 2;

            paneSize = updateAndGetPaneSize(maxWidth, maxHeight);

            //round width and height so borders appear properly
            paneSize = new ScrollBounds(Math.round(paneSize.getWidth()), Math.round(paneSize.getHeight()));

            float x = maxWidth - paneSize.getWidth();
            float y = getRotate180() ? menuTabPos.y + FMenuTab.PADDING : menuTabPos.y + menuTabPos.height - paneSize.getHeight() - FMenuTab.PADDING + 1;
            setBounds(Math.round(x), Math.round(y), paneSize.getWidth(), paneSize.getHeight());
        }

        private class MenuItem extends FMenuItem {
            private MenuItem(String text0, final FDropDown dropDown) {
                super(text0, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        dropDown.setRotate180(PlayerSpecificMenu.this.getRotate180());
                        Rectangle menuScreenPos = PlayerSpecificMenu.this.screenPos;
                        if (dropDown.getRotate180()) {
                            dropDown.getMenuTab().screenPos.setPosition(menuScreenPos.x + menuScreenPos.width, menuScreenPos.y);
                        }
                        else {
                            dropDown.getMenuTab().screenPos.setPosition(menuScreenPos.x + menuScreenPos.width, menuScreenPos.y + menuScreenPos.height);
                        }
                        dropDown.show();
                    }
                });
            }
        }

        @Override
        protected void buildMenu() {
            if (isTopHumanPlayerActive() == getRotate180()) {
                addItem(new MenuItem("Game", gameMenu));
                addItem(new MenuItem("Players (" + playerPanels.size() + ")", players));
                addItem(new MenuItem("Log", log));
                if (ForgePreferences.DEV_MODE) {
                    addItem(new MenuItem("Dev", devMenu));
                }
            }
            else { //TODO: Support using menu when player doesn't have priority
                FMenuItem item = new FMenuItem("Must wait for priority...", null);
                item.setEnabled(false);
                addItem(item);
            }
        }
    }

    @Override
    public void onActivate() {
        //update dev menu visibility here so returning from Settings screen allows update
        if (topPlayerPrompt == null) {
            devMenu.getMenuTab().setVisible(ForgePreferences.DEV_MODE);
        }
    }

    public boolean isTopHumanPlayerActive() {
        return topPlayerPrompt != null && topPlayerPanel.getPlayer() == MatchController.instance.getCurrentPlayer();
    }

    public VPrompt getActivePrompt() {
        if (isTopHumanPlayerActive()) {
            return topPlayerPrompt;
        }
        return bottomPlayerPrompt;
    }

    public VPrompt getPrompt(PlayerView playerView) {
        if (topPlayerPrompt == null || bottomPlayerPanel.getPlayer() == playerView) {
            return bottomPlayerPrompt;
        }
        return topPlayerPrompt;
    }

    public VLog getLog() {
        return log;
    }

    public VStack getStack() {
        return stack;
    }

    public VPlayerPanel getTopPlayerPanel() {
        return topPlayerPanel;
    }

    public VPlayerPanel getBottomPlayerPanel() {
        return bottomPlayerPanel;
    }

    public Map<PlayerView, VPlayerPanel> getPlayerPanels() {
        return playerPanels;
    }

    public List<VPlayerPanel> getPlayerPanelsList() {
        return playerPanelsList;
    }

    @Override
    public void onClose(Callback<Boolean> canCloseCallback) {
        MatchController.writeMatchPreferences();
        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS);
        super.onClose(canCloseCallback);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float scrollerWidth = width;
        if (topPlayerPrompt != null) {
            topPlayerPrompt.setBounds(0, 0, width, VPrompt.HEIGHT);
            float menuBarWidth = getHeader().getHeight();
            float menuBarHeight = height - 2 * VPrompt.HEIGHT;
            getHeader().setBounds(width - menuBarHeight, height - VPrompt.HEIGHT, menuBarHeight, menuBarWidth); //adjust position prior to rotate transform
            startY = VPrompt.HEIGHT;
            scrollerWidth -= menuBarWidth;
        }
        scroller.setBounds(0, startY, scrollerWidth, height - VPrompt.HEIGHT - startY);
        bottomPlayerPrompt.setBounds(0, height - VPrompt.HEIGHT, width, VPrompt.HEIGHT);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    @Override
    public Rectangle getDropDownBoundary() {
        if (topPlayerPrompt == null) {
            return new Rectangle(0, 0, getWidth(), getHeight() - VPrompt.HEIGHT); //prevent covering prompt
        }
        return new Rectangle(0, VPrompt.HEIGHT, scroller.getWidth(), getHeight() - 2 * VPrompt.HEIGHT);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        final GameView game = MatchController.instance.getGameView();
        if (game == null) { return; }

        //draw arrows for paired cards
        Set<CardView> pairedCards = new HashSet<CardView>();
        for (VPlayerPanel playerPanel : playerPanels.values()) {
            for (CardView card : playerPanel.getField().getRow1().getOrderedCards()) {
                if (pairedCards.contains(card)) { continue; } //prevent arrows going both ways

                CardView paired = card.getPairedWith();
                if (paired != null) {
                    TargetingOverlay.drawArrow(g, card, paired);
                }
            }
        }

        //draw arrows for combat
        final CombatView combat = game.getCombat();
        if (combat != null) {
            for (final CardView attacker : combat.getAttackers()) {
                //connect each attacker with planeswalker it's attacking if applicable
                final GameEntityView defender = combat.getDefender(attacker);
                if (defender instanceof CardView) {
                    TargetingOverlay.drawArrow(g, attacker, (CardView) defender);
                }
                final Iterable<CardView> blockers = combat.getBlockers(attacker);
                if (blockers != null) {
                    //connect each blocker with the attacker it's blocking
                    for (final CardView blocker : blockers) {
                        TargetingOverlay.drawArrow(g, blocker, attacker);
                    }
                }
                final Iterable<CardView> plannedBlockers = combat.getPlannedBlockers(attacker);
                if (plannedBlockers != null) {
                    //connect each planned blocker with the attacker it's blocking
                    for (final CardView blocker : plannedBlockers) {
                        TargetingOverlay.drawArrow(g, blocker, attacker);
                    }
                }
            }
        }

        if (activeEffect != null) {
            activeEffect.draw(g, 10, 10, 100, 100);
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
        case Keys.SPACE:
            if (getActivePrompt().getBtnOk().trigger()) { //trigger OK on Enter or Space
                return true;
            }
            return getActivePrompt().getBtnCancel().trigger(); //trigger Cancel if can't trigger OK
        case Keys.ESCAPE:
            if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ALLOW_ESC_TO_END_TURN)) {
                if (getActivePrompt().getBtnCancel().getText().equals("End Turn")) {
                    return false;
                }
            }
            return getActivePrompt().getBtnCancel().trigger(); //otherwise trigger Cancel
        case Keys.BACK:
            return true; //suppress Back button so it's not bumped when trying to press OK or Cancel buttons
        case Keys.A: //alpha strike on Ctrl+A on Android, A when running on desktop
            if (KeyInputAdapter.isCtrlKeyDown() || GuiBase.getInterface().isRunningOnDesktop()) {
                getGameController().alphaStrike();
                return true;
            }
            break;
        case Keys.E: //end turn on Ctrl+E on Android, E when running on desktop
            if (KeyInputAdapter.isCtrlKeyDown() || GuiBase.getInterface().isRunningOnDesktop()) {
                getGameController().passPriorityUntilEndOfTurn();
                return true;
            }
            break;
        case Keys.Q: //concede game on Ctrl+Q
            if (KeyInputAdapter.isCtrlKeyDown()) {
                MatchController.instance.concede();
                return true;
            }
            break;
        case Keys.Z: //undo on Ctrl+Z
            if (KeyInputAdapter.isCtrlKeyDown()) {
                getGameController().undoLastAction();
                return true;
            }
            break;
        }
        return super.keyDown(keyCode);
    }

    @Override
    public void showMenu() {
        //don't show menu from this screen since it's too easy to bump the menu button when trying to press OK or Cancel
    }

    public boolean stopAtPhase(final PlayerView turn, final PhaseType phase) {
        final PhaseLabel label = getPlayerPanel(turn).getPhaseIndicator().getLabel(phase);
        return label == null || label.getStopAtPhase();
    }

    public void resetAllPhaseButtons() {
        for (final VPlayerPanel panel : getPlayerPanels().values()) {
            panel.getPhaseIndicator().resetPhaseButtons();
        }
    }

    public VPlayerPanel getPlayerPanel(final PlayerView playerView) {
        return getPlayerPanels().get(playerView);
    }

    public void highlightCard(final CardView c) {
        for (VPlayerPanel playerPanel : getPlayerPanels().values()) {
            for (FCardPanel p : playerPanel.getField().getCardPanels()) {
                if (p.getCard().equals(c)) {
                    p.setHighlighted(true);
                    return;
                }
            }
        }
    }

    public void clearCardHighlights() {
        for (VPlayerPanel playerPanel : getPlayerPanels().values()) {
            for (FCardPanel p : playerPanel.getField().getCardPanels()) {
                p.setHighlighted(false);
            }
        }
    }

    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        for (final PlayerZoneUpdate update : zonesToUpdate) {
            final PlayerView owner = update.getPlayer();
            final VPlayerPanel panel = getPlayerPanel(owner);
            for (final ZoneType zone : update.getZones()) {
                panel.updateZone(zone);
            }
        }
    }

    public Iterable<PlayerZoneUpdate> tempShowZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
	// pfps needs to actually do something
    	return zonesToUpdate; // pfps should return only those zones newly shown
    }

    public void hideZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
	// pfps needs to actually do something
    }

    public void updateSingleCard(final CardView card) {
        final CardAreaPanel pnl = CardAreaPanel.get(card);
        if (pnl == null) { return; }
        final ZoneType zone = card.getZone();
        if (zone != null && zone == ZoneType.Battlefield) {
            pnl.updateCard(card);
        }
        else { //ensure card not on battlefield is reset such that it no longer thinks it's on the battlefield
            pnl.setTapped(false);
            pnl.getAttachedPanels().clear();
            pnl.setAttachedToPanel(null);
            pnl.setPrevPanelInStack(null);
            pnl.setNextPanelInStack(null);
        }
    }

    private class FieldScroller extends FScrollPane {
        private float extraHeight = 0;

        @Override
        public void drawBackground(Graphics g) {
            super.drawBackground(g);

            if (FModel.getPreferences().getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE)) {
                float midField = topPlayerPanel.getBottom();
                float x = topPlayerPanel.getField().getLeft();
                float y = midField - topPlayerPanel.getField().getHeight();
                float w = getWidth() - x;

                if(FModel.getPreferences().getPrefBoolean(FPref.UI_DYNAMIC_PLANECHASE_BG)
                        && hasActivePlane())
                    setPlanarBG(g, getPlaneName(), x, y, w, midField);
                else {
                    float bgFullWidth;
                    float scaledbgHeight;
                    float bgHeight = midField + bottomPlayerPanel.getField().getHeight() - y;
                    bgFullWidth = bgHeight * FSkinTexture.BG_MATCH.getWidth() / FSkinTexture.BG_MATCH.getHeight();
                    if (bgFullWidth < w) {
                        scaledbgHeight = w * (bgHeight / bgFullWidth);
                        bgFullWidth = w;
                        bgHeight = scaledbgHeight;
                    }
                    g.drawImage(FSkinTexture.BG_MATCH, x + (w - bgFullWidth) / 2, y, bgFullWidth, bgHeight);
                }
            }
        }

        @Override
        public void drawOverlay(Graphics g) {
            float midField;
            float x = 0;
            float y;
            float w = getWidth();

            //field separator lines
            if (!Forge.isLandscapeMode()) {
                for (VPlayerPanel playerPanel: playerPanelsList){
                    midField = playerPanel.getTop();
                    y = midField - playerPanel.getField().getHeight();
                    if (playerPanel.getSelectedTab() == null) {
                        y++;
                    }
                    g.drawLine(1, BORDER_COLOR, x, y, w, y);
                }
            }

            for (VPlayerPanel playerPanel: playerPanelsList){
                midField = playerPanel.getTop();
                y = midField - 0.5f;
                g.drawLine(1, BORDER_COLOR, x, y, w, y);
            }

            if (!Forge.isLandscapeMode()) {
                y = bottomPlayerPanel.getTop() + bottomPlayerPanel.getField().getHeight();
                g.drawLine(1, BORDER_COLOR, x, y, w, y);
            }
        }

        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float totalHeight = visibleHeight + extraHeight;
            float avatarHeight = VAvatar.HEIGHT;
            if (is4Player() || is3Player()){
                avatarHeight *= 0.5f;
            }
            float playerCount = getPlayerPanels().keySet().size();

            if (Forge.isLandscapeMode() && playerCount == 2) {
                // Ensure that players have equal player panel heights in two player Forge in Landscape mode
                float topPlayerPanelHeight = totalHeight / 2;
                float bottomPlayerPanelHeight = topPlayerPanelHeight;
                topPlayerPanel.setBounds(0, 0, visibleWidth, topPlayerPanelHeight);
                bottomPlayerPanel.setBounds(0, totalHeight - bottomPlayerPanelHeight, visibleWidth, bottomPlayerPanelHeight);
            } else {
                // Determine player panel heights based on visibility of zone displays
                float cardRowsHeight = totalHeight - playerCount * avatarHeight;
                float totalCardRows = 0;
                for (VPlayerPanel playerPanel : playerPanelsList) {
                    if (playerPanel.getSelectedTab() != null) {
                        totalCardRows += 1;
                    }
                    totalCardRows += 2;
                }
                float y = 0;
                for (VPlayerPanel playerPanel : playerPanelsList) {
                    float panelHeight;
                    if (playerPanel.getSelectedTab() != null) {
                        panelHeight = cardRowsHeight * 3f / totalCardRows;
                    } else {
                        panelHeight = cardRowsHeight * 2f / totalCardRows;
                    }
                    panelHeight += avatarHeight;
                    playerPanel.setBounds(0, y, visibleWidth, panelHeight);
                    y += panelHeight;
                }
            }

            return new ScrollBounds(visibleWidth, totalHeight);
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            //adjust position for current scroll positions
            float staticHeight = 2 * VAvatar.HEIGHT; //take out avatar rows that don't scale
            float oldScrollHeight = getScrollHeight() - staticHeight;
            float oldScrollTop = getScrollTop();
            y += oldScrollTop - VAvatar.HEIGHT;

            //build map of all horizontal scroll panes and their current scrollWidths and adjusted X values
            Map<FScrollPane, Pair<Float, Float>> horzScrollPanes = new HashMap<FScrollPane, Pair<Float, Float>>();
            backupHorzScrollPanes(topPlayerPanel, x, horzScrollPanes);
            backupHorzScrollPanes(bottomPlayerPanel, x, horzScrollPanes);

            float zoom = oldScrollHeight / (getHeight() - staticHeight);
            extraHeight += amount * zoom; //scale amount by current zoom
            if (extraHeight < 0) {
                extraHeight = 0;
            }
            revalidate(); //apply change in height to all scroll panes

            //adjust scroll top to keep y position the same
            float newScrollHeight = getScrollHeight() - staticHeight;
            float ratio = newScrollHeight / oldScrollHeight;
            float yAfter = y * ratio;
            setScrollTop(oldScrollTop + yAfter - y);

            //adjust scroll left of all horizontal scroll panes to keep x position the same
            float startX = x;
            for (Entry<FScrollPane, Pair<Float, Float>> entry : horzScrollPanes.entrySet()) {
                FScrollPane horzScrollPane = entry.getKey();
                float oldScrollLeft = entry.getValue().getLeft();
                x = startX + oldScrollLeft;
                float xAfter = x * ratio;
                horzScrollPane.setScrollLeft(oldScrollLeft + xAfter - x);
            }

            return true;
        }

        private void backupHorzScrollPanes(VPlayerPanel playerPanel, float x, Map<FScrollPane, Pair<Float, Float>> horzScrollPanes) {
            backupHorzScrollPane(playerPanel.getField().getRow1(), x, horzScrollPanes);
            backupHorzScrollPane(playerPanel.getField().getRow2(), x, horzScrollPanes);
            for (InfoTab tab : playerPanel.getTabs()) {
                if (tab.getDisplayArea() instanceof VManaPool) {
                    continue; //don't include Mana pool in this
                }
                backupHorzScrollPane(tab.getDisplayArea(), x, horzScrollPanes);
            }
            backupHorzScrollPane(playerPanel.getCommandZone(), x, horzScrollPanes);
        }
        private void backupHorzScrollPane(FScrollPane scrollPane, float x, Map<FScrollPane, Pair<Float, Float>> horzScrollPanes) {
            horzScrollPanes.put(scrollPane, Pair.of(scrollPane.getScrollLeft(), scrollPane.getScrollWidth()));
        }
        private String getPlaneName(){ return MatchController.instance.getGameView().getPlanarPlayer().getCurrentPlaneName(); }
        private boolean hasActivePlane(){
            if(MatchController.instance.getGameView() != null)
                if(MatchController.instance.getGameView().getPlanarPlayer() != null) {
                    return MatchController.instance.getGameView().getPlanarPlayer().getCurrentPlaneName() != "";
            }
            return false;
        }
        private void setPlanarBG(Graphics g, String planeName, float x, float y, float w, float midField ){
            float planeFullWidth;
            float scaledPlaneHeight;
            float planeHeight = midField + bottomPlayerPanel.getField().getHeight() - y;
            switch (planeName) {
                case "Academy at Tolaria West": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE1.getWidth() / FSkinTexture.BG_PLANE1.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE1, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Agyrem": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE2.getWidth() / FSkinTexture.BG_PLANE2.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE2, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Akoum": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE3.getWidth() / FSkinTexture.BG_PLANE3.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE3, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Aretopolis": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE4.getWidth() / FSkinTexture.BG_PLANE4.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE4, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Astral Arena": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE5.getWidth() / FSkinTexture.BG_PLANE5.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE5, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Bant": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE6.getWidth() / FSkinTexture.BG_PLANE6.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE6, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Bloodhill Bastion": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE7.getWidth() / FSkinTexture.BG_PLANE7.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE7, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Cliffside Market": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE8.getWidth() / FSkinTexture.BG_PLANE8.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE8, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Edge of Malacol": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE9.getWidth() / FSkinTexture.BG_PLANE9.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE9, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Eloren Wilds": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE10.getWidth() / FSkinTexture.BG_PLANE10.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE10, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Feeding Grounds": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE11.getWidth() / FSkinTexture.BG_PLANE11.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE11, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Fields of Summer": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE12.getWidth() / FSkinTexture.BG_PLANE12.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE12, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Furnace Layer": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE13.getWidth() / FSkinTexture.BG_PLANE13.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE13, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Gavony": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE14.getWidth() / FSkinTexture.BG_PLANE14.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE14, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Glen Elendra": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE15.getWidth() / FSkinTexture.BG_PLANE15.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE15, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Glimmervoid Basin": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE16.getWidth() / FSkinTexture.BG_PLANE16.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE16, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Goldmeadow": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE17.getWidth() / FSkinTexture.BG_PLANE17.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE17, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Grand Ossuary": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE18.getWidth() / FSkinTexture.BG_PLANE18.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE18, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Grixis": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE19.getWidth() / FSkinTexture.BG_PLANE19.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE19, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Grove of the Dreampods": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE20.getWidth() / FSkinTexture.BG_PLANE20.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE20, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Hedron Fields of Agadeem": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE21.getWidth() / FSkinTexture.BG_PLANE21.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE21, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Immersturm": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE22.getWidth() / FSkinTexture.BG_PLANE22.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE22, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Isle of Vesuva": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE23.getWidth() / FSkinTexture.BG_PLANE23.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE23, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Izzet Steam Maze": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE24.getWidth() / FSkinTexture.BG_PLANE24.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE24, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Jund": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE25.getWidth() / FSkinTexture.BG_PLANE25.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE25, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Kessig": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE26.getWidth() / FSkinTexture.BG_PLANE26.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE26, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Kharasha Foothills": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE27.getWidth() / FSkinTexture.BG_PLANE27.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE27, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Kilnspire District": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE28.getWidth() / FSkinTexture.BG_PLANE28.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE28, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Krosa": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE29.getWidth() / FSkinTexture.BG_PLANE29.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE29, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Lair of the Ashen Idol": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE30.getWidth() / FSkinTexture.BG_PLANE30.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE30, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Lethe Lake": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE31.getWidth() / FSkinTexture.BG_PLANE31.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE31, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Llanowar": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE32.getWidth() / FSkinTexture.BG_PLANE32.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE32, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Minamo": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE33.getWidth() / FSkinTexture.BG_PLANE33.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE33, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Mount Keralia": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE34.getWidth() / FSkinTexture.BG_PLANE34.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE34, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Murasa": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE35.getWidth() / FSkinTexture.BG_PLANE35.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE35, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Naar Isle": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE36.getWidth() / FSkinTexture.BG_PLANE36.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE36, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Naya": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE37.getWidth() / FSkinTexture.BG_PLANE37.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE37, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Nephalia": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE38.getWidth() / FSkinTexture.BG_PLANE38.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE38, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Norn's Dominion": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE39.getWidth() / FSkinTexture.BG_PLANE39.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE39, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Onakke Catacomb": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE40.getWidth() / FSkinTexture.BG_PLANE40.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE40, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Orochi Colony": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE41.getWidth() / FSkinTexture.BG_PLANE41.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE41, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Orzhova": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE42.getWidth() / FSkinTexture.BG_PLANE42.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE42, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Otaria": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE43.getWidth() / FSkinTexture.BG_PLANE43.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE43, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Panopticon": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE44.getWidth() / FSkinTexture.BG_PLANE44.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE44, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Pools of Becoming": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE45.getWidth() / FSkinTexture.BG_PLANE45.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE45, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Prahv": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE46.getWidth() / FSkinTexture.BG_PLANE46.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE46, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Quicksilver Sea": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE47.getWidth() / FSkinTexture.BG_PLANE47.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE47, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Raven's Run": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE48.getWidth() / FSkinTexture.BG_PLANE48.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE48, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Sanctum of Serra": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE49.getWidth() / FSkinTexture.BG_PLANE49.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE49, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Sea of Sand": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE50.getWidth() / FSkinTexture.BG_PLANE50.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE50, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Selesnya Loft Gardens": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE51.getWidth() / FSkinTexture.BG_PLANE51.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE51, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Shiv": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE52.getWidth() / FSkinTexture.BG_PLANE52.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE52, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Skybreen": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE53.getWidth() / FSkinTexture.BG_PLANE53.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE53, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Sokenzan": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE54.getWidth() / FSkinTexture.BG_PLANE54.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE54, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Stairs to Infinity": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE55.getWidth() / FSkinTexture.BG_PLANE55.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE55, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Stensia": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE56.getWidth() / FSkinTexture.BG_PLANE56.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE56, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Stronghold Furnace": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE57.getWidth() / FSkinTexture.BG_PLANE57.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE57, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Takenuma": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE58.getWidth() / FSkinTexture.BG_PLANE58.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE58, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Tazeem": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE59.getWidth() / FSkinTexture.BG_PLANE59.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE59, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Aether Flues": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE60.getWidth() / FSkinTexture.BG_PLANE60.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE60, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Dark Barony": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE61.getWidth() / FSkinTexture.BG_PLANE61.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE61, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Eon Fog": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE62.getWidth() / FSkinTexture.BG_PLANE62.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE62, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Fourth Sphere": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE63.getWidth() / FSkinTexture.BG_PLANE63.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE63, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Great Forest": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE64.getWidth() / FSkinTexture.BG_PLANE64.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE64, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Hippodrome": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE65.getWidth() / FSkinTexture.BG_PLANE65.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE65, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Maelstrom": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE66.getWidth() / FSkinTexture.BG_PLANE66.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE66, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "The Zephyr Maze": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE67.getWidth() / FSkinTexture.BG_PLANE67.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE67, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Trail of the Mage-Rings": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE68.getWidth() / FSkinTexture.BG_PLANE68.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE68, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Truga Jungle": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE69.getWidth() / FSkinTexture.BG_PLANE69.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE69, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Turri Island": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE70.getWidth() / FSkinTexture.BG_PLANE70.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE70, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Undercity Reaches": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE71.getWidth() / FSkinTexture.BG_PLANE71.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE71, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Velis Vel": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE72.getWidth() / FSkinTexture.BG_PLANE72.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE72, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Windriddle Palaces": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE73.getWidth() / FSkinTexture.BG_PLANE73.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE73, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Tember City": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE74.getWidth() / FSkinTexture.BG_PLANE74.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE74, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Celestine Reef": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE75.getWidth() / FSkinTexture.BG_PLANE75.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE75, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Horizon Boughs": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE76.getWidth() / FSkinTexture.BG_PLANE76.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE76, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Mirrored Depths": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE77.getWidth() / FSkinTexture.BG_PLANE77.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE77, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;
                case "Talon Gates": {
                    planeFullWidth = planeHeight * FSkinTexture.BG_PLANE78.getWidth() / FSkinTexture.BG_PLANE78.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_PLANE78, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight, true);
                }
                break;

                default: {
                    planeFullWidth = planeHeight * FSkinTexture.BG_MATCH.getWidth() / FSkinTexture.BG_MATCH.getHeight();
                    if (planeFullWidth < w) {
                        scaledPlaneHeight = w * (planeHeight / planeFullWidth);
                        planeFullWidth = w;
                        planeHeight = scaledPlaneHeight;
                    }
                    g.drawImage(FSkinTexture.BG_MATCH, x + (w - planeFullWidth) / 2, y, planeFullWidth, planeHeight);
                }
            }
        }
    }
}
