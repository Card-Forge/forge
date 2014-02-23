package forge.screens.draft;

import forge.screens.LaunchScreen;
import forge.game.GameType;

public class DraftScreen extends LaunchScreen {
    public DraftScreen() {
    	super("Draft");
    }

	@Override
	protected void doLayoutAboveBtnStart(float startY, float width, float height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean buildLaunchParams(LaunchParams launchParams) {
		launchParams.gameType = GameType.Draft;
		return false; //TODO: Support launching match
	}
}
