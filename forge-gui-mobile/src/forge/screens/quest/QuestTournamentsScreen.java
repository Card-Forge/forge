package forge.screens.quest;

import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;

public class QuestTournamentsScreen extends LaunchScreen {
    public QuestTournamentsScreen() {
        super("Quest Duels", QuestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Tournaments\n(" + FModel.getQuest().getRank() + ")");
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
