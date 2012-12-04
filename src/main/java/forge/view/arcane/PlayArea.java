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
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;

import forge.Card;
import forge.view.arcane.util.CardPanelMouseListener;

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

    private final int landStackMax = 5;
    private final int tokenStackMax = 5;

    private final boolean mirror;

    // Computed in layout.
    private List<CardStackRow> rows = new ArrayList<CardStackRow>();
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

    private final CardStackRow collectAllLands() {
        final CardStackRow allLands = new CardStackRow();

        outerLoop:
        //
        for (final CardPanel panel : this.getCardPanels()) {
            if (!panel.getGameCard().isLand() || panel.getGameCard().isCreature()) {
                continue;
            }

            int insertIndex = -1;

            // Find lands with the same name.
            for (int i = 0, n = allLands.size(); i < n; i++) {
                final CardStack stack = allLands.get(i);
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

            final CardStack stack = new CardStack();
            stack.add(panel);
            allLands.add(insertIndex == -1 ? allLands.size() : insertIndex, stack);
        }
        return allLands;
    }


    private final CardStackRow collectAllTokens() {
        final CardStackRow allTokens = new CardStackRow();
        outerLoop:
        //
        for (final CardPanel panel : this.getCardPanels()) {
            if (!panel.getGameCard().isToken()) {
                continue;
            }

            int insertIndex = -1;

            // Find tokens with the same name.
            for (int i = 0, n = allTokens.size(); i < n; i++) {
                final CardStack stack = allTokens.get(i);
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

            final CardStack stack = new CardStack();
            stack.add(panel);
            allTokens.add(insertIndex == -1 ? allTokens.size() : insertIndex, stack);
        }
        return allTokens;
    }

    @Override
    public final CardPanel addCard(final Card card) {
        final CardPanel placeholder = new CardPanel(card);
        placeholder.setDisplayEnabled(false);
        this.getCardPanels().add(placeholder);
        this.add(placeholder);
        return placeholder;
    }

    @Override
    public final void doLayout() {
        final Rectangle rect = this.getScrollPane().getVisibleRect();

        this.playAreaWidth = rect.width;
        this.playAreaHeight = rect.height;

        final CardStackRow allLands = collectAllLands();
        final CardStackRow allTokens = collectAllTokens();
        final CardStackRow allCreatures = new CardStackRow(this.getCardPanels(), RowType.creatureNonToken);
        final CardStackRow allOthers = new CardStackRow(this.getCardPanels(), RowType.other);

        // should find an appropriate width of card
        this.cardWidth = this.getCardWidthMax();
        int maxCardWidth = this.getCardWidthMax();
        int minCardWidth = this.getCardWidthMin();
        int lastGoodCardWidth = minCardWidth;
        int deltaCardWidth = (maxCardWidth - minCardWidth) / 2;
        boolean workedLastTime = false;
        //boolean isFirstRun = true;

        while (deltaCardWidth > 0) {
            final CardStackRow creatures = (CardStackRow) allCreatures.clone();
            final CardStackRow tokens = (CardStackRow) allTokens.clone();
            final CardStackRow lands = (CardStackRow) allLands.clone();
            CardStackRow others = (CardStackRow) allOthers.clone();
            workedLastTime = canAdjustWidth(lands, tokens, creatures, others);

            deltaCardWidth = (cardWidth - lastGoodCardWidth) / 2;
            if (workedLastTime) {
                lastGoodCardWidth = cardWidth;
                cardWidth += deltaCardWidth;
                if (lastGoodCardWidth == maxCardWidth) {
                    break;
                }
            }
            else {
                cardWidth -= deltaCardWidth;
            }
        }
        cardWidth = lastGoodCardWidth;
        final CardStackRow creatures = (CardStackRow) allCreatures.clone();
        final CardStackRow tokens = (CardStackRow) allTokens.clone();
        final CardStackRow lands = (CardStackRow) allLands.clone();
        CardStackRow others = (CardStackRow) allOthers.clone();
        workedLastTime = canAdjustWidth(lands, tokens, creatures, others);

        // Get size of all the rows.
        int x, y = PlayArea.GUTTER_Y;
        int maxRowWidth = 0;
        for (final CardStackRow row : this.rows) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                final CardStack stack = row.get(stackIndex);
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
            maxRowWidth = Math.max(maxRowWidth, x);
        }
        this.setPreferredSize(new Dimension(maxRowWidth - this.cardSpacingX, y - this.cardSpacingY));
        this.revalidate();
        positionAllCards();
    }

    private void positionAllCards()  {

        // Position all card panels.
        int x = 0;
        int y = PlayArea.GUTTER_Y;

        for (final CardStackRow row : this.rows) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                final CardStack stack = row.get(stackIndex);
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

    private boolean canAdjustWidth(final CardStackRow lands,  final CardStackRow tokens, final CardStackRow creatures, CardStackRow others) {
        this.rows.clear();
        this.cardHeight = Math.round(this.cardWidth * CardPanel.ASPECT_RATIO);
        this.extraCardSpacingX = Math.round(this.cardWidth * PlayArea.EXTRA_CARD_SPACING_X);
        this.cardSpacingX = (this.cardHeight - this.cardWidth) + this.extraCardSpacingX;
        this.cardSpacingY = Math.round(this.cardHeight * PlayArea.CARD_SPACING_Y);
        this.stackSpacingX = Math.round(this.cardWidth * PlayArea.STACK_SPACING_X);
        this.stackSpacingY = Math.round(this.cardHeight * PlayArea.STACK_SPACING_Y);

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
        final List<CardStackRow> storedRows = new ArrayList<CardStackRow>(this.rows.size());
        for (final CardStackRow row : this.rows) {
            try {
                storedRows.add((CardStackRow) row.clone());
            }
            catch (NullPointerException e) {
                System.out.println("Null pointer exception in Row Spacing. Possibly also part of the issue.");
            }
        }
        final CardStackRow storedOthers = (CardStackRow) others.clone();
        // Fill in all rows with others.
        for (final CardStackRow row : this.rows) {
            this.fillRow(others, this.rows, row);
        }
        // Stop if everything fits, otherwise revert back to the stored
        // values.
        if (creatures.isEmpty() && tokens.isEmpty() && lands.isEmpty() && others.isEmpty()) {
            return true;
        }
        this.rows = storedRows;
        others = storedOthers;
        // Try to put others on their own row(s) and fill in the rest.
        this.wrap(others, this.rows, afterFirstRow);
        for (final CardStackRow row : this.rows) {
            this.fillRow(others, this.rows, row);
        }
        // If that still doesn't fit, scale down.
        return creatures.isEmpty() && tokens.isEmpty() && lands.isEmpty() && others.isEmpty();
    }

    /**
     * <p>
     * wrap.
     * </p>
     * 
     * @param sourceRow
     *            a {@link forge.view.arcane.PlayArea.CardStackRow} object.
     * @param rows
     *            a {@link java.util.List} object.
     * @param insertIndex
     *            a int.
     * @return a int.
     */
//    private int cntRepaints = 0;
    private int wrap(final CardStackRow sourceRow, final List<CardStackRow> rows, final int insertIndex) {
        // The cards are sure to fit (with vertical scrolling) at the minimum
        // card width.
        final boolean allowHeightOverflow = this.cardWidth == this.getCardWidthMin();

//        System.err.format("[%d] @ %d - Repaint playarea - %s %n", new Date().getTime(), cntRepaints++, mirror ? "MIRROR" : "DIRECT");

        CardStackRow currentRow = new CardStackRow();
        for (int i = 0, n = sourceRow.size() - 1; i <= n; i++) {
            final CardStack stack = sourceRow.get(i);
            // If the row is not empty and this stack doesn't fit, add the row.
            final int rowWidth = currentRow.getWidth();
            if (!currentRow.isEmpty() && ((rowWidth + stack.getWidth()) > this.playAreaWidth)) {
                // Stop processing if the row is too wide or tall.
                if (!allowHeightOverflow && (rowWidth > this.playAreaWidth)) {
                    break;
                }
                if (!allowHeightOverflow && ((this.getRowsHeight(rows) + sourceRow.getHeight()) > this.playAreaHeight)) {
                    break;
                }
                try {
                    rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("ArrayIndex Out of Bounds when trying to add row in PlayArea. Someone fix this logic, "
                            + " I believe it causes the no cards loading in issue we've noticed.");
                    // TODO: There's a crash here, maybe when rows == [null] and currentRow == [[Plant Wall]] and insertIndex is 0
                }
                currentRow = new CardStackRow();
            }
            currentRow.add(stack);
        }
        // Add the last row if it is not empty and it fits.
        if (!currentRow.isEmpty()) {
            final int rowWidth = currentRow.getWidth();
            if (allowHeightOverflow
                    || (rowWidth <= this.playAreaWidth)
                    && (allowHeightOverflow || ((this.getRowsHeight(rows) + sourceRow.getHeight()) <= this.playAreaHeight))) {
                try {
                    rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("ArrayIndex Out of Bounds when trying to add row in PlayArea. Someone fix this logic, "
                            + " I believe it causes the no cards loading in issue we've noticed.");
                    // TODO: There's a crash here, maybe when rows == [null] and currentRow == [[Plant Wall]] and insertIndex is 0
                }
            }
        }
        // Remove the wrapped stacks from the source row.
        for (int iRow = 0; iRow < rows.size(); iRow++) {
            CardStackRow row = rows.get(iRow);
            if (row != null) {
                sourceRow.removeAll(row);
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
     *            a {@link forge.view.arcane.PlayArea.CardStackRow} object.
     * @param rows
     *            a {@link java.util.List} object.
     * @param rows
     *            a {@link java.util.List} object.
     * @param row
     *            a {@link forge.view.arcane.PlayArea.CardStackRow} object.
     */
    private void fillRow(final CardStackRow sourceRow, final List<CardStackRow> rows, final CardStackRow row) {
        int rowWidth = row.getWidth();

        final Iterator<CardStack> itr = sourceRow.iterator();

        while (itr.hasNext()) {
            final CardStack stack = itr.next();

            rowWidth += stack.getWidth();
            if (rowWidth > this.playAreaWidth) {
                break;
            }
            if (stack.getHeight() > row.getHeight()
                    && (((this.getRowsHeight(rows) - row.getHeight()) + stack.getHeight()) > this.playAreaHeight)) {
                break;
            }
            row.add(stack);
            itr.remove();
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
    private int getRowsHeight(final List<CardStackRow> rows) {
        int height = 0;
        for (final CardStackRow row : rows) {
            height += row.getHeight();
        }
        return (height - this.cardSpacingY) + (PlayArea.GUTTER_Y * 2);
    }

    /** {@inheritDoc} */
    @Override
    public final CardPanel getCardPanel(final int x, final int y) {
        for (final CardStackRow row : this.rows) {
            for (final CardStack stack : row) {
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

    private class CardStackRow extends ArrayList<CardStack> {
        private static final long serialVersionUID = 716489891951011846L;

        public CardStackRow() {
            super(16);
        }

        public CardStackRow(final List<CardPanel> cardPanels, final RowType type) {
            this();
            this.addAll(cardPanels, type);
        }

        private void addAll(final List<CardPanel> cardPanels, final RowType type) {
            for (final CardPanel panel : cardPanels) {
                if (!type.isType(panel.getGameCard()) || (panel.getAttachedToPanel() != null)) {
                    continue;
                }
                final CardStack stack = new CardStack();
                stack.add(panel);
                this.add(stack);
            }
        }

        @Override
        public boolean addAll(final Collection<? extends CardStack> c) {
            final boolean changed = super.addAll(c);
            c.clear();
            return changed;
        }

        private int getWidth() {
            if (this.isEmpty()) {
                return 0;
            }
            int width = 0;
            for (final CardStack stack : this) {
                width += stack.getWidth();
            }
            return (width + (PlayArea.GUTTER_X * 2)) - PlayArea.this.extraCardSpacingX;
        }

        private int getHeight() {
            if (this.isEmpty()) {
                return 0;
            }
            int height = 0;
            for (final CardStack stack : this) {
                height = Math.max(height, stack.getHeight());
            }
            return height;
        }
    }

    private class CardStack extends ArrayList<CardPanel> {
        private static final long serialVersionUID = 3863135156832080368L;

        public CardStack() {
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
