package arcane.ui;

import arcane.ui.util.CardPanelMouseListener;
import forge.Card;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>PlayArea class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class PlayArea extends CardPanelContainer implements CardPanelMouseListener {
    /** Constant <code>serialVersionUID=8333013579724492513L</code> */
    private static final long serialVersionUID = 8333013579724492513L;
    /** Constant <code>GUTTER_Y=5</code> */
    static private final int GUTTER_Y = 5;
    /** Constant <code>GUTTER_X=5</code> */
    static private final int GUTTER_X = 5;
    /** Constant <code>EXTRA_CARD_SPACING_X=0.04f</code> */
    static final float EXTRA_CARD_SPACING_X = 0.04f;
    /** Constant <code>CARD_SPACING_Y=0.06f</code> */
    static private final float CARD_SPACING_Y = 0.06f;
    /** Constant <code>STACK_SPACING_X=0.07f</code> */
    static private final float STACK_SPACING_X = 0.07f;
    /** Constant <code>STACK_SPACING_Y=0.07f</code> */
    static private final float STACK_SPACING_Y = 0.07f;

    private int landStackMax = 5;

    private boolean stackVertical;
    private boolean mirror;

    // Computed in layout.
    private List<Row> rows = new ArrayList<Row>();
    private int cardWidth, cardHeight;
    private int playAreaWidth, playAreaHeight;
    private int extraCardSpacingX, cardSpacingX, cardSpacingY;
    private int stackSpacingX, stackSpacingY;

    /**
     * <p>Constructor for PlayArea.</p>
     *
     * @param scrollPane a {@link javax.swing.JScrollPane} object.
     * @param mirror a boolean.
     */
    public PlayArea(JScrollPane scrollPane, boolean mirror) {
        super(scrollPane);
        setBackground(Color.white);
        this.mirror = mirror;
    }

    /**
     * <p>doLayout.</p>
     *
     * @since 1.0.15
     */
    public void doLayout() {
        int tokenStackMax = 5;
        // Collect lands.
        Row allLands = new Row();
        outerLoop:
        //
        for (CardPanel panel : cardPanels) {
            if (!panel.gameCard.isLand()) continue;

            int insertIndex = -1;

            // Find lands with the same name.
            for (int i = 0, n = allLands.size(); i < n; i++) {
                Stack stack = allLands.get(i);
                CardPanel firstPanel = stack.get(0);
                if (firstPanel.gameCard.getName().equals(panel.gameCard.getName()) && !panel.gameCard.isCreature()) {
                    if (!firstPanel.attachedPanels.isEmpty() || firstPanel.gameCard.isEnchanted()) {
                        // Put this land to the left of lands with the same name and attachments.
                        insertIndex = i;
                        break;
                    }
                    if (!panel.attachedPanels.isEmpty()  || !panel.gameCard.getCounters().equals(firstPanel.gameCard.getCounters())
                            || firstPanel.gameCard.isEnchanted() || stack.size() == landStackMax) {
                        // If this land has attachments or the stack is full, put it to the right.
                        insertIndex = i + 1;
                        continue;
                    }
                    // Add to stack.
                    stack.add(0, panel);
                    continue outerLoop;
                }
                if (insertIndex != -1) break;
            }

            Stack stack = new Stack();
            stack.add(panel);
            allLands.add(insertIndex == -1 ? allLands.size() : insertIndex, stack);
        }

        // Collect tokens.
        Row allTokens = new Row();
        outerLoop:
        //
        for (CardPanel panel : cardPanels) {
            if (!panel.gameCard.isToken()) continue;

            int insertIndex = -1;

            // Find tokens with the same name.
            for (int i = 0, n = allTokens.size(); i < n; i++) {
                Stack stack = allTokens.get(i);
                CardPanel firstPanel = stack.get(0);
                if (firstPanel.gameCard.getName().equals(panel.gameCard.getName())) {
                    if (!firstPanel.attachedPanels.isEmpty()) {
                        // Put this token to the left of tokens with the same name and attachments.
                        insertIndex = i;
                        break;
                    }
                    if (!panel.attachedPanels.isEmpty() || !panel.gameCard.getCounters().equals(firstPanel.gameCard.getCounters()) 
                            || stack.size() == tokenStackMax) {
                        // If this token has attachments or the stack is full, put it to the right.
                        insertIndex = i + 1;
                        continue;
                    }
                    // Add to stack.
                    stack.add(0, panel);
                    continue outerLoop;
                }
                if (insertIndex != -1) break;
            }

            Stack stack = new Stack();
            stack.add(panel);
            allTokens.add(insertIndex == -1 ? allTokens.size() : insertIndex, stack);
        }

        Row allCreatures = new Row(cardPanels, RowType.creatureNonToken);
        Row allOthers = new Row(cardPanels, RowType.other);

        cardWidth = cardWidthMax;
        Rectangle rect = scrollPane.getVisibleRect();
        playAreaWidth = rect.width;
        playAreaHeight = rect.height;
        while (true) {
            rows.clear();
            cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
            extraCardSpacingX = Math.round(cardWidth * EXTRA_CARD_SPACING_X);
            cardSpacingX = cardHeight - cardWidth + extraCardSpacingX;
            cardSpacingY = Math.round(cardHeight * CARD_SPACING_Y);
            stackSpacingX = stackVertical ? 0 : (int) Math.round(cardWidth * STACK_SPACING_X);
            stackSpacingY = Math.round(cardHeight * STACK_SPACING_Y);
            Row creatures = (Row) allCreatures.clone();
            Row tokens = (Row) allTokens.clone();
            Row lands = (Row) allLands.clone();
            Row others = (Row) allOthers.clone();
            int afterFirstRow;
            if (mirror) {
                // Wrap all creatures and lands.
                wrap(lands, rows, -1);
                afterFirstRow = rows.size();
                wrap(tokens, rows, afterFirstRow);
                wrap(creatures, rows, rows.size());
            } else {
                // Wrap all creatures and lands.
                wrap(creatures, rows, -1);
                afterFirstRow = rows.size();
                wrap(tokens, rows, afterFirstRow);
                wrap(lands, rows, rows.size());
            }
            // Store the current rows and others.
            List<Row> storedRows = new ArrayList<Row>(rows.size());
            for (Row row : rows)
                storedRows.add((Row) row.clone());
            Row storedOthers = (Row) others.clone();
            // Fill in all rows with others.
            for (Row row : rows)
                fillRow(others, rows, row);
            // Stop if everything fits, otherwise revert back to the stored values.
            if (creatures.isEmpty() && tokens.isEmpty() && lands.isEmpty() && others.isEmpty()) break;
            rows = storedRows;
            others = storedOthers;
            // Try to put others on their own row(s) and fill in the rest.
            wrap(others, rows, afterFirstRow);
            for (Row row : rows)
                fillRow(others, rows, row);
            // If that still doesn't fit, scale down.
            if (creatures.isEmpty() && tokens.isEmpty() && lands.isEmpty() && others.isEmpty()) break;
            cardWidth--;
        }

        // Get size of all the rows.
        int x, y = GUTTER_Y;
        int maxRowWidth = 0;
        for (Row row : rows) {
            int rowBottom = 0;
            x = GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                Stack stack = row.get(stackIndex);
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
            maxRowWidth = Math.max(maxRowWidth, x);
        }
        setPreferredSize(new Dimension(maxRowWidth - cardSpacingX, y - cardSpacingY));
        revalidate();

        // Position all card panels.
        x = 0;
        y = GUTTER_Y;
        for (Row row : rows) {
            int rowBottom = 0;
            x = GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                Stack stack = row.get(stackIndex);
                // Align others to the right.
                if (RowType.other.isType(stack.get(0).gameCard)) {
                    x = playAreaWidth - GUTTER_X + extraCardSpacingX;
                    for (int i = stackIndex, n = row.size(); i < n; i++)
                        x -= row.get(i).getWidth();
                }
                for (int panelIndex = 0, panelCount = stack.size(); panelIndex < panelCount; panelIndex++) {
                    CardPanel panel = stack.get(panelIndex);
                    int stackPosition = panelCount - panelIndex - 1;
                    setComponentZOrder(panel, panelIndex);
                    int panelX = x + (stackPosition * stackSpacingX);
                    int panelY = y + (stackPosition * stackSpacingY);
                    panel.setCardBounds(panelX, panelY, cardWidth, cardHeight);
                }
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
        }
    }

    /**
     * <p>wrap.</p>
     *
     * @param sourceRow a {@link arcane.ui.PlayArea.Row} object.
     * @param rows a {@link java.util.List} object.
     * @param insertIndex a int.
     * @return a int.
     */
    private int wrap(Row sourceRow, List<Row> rows, int insertIndex) {
        // The cards are sure to fit (with vertical scrolling) at the minimum card width.
        boolean allowHeightOverflow = cardWidth == cardWidthMin;

        Row currentRow = new Row();
        for (int i = 0, n = sourceRow.size() - 1; i <= n; i++) {
            Stack stack = sourceRow.get(i);
            // If the row is not empty and this stack doesn't fit, add the row.
            int rowWidth = currentRow.getWidth();
            if (!currentRow.isEmpty() && rowWidth + stack.getWidth() > playAreaWidth) {
                // Stop processing if the row is too wide or tall.
                if (!allowHeightOverflow && rowWidth > playAreaWidth) break;
                if (!allowHeightOverflow && getRowsHeight(rows) + sourceRow.getHeight() > playAreaHeight) break;
                rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                currentRow = new Row();
            }
            currentRow.add(stack);
        }
        // Add the last row if it is not empty and it fits.
        if (!currentRow.isEmpty()) {
            int rowWidth = currentRow.getWidth();
            if (allowHeightOverflow || rowWidth <= playAreaWidth) {
                if (allowHeightOverflow || getRowsHeight(rows) + sourceRow.getHeight() <= playAreaHeight) {
                    rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                }
            }
        }
        // Remove the wrapped stacks from the source row.
        for (Row row : rows)
            for (Stack stack : row)
                sourceRow.remove(stack);
        return insertIndex;
    }

    /**
     * <p>fillRow.</p>
     *
     * @param sourceRow a {@link arcane.ui.PlayArea.Row} object.
     * @param rows a {@link java.util.List} object.
     * @param rows a {@link java.util.List} object.
     * @param row a {@link arcane.ui.PlayArea.Row} object.
     */
    private void fillRow(Row sourceRow, List<Row> rows, Row row) {
        int rowWidth = row.getWidth();
        while (!sourceRow.isEmpty()) {
            Stack stack = sourceRow.get(0);
            rowWidth += stack.getWidth();
            if (rowWidth > playAreaWidth) break;
            if (stack.getHeight() > row.getHeight()) {
                if (getRowsHeight(rows) - row.getHeight() + stack.getHeight() > playAreaHeight) break;
            }
            row.add(sourceRow.remove(0));
        }
    }

    /**
     * <p>getRowsHeight.</p>
     *
     * @param rows a {@link java.util.List} object.
     * @return a int.
     */
    private int getRowsHeight(List<Row> rows) {
        int height = 0;
        for (Row row : rows)
            height += row.getHeight();
        return height - cardSpacingY + GUTTER_Y * 2;
    }

    /** {@inheritDoc} */
    public CardPanel getCardPanel(int x, int y) {
        for (Row row : rows) {
            for (Stack stack : row) {
                for (CardPanel panel : stack) {
                    int panelX = panel.getCardX();
                    int panelY = panel.getCardY();
                    int panelWidth, panelHeight;
                    if (panel.tapped) {
                        panelWidth = panel.getCardHeight();
                        panelHeight = panel.getCardWidth();
                        panelY += panelWidth - panelHeight;
                    } else {
                        panelWidth = panel.getCardWidth();
                        panelHeight = panel.getCardHeight();
                    }
                    if (x > panelX && x < panelX + panelWidth) {
                        if (y > panelY && y < panelY + panelHeight) {
                            if (!panel.isDisplayEnabled()) return null;
                            return panel;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public void mouseLeftClicked(CardPanel panel, MouseEvent evt) {
        if (panel.tappedAngle != 0 && panel.tappedAngle != CardPanel.TAPPED_ANGLE) return;
        super.mouseLeftClicked(panel, evt);
    }

    /**
     * <p>Getter for the field <code>landStackMax</code>.</p>
     *
     * @return a int.
     */
    public int getLandStackMax() {
        return landStackMax;
    }

    /**
     * <p>Setter for the field <code>landStackMax</code>.</p>
     *
     * @param landStackMax a int.
     */
    public void setLandStackMax(int landStackMax) {
        this.landStackMax = landStackMax;
    }

    /**
     * <p>Getter for the field <code>stackVertical</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getStackVertical() {
        return stackVertical;
    }

    /**
     * <p>Setter for the field <code>stackVertical</code>.</p>
     *
     * @param stackVertical a boolean.
     */
    public void setStackVertical(boolean stackVertical) {
        this.stackVertical = stackVertical;
    }

    static private enum RowType {
        land, creature, creatureNonToken, other;

        public boolean isType(Card card) {
            switch (this) {
                case land:
                    return card.isLand();
                case creature:
                    return card.isCreature();
                case creatureNonToken:
                    return card.isCreature() && !card.isToken();
                case other:
                    return !card.isLand() && !card.isCreature();
                default:
                    throw new RuntimeException("Unhandled type: " + this);
            }
        }
    }

    private class Row extends ArrayList<Stack> {
        private static final long serialVersionUID = 716489891951011846L;

        public Row() {
            super(16);
        }

        public Row(List<CardPanel> cardPanels, RowType type) {
            this();
            addAll(cardPanels, type);
        }

        private void addAll(List<CardPanel> cardPanels, RowType type) {
            for (CardPanel panel : cardPanels) {
                if (!type.isType(panel.gameCard) || panel.attachedToPanel != null) continue;
                Stack stack = new Stack();
                stack.add(panel);
                add(stack);
            }
        }

        public boolean addAll(Collection<? extends Stack> c) {
            boolean changed = super.addAll(c);
            c.clear();
            return changed;
        }

        private int getWidth() {
            if (isEmpty()) return 0;
            int width = 0;
            for (Stack stack : this)
                width += stack.getWidth();
            return width + GUTTER_X * 2 - extraCardSpacingX;
        }

        private int getHeight() {
            if (isEmpty()) return 0;
            int height = 0;
            for (Stack stack : this)
                height = Math.max(height, stack.getHeight());
            return height;
        }
    }

    private class Stack extends ArrayList<CardPanel> {
        private static final long serialVersionUID = 3863135156832080368L;

        public Stack() {
            super(8);
        }

        public boolean add(CardPanel panel) {
            boolean appended = super.add(panel);
            for (CardPanel attachedPanel : panel.attachedPanels)
                add(attachedPanel);
            return appended;
        }

        private int getWidth() {
            return cardWidth + (size() - 1) * stackSpacingX + cardSpacingX;
        }

        private int getHeight() {
            return cardHeight + (size() - 1) * stackSpacingY + cardSpacingY;
        }
    }
}
