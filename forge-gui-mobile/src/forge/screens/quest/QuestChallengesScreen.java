package forge.screens.quest;

import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;

public class QuestChallengesScreen extends LaunchScreen {
    public QuestChallengesScreen() {
        super("Quest Duels", QuestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
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
