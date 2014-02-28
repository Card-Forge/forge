package forge.screens.match;

import forge.screens.FScreen;
import forge.game.Match;

public class MatchScreen extends FScreen {
    private final Match match;
    private final MatchController controller;

    public MatchScreen(Match match0) {
        super(false, null, true);
        match = match0;
        controller = new MatchController(this);
        controller.startGameWithUi(match0);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
    }
}
