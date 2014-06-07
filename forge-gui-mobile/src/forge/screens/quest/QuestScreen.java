package forge.screens.quest;

import forge.game.GameType;
import forge.screens.LaunchScreen;

public class QuestScreen extends LaunchScreen {
    public QuestScreen() {
        super("Quest Mode");
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Quest;
        return false; //TODO: Support launching match
    }
}
