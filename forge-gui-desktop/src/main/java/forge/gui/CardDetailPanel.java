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

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.item.IPaperCard;
import forge.item.InventoryItemFromSet;
import forge.model.FModel;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
/**
 * The class CardDetailPanel. Shows the details of a card.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public class CardDetailPanel extends SkinnedPanel {
    /** Constant <code>serialVersionUID=-8461473263764812323L</code>. */
    private static final long serialVersionUID = -8461473263764812323L;

    private static Color fromDetailColor(final DetailColors detailColor) {
        return new Color(detailColor.r, detailColor.g, detailColor.b);
    }

    private final FLabel nameCostLabel;
    private final FLabel typeLabel;
    private final FLabel powerToughnessLabel;
    private final FLabel idLabel;
    private final JLabel setInfoLabel;
    private final FHtmlViewer cdArea;
    private final FScrollPane scrArea;

    private GameView gameView = null;

    public CardDetailPanel() {
        super();
        setLayout(null);
        setOpaque(false);

        nameCostLabel = new FLabel.Builder().fontAlign(SwingConstants.CENTER).tooltip(Localizer.getInstance().getMessage("lblCardNameAndCost")).build();
        typeLabel = new FLabel.Builder().fontAlign(SwingConstants.CENTER).tooltip(Localizer.getInstance().getMessage("lblCardType")).build();
        idLabel = new FLabel.Builder().fontAlign(SwingConstants.LEFT).tooltip(Localizer.getInstance().getMessage("lblCardID")).build();
        powerToughnessLabel = new FLabel.Builder().fontAlign(SwingConstants.CENTER).tooltip(Localizer.getInstance().getMessage("lblPrimaryCharacteristic")).build();
        setInfoLabel = new JLabel();
        setInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        final Integer fontSizeR12 = FSkin.getRelativeFontSize(12);
        final Integer fontSizeR14 = FSkin.getRelativeFontSize(14);
        final Font font = new Font("Dialog", 0, fontSizeR14);
        nameCostLabel.setFont(font);
        typeLabel.setFont(font);
        idLabel.setFont(font);
        powerToughnessLabel.setFont(font);

        cdArea = new FHtmlViewer();
        cdArea.setFont(new Font("Dialog", 0, fontSizeR12));
        cdArea.setBorder(new EmptyBorder(2, 6, 2, 6));
        cdArea.setOpaque(false);
        cdArea.setFocusable(true);
        cdArea.getAccessibleContext().setAccessibleName("Card textbox");
        scrArea = new FScrollPane(cdArea, false);

        add(nameCostLabel);
        add(typeLabel);
        add(idLabel);
        add(setInfoLabel);
        add(powerToughnessLabel);
        add(scrArea);
    }

    public void setGameView(final GameView gameView) {
        this.gameView = gameView;
    }

    @Override
    public void doLayout() {
        final int insets = 3;
        final int setInfoWidth = 40;
        final int x = insets;
        int y = insets;
        final int lineWidth = getWidth() - 2 * insets;
        // final int lineHeight = nameCostLabel.getPreferredSize().height;
        // final int dy = lineHeight + 1;

        int areaHeight = nameCostLabel.getPreferredSize().height;
        nameCostLabel.setBounds(x, y, lineWidth, areaHeight);
        y += areaHeight + 1 ;

        areaHeight = typeLabel.getPreferredSize().height;
        typeLabel.setBounds(x, y, lineWidth, areaHeight);
        y += areaHeight + 1 ;

        areaHeight = Math.max(Math.max(idLabel.getPreferredSize().height,
        		powerToughnessLabel.getPreferredSize().height),
        		setInfoLabel.getPreferredSize().height);
        idLabel.setBounds(x, y, idLabel.getAutoSizeWidth(), areaHeight);
        powerToughnessLabel.setBounds(x, y, lineWidth, areaHeight);
        //+1 to x,y so set info label right up against border and the baseline matches ID and P/T
        setInfoLabel.setBounds(x + lineWidth - setInfoWidth + 1, y + 1, setInfoWidth, areaHeight);
        y += areaHeight + 1 ;
        scrArea.setBounds(0, y, getWidth(), getHeight() - y);
    }

    public final void setItem(final InventoryItemFromSet item) {
        nameCostLabel.setText(item.getDisplayName());
        typeLabel.setVisible(false);
        powerToughnessLabel.setVisible(false);
        idLabel.setText("");
        cdArea.setText(CardDetailUtil.getItemDescription(item));
        updateBorder(item instanceof IPaperCard ? Card.getCardForUi((IPaperCard)item).getView().getCurrentState() : null, true);

        final String set = item.getEdition();
        setInfoLabel.setText(set);
        setInfoLabel.setToolTipText("");
        if (StringUtils.isEmpty(set)) {
            setInfoLabel.setOpaque(false);
            setInfoLabel.setBorder(null);
        } else {
            final CardEdition edition = FModel.getMagicDb().getEditions().get(set);
            if (null != edition) {
                setInfoLabel.setToolTipText(edition.getName());
            }

            setInfoLabel.setOpaque(true);
            setInfoLabel.setBackground(Color.BLACK);
            setInfoLabel.setForeground(Color.WHITE);
            setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        }

        SwingUtilities.invokeLater(() -> scrArea.getVerticalScrollBar().setValue(scrArea.getVerticalScrollBar().getMinimum()));
    }

    public final void setCard(final CardView card) {
        setCard(card, true, false);
    }

    public final void setCard(final CardView card, final boolean mayView, final boolean isInAltState) {
        typeLabel.setVisible(true);
        powerToughnessLabel.setVisible(true);

        final CardStateView state = card == null ? null : card.getState(isInAltState);
        if (state == null) {
            nameCostLabel.setText("");
            typeLabel.setText("");
            powerToughnessLabel.setText("");
            idLabel.setText("");
            setInfoLabel.setText("");
            setInfoLabel.setToolTipText("");
            setInfoLabel.setOpaque(false);
            setInfoLabel.setBorder(null);
            cdArea.setText("");
            updateBorder(null, false);
            return;
        }

        final String name = CardDetailUtil.formatCardName(card, mayView, isInAltState), nameCost;
        if (state.getOriginalManaCost().isNoCost() || !mayView) {
            nameCost = name;
        } else {
            final String manaCost;
            if (card.isSplitCard() && card.hasAlternateState() && !card.isFaceDown() && card.getZone() != ZoneType.Stack && card.getZone() != ZoneType.Battlefield) { //only display current state's mana cost when on stack
                manaCost = card.getLeftSplitState().getOriginalManaCost() + " // " + card.getAlternateState().getOriginalManaCost();
            } else {
                manaCost = state.getOriginalManaCost().toString();
            }
            nameCost = String.format("%s - %s", name, manaCost);
        }
        nameCostLabel.setText(FSkin.encodeSymbols(nameCost, false));
        typeLabel.setText(FSkin.encodeSymbols(CardDetailUtil.formatCardType(state, mayView),false));

        final String set;
        final CardRarity rarity;
        if (mayView) {
            set = state.getSetCode();
            rarity = state.getRarity();
        } else {
            set = CardEdition.UNKNOWN_CODE;
            rarity = CardRarity.Unknown;
        }
        setInfoLabel.setText(set);

        if (null != set && !set.isEmpty()) {
            if (mayView) {
                final CardEdition edition = FModel.getMagicDb().getEditions().get(set);
                final String setTooltip;
                if (null == edition) {
                    setTooltip = rarity.name();
                } else {
                    setTooltip = String.format("%s (%s)", edition.getName(), rarity.name());
                }
                setInfoLabel.setToolTipText(setTooltip);
            }

            setInfoLabel.setOpaque(true);

            final Color backColor = fromDetailColor(CardDetailUtil.getRarityColor(rarity));
            setInfoLabel.setBackground(backColor);
            final Color foreColor = FSkin.getHighContrastColor(backColor);
            setInfoLabel.setForeground(foreColor);
            setInfoLabel.setBorder(BorderFactory.createLineBorder(foreColor));
        }

        if (card.isFaceDown()) {
            updateBorder(state, false); // TODO: HACK! A temporary measure until the morphs still leaking color can be fixed properly.
        } else {
            updateBorder(state, mayView);
        }

        powerToughnessLabel.setText(FSkin.encodeSymbols(CardDetailUtil.formatPrimaryCharacteristic(state, mayView), false));

        idLabel.setText(mayView ? CardDetailUtil.formatCardId(state) : "");

        // fill the card text
        cdArea.setText(FSkin.encodeSymbols(CardDetailUtil.composeCardText( state, gameView, mayView), true));

        SwingUtilities.invokeLater(() -> scrArea.getVerticalScrollBar().setValue(scrArea.getVerticalScrollBar().getMinimum()));
    }

    /** @return FLabel */
    public FLabel getNameCostLabel() {
        return nameCostLabel;
    }

    /** @return FLabel */
    public FLabel getTypeLabel() {
        return typeLabel;
    }

    /** @return FLabel */
    public FLabel getPowerToughnessLabel() {
        return powerToughnessLabel;
    }

    /** @return JLabel */
    public JLabel getSetInfoLabel() {
        return setInfoLabel;
    }

    /** @return FHtmlViewer */
    public FHtmlViewer getCDArea() {
        return cdArea;
    }

    private void updateBorder(final CardStateView card, final boolean canShow) {
        // color info
        if (card == null) {
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            scrArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            return;
        }

        final Color color = fromDetailColor(CardDetailUtil.getBorderColor(card, canShow));
        setBorder(BorderFactory.createLineBorder(color, 2));
        scrArea.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, color));
    }

    //ensure mouse listener hooked up to all certain opaque child components so it can get raised properly
    @Override
    public synchronized void addMouseListener(final MouseListener l) {
        super.addMouseListener(l);
        setInfoLabel.addMouseListener(l);
        cdArea.addMouseListener(l);
    }
}
