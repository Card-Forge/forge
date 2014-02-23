package forge.screens.match;

import forge.FScreen;
import forge.game.Match;

public class MatchScreen extends FScreen {
	private final Match match;

    public MatchScreen(Match match0) {
    	super(false, null, true);
    	this.match = match0;
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
    }
}
