package forge.screens.constructed;

import forge.game.GameType;
import forge.screens.LaunchScreen;

public class ConstructedScreen extends LaunchScreen {
    public ConstructedScreen() {
    	super("Constructed");
    }

	@Override
	protected void doLayoutAboveBtnStart(float startY, float width, float height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean buildLaunchParams(LaunchParams launchParams) {
		launchParams.gameType = GameType.Constructed;
		return false; //TODO: Support launching match
	}
}
