package forge.screens.planarconquest;

import forge.Forge;
import forge.assets.FImage;
import forge.card.CardListPreview;
import forge.deck.FDeckChooser;
import forge.item.PaperCard;
import forge.localinstance.achievements.PlaneswalkerAchievements;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.screens.FScreen;
import forge.toolbox.FChoiceList;
import forge.util.Callback;

public class ConquestPlaneswalkersScreen extends FScreen {
    private static final float PADDING = FDeckChooser.PADDING;

    private final FChoiceList<PaperCard> lstPlaneswalkers = add(new FChoiceList<PaperCard>(FModel.getPlanes().iterator().next().getCommanders()) { //just use commanders as temporary list
        @Override
        protected void onItemActivate(Integer index, PaperCard value) {
            Forge.back();
        }

        @Override
        protected void onSelectionChange() {
            if (tokenDisplay == null) { return; }
            updatePreview();
        }
    });
    private final CardListPreview tokenDisplay = add(new CardListPreview(lstPlaneswalkers));

    public ConquestPlaneswalkersScreen() {
        super("Select Planeswalker", ConquestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        refreshPlaneswalkers();
    }

    @Override
    public void onClose(final Callback<Boolean> canCloseCallback) {
        if (canCloseCallback == null) { return; }

        final PaperCard planeswalker = lstPlaneswalkers.getSelectedItem();
        if (planeswalker == null) {
            canCloseCallback.run(true); //shouldn't happen, but don't block closing screen if no commanders
            return;
        }

        ConquestData model = FModel.getConquest().getModel();
        if (model.getPlaneswalker() != planeswalker) {
            model.setPlaneswalker(planeswalker);
            model.saveData();
        }
        canCloseCallback.run(true);
    }

    private void refreshPlaneswalkers() {
        ConquestData model = FModel.getConquest().getModel();
        lstPlaneswalkers.setListData(model.getSortedPlaneswalkers());
        lstPlaneswalkers.setSelectedItem(model.getPlaneswalker());
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
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        tokenDisplay.setBounds(x, y, w, (height - startY) * CardListPreview.CARD_PREVIEW_RATIO);
        y += tokenDisplay.getHeight() + PADDING;
        lstPlaneswalkers.setBounds(x, y, w, height - y - PADDING);
    }
}
