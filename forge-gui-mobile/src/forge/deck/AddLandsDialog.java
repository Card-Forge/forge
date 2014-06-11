/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.deck;

import forge.StaticData;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardEdition;
import forge.card.CardZoom;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FCardPanel;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.MyRandom;
import forge.util.Utils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;


public class AddLandsDialog extends FDialog {
    private static final float ADD_BTN_SIZE = Utils.AVG_FINGER_HEIGHT * 0.75f;
    private static final float LAND_PANEL_PADDING = Utils.scaleY(3);

    private final Deck deck;
    private final Callback<Boolean> callback;

    private final FLabel lblLandSet = add(new FLabel.Builder().text("Land Set:").font(FSkinFont.get(12)).textColor(FLabel.INLINE_LABEL_COLOR).build());
    private final FComboBox<CardEdition> cbLandSet = add(new FComboBox<CardEdition>(StaticData.instance().getEditions()));
    private final LandPanel pnlPlains = add(new LandPanel("Plains"));
    private final LandPanel pnlIsland = add(new LandPanel("Island"));
    private final LandPanel pnlSwamp = add(new LandPanel("Swamp"));
    private final LandPanel pnlMountain = add(new LandPanel("Mountain"));
    private final LandPanel pnlForest = add(new LandPanel("Forest"));
    private final FButton btnAuto  = add(new FButton("Auto"));
    private final FButton btnOK    = add(new FButton("OK"));
    private final FButton btnCancel = add(new FButton("Cancel"));

    private CardEdition landSet;

    public AddLandsDialog(Deck deck0, CardEdition defaultLandSet, final Callback<Boolean> callback0) {
        this(deck0, defaultLandSet, null, callback0);
    }
    public AddLandsDialog(Deck deck0, CardEdition defaultLandSet, CardPool restrictedCatalog0, final Callback<Boolean> callback0) {
        super("Add Lands to " + deck0.getName());

        deck = deck0;
        callback = callback0;

        cbLandSet.setFont(lblLandSet.getFont());
        cbLandSet.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                landSet = cbLandSet.getSelectedItem();
                pnlPlains.refreshArtChoices();
                pnlIsland.refreshArtChoices();
                pnlSwamp.refreshArtChoices();
                pnlMountain.refreshArtChoices();
                pnlForest.refreshArtChoices();
            }
        });
        cbLandSet.setSelectedItem(defaultLandSet);

        btnAuto.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                //TODO: Support automatically determining lands to add
            }
        });
        btnOK.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
                callback.run(true);
            }
        });
        btnCancel.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
                callback.run(false);
            }
        });
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;

        //layout land set combo box
        float comboBoxHeight = cbLandSet.getHeight();
        lblLandSet.setBounds(x, y, lblLandSet.getAutoSizeBounds().width, comboBoxHeight);
        cbLandSet.setBounds(x + lblLandSet.getWidth(), y, w - lblLandSet.getWidth(), comboBoxHeight);

        //layout card panels
        x = padding;
        y += comboBoxHeight + padding;
        float maxPanelHeight = (maxHeight - y - FOptionPane.BUTTON_HEIGHT - FOptionPane.GAP_BELOW_BUTTONS - 2 * padding) / 2;
        float panelExtraHeight = pnlPlains.cbLandArt.getHeight() + ADD_BTN_SIZE + 2 * LAND_PANEL_PADDING;
        float panelWidth = (w - 2 * padding) / 3;
        float panelHeight = panelWidth * FCardPanel.ASPECT_RATIO + panelExtraHeight;
        if (panelHeight > maxPanelHeight) { //ensure panels short enough that dialog doesn't exceed max height
            panelHeight = maxPanelHeight;
        }

        pnlPlains.setBounds(x, y, panelWidth, panelHeight);
        x += panelWidth + padding;
        pnlIsland.setBounds(x, y, panelWidth, panelHeight);
        x += panelWidth + padding;
        pnlSwamp.setBounds(x, y, panelWidth, panelHeight);

        x = (width - padding) / 2 - panelWidth;
        y += panelHeight + padding;
        pnlMountain.setBounds(x, y, panelWidth, panelHeight);
        x += panelWidth + padding;
        pnlForest.setBounds(x, y, panelWidth, panelHeight);

        //layout buttons
        x = padding;
        y += panelHeight + padding;
        float gapBetweenButtons = padding / 2;
        float buttonWidth = (w - gapBetweenButtons * 2) / 3;
        btnAuto.setBounds(x, y, buttonWidth, FOptionPane.BUTTON_HEIGHT);
        x += buttonWidth + gapBetweenButtons;
        btnOK.setBounds(x, y, buttonWidth, FOptionPane.BUTTON_HEIGHT);
        x += buttonWidth + gapBetweenButtons;
        btnCancel.setBounds(x, y, buttonWidth, FOptionPane.BUTTON_HEIGHT);

        return y + FOptionPane.BUTTON_HEIGHT + FOptionPane.GAP_BELOW_BUTTONS;
    }

    private class LandPanel extends FContainer {
        private final LandCardPanel cardPanel;
        private final FLabel lblCount, btnSubtract, btnAdd;
        private final FComboBox<String> cbLandArt;
        private final String cardName;
        private PaperCard card;
        private int count, maxCount, artChoiceCount;

        private LandPanel(String cardName0) {
            cardName = cardName0;
            cardPanel = add(new LandCardPanel());
            cbLandArt = add(new FComboBox<String>());
            cbLandSet.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    int artIndex = cbLandArt.getSelectedIndex() - 1;
                    if (artIndex < -1) { return; }
                    if (artIndex == -1) { //use a random art index if "Assorted Art" selected
                        artIndex = MyRandom.getRandom().nextInt(artChoiceCount);
                    }
                    setCard(FModel.getMagicDb().getCommonCards().getCard(cardName, landSet.getCode(), artIndex));
                }
            });
            lblCount = add(new FLabel.Builder().text("0").font(FSkinFont.get(18)).align(HAlignment.CENTER).build());
            btnSubtract = add(new FLabel.ButtonBuilder().icon(FSkinImage.MINUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (count > 0) {
                        count--;
                        lblCount.setText(String.valueOf(count));
                    }
                }
            }).build());
            btnAdd = add(new FLabel.ButtonBuilder().icon(FSkinImage.PLUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (maxCount == 0 || count < maxCount) {
                        count++;
                        lblCount.setText(String.valueOf(count));
                    }
                }
            }).build());
        }

        private void refreshArtChoices() {
            cbLandArt.clearItems();
            if (landSet == null) { return; }

            artChoiceCount = FModel.getMagicDb().getCommonCards().getArtCount(cardName, landSet.getCode());
            cbLandArt.addItem("Assorted Art");
            for (int i = 1; i <= artChoiceCount; i++) {
                cbLandArt.addItem("Card Art " + i);
            }
        }

        private PaperCard getCard() {
            return card;
        }
        private void setCard(PaperCard card0) {
            if (card == card0) { return; }
            card = card0;
            cardPanel.setCard(Card.getCardForUi(card));
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = height - ADD_BTN_SIZE;
            btnSubtract.setBounds(0, y, ADD_BTN_SIZE, ADD_BTN_SIZE);
            lblCount.setBounds(ADD_BTN_SIZE, y, width - 2 * ADD_BTN_SIZE, ADD_BTN_SIZE);
            btnAdd.setBounds(width - ADD_BTN_SIZE, y, ADD_BTN_SIZE, ADD_BTN_SIZE);

            y -= cbLandArt.getHeight() + LAND_PANEL_PADDING;
            cbLandArt.setBounds(0, y, width, cbLandArt.getHeight());

            float cardPanelHeight = y - LAND_PANEL_PADDING;
            float cardPanelWidth = cardPanelHeight / FCardPanel.ASPECT_RATIO;
            cardPanel.setBounds(0, 0, cardPanelWidth, cardPanelHeight);
        }

        private class LandCardPanel extends FCardPanel {
            private LandCardPanel() {
                super(null);
            }

            @Override
            public boolean tap(float x, float y, int count) {
                if (getCard() == null) { return false; }
                CardZoom.show(getCard());
                return true;
            }

            @Override
            public boolean longPress(float x, float y) {
                if (getCard() == null) { return false; }
                CardZoom.show(getCard());
                return true;
            }

            @Override
            protected float getPadding() {
                return 0;
            }
        }
    }
}
