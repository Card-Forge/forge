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
package forge.screens.deckeditor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import forge.ImageCache;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckgenUtil;
import forge.gui.UiCommand;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FComboBox;
import forge.toolbox.FComboBoxPanel;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextField;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.view.arcane.CardPanel;


@SuppressWarnings("serial")
public class AddBasicLandsDialog {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    private static final int ADD_BTN_SIZE = 22;
    private static final int LAND_PANEL_PADDING = 3;

    private final FComboBoxPanel<CardEdition> cbLandSet = new FComboBoxPanel<>(Localizer.getInstance().getMessage("lblLandSet") + ":", FlowLayout.CENTER,
            Iterables.filter(StaticData.instance().getSortedEditions(), CardEdition.Predicates.hasBasicLands));

    private final MainPanel panel = new MainPanel();
    private final LandPanel pnlPlains = new LandPanel("Plains");
    private final LandPanel pnlIsland = new LandPanel("Island");
    private final LandPanel pnlSwamp = new LandPanel("Swamp");
    private final LandPanel pnlMountain = new LandPanel("Mountain");
    private final LandPanel pnlForest = new LandPanel("Forest");

    private final FHtmlViewer lblDeckInfo = new FHtmlViewer();

    private final Deck deck;

    private FOptionPane optionPane;
    private int nonLandCount, oldLandCount;
    private CardEdition landSet;

    public AddBasicLandsDialog(Deck deck0) {
        this(deck0, null);
    }
    public AddBasicLandsDialog(Deck deck0, CardEdition defaultLandSet) {
        deck = deck0;

        if (defaultLandSet == null)
            defaultLandSet = DeckProxy.getDefaultLandSet(deck);

        panel.setMinimumSize(new Dimension(WIDTH, HEIGHT));
        panel.add(cbLandSet);
        panel.add(pnlPlains);
        panel.add(pnlIsland);
        panel.add(pnlSwamp);
        panel.add(pnlMountain);
        panel.add(pnlForest);
        panel.add(lblDeckInfo);

        lblDeckInfo.setFont(FSkin.getRelativeFont(14));
        lblDeckInfo.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftDoubleClick(MouseEvent e) {
                Map<ManaCostShard, Integer> suggestionMap = DeckgenUtil.suggestBasicLandCount(deck);
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
        });
        lblDeckInfo.setToolTipText(Localizer.getInstance().getMessage("lblDeckStatisticsAutoSuggest"));

        cbLandSet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                landSet = cbLandSet.getSelectedItem();
                pnlPlains.refreshArtChoices();
                pnlIsland.refreshArtChoices();
                pnlSwamp.refreshArtChoices();
                pnlMountain.refreshArtChoices();
                pnlForest.refreshArtChoices();
            }
        });
        cbLandSet.setSelectedItem(defaultLandSet);

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

    public CardPool show() {
        optionPane = new FOptionPane(null, Localizer.getInstance().getMessage("lblAddBasicLands"), null, panel, ImmutableList.of(Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel")), 0);
        panel.revalidate();
        panel.repaint();
        optionPane.setVisible(true);

        int result = optionPane.getResult();

        optionPane.dispose();

        if (result == 0) {
            CardPool landsToAdd = new CardPool();
            pnlPlains.addToCardPool(landsToAdd);
            pnlIsland.addToCardPool(landsToAdd);
            pnlSwamp.addToCardPool(landsToAdd);
            pnlMountain.addToCardPool(landsToAdd);
            pnlForest.addToCardPool(landsToAdd);
            return landsToAdd;
        }
        return null;
    }

    private void updateDeckInfoLabel() {
        int newLandCount = pnlPlains.count + pnlIsland.count + pnlSwamp.count + pnlMountain.count + pnlForest.count;
        int totalSymbolCount = pnlPlains.symbolCount + pnlIsland.symbolCount + pnlSwamp.symbolCount
                + pnlMountain.symbolCount + pnlForest.symbolCount;
        int newTotalCount = nonLandCount + oldLandCount + newLandCount;
        if (totalSymbolCount == 0) {
            totalSymbolCount = 1; //prevent divide by 0 error
        }
        Localizer localizer = Localizer.getInstance();
        String infoLabel = String.format("" +
                        "<div 'text-align: center;'>%s</div>" +
                        "<div 'text-align: center;'> %s + %s + %s = %s</div>" +
                        "<div style='text-align: center;'>" +
                        "{W} %d (%d%%) | {U} %d (%d%%) ) | {B} %d (%d%%) | {R} %d (%d%%) | {G} %d (%d%%) </div>",
                            localizer.getMessage("lblMainDeck"),
                            String.format(localizer.getMessage("lblNonLandCount"), nonLandCount),
                            String.format(localizer.getMessage("lblOldLandCount"), oldLandCount),
                            String.format(localizer.getMessage("lblNewLandCount"), newLandCount),
                            String.format(localizer.getMessage("lblNewTotalCount"), newTotalCount),
                            pnlPlains.symbolCount, (pnlPlains.symbolCount * 100 / totalSymbolCount),
                            pnlIsland.symbolCount, (pnlIsland.symbolCount * 100 / totalSymbolCount),
                            pnlSwamp.symbolCount, (pnlSwamp.symbolCount * 100 / totalSymbolCount),
                            pnlMountain.symbolCount, (pnlMountain.symbolCount * 100 / totalSymbolCount),
                            pnlForest.symbolCount, (pnlForest.symbolCount * 100 / totalSymbolCount)
                );
        lblDeckInfo.setText(FSkin.encodeSymbols(infoLabel, false));
    }

    private class MainPanel extends SkinnedPanel {
        private MainPanel() {
            super(null);
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int padding = 10;
            int x = padding;
            int y = padding;
            int w = getWidth() - 2 * padding;

            //layout land set combo box
            int comboBoxHeight = FTextField.HEIGHT;
            cbLandSet.setBounds(x, y, w, comboBoxHeight);

            //layout card panel scroller
            y += comboBoxHeight + padding;
            int panelExtraHeight = pnlPlains.cbLandArt.getHeight() + ADD_BTN_SIZE + 2 * LAND_PANEL_PADDING;
            int panelWidth = (getWidth() - 6 * padding) / 5;
            int panelHeight = Math.round((float)panelWidth * CardPanel.ASPECT_RATIO) + panelExtraHeight;

            pnlPlains.setBounds(x, y, panelWidth, panelHeight);
            x += panelWidth + padding;
            pnlIsland.setBounds(x, y, panelWidth, panelHeight);
            x += panelWidth + padding;
            pnlSwamp.setBounds(x, y, panelWidth, panelHeight);
            x += panelWidth + padding;
            pnlMountain.setBounds(x, y, panelWidth, panelHeight);
            x += panelWidth + padding;
            pnlForest.setBounds(x, y, panelWidth, panelHeight);

            //layout info label
            x = padding;
            y += panelHeight + padding;
            lblDeckInfo.setBounds(x, y, w, getHeight() - y - padding);
        }
    }

    private class LandPanel extends SkinnedPanel {
        private final LandCardPanel cardPanel;
        private final FLabel lblCount, btnSubtract, btnAdd;
        private final FComboBox<String> cbLandArt;
        private final String cardName;
        private PaperCard card;
        private int count, maxCount;
        private int symbolCount;

        private LandPanel(String cardName0) {
            super(null);
            setOpaque(false);

            cardName = cardName0;
            cardPanel = new LandCardPanel();
            cbLandArt = new FComboBox<>();
            cbLandArt.setFont(FSkin.getFont());
            cbLandArt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    int artIndex = cbLandArt.getSelectedIndex();
                    if (artIndex < 0) { return; }
                    card = generateCard(artIndex); //generate card for display
                    cardPanel.repaint();
                }
            });
            lblCount = new FLabel.Builder().text("0").fontSize(22).fontAlign(SwingConstants.CENTER).build();
            btnSubtract = new FLabel.ButtonBuilder().icon(FSkin.getIcon(FSkinProp.ICO_MINUS)).cmdClick(new UiCommand() {
                @Override
                public void run() {
                    if (count > 0) {
                        count--;
                        lblCount.setText(String.valueOf(count));
                        updateDeckInfoLabel();
                    }
                }
            }).build();
            btnAdd = new FLabel.ButtonBuilder().icon(FSkin.getIcon(FSkinProp.ICO_PLUS)).cmdClick(new UiCommand() {
                @Override
                public void run() {
                    if (maxCount == 0 || count < maxCount) {
                        count++;
                        lblCount.setText(String.valueOf(count));
                        updateDeckInfoLabel();
                    }
                }
            }).build();
            btnSubtract.getAccessibleContext().setAccessibleName("Remove " + cardName);
            btnAdd.getAccessibleContext().setAccessibleName("Add " + cardName);
            add(cardPanel);
            add(cbLandArt);
            add(lblCount);
            add(btnSubtract);
            add(btnAdd);
        }

        private void addToCardPool(CardPool pool) {
            if (count == 0) { return; }
            int artIndex = cbLandArt.getSelectedIndex();
            if (artIndex < 0) { return; }

            if (artIndex > 0 && card != null) {
                pool.add(card, count); //simplify things if art index specified
            }
            else {
                int artChoiceCount = FModel.getMagicDb().getCommonCards().getArtCount(cardName, landSet.getCode());
                for (int i = 0; i < count; i++) {
                    int rndArtIndex = MyRandom.getRandom().nextInt(artChoiceCount) + 1;
                    pool.add(generateCard(rndArtIndex));

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
        public void doLayout() {
            int width = getWidth();
            int height = getHeight();
            int y = height - ADD_BTN_SIZE;
            int buttonWidth = ADD_BTN_SIZE;
            int labelWidth = width - 2 * ADD_BTN_SIZE;
            int minLabelWidth = lblCount.getSkin().getFont().measureTextWidth(JOptionPane.getRootFrame().getGraphics(), "0") + 2 * lblCount.getInsets().left;
            if (labelWidth < minLabelWidth) { //ensure count label has enough room for display a single digit count at normal font size
                labelWidth = minLabelWidth;
                buttonWidth = (width - labelWidth) / 2;
            }
            btnAdd.setBounds(0, y, buttonWidth, ADD_BTN_SIZE);
            lblCount.setBounds(buttonWidth, y, labelWidth, ADD_BTN_SIZE);
            btnSubtract.setBounds(width - buttonWidth, y, buttonWidth, ADD_BTN_SIZE);

            y -= FTextField.HEIGHT + LAND_PANEL_PADDING;
            cbLandArt.setBounds(0, y, width, FTextField.HEIGHT);

            int cardPanelHeight = y - LAND_PANEL_PADDING;
            int cardPanelWidth = Math.round((float)cardPanelHeight / CardPanel.ASPECT_RATIO);
            cardPanel.setBounds((width - cardPanelWidth) / 2, 0, cardPanelWidth, cardPanelHeight);
        }

        private class LandCardPanel extends SkinnedPanel {
            private LandCardPanel() {
                super(null);
                setOpaque(false);
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (card == null) { return; }

                final Graphics2D g2d = (Graphics2D) g;

                int width = getWidth();
                int height = getHeight();

                final BufferedImage img = ImageCache.getImage(card, width, height);
                if (img != null) {
                    g2d.drawImage(img, null, (width - img.getWidth()) / 2, (height - img.getHeight()) / 2);
                }
            }
        }
    }
}
