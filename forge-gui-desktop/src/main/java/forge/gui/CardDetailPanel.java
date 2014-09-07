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

package forge.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardDetailUtil;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.CardEdition;
import forge.item.IPaperCard;
import forge.item.InventoryItemFromSet;
import forge.model.FModel;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.view.CardView;
import forge.view.CardView.CardStateView;
import forge.view.ViewUtil;

/**
 * The class CardDetailPanel. Shows the details of a card.
 * 
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public class CardDetailPanel extends SkinnedPanel {
    /** Constant <code>serialVersionUID=-8461473263764812323L</code>. */
    private static final long serialVersionUID = -8461473263764812323L;

    private static Color fromDetailColor(DetailColors detailColor) {
        return new Color(detailColor.r, detailColor.g, detailColor.b);
    }

    private final FLabel nameCostLabel;
    private final FLabel typeLabel;
    private final FLabel powerToughnessLabel;
    private final FLabel idLabel;
    private final JLabel setInfoLabel;
    private final FHtmlViewer cdArea;
    private final FScrollPane scrArea;

    public CardDetailPanel() {
        super();
        this.setLayout(null);
        this.setOpaque(false);

        this.nameCostLabel = new FLabel.Builder().fontAlign(SwingConstants.CENTER).build();
        this.typeLabel = new FLabel.Builder().fontAlign(SwingConstants.CENTER).build();
        this.idLabel = new FLabel.Builder().fontAlign(SwingConstants.LEFT).tooltip("Card ID").build();
        this.powerToughnessLabel = new FLabel.Builder().fontAlign(SwingConstants.CENTER).build();
        this.setInfoLabel = new JLabel();
        this.setInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        final Font font = new Font("Dialog", 0, 14);
        this.nameCostLabel.setFont(font);
        this.typeLabel.setFont(font);
        this.idLabel.setFont(font);
        this.powerToughnessLabel.setFont(font);

        this.cdArea = new FHtmlViewer();
        this.cdArea.setBorder(new EmptyBorder(2, 6, 2, 6));
        this.cdArea.setOpaque(false);
        this.scrArea = new FScrollPane(this.cdArea, false);

        this.add(this.nameCostLabel);
        this.add(this.typeLabel);
        this.add(this.idLabel);
        this.add(this.powerToughnessLabel);
        this.add(this.setInfoLabel);
        this.add(this.scrArea);
    }

    @Override
    public void doLayout() {
    	int insets = 3;
    	int setInfoWidth = 40;
    	int x = insets;
    	int y = insets;
    	int lineWidth = getWidth() - 2 * insets;
    	int lineHeight = this.nameCostLabel.getPreferredSize().height;
    	int dy = lineHeight + 1;

    	this.nameCostLabel.setBounds(x, y, lineWidth, lineHeight);
    	y += dy;

    	this.typeLabel.setBounds(x, y, lineWidth, lineHeight);
    	y += dy;

    	this.idLabel.setBounds(x, y, this.idLabel.getAutoSizeWidth(), lineHeight);
    	this.powerToughnessLabel.setBounds(x, y, lineWidth, lineHeight);

    	//+1 to x,y so set info label right up against border and the baseline matches ID and P/T
    	this.setInfoLabel.setBounds(x + lineWidth - setInfoWidth + 1, y + 1, setInfoWidth, lineHeight);
    	y += dy;

    	this.scrArea.setBounds(0, y, getWidth(), getHeight() - y);
    }

    public final void setItem(final InventoryItemFromSet item) {
        nameCostLabel.setText(item.getName());
        typeLabel.setVisible(false);
        powerToughnessLabel.setVisible(false);
        idLabel.setText("");
        cdArea.setText(CardDetailUtil.getItemDescription(item));
        this.updateBorder(item instanceof IPaperCard ? ViewUtil.getCardForUi((IPaperCard)item).getOriginal() : null, false);

        String set = item.getEdition();
        setInfoLabel.setText(set);
        setInfoLabel.setToolTipText("");
        if (StringUtils.isEmpty(set)) {
            setInfoLabel.setOpaque(false);
            setInfoLabel.setBorder(null);
        } else {
            CardEdition edition = FModel.getMagicDb().getEditions().get(set);
            if (null != edition) {
                setInfoLabel.setToolTipText(edition.getName());
            }
            
            this.setInfoLabel.setOpaque(true);
            this.setInfoLabel.setBackground(Color.BLACK);
            this.setInfoLabel.setForeground(Color.WHITE);
            this.setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrArea.getVerticalScrollBar().setValue(scrArea.getVerticalScrollBar().getMinimum());
            }
        });
    }

    public final void setCard(final CardView card) {
        this.setCard(card, false);
    }

    public final void setCard(final CardView card, final boolean isInAltState) {
        this.nameCostLabel.setText("");
        this.typeLabel.setVisible(true);
        this.typeLabel.setText("");
        this.powerToughnessLabel.setVisible(true);
        this.powerToughnessLabel.setText("");
        this.idLabel.setText("");
        this.setInfoLabel.setText("");
        this.setInfoLabel.setToolTipText("");
        this.setInfoLabel.setOpaque(false);
        this.setInfoLabel.setBorder(null);
        this.cdArea.setText("");
        if (card == null) {
            this.updateBorder((CardStateView)null, false);
            return;
        }

        final CardStateView state = card.getState(isInAltState);
        //this.setCard(card.card);

        boolean canShowThis = false;

        //if (Singletons.getControl().mayShowCard(card) || FDialog.isModalOpen()) { //allow showing cards while modal open to account for revealing, picking, and ordering cards
        canShowThis = true;

        if (state.getManaCost().isNoCost()) {
            this.nameCostLabel.setText(state.getName());
        } else {
            String manaCost = state.getManaCost().toString();
            //if (state.isSplitCard() && card.getCurState() == CardCharacteristicName.Original) {
            //    manaCost = card.getRules().getMainPart().getManaCost().toString() + " // " + card.getRules().getOtherPart().getManaCost().toString();
            //}
            this.nameCostLabel.setText(FSkin.encodeSymbols(state.getName() + " - " + manaCost, true));
        }
        this.typeLabel.setText(CardDetailUtil.formatCardType(state));

        String set = card.getSetCode();
        this.setInfoLabel.setText(set);
        if (null != set && !set.isEmpty()) {
            CardEdition edition = FModel.getMagicDb().getEditions().get(set);
            if (null == edition) {
                setInfoLabel.setToolTipText(card.getRarity().name());
            }
            else {
                setInfoLabel.setToolTipText(String.format("%s (%s)", edition.getName(), card.getRarity().name()));
            }

            this.setInfoLabel.setOpaque(true);

            Color backColor;
            switch(card.getRarity()) {
            case Uncommon:
                backColor = fromDetailColor(DetailColors.UNCOMMON);
                break;

            case Rare:
                backColor = fromDetailColor(DetailColors.RARE);
                break;

            case MythicRare:
                backColor = fromDetailColor(DetailColors.MYTHIC);
                break; 

            case Special: //"Timeshifted" or other Special Rarity Cards
                backColor = fromDetailColor(DetailColors.SPECIAL);
                break;

            default: //case BasicLand: + case Common:
                backColor = fromDetailColor(DetailColors.COMMON);
                break;
            }

            Color foreColor = FSkin.getHighContrastColor(backColor);
            this.setInfoLabel.setBackground(backColor);
            this.setInfoLabel.setForeground(foreColor);
            this.setInfoLabel.setBorder(BorderFactory.createLineBorder(foreColor));
        }

        this.updateBorder(state, canShowThis);

        this.powerToughnessLabel.setText(CardDetailUtil.formatPowerToughness(state));

        this.idLabel.setText(CardDetailUtil.formatCardId(state));

        // fill the card text
        this.cdArea.setText(FSkin.encodeSymbols(CardDetailUtil.composeCardText(state, canShowThis), true));

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrArea.getVerticalScrollBar().setValue(scrArea.getVerticalScrollBar().getMinimum());
            }
        });
    }

    /** @return FLabel */
    public FLabel getNameCostLabel() {
        return this.nameCostLabel;
    }

    /** @return FLabel */
    public FLabel getTypeLabel() {
        return this.typeLabel;
    }

    /** @return FLabel */
    public FLabel getPowerToughnessLabel() {
        return this.powerToughnessLabel;
    }

    /** @return JLabel */
    public JLabel getSetInfoLabel() {
        return this.setInfoLabel;
    }

    /** @return FHtmlViewer */
    public FHtmlViewer getCDArea() {
        return this.cdArea;
    }

    private void updateBorder(final CardStateView card, final boolean canShow) {
        // color info
        if (card == null) {
            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            scrArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            return;
        }

        Color color = fromDetailColor(CardDetailUtil.getBorderColor(card, canShow));
        this.setBorder(BorderFactory.createLineBorder(color, 2));
        scrArea.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, color));
    }

    //ensure mouse listener hooked up to all certain opaque child components so it can get raised properly
    @Override
    public void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
        setInfoLabel.addMouseListener(l);
        cdArea.addMouseListener(l);
    }
}
