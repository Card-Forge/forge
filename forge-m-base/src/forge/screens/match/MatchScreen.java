package forge.screens.match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.screens.FScreen;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VStack;
import forge.utils.Utils;
import forge.game.Match;
import forge.game.player.Player;
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
        float logHeight = btnMenu.getHeight();
        float stackWidth = Utils.AVG_FINGER_WIDTH;
        float stackHeight = stackWidth * Utils.CARD_ASPECT_RATIO;
        float promptHeight = Utils.AVG_FINGER_HEIGHT;
        float playerPanelHeight = (height - startY - promptHeight - logHeight) / 2f;

        log.setBounds(0, startY, width - btnMenu.getWidth(), logHeight);
        topPlayerPanel.setBounds(0, startY + logHeight, width, playerPanelHeight);
        stack.setBounds(0, startY + logHeight + playerPanelHeight - stackHeight / 2, stackWidth, stackHeight);
        bottomPlayerPanel.setBounds(0, height - promptHeight - playerPanelHeight, width, playerPanelHeight);
        prompt.setBounds(0, height - promptHeight, width, promptHeight);
    }
}
