package forge.screens.guantlet;

import forge.game.GameType;
import forge.screens.LaunchScreen;

public class GuantletScreen extends LaunchScreen {
    public GuantletScreen() {
    	super("Guantlet");
    }

	@Override
	protected void doLayoutAboveBtnStart(float startY, float width, float height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean buildLaunchParams(LaunchParams launchParams) {
		launchParams.gameType = GameType.Gauntlet;
		return false; //TODO: Support launching match
	}
}
