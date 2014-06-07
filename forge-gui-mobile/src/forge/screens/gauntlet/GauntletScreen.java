package forge.screens.gauntlet;

import forge.game.GameType;
import forge.screens.LaunchScreen;

public class GauntletScreen extends LaunchScreen {
    public GauntletScreen() {
        super("Gauntlets");
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
