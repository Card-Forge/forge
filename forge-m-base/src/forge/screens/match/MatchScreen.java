package forge.screens.match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VGameDetails;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class MatchScreen extends FScreen {
    public static FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);

    private final Map<Player, VPlayerPanel> playerPanels = new HashMap<Player, VPlayerPanel>();
    private final VLog log;
    private final VStack stack;
    private final VPrompt prompt;
    private final VGameDetails gameDetails;

    private VPlayerPanel bottomPlayerPanel, topPlayerPanel;

    public MatchScreen(List<VPlayerPanel> playerPanels0) {
        super(true, "Game", true);

        for (VPlayerPanel playerPanel : playerPanels0) {
            playerPanels.put(playerPanel.getPlayer(), add(playerPanel));
        }
        bottomPlayerPanel = playerPanels0.get(0);
        topPlayerPanel = playerPanels0.get(1);
        topPlayerPanel.setFlipped(true);
        bottomPlayerPanel.setSelectedZone(ZoneType.Hand);

        prompt = add(new VPrompt());

        log = add(new VLog());
        stack = add(new VStack());
        gameDetails = add(new VGameDetails());
        log.setVisible(false);
        stack.setVisible(false);
        gameDetails.setVisible(false);
    }

    public VPrompt getPrompt() {
        return prompt;
    }

    public VStack getStack() {
        return stack;
    }

    public VLog getLog() {
        return log;
    }

    public VGameDetails getGameDetails() {
        return gameDetails;
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
        FModel.getPreferences().writeMatchPreferences();
        FModel.getPreferences().save();
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
        if (topPlayerPanel.getSelectedZone() == null) {
            y++; //ensure border goes all the way across under avatar
        }
        g.drawLine(1, BORDER_COLOR, 0, y, w, y);
        y = midField;
        g.drawLine(1, BORDER_COLOR, 0, y, w, y);
        y += bottomPlayerPanel.getField().getHeight();
        g.drawLine(1, BORDER_COLOR, 0, y, w, y);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        //determine player panel heights based on visibility of zone displays
        float topPlayerPanelHeight, bottomPlayerPanelHeight;
        float cardRowsHeight = height - startY - VPrompt.HEIGHT - 2 * VAvatar.HEIGHT;
        if (topPlayerPanel.getSelectedZone() == null) {
            if (bottomPlayerPanel.getSelectedZone() != null) {
                topPlayerPanelHeight = cardRowsHeight * 2f / 5f;
                bottomPlayerPanelHeight = cardRowsHeight * 3f / 5f;
            }
            else {
                topPlayerPanelHeight = cardRowsHeight / 2f;
                bottomPlayerPanelHeight = topPlayerPanelHeight;
            }
        }
        else if (bottomPlayerPanel.getSelectedZone() == null) {
            topPlayerPanelHeight = cardRowsHeight * 3f / 5f;
            bottomPlayerPanelHeight = cardRowsHeight * 2f / 5f;
        }
        else {
            topPlayerPanelHeight = cardRowsHeight / 2f;
            bottomPlayerPanelHeight = topPlayerPanelHeight;
        }
        topPlayerPanelHeight += VAvatar.HEIGHT;
        bottomPlayerPanelHeight += VAvatar.HEIGHT;

        //log.setBounds(0, startY, width - FScreen.HEADER_HEIGHT, VLog.HEIGHT);
        topPlayerPanel.setBounds(0, startY, width, topPlayerPanelHeight);
        stack.setBounds(0, startY + topPlayerPanelHeight - VStack.HEIGHT / 2, VStack.WIDTH, VStack.HEIGHT);
        bottomPlayerPanel.setBounds(0, height - VPrompt.HEIGHT - bottomPlayerPanelHeight, width, bottomPlayerPanelHeight);
        prompt.setBounds(0, height - VPrompt.HEIGHT, width, VPrompt.HEIGHT);
    }
}
