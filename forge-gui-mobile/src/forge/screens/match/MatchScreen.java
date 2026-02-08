package forge.screens.match;

import static forge.Forge.getLocalizer;

import forge.toolbox.FOptionPane;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.scene.GameScene;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.game.spellability.StackItemView;
import forge.gui.interfaces.IGuiGame;
import forge.screens.match.views.VField;
import forge.screens.match.views.VReveal;
import forge.toolbox.FDisplayObject;
import forge.util.CardRendererUtils;
import forge.util.Utils;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.Pair;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;
import com.google.common.collect.Maps;

import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.animation.AbilityEffect;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinTexture;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FDropDown;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuBar;
import forge.menu.FMenuItem;
import forge.menu.FMenuTab;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.screens.FScreen;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.screens.match.views.VDevMenu;
import forge.screens.match.views.VGameMenu;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPlayerPanel.InfoTab;
import forge.screens.match.views.VPlayers;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.screens.match.winlose.ViewWinLose;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FCardPanel;
import forge.toolbox.FScrollPane;

public class MatchScreen extends FScreen {
    public static FSkinColor getBorderColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_BORDERS);
        return FSkinColor.get(Colors.CLR_BORDERS);
    }

    private static final Map<PlayerView, VPlayerPanel> playerPanels = Maps.newHashMap();
    private List<VPlayerPanel> playerPanelsList;
    private final VGameMenu gameMenu;
    private final VPlayers players;
    private final VReveal revealed;
    private final VLog log;
    private final VStack stack;
    private final VDevMenu devMenu;
    private final FieldScroller scroller;
    private final VPrompt bottomPlayerPrompt, topPlayerPrompt;
    private VPlayerPanel bottomPlayerPanel, topPlayerPanel;
    private AbilityEffect activeEffect;
    private BGAnimation bgAnimation;
    private ViewWinLose viewWinLose = null;
    private static List<FDisplayObject> potentialListener;
    private int selectedPlayer;


    private final Map<Integer, Vector2> endpoints;
    private final Set<CardView> cardsonBattlefield;
    private final Set<PlayerView> playerViewSet;

    public MatchScreen(List<VPlayerPanel> playerPanels0) {
        super(new FMenuBar());

        scroller = add(new FieldScroller());

        int humanCount = 0;

        playerPanels.clear();

        for (VPlayerPanel playerPanel : playerPanels0) {
            playerPanels.put(playerPanel.getPlayer(), scroller.add(playerPanel));
            playerPanel.setFlipped(true);
            if (!playerPanel.getPlayer().isAI())
                humanCount++;
        }
        bottomPlayerPanel = playerPanels0.get(0);
        bottomPlayerPanel.setFlipped(false);
        topPlayerPanel = playerPanels0.get(1);
        playerPanelsList = playerPanels0;
        //reorder list so bottom player is at the end of the list ensuring top to bottom turn order
        playerPanelsList.remove(bottomPlayerPanel);
        playerPanelsList.add(bottomPlayerPanel);
        selectedPlayer = playerPanelsList.size() - 1;

        bottomPlayerPrompt = add(new VPrompt("", "",
                e -> getGameController().selectButtonOk(),
                e -> getGameController().selectButtonCancel()));

        if (humanCount < 2 || MatchController.instance.hotSeatMode() || GuiBase.isNetworkplay(MatchController.instance))
            topPlayerPrompt = null;
        else {
            //show top prompt if multiple human players and not playing in Hot Seat mode and not in network play
            topPlayerPrompt = add(new VPrompt("", "",
                    e -> getGameController().selectButtonOk(),
                    e -> getGameController().selectButtonCancel()));
            topPlayerPrompt.setRotate180(true);
            topPlayerPanel.setRotate180(true);
            getHeader().setRotate90(true);
        }

        gameMenu = new VGameMenu();
        gameMenu.setDropDownContainer(this);
        players = new VPlayers();
        players.setDropDownContainer(this);
        revealed = new VReveal();
        revealed.setDropDownContainer(this);
        log = new VLog(() -> MatchController.instance.getGameView().getGameLog());
        log.setDropDownContainer(this);
        devMenu = new VDevMenu();
        devMenu.setDropDownContainer(this);
        stack = new VStack();
        stack.setDropDownContainer(this);

        FMenuBar menuBar = (FMenuBar) getHeader();
        if (topPlayerPrompt == null) {
            menuBar.addTab("", revealed, true);
            menuBar.addTab(Forge.getLocalizer().getMessage("lblGame"), gameMenu);
            menuBar.addTab(Forge.getLocalizer().getMessage("lblPlayers") + " (" + playerPanels.size() + ")", players);
            menuBar.addTab(Forge.getLocalizer().getMessage("lblLog"), log);
            menuBar.addTab(Forge.getLocalizer().getMessage("lblDev"), devMenu);
            menuBar.addTab(Forge.getLocalizer().getMessage("lblStack") + " (0)", stack);
        } else {
            menuBar.addTab("\u2022 \u2022 \u2022", new PlayerSpecificMenu(true));
            stack.setRotate90(true);
            menuBar.addTab(Forge.getLocalizer().getMessage("lblStack") + " (0)", stack);
            menuBar.addTab("\u2022 \u2022 \u2022", new PlayerSpecificMenu(false));

            //create fake menu tabs for other drop downs so they can be positioned as needed
            gameMenu.setMenuTab(new HiddenMenuTab(gameMenu));
            players.setMenuTab(new HiddenMenuTab(players));
            log.setMenuTab(new HiddenMenuTab(log));
            devMenu.setMenuTab(new HiddenMenuTab(devMenu));
        }
        endpoints = new HashMap<>();
        cardsonBattlefield  = new HashSet<>();
        playerViewSet = new HashSet<>();
    }

    private boolean is4Player() {
        return playerPanels.keySet().size() == 4;
    }

    private boolean is3Player() {
        return playerPanels.keySet().size() == 3;
    }

    private IGameController getGameController() {
        return MatchController.instance.getGameController();
    }

    private class HiddenMenuTab extends FMenuTab {
        private HiddenMenuTab(FDropDown dropDown0) {
            super(null, null, dropDown0, -1, false);
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
                super(text0, e -> {
                    dropDown.setRotate180(PlayerSpecificMenu.this.getRotate180());
                    Rectangle menuScreenPos = PlayerSpecificMenu.this.screenPos;
                    if (dropDown.getRotate180()) {
                        dropDown.getMenuTab().screenPos.setPosition(menuScreenPos.x + menuScreenPos.width, menuScreenPos.y);
                    } else {
                        dropDown.getMenuTab().screenPos.setPosition(menuScreenPos.x + menuScreenPos.width, menuScreenPos.y + menuScreenPos.height);
                    }
                    dropDown.show();
                });
            }
        }

        @Override
        protected void buildMenu() {

            if (isTopHumanPlayerActive() == getRotate180()) {
                addItem(new MenuItem(Forge.getLocalizer().getMessage("lblGame"), gameMenu));
                addItem(new MenuItem(Forge.getLocalizer().getMessage("lblPlayers") + " (" + playerPanels.size() + ")", players));
                addItem(new MenuItem(Forge.getLocalizer().getMessage("lblLog"), log));
                if (ForgePreferences.DEV_MODE) {
                    addItem(new MenuItem(Forge.getLocalizer().getMessage("lblDev"), devMenu));
                }
            } else { //TODO: Support using menu when player doesn't have priority
                FMenuItem item = new FMenuItem(Forge.getLocalizer().getMessage("lblMustWaitPriority"), null);
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

    public void setViewWinLose(ViewWinLose viewWinLose) {
        this.viewWinLose = viewWinLose;
    }

    public ViewWinLose getViewWinLose() {
        return viewWinLose;
    }

    public VPlayerPanel getBottomPlayerPanel() {
        return bottomPlayerPanel;
    }

    public static Map<PlayerView, VPlayerPanel> getPlayerPanels() {
        return playerPanels;
    }

    public List<VPlayerPanel> getPlayerPanelsList() {
        return playerPanelsList;
    }

    @Override
    public void onClose(Consumer<Boolean> canCloseCallback) {
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
        if (game == null) {
            return;
        }

        if (gameMenu != null) {
            if (gameMenu.getChildCount() > 1) {
                if (viewWinLose == null) {
                    gameMenu.getChildAt(0).setEnabled(!game.isMulligan());
                    gameMenu.getChildAt(1).setEnabled(!game.isMulligan());
                    if (!Forge.isMobileAdventureMode) {
                        gameMenu.getChildAt(2).setEnabled(!game.isMulligan());
                        gameMenu.getChildAt(3).setEnabled(false);
                    }
                } else {
                    gameMenu.getChildAt(0).setEnabled(false);
                    gameMenu.getChildAt(1).setEnabled(false);
                    if (!Forge.isMobileAdventureMode) {
                        gameMenu.getChildAt(2).setEnabled(false);
                        gameMenu.getChildAt(3).setEnabled(true);
                    }
                }
            }
        }
        if (devMenu != null) {
            if (devMenu.isVisible()) {
                try {
                    //rollbackphase enable -- todo limit by gametype?
                    devMenu.getChildAt(2).setEnabled(game.getPlayers().size() == 2 && game.getStack().size() == 0 && !GuiBase.isNetworkplay(MatchController.instance) && game.getPhase().isMain() && !game.getPlayerTurn().isAI());
                } catch (Exception e) {/*NPE when the game hasn't started yet and you click dev mode*/}
            }
        }

        if (activeEffect != null) {
            activeEffect.draw(g, 10, 10, 100, 100);
        }

        if (game.getNeedsPhaseRedrawn()) {
            resetAllPhaseButtons();
            if (game.getPlayerTurn() != null && game.getPhase() != null) {
                final PhaseLabel phaseLabel = getPlayerPanel(game.getPlayerTurn()).getPhaseIndicator().getLabel(game.getPhase());
                if (phaseLabel != null) {
                    phaseLabel.setActive(true);
                    game.clearNeedsPhaseRedrawn();
                }
            }
        }
        drawArcs(g);
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_MAGNIFIER) && Forge.magnify && Forge.magnifyToggle) {
            if (Forge.isLandscapeMode() && (!GuiBase.isAndroid() || Forge.hasGamepad()) && !CardZoom.isOpen() && potentialListener != null) {
                for (FDisplayObject object : potentialListener) {
                    if (object != null) {
                        if (object instanceof FCardPanel cardPanel) {
                            try {
                                if (cardPanel.isHovered()) {
                                    CardView cardView = cardPanel.getCard();
                                    VPlayerPanel vPlayerPanel = getPlayerPanel(cardView.getController());
                                    if (vPlayerPanel == null)
                                        vPlayerPanel = getPlayerPanel(cardView.getOwner());
                                    if (vPlayerPanel != null) {
                                        boolean rotate = CardRendererUtils.needsRotation(cardView) && !Forge.magnifyShowDetails;
                                        boolean inBattlefield = ZoneType.Battlefield.equals(cardView.getZone());
                                        float mul = 0.45f;
                                        float div = inBattlefield ? cardPanel.isTapped() ? 2.7f : 2.4f : 1.6f;
                                        float adjX = rotate ? cardPanel.getWidth() / div : 0f;
                                        float adjY = rotate ? cardPanel.getHeight() / 2.2f : 0f;
                                        float cardW = getHeight() * mul;
                                        float cardH = FCardPanel.ASPECT_RATIO * cardW;
                                        float cardX = !inBattlefield ? cardPanel.screenPos.x - (cardW + adjX)
                                                : cardPanel.screenPos.x + (cardPanel.isTapped() ? cardPanel.getWidth()
                                                : cardPanel.getWidth() / 1.4f) + adjX;
                                        if (vPlayerPanel.getSelectedTab() != null && vPlayerPanel.getSelectedTab().isVisible()
                                                && cardX > vPlayerPanel.getSelectedTab().getDisplayArea().getLeft()) {
                                            cardX = cardPanel.screenPos.x - (cardW + adjX);
                                        }
                                        if ((cardX + cardW + adjX) > scroller.getWidth() + scroller.getLeft())
                                            cardX = cardPanel.screenPos.x - (cardW + adjX);
                                        if (vPlayerPanel.getCommandZone() != null
                                                && vPlayerPanel.getCommandZone().isVisible() && cardX > vPlayerPanel.getCommandZone().screenPos.x)
                                            cardX = cardPanel.screenPos.x - (cardW + adjX);
                                        float cardY = (cardPanel.screenPos.y - (cardH - adjY)) + cardPanel.getHeight();
                                        if (vPlayerPanel.getPlayer() == bottomPlayerPanel.getPlayer()) {
                                            cardY = bottomPlayerPrompt.screenPos.y - (cardH - adjY);
                                        } else if (cardY < vPlayerPanel.getField().screenPos.y && vPlayerPanel.getPlayer() != bottomPlayerPanel.getPlayer()) {
                                            cardY = vPlayerPanel.getField().screenPos.y - adjY;
                                            if ((cardY + (cardH - adjY)) > bottomPlayerPrompt.screenPos.y)
                                                cardY = bottomPlayerPrompt.screenPos.y - (cardH - adjY);
                                        }
                                        if (Forge.magnifyShowDetails)
                                            CardImageRenderer.drawDetails(g, cardView, MatchController.instance.getGameView(), false, cardX, cardY, cardW, cardH);
                                        else
                                            CardRenderer.drawCard(g, cardView, cardX, cardY, cardW, cardH, CardRenderer.CardStackPosition.Top, rotate, false, false, true);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (object instanceof VStack.StackInstanceDisplay vstackDisplay) {
                            try {
                                CardView cardView = vstackDisplay.stackInstance.getSourceCard();
                                if (object.isHovered() && cardView != null && getStack().isVisible()) {
                                    float cardW = getHeight() * 0.45f;
                                    float cardH = FCardPanel.ASPECT_RATIO * cardW;
                                    float cardX = object.screenPos.x - cardW - Utils.scale(4);
                                    float cardY = object.screenPos.y - Utils.scale(2);
                                    if (cardY < topPlayerPanel.getField().screenPos.y)
                                        cardY = topPlayerPanel.getField().screenPos.y;
                                    if ((cardY + cardH) > bottomPlayerPrompt.screenPos.y)
                                        cardY = bottomPlayerPrompt.screenPos.y - cardH;
                                    if (Forge.magnifyShowDetails)
                                        CardImageRenderer.drawDetails(g, cardView, MatchController.instance.getGameView(), false, cardX, cardY, cardW, cardH);
                                    else
                                        CardRenderer.drawCard(g, cardView, cardX, cardY, cardW, cardH, CardRenderer.CardStackPosition.Top, false, false, false, true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    void drawArcs(Graphics g) {
        final GameView game = MatchController.instance.getGameView();
        if (game == null)
            return;
        //get all card targeting arrow origins on the battlefield
        endpoints.clear();
        cardsonBattlefield.clear();
        playerViewSet.clear();
        try {
            for (PlayerView p : game.getPlayers()) {
                if (p == null)
                    continue;
                VPlayerPanel playerPanel = getPlayerPanel(p);
                if (playerPanel != null && playerPanelsList.contains(playerPanel)) {
                    playerViewSet.add(p);
                    if (p.getBattlefield() != null) {
                        for (CardView c : p.getBattlefield()) {
                            CardAreaPanel panel = CardAreaPanel.get(c);
                            Vector2 origin = panel.getTargetingArrowOrigin();
                            //outside left bounds
                            if (origin.x < playerPanel.getField().getLeft())
                                continue;
                            //outside right bounds
                            if (origin.x > playerPanel.getField().getRight())
                                continue;
                            endpoints.put(c.getId(), origin);
                            cardsonBattlefield.add(c);
                        }
                    }
                }
            }
            if (endpoints.isEmpty())
                return;
            TargetingOverlay.assembleArrows(g, cardsonBattlefield, endpoints, game.getCombat(), playerViewSet);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean keyDown(int keyCode) {
        // TODO: make the keyboard shortcuts configurable on Mobile
        if (Forge.hasGamepad() && ((FMenuBar) getHeader()).isShowingMenu(false) && (keyCode == Keys.ESCAPE || keyCode == Keys.ENTER))
            return false;
        switch (keyCode) {
            case Keys.DPAD_DOWN:
                if (!((FMenuBar) getHeader()).isShowingMenu(true)) {
                    try {
                        InfoTab selected = selectedPlayerPanel().getSelectedTab();
                        if (selected != null && selected.getDisplayArea().isVisible()) {
                            selectedPlayerPanel().getSelectedTab().getDisplayArea().setNextSelected(2);
                        } else {
                            nullPotentialListener();
                            VField.FieldRow row = selectedPlayerPanel() != getBottomPlayerPanel()
                                    ? selectedPlayerPanel().getField().getRow2()
                                    : selectedPlayerPanel().getField().getRow1();
                            if (selectedPlayerPanel().getSelectedRow() == row) {
                                selectedPlayerPanel().getSelectedRow().unselectCurrent();
                                selectedPlayerPanel().switchRow();
                                selectedPlayerPanel().getSelectedRow().selectCurrent();
                            } else {
                                selectedPlayerPanel().getSelectedRow().selectCurrent();
                            }
                        }
                        revalidate(true);
                    } catch (Exception ignored) {
                    }
                }
                break;
            case Keys.DPAD_RIGHT:
                if (!((FMenuBar) getHeader()).isShowingMenu(true)) {
                    try {
                        InfoTab selected = selectedPlayerPanel().getSelectedTab();
                        if (selected != null && selected.getDisplayArea().isVisible()) {
                            selectedPlayerPanel().getSelectedTab().getDisplayArea().setNextSelected(1);
                        } else {
                            selectedPlayerPanel().getSelectedRow().setNextSelected(1);
                        }
                        revalidate(true);
                    } catch (Exception ignored) {
                    }
                }
                break;
            case Keys.DPAD_UP:
                if (!((FMenuBar) getHeader()).isShowingMenu(true)) {
                    try {
                        InfoTab selected = selectedPlayerPanel().getSelectedTab();
                        if (selected != null && selected.getDisplayArea().isVisible()) {
                            selectedPlayerPanel().getSelectedTab().getDisplayArea().setPreviousSelected(2);
                        } else {
                            nullPotentialListener();
                            VField.FieldRow row = selectedPlayerPanel() != getBottomPlayerPanel()
                                    ? selectedPlayerPanel().getField().getRow1()
                                    : selectedPlayerPanel().getField().getRow2();
                            if (selectedPlayerPanel().getSelectedRow() == row) {
                                selectedPlayerPanel().getSelectedRow().unselectCurrent();
                                selectedPlayerPanel().switchRow();
                                selectedPlayerPanel().getSelectedRow().selectCurrent();
                            } else {
                                selectedPlayerPanel().getSelectedRow().selectCurrent();
                            }
                        }
                        revalidate(true);
                    } catch (Exception ignored) {
                    }
                }
                break;
            case Keys.DPAD_LEFT:
                if (!((FMenuBar) getHeader()).isShowingMenu(true)) {
                    try {
                        InfoTab selected = selectedPlayerPanel().getSelectedTab();
                        if (selected != null && selected.getDisplayArea().isVisible()) {
                            selectedPlayerPanel().getSelectedTab().getDisplayArea().setPreviousSelected(1);
                        } else {
                            selectedPlayerPanel().getSelectedRow().setPreviousSelected(1);
                        }
                        revalidate(true);
                    } catch (Exception ignored) {
                    }
                }
                break;
            case Keys.BUTTON_Y:
                if (!((FMenuBar) getHeader()).isShowingMenu(true)) {
                    try {
                        InfoTab selected = selectedPlayerPanel().getSelectedTab();
                        if (selected != null && selected.getDisplayArea().isVisible()) {
                            selectedPlayerPanel().getSelectedTab().getDisplayArea().showZoom();
                        } else {
                            selectedPlayerPanel().getSelectedRow().showZoom();
                        }
                    } catch (Exception ignored) {
                    }
                }
                break;
            case Keys.BUTTON_A:
                if (!((FMenuBar) getHeader()).isShowingMenu(true)) {
                    try {
                        InfoTab selected = selectedPlayerPanel().getSelectedTab();
                        if (selected != null && selected.getDisplayArea().isVisible()) {
                            //nullPotentialListener();
                            selectedPlayerPanel().getSelectedTab().getDisplayArea().tapChild();
                        } else {
                            //nullPotentialListener();
                            selectedPlayerPanel().getSelectedRow().tapChild();
                        }
                    } catch (Exception ignored) {
                    }
                }
                break;
            case Keys.BUTTON_L1: //switch selected panels
                if (Forge.hasGamepad()) {
                    //nullPotentialListener();
                    selectedPlayerPanel().hideSelectedTab();
                    selectedPlayer--;
                    if (selectedPlayer < 0)
                        selectedPlayer = playerPanelsList.size() - 1;
                    selectedPlayerPanel().closeSelectedTab();
                    selectedPlayerPanel().getSelectedRow().unselectCurrent();
                    //selectedPlayerPanel().setNextSelectedTab(true);
                }
                break;
            case Keys.ENTER:
            case Keys.SPACE:
                if (getActivePrompt().getBtnOk().trigger()) { //trigger OK on Enter or Space
                    return true;
                }
                return getActivePrompt().getBtnCancel().trigger(); //trigger Cancel if can't trigger OK
            case Keys.ESCAPE:
                if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ALLOW_ESC_TO_END_TURN) && !Forge.hasGamepad()) {//bypass check
                    if (getActivePrompt().getBtnCancel().getText().equals(Forge.getLocalizer().getMessage("lblEndTurn"))) {
                        return false;
                    }
                }
                return getActivePrompt().getBtnCancel().trigger(); //otherwise trigger Cancel
            case Keys.BACK:
                return true; //suppress Back button so it's not bumped when trying to press OK or Cancel buttons
            case Keys.A: //alpha strike on Ctrl+A on Android, A when running on desktop
                if (KeyInputAdapter.isCtrlKeyDown() || GuiBase.getInterface().isRunningOnDesktop() || Forge.hasGamepad()) {
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
                    confirmUserConcedes();
                    return true;
                }
                break;
            case Keys.Z: //undo on Ctrl+Z
                if (KeyInputAdapter.isCtrlKeyDown() || Forge.hasGamepad()) {
                    getGameController().undoLastAction();
                    return true;
                }
                break;
            case Keys.Y: //auto-yield, always yes, Ctrl+Y on Android, Y when running on desktop
                if (KeyInputAdapter.isCtrlKeyDown() || GuiBase.getInterface().isRunningOnDesktop()) {
                    final IGuiGame gui = MatchController.instance;
                    final IGameController controller = MatchController.instance.getGameController();
                    final GameView gameView = MatchController.instance.getGameView();
                    final FCollectionView<StackItemView> stack = MatchController.instance.getGameView().getStack();
                    if (stack.isEmpty()) {
                        return false;
                    }
                    StackItemView stackInstance = stack.getLast();
                    if (!stackInstance.isAbility()) {
                        return false;
                    }
                    final int triggerID = stackInstance.getSourceTrigger();

                    if (gui.shouldAlwaysAcceptTrigger(triggerID)) {
                        gui.setShouldAlwaysAskTrigger(triggerID);
                    } else {
                        gui.setShouldAlwaysAcceptTrigger(triggerID);
                        if (stackInstance.equals(gameView.peekStack())) {
                            //auto-yes if ability is on top of stack
                            controller.selectButtonOk();
                        }
                    }

                    final String key = stackInstance.getKey();
                    gui.setShouldAutoYield(key, true);
                    if (stackInstance.equals(gameView.peekStack())) {
                        //auto-pass priority if ability is on top of stack
                        controller.passPriority();
                    }
                }
                break;
            case Keys.N: //auto-yield, always no, Ctrl+N on Android, N when running on desktop
                if (KeyInputAdapter.isCtrlKeyDown() || GuiBase.getInterface().isRunningOnDesktop()) {
                    final IGuiGame gui = MatchController.instance;
                    final IGameController controller = MatchController.instance.getGameController();
                    final GameView gameView = MatchController.instance.getGameView();
                    final FCollectionView<StackItemView> stack = MatchController.instance.getGameView().getStack();
                    if (stack.isEmpty()) {
                        return false;
                    }
                    StackItemView stackInstance = stack.getLast();
                    if (!stackInstance.isAbility()) {
                        return false;
                    }
                    final int triggerID = stackInstance.getSourceTrigger();

                    if (gui.shouldAlwaysDeclineTrigger(triggerID)) {
                        gui.setShouldAlwaysAskTrigger(triggerID);
                    } else {
                        gui.setShouldAlwaysDeclineTrigger(triggerID);
                        if (stackInstance.equals(gameView.peekStack())) {
                            //auto-no if ability is on top of stack
                            controller.selectButtonCancel();
                        }
                    }

                    final String key = stackInstance.getKey();
                    gui.setShouldAutoYield(key, true);
                    if (stackInstance.equals(gameView.peekStack())) {
                        //auto-pass priority if ability is on top of stack
                        controller.passPriority();
                    }
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

    public static VPlayerPanel getPlayerPanel(final PlayerView playerView) {
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

    public void resetFields() {
        CardAreaPanel.resetForNewGame();
        for (VPlayerPanel playerPanel : getPlayerPanels().values()) {
            for (CardAreaPanel p : playerPanel.getField().getCardPanels()) {
                p.reset();
            }
            playerPanel.resetZoneTabs();
        }
    }

    public void forceRevalidate() {
        for (VPlayerPanel playerPanel : getPlayerPanels().values()) {
            playerPanel.revalidate(true);
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
        if (card == null)
            return;
        final CardAreaPanel pnl = CardAreaPanel.get(card);
        final ZoneType zone = card.getZone();
        if (zone == ZoneType.Battlefield) {
            pnl.updateCard(card);
        } else { //ensure card not on battlefield is reset such that it no longer thinks it's on the battlefield
            pnl.setTapped(false);
            pnl.getAttachedPanels().clear();
            pnl.setAttachedToPanel(null);
            pnl.setPrevPanelInStack(null);
            pnl.setNextPanelInStack(null);
        }
    }

    private String daytime = null;
    private Float time = null;
    FSkinTexture currentBG = getBG();

    FSkinTexture getBG() {
        if (Forge.isMobileAdventureMode) {
            return switch (GameScene.instance().getAdventurePlayerLocation(false, true)) {
                case "green" -> FSkinTexture.ADV_BG_FOREST;
                case "black" -> FSkinTexture.ADV_BG_SWAMP;
                case "red" -> FSkinTexture.ADV_BG_MOUNTAIN;
                case "blue" -> FSkinTexture.ADV_BG_ISLAND;
                case "white" -> FSkinTexture.ADV_BG_PLAINS;
                case "waste" -> FSkinTexture.ADV_BG_WASTE;
                case "cave" -> FSkinTexture.ADV_BG_CAVE;
                case "dungeon" -> FSkinTexture.ADV_BG_DUNGEON;
                case "castle" -> FSkinTexture.ADV_BG_CASTLE;
                default -> FSkinTexture.ADV_BG_COMMON;
            };
        }
        return FSkinTexture.BG_MATCH;
    }

    private class BGAnimation extends ForgeAnimation {
        private static final float DURATION = 1.4f;
        private float progress = 0;
        private boolean recomputed = false;

        private void drawBackground(Graphics g, FImage image, float x, float y, float w, float h, boolean darkoverlay, boolean daynightTransition) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            if (MatchController.instance.getGameView().isMatchOver())
                percentage = 1;
            if (Forge.isMobileAdventureMode) {
                if (percentage < 1)
                    g.drawNightDay(image, x, y, w, h, time, false, 0);
                if (MatchController.instance.getGameView().getGame().isDay()) {
                    g.setAlphaComposite(percentage);
                    g.drawNightDay(image, x, y, w, h, 100f, false, 0);
                    g.setAlphaComposite(oldAlpha);
                } else if (MatchController.instance.getGameView().getGame().isNight()) {
                    g.setAlphaComposite(percentage);
                    g.drawNightDay(image, x, y, w, h, -100f, false, 0);
                    g.setAlphaComposite(oldAlpha);
                }
            } else {
                if (!daynightTransition) {
                    g.setAlphaComposite(percentage);
                    if (image instanceof FSkinTexture) //for loading Planechase BG
                        g.drawRipple(image, x, y, w, h, 1 - percentage);
                    else
                        g.drawGrayTransitionImage(image, x, y, w, h, 1 - percentage);
                    g.setAlphaComposite(oldAlpha);
                } else {
                    if (hasActivePlane()) {
                        String dt = MatchController.instance.getDayTime() == null ? "" : MatchController.instance.getDayTime();
                        if (percentage < 1)
                            g.drawRipple(image, x, y, w, h, 1 - percentage);
                        if ("Day".equalsIgnoreCase(dt)) {
                            g.setAlphaComposite(percentage);
                            g.drawNightDay(image, x, y, w, h, 100f, true, 0/*1 - percentage*/); // disable extra ripples
                            g.setAlphaComposite(oldAlpha);
                        } else if ("Night".equalsIgnoreCase(dt)) {
                            g.setAlphaComposite(percentage);
                            g.drawNightDay(image, x, y, w, h, -100f, true, 0/*1 - percentage*/); // disable extra ripples
                            g.setAlphaComposite(oldAlpha);
                        }
                    } else {
                        //recompute since we don't use the current image when the animation first started
                        FSkinTexture matchBG = MatchController.instance.getDayTime() == null ? FSkinTexture.BG_MATCH : MatchController.instance.getDayTime().equals("Day") ? FSkinTexture.BG_MATCH_DAY : FSkinTexture.BG_MATCH_NIGHT;
                        if (!recomputed) {
                            float midField = topPlayerPanel.getBottom();
                            float promptHeight = !Forge.isLandscapeMode() || bottomPlayerPrompt == null ? 0f : bottomPlayerPrompt.getHeight() / 1.3f;
                            float xx = topPlayerPanel.getField().getLeft();
                            float yy = midField - topPlayerPanel.getField().getHeight() - promptHeight;
                            float ww = getWidth() - xx;
                            float bgFullWidth, scaledbgHeight;
                            int multiplier = playerPanels.keySet().size() - 1; //fix scaling of background when zoomed in multiplayer
                            float bgHeight = (midField + bottomPlayerPanel.getField().getHeight() * multiplier) - yy;
                            bgFullWidth = bgHeight * matchBG.getWidth() / matchBG.getHeight();
                            if (bgFullWidth < ww) {
                                scaledbgHeight = ww * (bgHeight / bgFullWidth);
                                bgFullWidth = ww;
                                bgHeight = scaledbgHeight;
                            }
                            g.drawRipple(matchBG, xx + (ww - bgFullWidth) / 2, yy, bgFullWidth, bgHeight, 1 - percentage);
                            if (percentage == 1)
                                recomputed = true;
                        } else {
                            g.drawRipple(matchBG, x, y, w, h, 1 - percentage);
                        }
                    }
                }
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            daytime = MatchController.instance.getDayTime();
            if (MatchController.instance.getGameView().getGame().isDay())
                time = 100f;
            if (MatchController.instance.getGameView().getGame().isNight())
                time = -100f;

        }
    }

    private class FieldScroller extends FScrollPane {
        private float extraHeight = 0;
        private String plane = "";
        private String imageName = "";

        @Override
        public void drawBackground(Graphics g) {
            super.drawBackground(g);
            if (!FModel.getPreferences().getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE)) {
                if (!Forge.isMobileAdventureMode)
                    if (!hasActivePlane())
                        return;
            }
            //boolean isGameFast = MatchController.instance.isGameFast(); //this used to control animation speed
            float midField = topPlayerPanel.getBottom();
            float promptHeight = !Forge.isLandscapeMode() || bottomPlayerPrompt == null ? 0f : bottomPlayerPrompt.getHeight() / 1.3f;
            float x = topPlayerPanel.getField().getLeft();
            float y = midField - topPlayerPanel.getField().getHeight() - promptHeight;
            float w = getWidth() - x;
            float bgFullWidth, scaledbgHeight;
            int multiplier = playerPanels.keySet().size() - 1; //fix scaling of background when zoomed in multiplayer
            float bgHeight = (midField + bottomPlayerPanel.getField().getHeight() * multiplier) - y;
            if (bgAnimation == null)
                bgAnimation = new BGAnimation();
            FSkinTexture matchBG = currentBG;
            //overrideBG
            if (!Forge.isMobileAdventureMode) {
                if (hasActivePlane()) {
                    imageName = getPlaneName();
                    if (!plane.equals(imageName)) {
                        plane = imageName;
                        bgAnimation.progress = 0;
                    }
                    String dt = MatchController.instance.getDayTime() == null ? "" : MatchController.instance.getDayTime();
                    String t = time == null ? "" : time > 0 ? "Day" : "Night";
                    if (!dt.equalsIgnoreCase(t))
                        bgAnimation.progress = 0;
                    if (FSkinTexture.GENERIC_PLANE.load(imageName))
                        matchBG = FSkinTexture.GENERIC_PLANE;
                    else {
                        if (daytime == null) {
                            matchBG = FSkinTexture.BG_MATCH;
                        } else {
                            matchBG = daytime.equals("Day") ? FSkinTexture.BG_MATCH_DAY : FSkinTexture.BG_MATCH_NIGHT;
                        }
                    }
                } else if (daytime == null) {
                    matchBG = FSkinTexture.BG_MATCH;
                } else {
                    matchBG = daytime.equals("Day") ? FSkinTexture.BG_MATCH_DAY : FSkinTexture.BG_MATCH_NIGHT;
                }
            }
            bgFullWidth = bgHeight * matchBG.getWidth() / matchBG.getHeight();
            if (bgFullWidth < w) {
                scaledbgHeight = w * (bgHeight / bgFullWidth);
                bgFullWidth = w;
                bgHeight = scaledbgHeight;
            }
            if (daytime != MatchController.instance.getDayTime() || hasActivePlane()) {
                bgAnimation.start();
                bgAnimation.drawBackground(g, matchBG, x + (w - bgFullWidth) / 2, y, bgFullWidth, bgHeight, hasActivePlane(), MatchController.instance.getDayTime() != null);
            } else {
                bgAnimation.progress = 0;
                if (MatchController.instance.getDayTime() == null)
                    g.drawImage(matchBG, x + (w - bgFullWidth) / 2, y, bgFullWidth, bgHeight);
                else {
                    if (hasActivePlane() || Forge.isMobileAdventureMode)
                        g.drawNightDay(matchBG, x + (w - bgFullWidth) / 2, y, bgFullWidth, bgHeight, time, !Forge.isMobileAdventureMode, 0f);
                    else
                        g.drawRipple(matchBG, x + (w - bgFullWidth) / 2, y, bgFullWidth, bgHeight, 0f);
                }
            }
        }

        //auto adjust zoom for local multiplayer landscape mode
        List<VPlayerPanel> losers = new ArrayList<>();

        @Override
        public void drawOverlay(Graphics g) {
            if (Forge.isLandscapeMode()) {
                if (playerPanelsList.size() > 2) {
                    for (VPlayerPanel playerPanel : playerPanelsList) {
                        if (playerPanel.getPlayer().getHasLost()) {
                            losers.add(playerPanel);
                        }
                    }
                }
                if (!losers.isEmpty()) {
                    float height = 0;
                    for (VPlayerPanel p : losers) {
                        if (playerPanelsList.size() > 2) {
                            height = p.getAvatar().getHeight();
                            p.setVisible(false);
                            playerPanelsList.remove(p);
                            System.out.println("Removed panel: " + p.getPlayer().toString());
                        }
                    }
                    losers.clear();
                    if (playerPanelsList.size() == 2) {
                        //reset avatar size
                        for (VPlayerPanel playerPanel : playerPanelsList) {
                            float size = playerPanel.getAvatar().getWidth() * 2;
                            playerPanel.getAvatar().setSize(size, size);
                            playerPanel.revalidate(true);
                            System.out.println("Panel Resized: " + playerPanel.getPlayer().toString());
                        }
                    }
                    zoom(0, 0, height);
                }
            }

            float midField;
            float x = 0;
            float y;
            float w = getWidth();

            //field separator lines
            if (!Forge.isLandscapeMode()) {
                for (VPlayerPanel playerPanel : playerPanelsList) {
                    midField = playerPanel.getTop();
                    y = midField - playerPanel.getField().getHeight();
                    if (playerPanel.getSelectedTab() == null) {
                        y++;
                    }
                    g.drawLine(1, getBorderColor(), x, y, w, y);
                }
            }

            for (VPlayerPanel playerPanel : playerPanelsList) {
                midField = playerPanel.getTop();
                y = midField - 0.5f;
                g.drawLine(1, getBorderColor(), x, y, w, y);
            }

            if (!Forge.isLandscapeMode()) {
                y = bottomPlayerPanel.getTop() + bottomPlayerPanel.getField().getHeight();
                g.drawLine(1, getBorderColor(), x, y, w, y);
            }
        }

        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float totalHeight = visibleHeight + extraHeight;
            float avatarHeight = VAvatar.HEIGHT;
            if (is4Player() || is3Player()) {
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
            Map<FScrollPane, Pair<Float, Float>> horzScrollPanes = new HashMap<>();
            backupHorzScrollPanes(topPlayerPanel, horzScrollPanes);
            backupHorzScrollPanes(bottomPlayerPanel, horzScrollPanes);

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

        private void backupHorzScrollPanes(VPlayerPanel playerPanel, Map<FScrollPane, Pair<Float, Float>> horzScrollPanes) {
            for(FScrollPane scrollPane : playerPanel.getAllScrollPanes())
                horzScrollPanes.put(scrollPane, Pair.of(scrollPane.getScrollLeft(), scrollPane.getScrollWidth()));
        }
    }

    private String getPlaneName() {
        return MatchController.instance.getGameView().getPlanarPlayer().getCurrentPlaneName();
    }

    private boolean hasActivePlane() {
        if (MatchController.instance.getGameView() != null)
            if (MatchController.instance.getGameView().getPlanarPlayer() != null) {
                return !MatchController.instance.getGameView().getPlanarPlayer().getCurrentPlaneName().isEmpty();
            }
        return false;
    }

    private void confirmUserConcedes() {
        final Consumer<Boolean> callback = result -> {
            if (result) {
                getGameController().concede();
            }
        };

        FOptionPane.showConfirmDialog(getLocalizer().getMessage("lblConcedeCurrentGame"),
            getLocalizer().getMessage("lblConcedeTitle"),
            getLocalizer().getMessage("lblConcede"),
            getLocalizer().getMessage("lblCancel"), callback);
    }

    @Override
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        setPotentialListener(listeners);
        super.buildTouchListeners(screenX, screenY, listeners);
    }

    public VPlayerPanel selectedPlayerPanel() {
        if (selectedPlayer >= playerPanelsList.size())
            selectedPlayer = playerPanelsList.size() - 1;
        if (playerPanelsList.isEmpty())
            return null;
        return playerPanelsList.get(selectedPlayer);
    }

    public static void setPotentialListener(List<FDisplayObject> listener) {
        if (potentialListener != null)
            for (FDisplayObject f : potentialListener)
                f.setHovered(false);
        potentialListener = listener;
    }

    public static void nullPotentialListener() {
        if (potentialListener != null)
            for (FDisplayObject f : potentialListener)
                f.setHovered(false);
        potentialListener = null;
    }
}
