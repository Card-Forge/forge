package forge.screens.quest;

import forge.screens.LaunchScreen;

public class QuestChallengesScreen extends LaunchScreen {
    public QuestChallengesScreen() {
        super("Quest Duels", new QuestMenu());
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        // TODO Auto-generated method stub
        return false;
    }
}
