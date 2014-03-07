package forge.screens.match;

import java.util.HashMap;
import java.util.Map;

import forge.screens.FScreen;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.Forge.Graphics;
import forge.assets.FSkinTexture;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;

public class MatchScreen extends FScreen {
    private final Match match;
    private final MatchController controller;
    private final Map<RegisteredPlayer, VPlayerPanel> playerPanels;
    //private final VLog log;
    private final VStack stack;
    private final VPrompt prompt;

    private VPlayerPanel bottomPlayerPanel, topPlayerPanel;

    public MatchScreen(Match match0) {
        super(true, "Game 1 Turn 1", true);
        match = match0;
        controller = new MatchController(this);

        playerPanels = new HashMap<RegisteredPlayer, VPlayerPanel>();
        for (RegisteredPlayer player : match.getPlayers()) {
            playerPanels.put(player, add(new VPlayerPanel(player)));
        }
        bottomPlayerPanel = playerPanels.get(match.getPlayers().get(0));
        topPlayerPanel = playerPanels.get(match.getPlayers().get(1));
        topPlayerPanel.setFlipped(true);

        //log = add(new VLog());
        stack = add(new VStack());
        prompt = add(new VPrompt());

        controller.startGameWithUi(match0);
    }

    @Override
    public void drawBackground(Graphics g) {
        super.drawBackground(g);
        float y = topPlayerPanel.getTop() + VAvatar.HEIGHT;
        g.drawImage(FSkinTexture.BG_MATCH, 0, y, getWidth(), bottomPlayerPanel.getBottom() - VAvatar.HEIGHT - y);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float playerPanelHeight = (height - startY - VPrompt.HEIGHT) / 2f;

        //log.setBounds(0, startY, width - FScreen.HEADER_HEIGHT, VLog.HEIGHT);
        topPlayerPanel.setBounds(0, startY, width, playerPanelHeight);
        stack.setBounds(0, startY + playerPanelHeight - VStack.HEIGHT / 2, VStack.WIDTH, VStack.HEIGHT);
        bottomPlayerPanel.setBounds(0, height - VPrompt.HEIGHT - playerPanelHeight, width, playerPanelHeight);
        prompt.setBounds(0, height - VPrompt.HEIGHT, width, VPrompt.HEIGHT);
    }
}
