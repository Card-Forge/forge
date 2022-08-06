package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FList;

public class ConquestPlaneswalkScreen extends FScreen {
    private static final float PADDING = FList.PADDING;

    private final ConquestPlaneSelector planeSelector = add(new ConquestPlaneSelector() {
        @Override
        protected void onSelectionChange() {
            if (btnPlaneswalk == null) { return; }
            btnPlaneswalk.updateCaption();
        }
    });
    private final PlaneswalkButton btnPlaneswalk = add(new PlaneswalkButton());

    public ConquestPlaneswalkScreen() {
        super("", ConquestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        ConquestData model = FModel.getConquest().getModel();
        setHeaderCaption(model.getName());
        planeSelector.updateReachablePlanes();
        planeSelector.setCurrentPlane(model.getCurrentPlane());
        planeSelector.activate();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        planeSelector.setBounds(0, startY, width, height - startY);
        float btnMod = Forge.extrawide.equals("extrawide") ? 1.5f : 2.5f;
        float buttonWidth = width * 0.6f;
        float buttonHeight = btnPlaneswalk.getFont().getCapHeight() * btnMod;
        btnPlaneswalk.setBounds((width - buttonWidth) / 2, height - buttonHeight - PADDING, buttonWidth, buttonHeight);
    }

    private class PlaneswalkButton extends FLabel {
        private int unlockCost;

        private PlaneswalkButton() {
            super(new FLabel.Builder().font(FSkinFont.get(20)).parseSymbols().pressedColor(ConquestAEtherScreen.FILTER_BUTTON_PRESSED_COLOR)
                    .textColor(ConquestAEtherScreen.FILTER_BUTTON_TEXT_COLOR).alphaComposite(1f).align(Align.center));
            setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    ConquestData model = FModel.getConquest().getModel();
                    ConquestPlane selectedPlane = planeSelector.getSelectedPlane();
                    if (model.isPlaneUnlocked(selectedPlane)) {
                        model.planeswalkTo(selectedPlane);
                        model.saveData();
                        Forge.back();
                    }
                    else if (model.spendPlaneswalkEmblems(unlockCost)) {
                        model.unlockPlane(selectedPlane);
                        model.saveData();
                        updateCaption();
                    }
                }
            });
        }

        public void updateCaption() {
            ConquestData model = FModel.getConquest().getModel();
            ConquestPlane selectedPlane = planeSelector.getSelectedPlane();
            if (model.isPlaneUnlocked(selectedPlane)) {
                unlockCost = 0;
                setText("Planeswalk");
            }
            else {
                ConquestPreferences prefs = FModel.getConquestPreferences();
                unlockCost = prefs.getPrefInt(CQPref.PLANESWALK_FIRST_UNLOCK) + prefs.getPrefInt(CQPref.PLANESWALK_UNLOCK_INCREASE) * (model.getUnlockedPlaneCount() - 1);

                int emblems = model.getPlaneswalkEmblems();
                String message = "Unlock {PW}";
                if (emblems < unlockCost) {
                    message += TextRenderer.startColor(Color.RED) + emblems + TextRenderer.endColor();
                }
                else {
                    message += emblems;
                }
                message += "/" + unlockCost;
                setText(message);
            }
        }

        @Override
        protected void drawContent(Graphics g, float w, float h, final boolean pressed) {
            if (!pressed) {
                g.fillRect(ConquestAEtherScreen.FILTER_BUTTON_COLOR, 0, 0, w, h);
            }
            super.drawContent(g, w, h, pressed);
        }
    }
}
