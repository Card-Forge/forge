package forge.screens.planarconquest;

import forge.FThreads;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FOptionPane;

public class ConquestMapScreen extends LaunchScreen {
    protected static final float PADDING = FOptionPane.PADDING;

    public ConquestMapScreen() {
        super("", ConquestMenu.getMenu());
    }

    @Override
    public final void onActivate() {
        update();
    }

    @Override
    protected void startMatch() {
        if (creatingMatch) { return; }
        creatingMatch = true; //ensure user doesn't create multiple matches by tapping multiple times

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
                                    creatingMatch = false;
                                }
                            });
                        }
                    });
                    return;
                }
                creatingMatch = false;
            }
        });
    }

    public void update() {
        setHeaderCaption(FModel.getConquest().getName() + " - " + FModel.getConquest().getCurrentPlane());
    }

    @Override
    protected final boolean buildLaunchParams(LaunchParams launchParams) {
        return false; //this override isn't needed
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }
}
