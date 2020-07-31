package forge.screens.planarconquest;

import com.google.common.collect.Iterables;

import forge.FThreads;
import forge.achievement.PlaneswalkerAchievements;
import forge.assets.FImage;
import forge.card.CardImage;
import forge.card.CardListPreview;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestController;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.planarconquest.ConquestUtil;
import forge.screens.LoadingOverlay;
import forge.screens.MultiStepWizardScreen;
import forge.screens.home.NewGameMenu;
import forge.screens.planarconquest.ConquestMenu.LaunchReason;
import forge.toolbox.FChoiceList;
import forge.toolbox.FOptionPane;
import forge.util.ThreadUtil;
import forge.util.Localizer;

public class NewConquestScreen extends MultiStepWizardScreen<NewConquestScreenModel> {
    private static final float PADDING = FOptionPane.PADDING;

    @SuppressWarnings("unchecked")
    public NewConquestScreen() {
        super(null, NewGameMenu.getMenu(), new WizardStep[] {
            new SelectStartingPlaneStep(),
            new SelectStartingCommanderStep(),
            new SelectStartingPlaneswalkerStep()
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
                LoadingOverlay.show(Localizer.getInstance().getMessage("lblStartingNewConquest"), new Runnable() {
                    @Override
                    public void run() {
                        ConquestController qc = FModel.getConquest();
                        qc.setModel(new ConquestData(conquestName, model.startingPlane, model.startingPlaneswalker, model.startingCommander));
                        qc.getDecks().add(Iterables.getFirst(qc.getModel().getCommanders(), null).getDeck()); //ensure starting deck is saved
                        qc.getModel().saveData();

                        // Save in preferences.
                        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, conquestName);
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
            super(Localizer.getInstance().getMessage("lblSelectStartingPlane"));
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

    private static class SelectStartingCommanderStep extends WizardStep<NewConquestScreenModel> {
        private final FChoiceList<PaperCard> lstCommanders = add(new FChoiceList<PaperCard>(FModel.getPlanes().iterator().next().getCommanders()) {
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
            super(Localizer.getInstance().getMessage("lblSelectStartingCommander"));
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;
            cardDisplay.setBounds(x, y, w, height * CardListPreview.CARD_PREVIEW_RATIO);
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
                cardDisplay.setIcon(new CardImage(card));
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

    private static class SelectStartingPlaneswalkerStep extends WizardStep<NewConquestScreenModel> {
        private final FChoiceList<PaperCard> lstPlaneswalkers = add(new FChoiceList<PaperCard>(FModel.getPlanes().iterator().next().getCommanders()) { //just use commanders as temporary list
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
            super(Localizer.getInstance().getMessage("lblSelectStartingPlaneswalker"));
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;
            tokenDisplay.setBounds(x, y, w, height * CardListPreview.CARD_PREVIEW_RATIO);
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
            lstPlaneswalkers.setListData(ConquestUtil.getStartingPlaneswalkerOptions(model.startingCommander));
            reset();
        }

        @Override
        protected boolean updateModelAndAdvance(NewConquestScreenModel model) {
            model.startingPlaneswalker = lstPlaneswalkers.getSelectedItem();
            return model.startingPlaneswalker != null;
        }
    }
}
