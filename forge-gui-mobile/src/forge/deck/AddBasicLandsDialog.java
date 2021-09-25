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

import java.text.NumberFormat;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Iterables;

import forge.Forge;
import forge.Graphics;
import forge.StaticData;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardEdition;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardRules;
import forge.card.CardZoom;
import forge.card.mana.ManaCostShard;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FDialog;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.Utils;


public class AddBasicLandsDialog extends FDialog {
    private static final float ADD_BTN_SIZE = Utils.AVG_FINGER_HEIGHT * 0.75f;
    private static final float LAND_PANEL_PADDING = Utils.scale(3);

    private final Deck currentDeck;

    private final Callback<CardPool> callback;

    private final FLabel lblLandSet = add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblLandSet") + ":").font(FSkinFont.get(12)).textColor(FLabel.INLINE_LABEL_COLOR).build());
    private final FComboBox<CardEdition> cbLandSet = add(new FComboBox<>(Iterables.filter(StaticData.instance().getEditions(), CardEdition.Predicates.hasBasicLands)));

    private final FScrollPane scroller = add(new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float padding = FOptionPane.PADDING;
            float x = padding;
            float totalWidth = Forge.isLandscapeMode() ? visibleWidth : 2 * visibleWidth - ADD_BTN_SIZE;
            float panelWidth = (totalWidth - 6 * padding) / 5;

            pnlPlains.setBounds(x, 0, panelWidth, visibleHeight);
            x += panelWidth + padding;
            pnlIsland.setBounds(x, 0, panelWidth, visibleHeight);
            x += panelWidth + padding;
            pnlSwamp.setBounds(x, 0, panelWidth, visibleHeight);
            x += panelWidth + padding;
            pnlMountain.setBounds(x, 0, panelWidth, visibleHeight);
            x += panelWidth + padding;
            pnlForest.setBounds(x, 0, panelWidth, visibleHeight);

            return new ScrollBounds(totalWidth, visibleHeight);
        }
    });
    private final LandPanel pnlPlains = scroller.add(new LandPanel("Plains"));
    private final LandPanel pnlIsland = scroller.add(new LandPanel("Island"));
    private final LandPanel pnlSwamp = scroller.add(new LandPanel("Swamp"));
    private final LandPanel pnlMountain = scroller.add(new LandPanel("Mountain"));
    private final LandPanel pnlForest = scroller.add(new LandPanel("Forest"));

    private final FTextArea lblDeckInfo = add(new FTextArea(true) {
        @Override
        public boolean tap(float x, float y, int count) {
            if (count == 2) {
                Map<ManaCostShard, Integer> suggestionMap = DeckgenUtil.suggestBasicLandCount(currentDeck);
                pnlPlains.count = suggestionMap.get(ManaCostShard.WHITE);
                pnlIsland.count = suggestionMap.get(ManaCostShard.BLUE);
                pnlSwamp.count = suggestionMap.get(ManaCostShard.BLACK);
                pnlMountain.count = suggestionMap.get(ManaCostShard.RED);
                pnlForest.count = suggestionMap.get(ManaCostShard.GREEN);

                pnlPlains.lblCount.setText(String.valueOf(pnlPlains.count));
                pnlIsland.lblCount.setText(String.valueOf(pnlIsland.count));
                pnlSwamp.lblCount.setText(String.valueOf(pnlSwamp.count));
                pnlMountain.lblCount.setText(String.valueOf(pnlMountain.count));
                pnlForest.lblCount.setText(String.valueOf(pnlForest.count));
                
                updateDeckInfoLabel();
            }
            return true;
        }
    });

    private int nonLandCount, oldLandCount;
    private CardEdition landSet;

    public AddBasicLandsDialog(Deck deck, CardEdition defaultLandSet, final Callback<CardPool> callback0) {
        super(Localizer.getInstance().getMessage("lblAddBasicLandsAutoSuggest").replace("%s", deck.getName()), 2);

        callback = callback0;
        currentDeck = deck;

        lblDeckInfo.setAlignment(Align.center);
        lblDeckInfo.setFont(FSkinFont.get(12));

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

        initButton(0, Localizer.getInstance().getMessage("lblOK"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                CardPool landsToAdd = new CardPool();
                pnlPlains.addToCardPool(landsToAdd);
                pnlIsland.addToCardPool(landsToAdd);
                pnlSwamp.addToCardPool(landsToAdd);
                pnlMountain.addToCardPool(landsToAdd);
                pnlForest.addToCardPool(landsToAdd);

                hide();

                if (landsToAdd.countAll() > 0) {
                    callback.run(landsToAdd);
                }
            }
        });
        initButton(1, Localizer.getInstance().getMessage("lblCancel"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });

        //initialize land counts based on current deck contents
        int halfCountW = 0; //track half shard count for each color to add to symbol count only if a full symbol is also found
        int halfCountU = 0;
        int halfCountB = 0;
        int halfCountR = 0;
        int halfCountG = 0;
        for (Entry<PaperCard, Integer> entry : deck.getMain()) {
            CardRules cardRules = entry.getKey().getRules();
            int count = entry.getValue();
            if (cardRules.getType().isLand()) {
                oldLandCount += count;
            }
            else {
                nonLandCount += count;

                for (ManaCostShard shard : cardRules.getManaCost()) {
                    boolean isMonoColor = shard.isMonoColor();
                    if (shard.isWhite()) {
                        if (isMonoColor) {
                            pnlPlains.symbolCount += count;
                            continue;
                        }
                        halfCountW += count;
                    }
                    if (shard.isBlue()) {
                        if (isMonoColor) {
                            pnlIsland.symbolCount += count;
                            continue;
                        }
                        halfCountU += count;
                    }
                    if (shard.isBlack()) {
                        if (isMonoColor) {
                            pnlSwamp.symbolCount += count;
                            continue;
                        }
                        halfCountB += count;
                    }
                    if (shard.isRed()) {
                        if (isMonoColor) {
                            pnlMountain.symbolCount += count;
                            continue;
                        }
                        halfCountR += count;
                    }
                    if (shard.isGreen()) {
                        if (isMonoColor) {
                            pnlForest.symbolCount += count;
                            continue;
                        }
                        halfCountG += count;
                    }
                }
            }

            //only account for half shards if full shards exist for a given color
            if (pnlPlains.symbolCount > 0 && halfCountW > 0) {
                pnlPlains.symbolCount += halfCountW * 0.5;
            }
            if (pnlIsland.symbolCount > 0 && halfCountU > 0) {
                pnlIsland.symbolCount += halfCountU * 0.5;
            }
            if (pnlSwamp.symbolCount > 0 && halfCountB > 0) {
                pnlSwamp.symbolCount += halfCountB * 0.5;
            }
            if (pnlMountain.symbolCount > 0 && halfCountR > 0) {
                pnlMountain.symbolCount += halfCountR * 0.5;
            }
            if (pnlForest.symbolCount > 0 && halfCountG > 0) {
                pnlForest.symbolCount += halfCountG * 0.5;
            }
        }

        updateDeckInfoLabel();
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

        //layout card panel scroller
        y += comboBoxHeight + padding;
        float panelExtraHeight = pnlPlains.cbLandArt.getHeight() + ADD_BTN_SIZE + 2 * LAND_PANEL_PADDING;
        float panelWidth;
        if (Forge.isLandscapeMode()) {
            panelWidth = (width - 6 * padding) / 5;
        }
        else {
            panelWidth = (2 * width - ADD_BTN_SIZE - 6 * padding) / 5;
        }
        float panelHeight = panelWidth * FCardPanel.ASPECT_RATIO + panelExtraHeight;
        scroller.setBounds(0, y, width, panelHeight);

        //adjust scroll based on prevalent colors in deck
        if (pnlMountain.symbolCount + pnlForest.symbolCount > pnlPlains.symbolCount + pnlIsland.symbolCount) {
            scroller.scrollToRight();
        }
        else {
            scroller.scrollToLeft();
        }

        //layout info label
        y += panelHeight + padding;
        lblDeckInfo.setBounds(x, y, w, lblDeckInfo.getPreferredHeight(w));

        return y + lblDeckInfo.getHeight() + padding;
    }

    private void updateDeckInfoLabel() {
        NumberFormat integer = NumberFormat.getIntegerInstance();
        NumberFormat percent = NumberFormat.getPercentInstance();
        int newLandCount = pnlPlains.count + pnlIsland.count + pnlSwamp.count + pnlMountain.count + pnlForest.count;
        double totalSymbolCount = pnlPlains.symbolCount + pnlIsland.symbolCount + pnlSwamp.symbolCount + pnlMountain.symbolCount + pnlForest.symbolCount;
        if (totalSymbolCount == 0) {
            totalSymbolCount = 1; //prevent divide by 0 error
        }
        int newTotalCount = nonLandCount + oldLandCount + newLandCount;
        lblDeckInfo.setText(
                String.format(Localizer.getInstance().getMessage("lblNonLandCount"), nonLandCount) + " + " +
                String.format(Localizer.getInstance().getMessage("lblOldLandCount"), oldLandCount) + " + " +
                String.format(Localizer.getInstance().getMessage("lblNewLandCount"), newLandCount) + " = " +
                String.format(Localizer.getInstance().getMessage("lblNewTotalCount"), newTotalCount) + "\n" +
                "{W} " + integer.format(pnlPlains.symbolCount) + " (" + percent.format(pnlPlains.symbolCount / totalSymbolCount) + ") | " +
                "{U} " + integer.format(pnlIsland.symbolCount) + " (" + percent.format(pnlIsland.symbolCount / totalSymbolCount) + ") | " +
                "{B} " + integer.format(pnlSwamp.symbolCount) + " (" + percent.format(pnlSwamp.symbolCount / totalSymbolCount) + ") | " +
                "{R} " + integer.format(pnlMountain.symbolCount) + " (" + percent.format(pnlMountain.symbolCount / totalSymbolCount) + ") | " +
                "{G} " + integer.format(pnlForest.symbolCount) + " (" + percent.format(pnlForest.symbolCount / totalSymbolCount) + ")");
    }

    private class LandPanel extends FContainer {
        private final LandCardPanel cardPanel;
        private final FLabel lblCount, btnSubtract, btnAdd;
        private final FComboBox<String> cbLandArt;
        private final String cardName;
        private PaperCard card;
        private int count, maxCount;
        private double symbolCount;

        private LandPanel(String cardName0) {
            cardName = cardName0;
            cardPanel = add(new LandCardPanel());
            cbLandArt = add(new FComboBox<>());
            cbLandArt.setFont(cbLandSet.getFont());
            cbLandArt.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    int artIndex = cbLandArt.getSelectedIndex();
                    if (artIndex < 0) { return; }
                    card = generateCard(artIndex); //generate card for display
                }
            });
            lblCount = add(new FLabel.Builder().text("0").font(FSkinFont.get(18)).align(Align.center).build());
            btnSubtract = add(new FLabel.ButtonBuilder().icon(Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (count > 0) {
                        count--;
                        lblCount.setText(String.valueOf(count));
                        updateDeckInfoLabel();
                    }
                }
            }).build());
            btnAdd = add(new FLabel.ButtonBuilder().icon(Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (maxCount == 0 || count < maxCount) {
                        count++;
                        lblCount.setText(String.valueOf(count));
                        updateDeckInfoLabel();
                    }
                }
            }).build());
        }

        private void addToCardPool(CardPool pool) {
            if (count == 0) { return; }
            int artIndex = cbLandArt.getSelectedIndex();
            if (artIndex < 0) { return; }

            if (artIndex > 0 && card != null) {
                pool.add(card, count); //simplify things if art index specified
            }
            else {
                for (int i = 0; i < count; i++) {
                    pool.add(generateCard(MyRandom.getRandom().nextInt(cbLandArt.getItemCount())));
                }
            }
        }

        private PaperCard generateCard(int artIndex) {
            PaperCard c = FModel.getMagicDb().getCommonCards().getCard(cardName, landSet.getCode(), artIndex);
            if (c == null) {
                //if can't find land for this set, fall back to Zendikar lands
                c = FModel.getMagicDb().getCommonCards().getCard(cardName, "ZEN");
            }
            return c;
        }

        private void refreshArtChoices() {
            cbLandArt.removeAllItems();
            if (landSet == null) { return; }

            int artChoiceCount = FModel.getMagicDb().getCommonCards().getArtCount(cardName, landSet.getCode());
            cbLandArt.addItem(Localizer.getInstance().getMessage("lblAssortedArt"));
            for (int i = 1; i <= artChoiceCount; i++) {
                cbLandArt.addItem(Localizer.getInstance().getMessage("lblCardArtN", String.valueOf(i)));
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = height - ADD_BTN_SIZE;
            float buttonWidth = ADD_BTN_SIZE;
            float labelWidth = width - 2 * ADD_BTN_SIZE;
            float minLabelWidth = lblCount.getFont().getBounds("0").width + 2 * lblCount.getInsets().x;
            if (labelWidth < minLabelWidth) { //ensure count label has enough room for display a single digit count at normal font size
                labelWidth = minLabelWidth;
                buttonWidth = (width - labelWidth) / 2;
            }
            btnSubtract.setBounds(0, y, buttonWidth, ADD_BTN_SIZE);
            lblCount.setBounds(buttonWidth, y, labelWidth, ADD_BTN_SIZE);
            btnAdd.setBounds(width - buttonWidth, y, buttonWidth, ADD_BTN_SIZE);

            y -= cbLandArt.getHeight() + LAND_PANEL_PADDING;
            cbLandArt.setBounds(0, y, width, cbLandArt.getHeight());

            float cardPanelHeight = y - LAND_PANEL_PADDING;
            float cardPanelWidth = cardPanelHeight / FCardPanel.ASPECT_RATIO;
            cardPanel.setBounds((width - cardPanelWidth) / 2, 0, cardPanelWidth, cardPanelHeight);
        }

        private class LandCardPanel extends FDisplayObject {
            private LandCardPanel() {
            }

            @Override
            public boolean tap(float x, float y, int count) {
                if (card == null) { return false; }
                CardZoom.show(card);
                return true;
            }

            @Override
            public boolean longPress(float x, float y) {
                if (card == null) { return false; }
                CardZoom.show(card);
                return true;
            }

            @Override
            public void draw(Graphics g) {
                if (card == null) { return; }
                CardRenderer.drawCard(g, card, 0, 0, getWidth(), getHeight(), CardStackPosition.Top);
            }
        }
    }
}
