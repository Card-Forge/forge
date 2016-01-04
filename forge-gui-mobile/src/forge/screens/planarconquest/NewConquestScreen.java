package forge.screens.planarconquest;

import com.google.common.collect.Iterables;

import forge.FThreads;
import forge.achievement.PlaneswalkerAchievements;
import forge.assets.FImage;
import forge.assets.FTextureImage;
import forge.assets.ImageCache;
import forge.card.CardListPreview;
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
import forge.toolbox.FOptionPane;
import forge.util.ThreadUtil;

public class NewConquestScreen extends MultiStepWizardScreen<NewConquestScreenModel> {
    private static final float PADDING = FOptionPane.PADDING;
    private static final float CARD_PREVIEW_RATIO = 0.5f;

    @SuppressWarnings("unchecked")
    public NewConquestScreen() {
        super(null, NewGameMenu.getMenu(), new WizardStep[] {
            new SelectStartingPlaneStep(),
            new SelectStartingPlaneswalkerStep(),
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
                        qc.setModel(new ConquestData(conquestName, model.startingPlane, model.startingPlaneswalker, model.startingCommander));
                        qc.getDecks().add(Iterables.getFirst(qc.getModel().getCommanders(), null).getDeck()); //ensure starting deck is saved
                        qc.getModel().saveData();

                        // Save in preferences.
                        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, conquestName + ".dat");
                        FModel.getConquestPreferences().save();

                        ConquestMenu.launchPlanarConquest(LaunchReason.NewConquest);
                    }
                });
            }
        });
    }

    private static class SelectStartingPlaneStep extends WizardStep<NewConquestScreenModel> {
        private final ConquestPlaneSelector planeSelector = add(new ConquestPlaneSelector());

        protected SelectStartingPlaneStep() {
            super("Select Starting Plane");
        }

        @Override
        protected void doLayout(float width, float height) {
            planeSelector.setBounds(0, 0, width, height);
        }

        @Override
        protected void reset() {
            planeSelector.reset();
        }

        @Override
        protected void onActivate(NewConquestScreenModel model) {
            planeSelector.activate();
        }

        @Override
        protected boolean updateModelAndAdvance(NewConquestScreenModel model) {
            model.startingPlane = planeSelector.getSelectedPlane();
            if (model.startingPlane != null) {
                planeSelector.deactivate();
                return true;
            }
            return false;
        }
    }

    private static class SelectStartingPlaneswalkerStep extends WizardStep<NewConquestScreenModel> {
        private final FChoiceList<PaperCard> lstPlaneswalkers = add(new FChoiceList<PaperCard>(ConquestUtil.getAllPlaneswalkers()) {
            @Override
            protected void onItemActivate(Integer index, PaperCard value) {
                advance();
            }

            @Override
            protected void onSelectionChange() {
                if (tokenDisplay == null) { return; }
                updatePreview();
            }
        });
        private final CardListPreview tokenDisplay = add(new CardListPreview(lstPlaneswalkers));

        protected SelectStartingPlaneswalkerStep() {
            super("Select Starting Planeswalker");
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;
            tokenDisplay.setBounds(x, y, w, height * CARD_PREVIEW_RATIO);
            y += tokenDisplay.getHeight() + PADDING;
            lstPlaneswalkers.setBounds(x, y, w, height - y - PADDING);
        }

        @Override
        protected void reset() {
            if (lstPlaneswalkers.getCount() > 0) {
                lstPlaneswalkers.setSelectedIndex(0);
            }
        }

        private void updatePreview() {
            PaperCard planeswalker = lstPlaneswalkers.getSelectedItem();
            if (planeswalker != null) {
                tokenDisplay.setIcon((FImage)PlaneswalkerAchievements.getTrophyImage(planeswalker.getName()));
            }
            else {
                tokenDisplay.setIcon(null);
            }
        }

        @Override
        protected void onActivate(NewConquestScreenModel model) {
            updatePreview();
        }

        @Override
        protected boolean updateModelAndAdvance(NewConquestScreenModel model) {
            model.startingPlaneswalker = lstPlaneswalkers.getSelectedItem();
            return model.startingPlaneswalker != null;
        }
    }

    private static class SelectStartingCommanderStep extends WizardStep<NewConquestScreenModel> {
        private final FChoiceList<PaperCard> lstCommanders = add(new FChoiceList<PaperCard>(ConquestPlane.Alara.getCommanders()) {
            @Override
            protected void onItemActivate(Integer index, PaperCard value) {
                advance();
            }

            @Override
            protected void onSelectionChange() {
                if (cardDisplay == null) { return; }
                updatePreview();
            }
        });
        private final CardListPreview cardDisplay = add(new CardListPreview(lstCommanders));

        protected SelectStartingCommanderStep() {
            super("Select Starting Commander");
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;
            cardDisplay.setBounds(x, y, w, height * CARD_PREVIEW_RATIO);
            y += cardDisplay.getHeight() + PADDING;
            lstCommanders.setBounds(x, y, w, height - y - PADDING);
        }

        @Override
        protected void reset() {
            if (lstCommanders.getCount() > 0) {
                lstCommanders.setSelectedIndex(0);
            }
        }

        private void updatePreview() {
            PaperCard card = lstCommanders.getSelectedItem();
            if (card != null) {
                cardDisplay.setIcon(new FTextureImage(ImageCache.getImage(card)));
            }
            else {
                cardDisplay.setIcon(null);
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
