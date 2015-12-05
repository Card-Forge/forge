package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.FThreads;
import forge.achievement.PlaneswalkerAchievements;
import forge.assets.FImage;
import forge.card.CardZoom;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestController;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.planarconquest.ConquestUtil;
import forge.screens.LoadingOverlay;
import forge.screens.MultiStepWizardScreen;
import forge.screens.home.NewGameMenu;
import forge.screens.planarconquest.ConquestMenu.LaunchReason;
import forge.toolbox.FChoiceList;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.ThreadUtil;

public class NewConquestScreen extends MultiStepWizardScreen<NewConquestScreenModel> {
    private static final float PADDING = FOptionPane.PADDING;

    @SuppressWarnings("unchecked")
    public NewConquestScreen() {
        super(null, NewGameMenu.getMenu(), new WizardStep[] {
            new SelectPlaneswalkerStep(),
            new SelectStartingPlaneStep(),
            new SelectStartingCommanderStep()
        }, new NewConquestScreenModel());
    }

    @Override
    protected void finish() {
        //create new quest in game thread so option panes can wait for input
        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                newConquest();
            }
        });
    }

    private void newConquest() {
        String conquestName = ConquestUtil.promptForName();
        if (conquestName == null) { return; }
        startNewConquest(conquestName);
    }

    private void startNewConquest(final String conquestName) {
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                LoadingOverlay.show("Starting new conquest...", new Runnable() {
                    @Override
                    public void run() {
                        ConquestController qc = FModel.getConquest();
                        qc.load(new ConquestData(conquestName, model.planeswalker, model.startingPlane, model.startingCommander));
                        qc.save();

                        // Save in preferences.
                        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, conquestName + ".dat");
                        FModel.getConquestPreferences().save();

                        ConquestMenu.launchPlanarConquest(LaunchReason.NewConquest);
                    }
                });
            }
        });
    }

    private static class SelectPlaneswalkerStep extends WizardStep<NewConquestScreenModel> {
        private final TokenDisplay tokenDisplay = add(new TokenDisplay());
        private final FChoiceList<PaperCard> lstPlaneswalkers = add(new FChoiceList<PaperCard>(ConquestUtil.getAllPlaneswalkers()) {
            @Override
            protected void onItemActivate(Integer index, PaperCard value) {
                advance();
            }

            @Override
            protected void onSelectionChange() {
                PaperCard planeswalker = getSelectedItem();
                if (planeswalker != null) {
                    tokenDisplay.setIcon((FImage)PlaneswalkerAchievements.getTrophyImage(planeswalker.getName()));
                }
                else {
                    tokenDisplay.setIcon(null);
                }
            }
        });

        protected SelectPlaneswalkerStep() {
            super("Select Planeswalker");
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;
            tokenDisplay.setBounds(x, y, w, height * 0.5f);
            y += tokenDisplay.getHeight() + PADDING;
            lstPlaneswalkers.setBounds(x, y, w, height - y - PADDING);
        }

        @Override
        protected void reset() {
            if (lstPlaneswalkers.getCount() > 0) {
                lstPlaneswalkers.setSelectedIndex(0);
            }
        }

        @Override
        protected void onActivate(NewConquestScreenModel model) {
        }

        @Override
        protected boolean updateModelAndAdvance(NewConquestScreenModel model) {
            model.planeswalker = lstPlaneswalkers.getSelectedItem();
            return model.planeswalker != null;
        }

        private class TokenDisplay extends FLabel {
            protected TokenDisplay() {
                super(new FLabel.Builder().iconScaleFactor(1).insets(new Vector2(0, 0))
                        .iconInBackground(true).align(HAlignment.CENTER));
            }

            @Override
            public boolean tap(float x, float y, int count) {
                return zoom();
            }
            @Override
            public boolean longPress(float x, float y) {
                return zoom();
            }
            private boolean zoom() {
                int index = lstPlaneswalkers.getSelectedIndex();
                if (index == -1) { return false; }
                CardZoom.show(lstPlaneswalkers.extractListData(), index, lstPlaneswalkers);
                return true;
            }

            @Override
            public boolean fling(float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    int selectedIndex = lstPlaneswalkers.getSelectedIndex();
                    if (velocityX > 0) {
                        if (selectedIndex > 0) {
                            lstPlaneswalkers.setSelectedIndex(selectedIndex - 1);
                        }
                    }
                    else if (selectedIndex < lstPlaneswalkers.getCount() - 1) {
                        lstPlaneswalkers.setSelectedIndex(selectedIndex + 1);
                    }
                    return true;
                }
                return false;
            }
        }
    }

    private static class SelectStartingPlaneStep extends WizardStep<NewConquestScreenModel> {
        private static final ConquestPlane[] planes = ConquestPlane.values();
        private int selectedIndex;

        protected SelectStartingPlaneStep() {
            super("Select Starting Plane");
        }

        @Override
        protected void doLayout(float width, float height) {
        }

        @Override
        protected void reset() {
            selectedIndex = 0;
        }

        @Override
        protected void onActivate(NewConquestScreenModel model) {
        }

        @Override
        protected boolean updateModelAndAdvance(NewConquestScreenModel model) {
            model.startingPlane = planes[selectedIndex];
            return model.startingPlane != null;
        }
    }

    private static class SelectStartingCommanderStep extends WizardStep<NewConquestScreenModel> {
        private final FChoiceList<PaperCard> lstCommanders = add(new FChoiceList<PaperCard>(ConquestPlane.Alara.getCommanders()) {
            @Override
            protected void onItemActivate(Integer index, PaperCard value) {
                advance();
            }
        });

        protected SelectStartingCommanderStep() {
            super("Select Starting Commander");
        }

        @Override
        protected void doLayout(float width, float height) {
            lstCommanders.setBounds(PADDING, PADDING, width - 2 * PADDING, height - 2 * PADDING);
        }

        @Override
        protected void reset() {
            if (lstCommanders.getCount() > 0) {
                lstCommanders.setSelectedIndex(0);
            }
        }

        @Override
        protected void onActivate(NewConquestScreenModel model) {
            lstCommanders.setListData(model.startingPlane.getCommanders());
            reset();
        }

        @Override
        protected boolean updateModelAndAdvance(NewConquestScreenModel model) {
            model.startingCommander = lstCommanders.getSelectedItem();
            return model.startingCommander != null;
        }
    }
}
