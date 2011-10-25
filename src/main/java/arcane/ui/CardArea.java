package arcane.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import arcane.ui.util.Animation;
import arcane.ui.util.CardPanelMouseListener;

/**
 * <p>
 * CardArea class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardArea extends CardPanelContainer implements CardPanelMouseListener {
    /**
     *
     */
    private static final long serialVersionUID = -5836122075999621592L;
    /**
     * Constant <code>GUTTER_Y=5</code>.
     */
    public static final int GUTTER_Y = 5;
    /**
     * Constant <code>GUTTER_X=5</code>.
     */
    public static final int GUTTER_X = 5;
    /**
     *
     */
    private static final float HORIZ_CARD_SPACING_X = 0.04f;
    /**
     *
     */
    private static final float HORIZ_CARD_SPACING_Y = 0.06f;
    /**
     *
     */
    private static final float VERT_CARD_SPACING_X = 0.06f;
    /**
     *
     */
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

    /**
     * <p>
     * Constructor for CardArea.
     * </p>
     * 
     * @param scrollPane
     *            a {@link javax.swing.JScrollPane} object.
     */
    public CardArea(final JScrollPane scrollPane) {
        super(scrollPane);
        setBackground(Color.white);
    }

    /** {@inheritDoc} */
    public final CardPanel getCardPanel(final int x, final int y) {
        if (isVertical) {
            for (int i = cardPanels.size() - 1; i >= 0; i--) {
                CardPanel panel = cardPanels.get(i);
                int panelX = panel == mouseDragPanel ? mouseDragStartX : panel.getCardX();
                int panelY = panel == mouseDragPanel ? mouseDragStartY : panel.getCardY();
                int panelWidth = panel.getCardWidth();
                int panelHeight = panel.getCardHeight();
                if (x > panelX && x < panelX + panelWidth) {
                    if (y > panelY && y < panelY + panelHeight) {
                        if (!panel.isDisplayEnabled()) {
                            return null;
                        }
                        return panel;
                    }
                }
            }
        } else {
            for (int i = 0, n = cardPanels.size(); i < n; i++) {
                CardPanel panel = cardPanels.get(i);
                int panelX = panel == mouseDragPanel ? mouseDragStartX : panel.getCardX();
                int panelY = panel == mouseDragPanel ? mouseDragStartY : panel.getCardY();
                int panelWidth = panel.getCardWidth();
                int panelHeight = panel.getCardHeight();
                if (x > panelX && x < panelX + panelWidth) {
                    if (y > panelY && y < panelY + panelHeight) {
                        if (!panel.isDisplayEnabled()) {
                            return null;
                        }
                        return panel;
                    }
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * doLayout.
     * </p>
     * 
     * @since 1.0.15
     */
    public final void doLayout() {
        if (cardPanels.isEmpty()) {
            return;
        }

        Rectangle rect = scrollPane.getVisibleRect();
        Insets insets = scrollPane.getInsets();
        rect.width -= insets.left;
        rect.height -= insets.top;
        rect.width -= insets.right;
        rect.height -= insets.bottom;

        int cardAreaWidth = rect.width;
        int cardAreaHeight = rect.height;
        int cardWidth = cardWidthMax;
        int cardHeight;
        int cardSpacingY;

        int maxWidth = 0, maxHeight = 0;
        if (isVertical) {
            while (true) {
                cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
                cardSpacingX = Math.round(cardWidth * VERT_CARD_SPACING_X);
                cardSpacingY = cardHeight + Math.round(cardWidth * VERT_CARD_SPACING_Y);
                int maxRows = (int) Math.floor((cardAreaWidth - GUTTER_X * 2 + cardSpacingX)
                        / (cardWidth + cardSpacingX));
                if (this.maxRows > 0) {
                    maxRows = Math.min(this.maxRows, maxRows);
                }
                int availableRowHeight = cardAreaHeight - GUTTER_Y * 2;
                int availableCardsPerRow = (int) Math.floor((availableRowHeight - (cardHeight - cardSpacingY))
                        / (double) cardSpacingY);
                actualCardsPerRow = Math
                        .max(availableCardsPerRow, (int) Math.ceil(cardPanels.size() / (float) maxRows));
                int actualRowHeight = (int) Math.floor((actualCardsPerRow - 1) * cardSpacingY + cardHeight);
                float overflow = actualRowHeight - availableRowHeight;
                if (overflow > 0) {
                    float offsetY = overflow / (actualCardsPerRow - 1);
                    offsetY = Math.min(offsetY, cardHeight * maxCoverage);
                    cardSpacingY -= offsetY;
                }
                actualRowHeight = (int) Math.floor((actualCardsPerRow - 1) * cardSpacingY + cardHeight);
                if (actualRowHeight >= 0 && actualRowHeight <= availableRowHeight) {
                    break;
                }
                cardWidth--;
                if (cardWidth == cardWidthMin) {
                    break;
                }
            }

            float x = GUTTER_X;
            int y = GUTTER_Y;
            int zOrder = cardPanels.size() - 1, rowCount = 0;
            for (CardPanel panel : cardPanels) {
                if (panel != mouseDragPanel) {
                    panel.setCardBounds((int) Math.floor(x), y, cardWidth, cardHeight);
                }
                y += cardSpacingY;
                maxWidth = Math.round(x) + cardWidth + GUTTER_X;
                maxHeight = Math.max(maxHeight, (y + (cardHeight - cardSpacingY) + GUTTER_Y));
                setComponentZOrder(panel, zOrder);
                zOrder--;
                rowCount++;
                if (rowCount == actualCardsPerRow) {
                    rowCount = 0;
                    x += cardWidth + cardSpacingX;
                    y = GUTTER_Y;
                }
            }
        } else {
            while (true) {
                cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
                int extraCardSpacingX = Math.round(cardWidth * HORIZ_CARD_SPACING_X);
                cardSpacingY = Math.round(cardHeight * HORIZ_CARD_SPACING_Y);
                cardSpacingX = cardWidth + extraCardSpacingX;
                int maxRows = (int) Math.floor((cardAreaHeight - GUTTER_Y * 2 + cardSpacingY)
                        / (double) (cardHeight + cardSpacingY));
                if (this.maxRows > 0) {
                    maxRows = Math.min(this.maxRows, maxRows);
                }
                int availableRowWidth = cardAreaWidth - GUTTER_X * 2;
                int availableCardsPerRow = (int) Math.floor((availableRowWidth - (cardWidth - cardSpacingX))
                        / cardSpacingX);
                actualCardsPerRow = Math
                        .max(availableCardsPerRow, (int) Math.ceil(cardPanels.size() / (float) maxRows));
                int actualRowWidth = (int) Math.floor((actualCardsPerRow - 1) * cardSpacingX + cardWidth);
                float overflow = actualRowWidth - availableRowWidth;
                if (overflow > 0) {
                    float offsetX = overflow / (actualCardsPerRow - 1);
                    offsetX = Math.min(offsetX, cardWidth * maxCoverage);
                    cardSpacingX -= offsetX;
                }
                actualRowWidth = (int) Math.floor((actualCardsPerRow - 1) * cardSpacingX + cardWidth);
                if (actualRowWidth <= availableRowWidth) {
                    break;
                }
                cardWidth--;
                if (cardWidth == cardWidthMin) {
                    break;
                }
            }

            float x = GUTTER_X;
            int y = GUTTER_Y;
            int zOrder = 0, rowCount = 0;
            for (CardPanel panel : cardPanels) {
                if (panel != mouseDragPanel) {
                    panel.setCardBounds((int) Math.floor(x), y, cardWidth, cardHeight);
                }
                x += cardSpacingX;
                maxWidth = Math.max(maxWidth, Math.round(x + (cardWidth - cardSpacingX) + GUTTER_X) - 1);
                maxHeight = Math.max(maxHeight, y + (cardHeight - cardSpacingY) + GUTTER_Y);
                setComponentZOrder(panel, zOrder);
                zOrder++;
                rowCount++;
                if (rowCount == actualCardsPerRow) {
                    rowCount = 0;
                    x = GUTTER_X;
                    y += cardHeight + cardSpacingY;
                }
            }
        }

        Dimension oldPreferredSize = getPreferredSize();
        setPreferredSize(new Dimension(maxWidth, maxHeight));
        if (oldPreferredSize.width != maxWidth || oldPreferredSize.height != maxHeight) {
            getParent().invalidate();
            getParent().validate();
        }
    }

    /** {@inheritDoc} */
    public final void paint(final Graphics g) {
        boolean hasScrollbars = scrollPane.getVerticalScrollBar().isVisible();
        if (hasScrollbars != this.hasScrollbars) {
            revalidate();
        }
        this.hasScrollbars = hasScrollbars;

        super.paint(g);
    }

    /** {@inheritDoc} */
    public final void mouseDragStart(final CardPanel dragPanel, final MouseEvent evt) {
        super.mouseDragStart(dragPanel, evt);

        mouseDragStartX = dragPanel.getCardX();
        mouseDragStartY = dragPanel.getCardY();
        dragPanel.setDisplayEnabled(false);

        CardPanel.dragAnimationPanel = new CardPanel(dragPanel.gameCard);
        CardPanel.dragAnimationPanel.setImage(dragPanel);
        JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);
        final JLayeredPane layeredPane = frame.getLayeredPane();
        layeredPane.add(CardPanel.dragAnimationPanel);
        layeredPane.moveToFront(CardPanel.dragAnimationPanel);
        Point p = SwingUtilities.convertPoint(this, mouseDragStartX, mouseDragStartY, layeredPane);
        CardPanel.dragAnimationPanel.setCardBounds(p.x, p.y, dragPanel.getCardWidth(), dragPanel.getCardHeight());
    }

    /** {@inheritDoc} */
    public final void mouseDragged(final CardPanel dragPanel, final int dragOffsetX, final int dragOffsetY,
            final MouseEvent evt) {
        super.mouseDragged(dragPanel, dragOffsetX, dragOffsetY, evt);

        int mouseX = evt.getX();
        int mouseY = evt.getY();
        int dragPanelX = mouseX + dragOffsetX;
        int dragPanelY = mouseY + dragOffsetY;
        Point p = SwingUtilities.convertPoint(this, dragPanelX, dragPanelY, CardPanel.dragAnimationPanel.getParent());
        CardPanel.dragAnimationPanel.setLocation(p.x, p.y);

        CardPanel panel = getCardPanel(mouseX, mouseY);
        if (panel == null || panel == dragPanel) {
            return;
        }
        int index = cardPanels.size();
        while (--index >= 0) {
            if (cardPanels.get(index) == panel) {
                break;
            }
        }
        cardPanels.remove(dragPanel);
        cardPanels.add(index, dragPanel);
        mouseDragStartX = panel.getCardX();
        mouseDragStartY = panel.getCardY();
        revalidate();
    }

    /** {@inheritDoc} */
    public final void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
        super.mouseDragEnd(dragPanel, evt);
        doLayout();
        JLayeredPane layeredPane = SwingUtilities.getRootPane(CardPanel.dragAnimationPanel).getLayeredPane();
        int startX = CardPanel.dragAnimationPanel.getCardX();
        int startY = CardPanel.dragAnimationPanel.getCardY();
        int startWidth = CardPanel.dragAnimationPanel.getCardWidth();
        Point endPos = SwingUtilities.convertPoint(this, dragPanel.getCardLocation(), layeredPane);
        int endWidth = dragPanel.getCardWidth();
        Animation.moveCard(startX, startY, startWidth, endPos.x, endPos.y, endWidth, CardPanel.dragAnimationPanel,
                dragPanel, layeredPane, 200);
    }

    /**
     * <p>
     * Getter for the field <code>maxCoverage</code>.
     * </p>
     * 
     * @return a float.
     */
    public final float getMaxCoverage() {
        return maxCoverage;
    }

    /**
     * <p>
     * Setter for the field <code>maxCoverage</code>.
     * </p>
     * 
     * @param maxCoverage
     *            a float.
     */
    public final void setMaxCoverage(float maxCoverage) {
        this.maxCoverage = maxCoverage;
    }

    /**
     * <p>
     * Setter for the field <code>maxRows</code>.
     * </p>
     * 
     * @param maxRows
     *            a int.
     */
    public final void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * <p>
     * Getter for the field <code>maxRows</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getMaxRows() {
        return maxRows;
    }

    /**
     * <p>
     * setVertical.
     * </p>
     * 
     * @param isVertical
     *            a boolean.
     */
    public final void setVertical(final boolean isVertical) {
        this.isVertical = isVertical;
    }

    /**
     * <p>
     * isVertical.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isVertical() {
        return isVertical;
    }
}
