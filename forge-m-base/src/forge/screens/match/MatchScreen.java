package forge.screens.match;

import java.util.HashMap;
import java.util.Map;

import forge.screens.FScreen;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;

public class MatchScreen extends FScreen {
    private final Match match;
    private final MatchController controller;
    private final Map<RegisteredPlayer, VPlayerPanel> playerPanels;
    private final VLog log;
    private final VStack stack;
    private final VPrompt prompt;

    private VPlayerPanel bottomPlayerPanel, topPlayerPanel;

    public MatchScreen(Match match0) {
        super(false, null, true);
        match = match0;
        controller = new MatchController(this);

        playerPanels = new HashMap<RegisteredPlayer, VPlayerPanel>();
        for (RegisteredPlayer player : match.getPlayers()) {
            playerPanels.put(player, add(new VPlayerPanel(player)));
        }
        bottomPlayerPanel = playerPanels.get(match.getPlayers().get(0));
        topPlayerPanel = playerPanels.get(match.getPlayers().get(1));
        topPlayerPanel.setFlipped(true);

        log = add(new VLog());
        stack = add(new VStack());
        prompt = add(new VPrompt());

        controller.startGameWithUi(match0);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float playerPanelHeight = (height - startY - VPrompt.HEIGHT - VLog.HEIGHT) / 2f;

        log.setBounds(0, startY, width - FScreen.BTN_WIDTH, VLog.HEIGHT);
        topPlayerPanel.setBounds(0, startY + VLog.HEIGHT, width, playerPanelHeight);
        stack.setBounds(0, startY + VLog.HEIGHT + playerPanelHeight - VStack.HEIGHT / 2, VStack.WIDTH, VStack.HEIGHT);
        bottomPlayerPanel.setBounds(0, height - VPrompt.HEIGHT - playerPanelHeight, width, playerPanelHeight);
        prompt.setBounds(0, height - VPrompt.HEIGHT, width, VPrompt.HEIGHT);
    }
}
