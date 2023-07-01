/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.arcane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;

import forge.screens.match.CMatchUI;
import forge.toolbox.FScrollPane;
import forge.view.arcane.util.Animation;
import forge.view.arcane.util.CardPanelMouseListener;

/**
 * <p>
 * CardArea class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardArea.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class CardArea extends CardPanelContainer implements CardPanelMouseListener {
    private static final long serialVersionUID = -5836122075999621592L;

    public static final int GUTTER_Y = 5;
    public static final int GUTTER_X = 5;
    private static final float HORIZ_CARD_SPACING_X = 0.04f;
    private static final float HORIZ_CARD_SPACING_Y = 0.06f;
    private static final float VERT_CARD_SPACING_X = 0.06f;
    private static final float VERT_CARD_SPACING_Y = 0.10f;

    private float maxCoverage = 0.5f;
    private int maxRows = 0;

    // Computed in layout.
    private float cardSpacingX;
    private int actualCardsPerRow;
    private int mouseDragStartX;
    private int mouseDragStartY;
    private boolean isVertical;
    private boolean hasScrollbars;

    public CardArea(final CMatchUI matchUI, final FScrollPane scrollPane) {
        super(matchUI, scrollPane);
        this.setBackground(Color.white);
    }

    @Override
    public final CardPanel getCardPanel(final int x, final int y) {
        final List<CardPanel> panels = isVertical ? Lists.reverse(getCardPanels()) : getCardPanels();
        for (final CardPanel panel : panels) {
            final int panelX = panel == this.getMouseDragPanel() ? this.mouseDragStartX : panel.getCardX();
            final int panelY = panel == this.getMouseDragPanel() ? this.mouseDragStartY : panel.getCardY();
            final int panelWidth = panel.getCardWidth();
            final int panelHeight = panel.getCardHeight();
            if ((x > panelX) && (x < (panelX + panelWidth)) && (y > panelY) && (y < (panelY + panelHeight))) {
                if (!panel.isDisplayEnabled()) {
                    return null;
                }
                return panel;
            }
        }
        return null;
    }

    @Override
    public void doLayout() {
        if (this.getCardPanels().isEmpty()) {
            return;
        }

        final Rectangle rect = this.getScrollPane().getVisibleRect();
        final Insets insets = this.getScrollPane().getInsets();
        rect.width -= insets.left;
        rect.height -= insets.top;
        rect.width -= insets.right;
        rect.height -= insets.bottom;

        final int cardAreaWidth = rect.width;
        final int cardAreaHeight = rect.height;
        int cardWidth = this.getCardWidthMax();
        int cardHeight;
        int cardSpacingY;

        int maxWidth = 0, maxHeight = 0;
        if (this.isVertical) {
            while (true) {
                cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
                this.cardSpacingX = Math.round(cardWidth * CardArea.VERT_CARD_SPACING_X);
                cardSpacingY = cardHeight + Math.round(cardWidth * CardArea.VERT_CARD_SPACING_Y);
                int maxRows = Math.max((int) Math.floor(((cardAreaWidth - (CardArea.GUTTER_X * 2)) + this.cardSpacingX)
                        / (cardWidth + this.cardSpacingX)), 1);
                if (this.maxRows > 0) {
                    maxRows = Math.min(this.maxRows, maxRows);
                }
                final int availableRowHeight = cardAreaHeight - (CardArea.GUTTER_Y * 2);
                final int availableCardsPerRow = (int) Math.floor((availableRowHeight - (cardHeight - cardSpacingY))
                        / (double) cardSpacingY);
                this.actualCardsPerRow = Math.max(availableCardsPerRow,
                        (int) Math.ceil(this.getCardPanels().size() / (float) maxRows));
                int actualRowHeight = (int) Math.floor(((this.actualCardsPerRow - 1) * cardSpacingY) + cardHeight);
                final float overflow = actualRowHeight - availableRowHeight;
                if (overflow > 0) {
                    float offsetY = overflow / (this.actualCardsPerRow - 1);
                    offsetY = Math.min(offsetY, cardHeight * this.maxCoverage);
                    cardSpacingY -= offsetY;
                }
                actualRowHeight = (int) Math.floor(((this.actualCardsPerRow - 1) * cardSpacingY) + cardHeight);
                if ((actualRowHeight >= 0) && (actualRowHeight <= availableRowHeight)) {
                    break;
                }
                cardWidth--;
                if (cardWidth == this.getCardWidthMin()) {
                    break;
                }
            }

            float x = CardArea.GUTTER_X;
            int y = CardArea.GUTTER_Y;
            int zOrder = this.getCardPanels().size() - 1, rowCount = 0;
            int maxZOrder = this.getComponentCount() - 1; //needed to prevent crash for certain situations when not all card panels actually show up in component
            if (zOrder > maxZOrder) {
                zOrder = maxZOrder;
            }
            for (final CardPanel panel : this.getCardPanels()) {
                if (panel != this.getMouseDragPanel()) {
                    panel.setCardBounds((int) Math.floor(x), y, cardWidth, cardHeight);
                }
                y += cardSpacingY;
                maxWidth = Math.round(x) + cardWidth + CardArea.GUTTER_X;
                maxHeight = Math.max(maxHeight, (y + (cardHeight - cardSpacingY) + CardArea.GUTTER_Y));
                this.setComponentZOrder(panel, zOrder);
                if (zOrder > 0) {
                    zOrder--;
                }
                rowCount++;
                if (rowCount == this.actualCardsPerRow) {
                    rowCount = 0;
                    x += cardWidth + this.cardSpacingX;
                    y = CardArea.GUTTER_Y;
                }
            }
        }
        else {
            while (true) {
                cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
                final int extraCardSpacingX = Math.round(cardWidth * CardArea.HORIZ_CARD_SPACING_X);
                cardSpacingY = Math.round(cardHeight * CardArea.HORIZ_CARD_SPACING_Y);
                this.cardSpacingX = cardWidth + extraCardSpacingX;
                int maxRows = Math.max((int) Math.floor(((cardAreaHeight - (CardArea.GUTTER_Y * 2)) + cardSpacingY)
                        / (double) (cardHeight + cardSpacingY)), 1);
                if (this.maxRows > 0) {
                    maxRows = Math.min(this.maxRows, maxRows);
                }
                final int availableRowWidth = cardAreaWidth - (CardArea.GUTTER_X * 2);
                final int availableCardsPerRow = (int) Math.floor((availableRowWidth - (cardWidth - this.cardSpacingX))
                        / this.cardSpacingX);
                this.actualCardsPerRow = Math.max(availableCardsPerRow,
                        (int) Math.ceil(this.getCardPanels().size() / (float) maxRows));
                int actualRowWidth = (int) Math.floor(((this.actualCardsPerRow - 1) * this.cardSpacingX) + cardWidth);
                final float overflow = actualRowWidth - availableRowWidth;
                if (overflow > 0) {
                    float offsetX = overflow / (this.actualCardsPerRow - 1);
                    offsetX = Math.min(offsetX, cardWidth * this.maxCoverage);
                    this.cardSpacingX -= offsetX;
                }
                actualRowWidth = (int) Math.floor(((this.actualCardsPerRow - 1) * this.cardSpacingX) + cardWidth);
                if (actualRowWidth <= availableRowWidth) {
                    break;
                }
                cardWidth--;
                if (cardWidth == this.getCardWidthMin()) {
                    break;
                }
            }

            float x = CardArea.GUTTER_X;
            int y = CardArea.GUTTER_Y;
            int zOrder = 0, rowCount = 0;
            for (final CardPanel panel : this.getCardPanels()) {
                if (panel != this.getMouseDragPanel()) {
                    panel.setCardBounds((int) Math.floor(x), y, cardWidth, cardHeight);
                }
                x += this.cardSpacingX;
                maxWidth = Math.max(maxWidth, Math.round(x + (cardWidth - this.cardSpacingX) + CardArea.GUTTER_X) - 1);
                maxHeight = Math.max(maxHeight, y + (cardHeight - cardSpacingY) + CardArea.GUTTER_Y);
                this.setComponentZOrder(panel, zOrder);
                zOrder++;
                rowCount++;
                if (rowCount == this.actualCardsPerRow) {
                    rowCount = 0;
                    x = CardArea.GUTTER_X;
                    y += cardHeight + cardSpacingY;
                }
            }
        }

        final Dimension oldPreferredSize = this.getPreferredSize();
        this.setPreferredSize(new Dimension(maxWidth, maxHeight));
        if ((oldPreferredSize.width != maxWidth) || (oldPreferredSize.height != maxHeight)) {
            this.getParent().invalidate();
            this.getParent().validate();
        }

        super.doLayout();
    }

    @Override
    public final void paint(final Graphics g) {
        final boolean hasScrollbars = this.getScrollPane().getVerticalScrollBar().isVisible();
        if (hasScrollbars != this.hasScrollbars) {
            this.revalidate();
        }
        this.hasScrollbars = hasScrollbars;
        super.paint(g);
    }

    @Override
    public final void mouseDragStart(final CardPanel dragPanel, final MouseEvent evt) {
        super.setDragged(true);

        super.mouseDragStart(dragPanel, evt);

        this.mouseDragStartX = dragPanel.getCardX();
        this.mouseDragStartY = dragPanel.getCardY();
        dragPanel.setDisplayEnabled(false);

        CardPanel.setDragAnimationPanel(new CardPanel(dragPanel.getMatchUI(), dragPanel.getCard()));
        final RootPaneContainer frame = (RootPaneContainer) SwingUtilities.windowForComponent(this);
	final JLayeredPane layeredPane = frame.getLayeredPane();
        layeredPane.add(CardPanel.getDragAnimationPanel());
        layeredPane.moveToFront(CardPanel.getDragAnimationPanel());
        final Point p = SwingUtilities.convertPoint(this, this.mouseDragStartX, this.mouseDragStartY, layeredPane);
        CardPanel.getDragAnimationPanel().setCardBounds(p.x, p.y, dragPanel.getCardWidth(), dragPanel.getCardHeight());
    }

    @Override
    public final void mouseDragged(final CardPanel dragPanel, final int dragOffsetX, final int dragOffsetY,
            final MouseEvent evt) {
        super.mouseDragged(dragPanel, dragOffsetX, dragOffsetY, evt);

        final int mouseX = evt.getX();
        final int mouseY = evt.getY();
        final int dragPanelX = mouseX + dragOffsetX;
        final int dragPanelY = mouseY + dragOffsetY;
        final Point p = SwingUtilities.convertPoint(this, dragPanelX, dragPanelY, CardPanel.getDragAnimationPanel()
                .getParent());
        CardPanel.getDragAnimationPanel().setLocation(p.x, p.y);

        final CardPanel panel = this.getCardPanel(mouseX, mouseY);
        if ((panel == null) || (panel == dragPanel)) {
            return;
        }
        int index = this.getCardPanels().size();
        while (--index >= 0) {
            if (this.getCardPanels().get(index) == panel) {
                break;
            }
        }
        this.getCardPanels().remove(dragPanel);
        this.getCardPanels().add(index, dragPanel);
        this.mouseDragStartX = panel.getCardX();
        this.mouseDragStartY = panel.getCardY();
        this.revalidate();
    }

    @Override
    public final void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
        super.setDragged(false);

        super.mouseDragEnd(dragPanel, evt);
        this.doLayout();
        final JLayeredPane layeredPane = SwingUtilities.getRootPane(CardPanel.getDragAnimationPanel()).getLayeredPane();
        final int startX = CardPanel.getDragAnimationPanel().getCardX();
        final int startY = CardPanel.getDragAnimationPanel().getCardY();
        final int startWidth = CardPanel.getDragAnimationPanel().getCardWidth();
        final Point endPos = SwingUtilities.convertPoint(this, dragPanel.getCardLocation(), layeredPane);
        final int endWidth = dragPanel.getCardWidth();
        Animation.moveCard(startX, startY, startWidth, endPos.x, endPos.y, endWidth, CardPanel.getDragAnimationPanel(),
                dragPanel, layeredPane, 200);
    }

    public final float getMaxCoverage() {
        return this.maxCoverage;
    }
    public final void setMaxCoverage(final float maxCoverage) {
        this.maxCoverage = maxCoverage;
    }

    public final int getMaxRows() {
        return this.maxRows;
    }
    public final void setMaxRows(final int maxRows) {
        this.maxRows = maxRows;
    }

    public final boolean isVertical() {
        return this.isVertical;
    }
    public final void setVertical(final boolean isVertical) {
        this.isVertical = isVertical;
    }
}
