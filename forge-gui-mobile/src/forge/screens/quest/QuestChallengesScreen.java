package forge.screens.quest;

import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;

public class QuestChallengesScreen extends LaunchScreen {
    public QuestChallengesScreen() {
        super("Quest Duels", QuestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        update();
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }

    public void update() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Challenges\n(" + FModel.getQuest().getRank() + ")");
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        // TODO Auto-generated method stub
        return false;
    }
}
