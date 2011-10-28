package arcane.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JScrollPane;

import arcane.ui.util.CardPanelMouseListener;
import forge.Card;

/**
 * <p>
 * PlayArea class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PlayArea extends CardPanelContainer implements CardPanelMouseListener {
    /** Constant <code>serialVersionUID=8333013579724492513L</code>. */
    private static final long serialVersionUID = 8333013579724492513L;
    /** Constant <code>GUTTER_Y=5</code>. */
    private static final int GUTTER_Y = 5;
    /** Constant <code>GUTTER_X=5</code>. */
    private static final int GUTTER_X = 5;
    /** Constant <code>EXTRA_CARD_SPACING_X=0.04f</code>. */
    static final float EXTRA_CARD_SPACING_X = 0.04f;
    /** Constant <code>CARD_SPACING_Y=0.06f</code>. */
    private static final float CARD_SPACING_Y = 0.06f;
    /** Constant <code>STACK_SPACING_X=0.07f</code>. */
    private static final float STACK_SPACING_X = 0.07f;
    /** Constant <code>STACK_SPACING_Y=0.07f</code>. */
    private static final float STACK_SPACING_Y = 0.07f;

    private int landStackMax = 5;

    private boolean stackVertical;
    private final boolean mirror;

    // Computed in layout.
    private List<Row> rows = new ArrayList<Row>();
    private int cardWidth, cardHeight;
    private int playAreaWidth, playAreaHeight;
    private int extraCardSpacingX, cardSpacingX, cardSpacingY;
    private int stackSpacingX, stackSpacingY;

    /**
     * <p>
     * Constructor for PlayArea.
     * </p>
     * 
     * @param scrollPane
     *            a {@link javax.swing.JScrollPane} object.
     * @param mirror
     *            a boolean.
     */
    public PlayArea(final JScrollPane scrollPane, final boolean mirror) {
        super(scrollPane);
        this.setBackground(Color.white);
        this.mirror = mirror;
    }

    /**
     * <p>
     * doLayout.
     * </p>
     * 
     * @since 1.0.15
     */
    @Override
    public final void doLayout() {
        final int tokenStackMax = 5;
        // Collect lands.
        final Row allLands = new Row();
        outerLoop:
        //
        for (final CardPanel panel : this.getCardPanels()) {
            if (!panel.getGameCard().isLand() || panel.getGameCard().isCreature()) {
                continue;
            }

            int insertIndex = -1;

            // Find lands with the same name.
            for (int i = 0, n = allLands.size(); i < n; i++) {
                final Stack stack = allLands.get(i);
                final CardPanel firstPanel = stack.get(0);
                if (firstPanel.getGameCard().getName().equals(panel.getGameCard().getName())) {
                    if (!firstPanel.getAttachedPanels().isEmpty() || firstPanel.getGameCard().isEnchanted()) {
                        // Put this land to the left of lands with the same name
                        // and attachments.
                        insertIndex = i;
                        break;
                    }
                    if (!panel.getAttachedPanels().isEmpty()
                            || !panel.getGameCard().getCounters().equals(firstPanel.getGameCard().getCounters())
                            || firstPanel.getGameCard().isEnchanted() || (stack.size() == this.landStackMax)) {
                        // If this land has attachments or the stack is full,
                        // put it to the right.
                        insertIndex = i + 1;
                        continue;
                    }
                    // Add to stack.
                    stack.add(0, panel);
                    continue outerLoop;
                }
                if (insertIndex != -1) {
                    break;
                }
            }

            final Stack stack = new Stack();
            stack.add(panel);
            allLands.add(insertIndex == -1 ? allLands.size() : insertIndex, stack);
        }

        // Collect tokens.
        final Row allTokens = new Row();
        outerLoop:
        //
        for (final CardPanel panel : this.getCardPanels()) {
            if (!panel.getGameCard().isToken()) {
                continue;
            }

            int insertIndex = -1;

            // Find tokens with the same name.
            for (int i = 0, n = allTokens.size(); i < n; i++) {
                final Stack stack = allTokens.get(i);
                final CardPanel firstPanel = stack.get(0);
                if (firstPanel.getGameCard().getName().equals(panel.getGameCard().getName())) {
                    if (!firstPanel.getAttachedPanels().isEmpty()) {
                        // Put this token to the left of tokens with the same
                        // name and attachments.
                        insertIndex = i;
                        break;
                    }
                    if (!panel.getAttachedPanels().isEmpty()
                            || !panel.getGameCard().getCounters().equals(firstPanel.getGameCard().getCounters())
                            || (panel.getGameCard().isSick() != firstPanel.getGameCard().isSick())
                            || (panel.getGameCard().getNetAttack() != firstPanel.getGameCard().getNetAttack())
                            || (panel.getGameCard().getNetDefense() != firstPanel.getGameCard().getNetDefense())
                            || (stack.size() == tokenStackMax)) {
                        // If this token has attachments or the stack is full,
                        // put it to the right.
                        insertIndex = i + 1;
                        continue;
                    }
                    // Add to stack.
                    stack.add(0, panel);
                    continue outerLoop;
                }
                if (insertIndex != -1) {
                    break;
                }
            }

            final Stack stack = new Stack();
            stack.add(panel);
            allTokens.add(insertIndex == -1 ? allTokens.size() : insertIndex, stack);
        }

        final Row allCreatures = new Row(this.getCardPanels(), RowType.creatureNonToken);
        final Row allOthers = new Row(this.getCardPanels(), RowType.other);

        this.cardWidth = this.getCardWidthMax();
        final Rectangle rect = this.getScrollPane().getVisibleRect();
        this.playAreaWidth = rect.width;
        this.playAreaHeight = rect.height;
        while (true) {
            this.rows.clear();
            this.cardHeight = Math.round(this.cardWidth * CardPanel.ASPECT_RATIO);
            this.extraCardSpacingX = Math.round(this.cardWidth * PlayArea.EXTRA_CARD_SPACING_X);
            this.cardSpacingX = (this.cardHeight - this.cardWidth) + this.extraCardSpacingX;
            this.cardSpacingY = Math.round(this.cardHeight * PlayArea.CARD_SPACING_Y);
            this.stackSpacingX = this.stackVertical ? 0 : (int) Math.round(this.cardWidth * PlayArea.STACK_SPACING_X);
            this.stackSpacingY = Math.round(this.cardHeight * PlayArea.STACK_SPACING_Y);
            final Row creatures = (Row) allCreatures.clone();
            final Row tokens = (Row) allTokens.clone();
            final Row lands = (Row) allLands.clone();
            Row others = (Row) allOthers.clone();
            int afterFirstRow;
            if (this.mirror) {
                // Wrap all creatures and lands.
                this.wrap(lands, this.rows, -1);
                afterFirstRow = this.rows.size();
                this.wrap(tokens, this.rows, afterFirstRow);
                this.wrap(creatures, this.rows, this.rows.size());
            } else {
                // Wrap all creatures and lands.
                this.wrap(creatures, this.rows, -1);
                afterFirstRow = this.rows.size();
                this.wrap(tokens, this.rows, afterFirstRow);
                this.wrap(lands, this.rows, this.rows.size());
            }
            // Store the current rows and others.
            final List<Row> storedRows = new ArrayList<Row>(this.rows.size());
            for (final Row row : this.rows) {
                storedRows.add((Row) row.clone());
            }
            final Row storedOthers = (Row) others.clone();
            // Fill in all rows with others.
            for (final Row row : this.rows) {
                this.fillRow(others, this.rows, row);
            }
            // Stop if everything fits, otherwise revert back to the stored
            // values.
            if (creatures.isEmpty() && tokens.isEmpty() && lands.isEmpty() && others.isEmpty()) {
                break;
            }
            this.rows = storedRows;
            others = storedOthers;
            // Try to put others on their own row(s) and fill in the rest.
            this.wrap(others, this.rows, afterFirstRow);
            for (final Row row : this.rows) {
                this.fillRow(others, this.rows, row);
            }
            // If that still doesn't fit, scale down.
            if (creatures.isEmpty() && tokens.isEmpty() && lands.isEmpty() && others.isEmpty()) {
                break;
            }
            this.cardWidth--;
        }

        // Get size of all the rows.
        int x, y = PlayArea.GUTTER_Y;
        int maxRowWidth = 0;
        for (final Row row : this.rows) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                final Stack stack = row.get(stackIndex);
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
            maxRowWidth = Math.max(maxRowWidth, x);
        }
        this.setPreferredSize(new Dimension(maxRowWidth - this.cardSpacingX, y - this.cardSpacingY));
        this.revalidate();

        // Position all card panels.
        x = 0;
        y = PlayArea.GUTTER_Y;
        for (final Row row : this.rows) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                final Stack stack = row.get(stackIndex);
                // Align others to the right.
                if (RowType.other.isType(stack.get(0).getGameCard())) {
                    x = (this.playAreaWidth - PlayArea.GUTTER_X) + this.extraCardSpacingX;
                    for (int i = stackIndex, n = row.size(); i < n; i++) {
                        x -= row.get(i).getWidth();
                    }
                }
                for (int panelIndex = 0, panelCount = stack.size(); panelIndex < panelCount; panelIndex++) {
                    final CardPanel panel = stack.get(panelIndex);
                    final int stackPosition = panelCount - panelIndex - 1;
                    this.setComponentZOrder(panel, panelIndex);
                    final int panelX = x + (stackPosition * this.stackSpacingX);
                    final int panelY = y + (stackPosition * this.stackSpacingY);
                    panel.setCardBounds(panelX, panelY, this.cardWidth, this.cardHeight);
                }
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
        }
    }

    /**
     * <p>
     * wrap.
     * </p>
     * 
     * @param sourceRow
     *            a {@link arcane.ui.PlayArea.Row} object.
     * @param rows
     *            a {@link java.util.List} object.
     * @param insertIndex
     *            a int.
     * @return a int.
     */
    private int wrap(final Row sourceRow, final List<Row> rows, final int insertIndex) {
        // The cards are sure to fit (with vertical scrolling) at the minimum
        // card width.
        final boolean allowHeightOverflow = this.cardWidth == this.getCardWidthMin();

        Row currentRow = new Row();
        for (int i = 0, n = sourceRow.size() - 1; i <= n; i++) {
            final Stack stack = sourceRow.get(i);
            // If the row is not empty and this stack doesn't fit, add the row.
            final int rowWidth = currentRow.getWidth();
            if (!currentRow.isEmpty() && ((rowWidth + stack.getWidth()) > this.playAreaWidth)) {
                // Stop processing if the row is too wide or tall.
                if (!allowHeightOverflow && (rowWidth > this.playAreaWidth)) {
                    break;
                }
                if (!allowHeightOverflow && ((this.getRowsHeight(rows)
                        + sourceRow.getHeight()) > this.playAreaHeight)) {
                    break;
                }
                rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                currentRow = new Row();
            }
            currentRow.add(stack);
        }
        // Add the last row if it is not empty and it fits.
        if (!currentRow.isEmpty()) {
            final int rowWidth = currentRow.getWidth();
            if (allowHeightOverflow || (rowWidth <= this.playAreaWidth)) {
                if (allowHeightOverflow || ((this.getRowsHeight(rows)
                        + sourceRow.getHeight()) <= this.playAreaHeight)) {
                    rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                }
            }
        }
        // Remove the wrapped stacks from the source row.
        for (final Row row : rows) {
            for (final Stack stack : row) {
                sourceRow.remove(stack);
            }
        }
        return insertIndex;
    }

    /**
     * <p>
     * fillRow.
     * </p>
     * 
     * @param sourceRow
     *            a {@link arcane.ui.PlayArea.Row} object.
     * @param rows
     *            a {@link java.util.List} object.
     * @param rows
     *            a {@link java.util.List} object.
     * @param row
     *            a {@link arcane.ui.PlayArea.Row} object.
     */
    private void fillRow(final Row sourceRow, final List<Row> rows, final Row row) {
        int rowWidth = row.getWidth();
        while (!sourceRow.isEmpty()) {
            final Stack stack = sourceRow.get(0);
            rowWidth += stack.getWidth();
            if (rowWidth > this.playAreaWidth) {
                break;
            }
            if (stack.getHeight() > row.getHeight()) {
                if (((this.getRowsHeight(rows) - row.getHeight()) + stack.getHeight()) > this.playAreaHeight) {
                    break;
                }
            }
            row.add(sourceRow.remove(0));
        }
    }

    /**
     * <p>
     * getRowsHeight.
     * </p>
     * 
     * @param rows
     *            a {@link java.util.List} object.
     * @return a int.
     */
    private int getRowsHeight(final List<Row> rows) {
        int height = 0;
        for (final Row row : rows) {
            height += row.getHeight();
        }
        return (height - this.cardSpacingY) + (PlayArea.GUTTER_Y * 2);
    }

    /** {@inheritDoc} */
    @Override
    public final CardPanel getCardPanel(final int x, final int y) {
        for (final Row row : this.rows) {
            for (final Stack stack : row) {
                for (final CardPanel panel : stack) {
                    final int panelX = panel.getCardX();
                    int panelY = panel.getCardY();
                    int panelWidth, panelHeight;
                    if (panel.isTapped()) {
                        panelWidth = panel.getCardHeight();
                        panelHeight = panel.getCardWidth();
                        panelY += panelWidth - panelHeight;
                    } else {
                        panelWidth = panel.getCardWidth();
                        panelHeight = panel.getCardHeight();
                    }
                    if ((x > panelX) && (x < (panelX + panelWidth))) {
                        if ((y > panelY) && (y < (panelY + panelHeight))) {
                            if (!panel.isDisplayEnabled()) {
                                return null;
                            }
                            return panel;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        if ((panel.getTappedAngle() != 0) && (panel.getTappedAngle() != CardPanel.TAPPED_ANGLE)) {
            return;
        }
        super.mouseLeftClicked(panel, evt);
    }

    /**
     * <p>
     * Getter for the field <code>landStackMax</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getLandStackMax() {
        return this.landStackMax;
    }

    /**
     * <p>
     * Setter for the field <code>landStackMax</code>.
     * </p>
     * 
     * @param landStackMax
     *            a int.
     */
    public final void setLandStackMax(final int landStackMax) {
        this.landStackMax = landStackMax;
    }

    /**
     * <p>
     * Getter for the field <code>stackVertical</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getStackVertical() {
        return this.stackVertical;
    }

    /**
     * <p>
     * Setter for the field <code>stackVertical</code>.
     * </p>
     * 
     * @param stackVertical
     *            a boolean.
     */
    public final void setStackVertical(final boolean stackVertical) {
        this.stackVertical = stackVertical;
    }

    private static enum RowType {
        land, creature, creatureNonToken, other;

        public boolean isType(final Card card) {
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

        public Row(final List<CardPanel> cardPanels, final RowType type) {
            this();
            this.addAll(cardPanels, type);
        }

        private void addAll(final List<CardPanel> cardPanels, final RowType type) {
            for (final CardPanel panel : cardPanels) {
                if (!type.isType(panel.getGameCard()) || (panel.getAttachedToPanel() != null)) {
                    continue;
                }
                final Stack stack = new Stack();
                stack.add(panel);
                this.add(stack);
            }
        }

        @Override
        public boolean addAll(final Collection<? extends Stack> c) {
            final boolean changed = super.addAll(c);
            c.clear();
            return changed;
        }

        private int getWidth() {
            if (this.isEmpty()) {
                return 0;
            }
            int width = 0;
            for (final Stack stack : this) {
                width += stack.getWidth();
            }
            return (width + (PlayArea.GUTTER_X * 2)) - PlayArea.this.extraCardSpacingX;
        }

        private int getHeight() {
            if (this.isEmpty()) {
                return 0;
            }
            int height = 0;
            for (final Stack stack : this) {
                height = Math.max(height, stack.getHeight());
            }
            return height;
        }
    }

    private class Stack extends ArrayList<CardPanel> {
        private static final long serialVersionUID = 3863135156832080368L;

        public Stack() {
            super(8);
        }

        @Override
        public boolean add(final CardPanel panel) {
            final boolean appended = super.add(panel);
            for (final CardPanel attachedPanel : panel.getAttachedPanels()) {
                this.add(attachedPanel);
            }
            return appended;
        }

        private int getWidth() {
            return PlayArea.this.cardWidth + ((this.size() - 1) * PlayArea.this.stackSpacingX)
                    + PlayArea.this.cardSpacingX;
        }

        private int getHeight() {
            return PlayArea.this.cardHeight + ((this.size() - 1) * PlayArea.this.stackSpacingY)
                    + PlayArea.this.cardSpacingY;
        }
    }
}
