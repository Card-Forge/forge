package forge.screens.quest;

import forge.FThreads;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FOptionPane;

public abstract class QuestLaunchScreen extends LaunchScreen {
    protected static final float PADDING = FOptionPane.PADDING;

    public QuestLaunchScreen() {
        super("", QuestMenu.getMenu());
    }

    @Override
    public final void onActivate() {
        update();
    }

    @Override
    protected void startMatch() {
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                if (QuestUtil.canStartGame()) {
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            LoadingOverlay.show("Loading new game...", new Runnable() {
                                @Override
                                public void run() {
                                    QuestUtil.finishStartingGame();
                                }
                            });
                        }
                    });
                    return;
                }
            }
        });
    }

    public final void update() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        updateHeaderCaption();
        onUpdate();
    }

    protected void updateHeaderCaption() {
        setHeaderCaption(FModel.getQuest().getName() + " - " + getGameType() + "\n(" + FModel.getQuest().getRank() + ")");
    }

    protected abstract String getGameType();
    protected abstract void onUpdate();
}
