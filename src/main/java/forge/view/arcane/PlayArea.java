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
import java.util.List;

import javax.swing.JScrollPane;
import forge.Card;
import forge.view.arcane.util.Animation;
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

    private List<Card> model;
    
    /**
     * <p>
     * Constructor for PlayArea.
     * </p>
     * 
     * @param scrollPane
     *            a {@link javax.swing.JScrollPane} object.
     * @param mirror
     *            a boolean.
     * @param modelRef 
     */
    public PlayArea(final JScrollPane scrollPane, final boolean mirror, List<Card> modelRef) {
        super(scrollPane);
        this.setBackground(Color.white);
        this.mirror = mirror;
        this.model = modelRef;
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

        final CardStackRow lands = collectAllLands();
        final CardStackRow tokens = collectAllTokens();
        final CardStackRow creatures = new CardStackRow(this.getCardPanels(), RowType.CreatureNonToken);
        final CardStackRow others = new CardStackRow(this.getCardPanels(), RowType.Other);

        // should find an appropriate width of card
        int maxCardWidth = this.getCardWidthMax();
        setCardWidth(maxCardWidth);
        int minCardWidth = this.getCardWidthMin();
        int lastGoodCardWidth = minCardWidth;
        int deltaCardWidth = (maxCardWidth - minCardWidth) / 2;
        List<CardStackRow> lastTemplate = null;

        while (deltaCardWidth > 0) {
            List<CardStackRow> template = tryArrangePilesOfWidth(lands, tokens, creatures, others);
            //System.out.println(template == null ? "won't fit" : "Fits @ " + cardWidth + " !!! " + template.toString());
            
            deltaCardWidth = (getCardWidth() - lastGoodCardWidth) / 2;
            if (template != null) {
                lastTemplate = template;
                lastGoodCardWidth = getCardWidth();
                setCardWidth(getCardWidth() + deltaCardWidth);
                if (lastGoodCardWidth == maxCardWidth) {
                    break;
                }
            }
            else {
                setCardWidth(getCardWidth() - deltaCardWidth);
            }
        }
        setCardWidth(lastGoodCardWidth);
        if ( null == lastTemplate ) 
            lastTemplate = tryArrangePilesOfWidth(lands, tokens, creatures, others);

        this.rows = lastTemplate;
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
        positionAllCards(lastTemplate);
    }

    private void positionAllCards(List<CardStackRow> template)  {

        // Position all card panels.
        int x = 0;
        int y = PlayArea.GUTTER_Y;

        //System.out.println("-------- " + (mirror ? "^" : "_") + " (Positioning ) Card width = " + cardWidth + ". Playarea = " + playAreaWidth + " x " + playAreaHeight );
        for (final CardStackRow row : template) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                final CardStack stack = row.get(stackIndex);
                // Align others to the right.
                if (RowType.Other.isGoodFor(stack.get(0).getGameCard())) {
                    x = (this.playAreaWidth - PlayArea.GUTTER_X) + this.extraCardSpacingX;
                    for (int i = stackIndex, n = row.size(); i < n; i++) {
                        CardStack r = row.get(i);
                        x -= r.getWidth();
                    }
                }
                for (int panelIndex = 0, panelCount = stack.size(); panelIndex < panelCount; panelIndex++) {
                    final CardPanel panel = stack.get(panelIndex);
                    final int stackPosition = panelCount - panelIndex - 1;
                    this.setComponentZOrder(panel, panelIndex);
                    final int panelX = x + (stackPosition * this.stackSpacingX);
                    final int panelY = y + (stackPosition * this.stackSpacingY);
                    //System.out.println("... placinng " + panel.getCard() + " @ (" + panelX + ", " + panelY + ")" );
                    panel.setCardBounds(panelX, panelY, this.getCardWidth(), this.cardHeight);
                }
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
        }
    }

    private List<CardStackRow> tryArrangePilesOfWidth(final CardStackRow lands, final CardStackRow tokens, final CardStackRow creatures, CardStackRow others) {
        List<CardStackRow> template = new ArrayList<PlayArea.CardStackRow>();
        
        int afterFirstRow;

        //System.out.println( "======== "  + ( mirror ? "^" : "_" ) + " (try arrange) Card width = " + cardWidth + ". PlayArea = " + playAreaWidth + " x " + playAreaHeight + " ========");
        boolean landsFit, tokensFit, creaturesFit;
        if (this.mirror) {
            // Wrap all creatures and lands.
            landsFit = this.planRow(lands, template, -1);
            afterFirstRow = template.size();
            tokensFit = this.planRow(tokens, template, afterFirstRow);
            creaturesFit = this.planRow(creatures, template, template.size());
        } else {
            // Wrap all creatures and lands.
            creaturesFit = this.planRow(creatures, template, -1);
            afterFirstRow = template.size();
            tokensFit = this.planRow(tokens, template, afterFirstRow);
            landsFit = this.planRow(lands, template, template.size());
        }

        if ( !landsFit || !creaturesFit || !tokensFit ) 
            return null;
        
        // Other cards may be stored at end of usual rows or on their own row.
        int cntOthers = others.size();

        // Copy the template for the case 1st approach won't work
        final List<CardStackRow> templateCopy = new ArrayList<CardStackRow>(template.size());
        for (final CardStackRow row : template) {
            templateCopy.add((CardStackRow) row.clone());
        }

        // Fill in all rows with others.
        int nextOther = 0;
        for (final CardStackRow row : template) {
            nextOther = this.planOthersRow(others, nextOther, template, row);
            if ( nextOther == cntOthers )
                return template; // everything was successfully placed
        }

        template = templateCopy;
        // Try to put others on their own row(s)
        if ( this.planRow(others, template, afterFirstRow) ) 
            return template;
        
        
        return null; // Cannot fit everything with that width;
    }

    /**
     * <p>
     * wrap.
     * </p>
     * 
     * @param sourceRow
     *            a {@link forge.view.arcane.PlayArea.CardStackRow} object.
     * @param template
     *            a {@link java.util.List} object.
     * @param insertIndex
     *            a int.
     * @return a int.
     */
    // Won't modify the first parameter
    private boolean planRow(final CardStackRow sourceRow, final List<CardStackRow> template, final int insertIndex) {
        // The cards are sure to fit (with vertical scrolling) at the minimum
        // card width.
        final boolean isMinimalSize = this.getCardWidth() == this.getCardWidthMin();

        CardStackRow currentRow = new CardStackRow();
        for (final CardStack stack : sourceRow) {
            final int rowWidth = currentRow.getWidth();
            final int stackWidth = stack.getWidth();
            //System.out.printf("Adding %s (+%dpx), current row is %dpx and has %s \n", stack, stackWidth, rowWidth, currentRow ); 
            // If the row is not empty and this stack doesn't fit, add the row.
            if (rowWidth + stackWidth > this.playAreaWidth && !currentRow.isEmpty() ) {

                // Stop processing if the row is too wide or tall.
                if (rowWidth > this.playAreaWidth || this.getRowsHeight(template) + sourceRow.getHeight() > this.playAreaHeight) {
                    if ( !isMinimalSize ) 
                        return false;
                }

                if ( insertIndex == -1)
                    template.add(currentRow);
                else 
                    template.add(insertIndex, currentRow);
                
                currentRow = new CardStackRow();
            }

            currentRow.add(stack);
        }
        // Add the last row if it is not empty and it fits.
        if (!currentRow.isEmpty()) {
            final int rowWidth = currentRow.getWidth();
            if (isMinimalSize || rowWidth <= this.playAreaWidth && this.getRowsHeight(template) + sourceRow.getHeight() <= this.playAreaHeight) {
                if ( insertIndex == -1)
                    template.add(currentRow);
                else 
                    template.add(insertIndex, currentRow);
            } else return false;
        }
        //System.out.println("... row complete! " + currentRow.getWidth() + "px");
        return true;
    }


    /**
     * <p>
     * fillRow.
     * </p>
     * 
     * @param sourceRow
     *            a {@link forge.view.arcane.PlayArea.CardStackRow} object.
     * @param template
     *            a {@link java.util.List} object.
     * @param template
     *            a {@link java.util.List} object.
     * @param rowToFill
     *            a {@link forge.view.arcane.PlayArea.CardStackRow} object.
     */
    private int planOthersRow(final List<CardStack> sourceRow, final int firstPile, final List<CardStackRow> template, final CardStackRow rowToFill) {
        int rowWidth = rowToFill.getWidth();

        // System.out.println("This row has:" + rowToFill + "; want to add:" + sourceRow );
        for (int i = firstPile; i < sourceRow.size(); i++ ) {
            CardStack stack = sourceRow.get(i);

            rowWidth += stack.getWidth();
            if (rowWidth > this.playAreaWidth) return i; // cannot add any more piles in a row 
            
            if (stack.getHeight() > rowToFill.getHeight()) { // if row becomes taller
                int newAllRowsHeight = this.getRowsHeight(template) - rowToFill.getHeight() + stack.getHeight();
                if ( newAllRowsHeight > this.playAreaHeight) 
                    return i; // refuse to add here because it won't fit in height
            }
            rowToFill.add(stack);
        }
        return sourceRow.size();
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

    /**
     * <p>
     * setupPlayZone.
     * </p>
     * 
     * @param newList
     *            an array of {@link forge.Card} objects.
     */
    public void setupPlayZone() {
        List<Card> oldCards, toDelete;
        oldCards = new ArrayList<Card>();
        for (final CardPanel cpa : getCardPanels()) {
            oldCards.add(cpa.getGameCard());
        }
        toDelete = new ArrayList<Card>(oldCards);
        toDelete.removeAll(model);
        if (toDelete.size() == getCardPanels().size()) {
            clear();
        } else {
            for (final Card card : toDelete) {
                removeCardPanel(getCardPanel(card.getUniqueNumber()));
            }
        }
    
        List<Card> toAdd = new ArrayList<Card>(model);
        toAdd.removeAll(oldCards);
    
        List<CardPanel> newPanels = new ArrayList<CardPanel>();
        for (final Card card : toAdd) {
            newPanels.add(addCard(card));
        }
        if (!toAdd.isEmpty()) {
            doLayout();
        }
        for (final CardPanel toPanel : newPanels) {
            scrollRectToVisible(new Rectangle(toPanel.getCardX(), toPanel.getCardY(), toPanel.getCardWidth(), toPanel.getCardHeight()));
            Animation.moveCard(toPanel);
        }
    
        for (final Card card : model) {
            final CardPanel toPanel = getCardPanel(card.getUniqueNumber());
            if (card.isTapped()) {
                toPanel.setTapped(true);
                toPanel.setTappedAngle(forge.view.arcane.CardPanel.TAPPED_ANGLE);
            } else {
                toPanel.setTapped(false);
                toPanel.setTappedAngle(0);
            }
            toPanel.getAttachedPanels().clear();
            if (card.isEnchanted()) {
                final ArrayList<Card> enchants = card.getEnchantedBy();
                for (final Card e : enchants) {
                    final forge.view.arcane.CardPanel cardE = getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.getAttachedPanels().add(cardE);
                    }
                }
            }
    
            if (card.isEquipped()) {
                final ArrayList<Card> enchants = card.getEquippedBy();
                for (final Card e : enchants) {
                    final forge.view.arcane.CardPanel cardE = getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.getAttachedPanels().add(cardE);
                    }
                }
            }
    
            if (card.isEnchantingCard()) {
                toPanel.setAttachedToPanel(getCardPanel(card.getEnchantingCard().getUniqueNumber()));
            } else if (card.isEquipping()) {
                toPanel.setAttachedToPanel(getCardPanel(card.getEquipping().get(0).getUniqueNumber()));
            } else {
                toPanel.setAttachedToPanel(null);
            }
    
            toPanel.setCard(toPanel.getGameCard());
        }
        invalidate();
        repaint();
    }

    private static enum RowType {
        Land,
        Creature,
        CreatureNonToken,
        Other;

        public boolean isGoodFor(final Card card) {
            switch (this) {
            case Land:              return card.isLand();
            case Creature:          return card.isCreature();
            case CreatureNonToken:  return card.isCreature() && !card.isToken();
            case Other:             return !card.isLand() && !card.isCreature();
            default:                throw new RuntimeException("Unhandled type: " + this);
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
                if (!type.isGoodFor(panel.getGameCard()) || (panel.getAttachedToPanel() != null)) {
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
    private int getCardWidth() {
        return cardWidth;
    }

    private void setCardWidth(int cardWidth0) {
        this.cardWidth = cardWidth0;
        this.cardHeight = Math.round(this.cardWidth * CardPanel.ASPECT_RATIO);
        this.extraCardSpacingX = Math.round(this.cardWidth * PlayArea.EXTRA_CARD_SPACING_X);
        this.cardSpacingX = (this.cardHeight - this.cardWidth) + this.extraCardSpacingX;
        this.cardSpacingY = Math.round(this.cardHeight * PlayArea.CARD_SPACING_Y);
        this.stackSpacingX = Math.round(this.cardWidth * PlayArea.STACK_SPACING_X);
        this.stackSpacingY = Math.round(this.cardHeight * PlayArea.STACK_SPACING_Y);
    }    
}
