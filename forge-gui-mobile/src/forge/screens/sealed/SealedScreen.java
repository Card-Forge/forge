package forge.screens.sealed;

import forge.game.GameType;
import forge.screens.LaunchScreen;

public class SealedScreen extends LaunchScreen {
    public SealedScreen() {
        super("Sealed Deck");
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Sealed;
        return false; //TODO: Support launching match
    }
}
