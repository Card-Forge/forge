package forge.screens.quest;

import forge.Forge;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.FThreads;
import forge.model.FModel;
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
        FThreads.invokeInBackgroundThread(() -> {
            if (QuestUtil.canStartGame()) {
                FThreads.invokeInEdtLater(() -> LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingNewGame"), true, () -> QuestUtil.finishStartingGame()));
                return;
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
