package forge.screens.match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Input.Keys;

import forge.LobbyPlayer;
import forge.menu.FMenuBar;
import forge.properties.ForgePreferences;
import forge.screens.FScreen;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VCombat;
import forge.screens.match.views.VDevMenu;
import forge.screens.match.views.VGameMenu;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPlayers;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.Forge.Graphics;
import forge.Forge.KeyInputAdapter;
import forge.assets.FSkinColor;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class MatchScreen extends FScreen {
    public static FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);

    private final Map<Player, VPlayerPanel> playerPanels = new HashMap<Player, VPlayerPanel>();
    private final FMenuBar menuBar;
    private final VPrompt prompt;
    private final VLog log;
    private final VCombat combat;
    private final VStack stack;
    private final VDevMenu devMenu;

    private VPlayerPanel bottomPlayerPanel, topPlayerPanel;

    public MatchScreen(Game game, LobbyPlayer localPlayer, List<VPlayerPanel> playerPanels0) {
        super(false, null, false); //match screen has custom header

        for (VPlayerPanel playerPanel : playerPanels0) {
            playerPanels.put(playerPanel.getPlayer(), add(playerPanel));
        }
        bottomPlayerPanel = playerPanels0.get(0);
        topPlayerPanel = playerPanels0.get(1);
        topPlayerPanel.setFlipped(true);
        bottomPlayerPanel.setSelectedZone(ZoneType.Hand);

        prompt = add(new VPrompt("", "",
                new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        FControl.getInputProxy().selectButtonOK();
                    }
                },
                new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        FControl.getInputProxy().selectButtonCancel();
                    }
                }));

        log = new VLog(game.getGameLog());
        combat = new VCombat();
        stack = new VStack(game.getStack(), localPlayer);
        devMenu = new VDevMenu();

        menuBar = add(new FMenuBar());
        menuBar.addTab("Game", new VGameMenu());
        menuBar.addTab("Players (" + playerPanels.size() + ")", new VPlayers());
        menuBar.addTab("Log", log);
        menuBar.addTab("Combat", combat);
        menuBar.addTab("Dev", devMenu);
        menuBar.addTab("Stack (0)", stack);
    }

    @Override
    public void onActivate() {
        //update dev menu visibility here so returning from Settings screen allows update
        devMenu.getMenuTab().setVisible(ForgePreferences.DEV_MODE);
    }

    public VLog getLog() {
        return log;
    }

    public VCombat getCombat() {
        return combat;
    }

    public VStack getStack() {
        return stack;
    }

    public VPrompt getPrompt() {
        return prompt;
    }

    public VPlayerPanel getTopPlayerPanel() {
        return topPlayerPanel;
    }

    public VPlayerPanel getBottomPlayerPanel() {
        return bottomPlayerPanel;
    }

    public Map<Player, VPlayerPanel> getPlayerPanels() {
        return playerPanels;
    }

    @Override
    public boolean onClose(boolean canCancel) {
        FControl.writeMatchPreferences();
        return true;
    }

    @Override
    public void drawBackground(Graphics g) {
        super.drawBackground(g);
        float midField = topPlayerPanel.getBottom();
        float y = midField - topPlayerPanel.getField().getHeight();
        float w = getWidth();

        g.drawImage(FSkinTexture.BG_MATCH, 0, y, w, midField + bottomPlayerPanel.getField().getHeight() - y);

        //field separator lines
        if (topPlayerPanel.getSelectedTab() == null) {
            y++; //ensure border goes all the way across under avatar
        }
        g.drawLine(1, BORDER_COLOR, 0, y, w, y);

        y = midField;
        g.drawLine(1, BORDER_COLOR, 0, y, w, y);

        y = midField + bottomPlayerPanel.getField().getHeight();
        g.drawLine(1, BORDER_COLOR, 0, y, w, y);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        menuBar.setBounds(0, 0, width, menuBar.getPreferredHeight());
        startY = menuBar.getHeight();

        //determine player panel heights based on visibility of zone displays
        float topPlayerPanelHeight, bottomPlayerPanelHeight;
        float cardRowsHeight = height - startY - VPrompt.HEIGHT - 2 * VAvatar.HEIGHT;
        if (topPlayerPanel.getSelectedTab() == null) {
            if (bottomPlayerPanel.getSelectedTab() != null) {
                topPlayerPanelHeight = cardRowsHeight * 2f / 5f;
                bottomPlayerPanelHeight = cardRowsHeight * 3f / 5f;
            }
            else {
                topPlayerPanelHeight = cardRowsHeight / 2f;
                bottomPlayerPanelHeight = topPlayerPanelHeight;
            }
        }
        else if (bottomPlayerPanel.getSelectedTab() == null) {
            topPlayerPanelHeight = cardRowsHeight * 3f / 5f;
            bottomPlayerPanelHeight = cardRowsHeight * 2f / 5f;
        }
        else {
            topPlayerPanelHeight = cardRowsHeight / 2f;
            bottomPlayerPanelHeight = topPlayerPanelHeight;
        }
        topPlayerPanelHeight += VAvatar.HEIGHT;
        bottomPlayerPanelHeight += VAvatar.HEIGHT;

        topPlayerPanel.setBounds(0, startY, width, topPlayerPanelHeight);
        bottomPlayerPanel.setBounds(0, height - VPrompt.HEIGHT - bottomPlayerPanelHeight, width, bottomPlayerPanelHeight);
        prompt.setBounds(0, height - VPrompt.HEIGHT, width, VPrompt.HEIGHT);
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
        case Keys.SPACE:
            if (prompt.getBtnOk().trigger()) { //trigger OK on Enter or Space
                return true;
            }
            return prompt.getBtnCancel().trigger(); //trigger Cancel if can't trigger OK
        case Keys.ESCAPE:
            if (InputSelectCard.hide()) { //hide card selection if one active on Escape
                return true;
            }
            return prompt.getBtnCancel().trigger(); //otherwise trigger Cancel
        case Keys.A: //alpha strike on Ctrl+A
            if (KeyInputAdapter.isCtrlKeyDown()) {
                FControl.alphaStrike();
                return true;
            }
            break;
        case Keys.E: //end turn on Ctrl+E
            if (KeyInputAdapter.isCtrlKeyDown()) {
                FControl.endCurrentTurn();
                return true;
            }
            break;
        case Keys.Q: //concede game on Ctrl+Q
            if (KeyInputAdapter.isCtrlKeyDown()) {
                FControl.concede();
                return true;
            }
            break;
        case Keys.Z: //undo on Ctrl+Z
            if (KeyInputAdapter.isCtrlKeyDown()) {
                FControl.undoLastAction();
                return true;
            }
            break;
        }
        return super.keyDown(keyCode);
    }
}
