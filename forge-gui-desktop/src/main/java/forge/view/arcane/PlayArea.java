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

import com.google.common.collect.Lists;

import forge.FThreads;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.screens.match.CMatchUI;
import forge.toolbox.FScrollPane;
import forge.toolbox.MouseTriggerEvent;
import forge.view.arcane.util.Animation;
import forge.view.arcane.util.CardPanelMouseListener;

/**
 * <p>
 * PlayArea class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PlayArea.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class PlayArea extends CardPanelContainer implements CardPanelMouseListener {
    private static final long serialVersionUID = 8333013579724492513L;

    private static final int GUTTER_Y = 5;
    private static final int GUTTER_X = 5;
    static final float EXTRA_CARD_SPACING_X = 0.04f;
    private static final float CARD_SPACING_Y = 0.06f;
    private static final float STACK_SPACING_X = 0.12f;
    private static final float STACK_SPACING_Y = 0.12f;

    //private final int creatureStackMax = 4;
    private final int landStackMax = 5;
    private final int tokenStackMax = 5;
    private final int othersStackMax = 4;

    private final boolean mirror;

    // Computed in layout.
    private List<CardStackRow> rows = new ArrayList<CardStackRow>();
    private int cardWidth, cardHeight;
    private int playAreaWidth, playAreaHeight;
    private int extraCardSpacingX, cardSpacingX, cardSpacingY;
    private int stackSpacingX, stackSpacingY;

    private final PlayerView model;
    private final ZoneType zone;

    private boolean makeTokenRow = true;

    public PlayArea(final CMatchUI matchUI, final FScrollPane scrollPane, final boolean mirror, final PlayerView player, final ZoneType zone) {
        super(matchUI, scrollPane);
        this.setBackground(Color.white);
        this.mirror = mirror;
        this.model = player;
        this.zone = zone;
        this.makeTokenRow = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_TOKENS_IN_SEPARATE_ROW);
    }

    private final CardStackRow collectAllLands() {
        final CardStackRow allLands = new CardStackRow();

        outerLoop:
        //
        for (final CardPanel panel : this.getCardPanels()) {
            final CardView card = panel.getCard();
            final CardStateView state = card.getCurrentState();

            if (!state.isLand() || state.isCreature()) {
                continue;
            }

            int insertIndex = -1;

            // Find lands with the same name.
            for (int i = 0, n = allLands.size(); i < n; i++) {
                final CardStack stack = allLands.get(i);
                final CardPanel firstPanel = stack.get(0);
                if (firstPanel.getCard().getCurrentState().getName().equals(state.getName())) {
                    if (!firstPanel.getAttachedPanels().isEmpty() || firstPanel.getCard().isEnchanted()) {
                        // Put this land to the left of lands with the same name
                        // and attachments.
                        insertIndex = i;
                        break;
                    }
                    if (!panel.getAttachedPanels().isEmpty()
                            || !panel.getCard().hasSameCounters(firstPanel.getCard())
                            || firstPanel.getCard().isEnchanted() || (stack.size() == this.landStackMax)) {
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
            final CardView card = panel.getCard();
            final CardStateView state = card.getCurrentState();

            if (!card.isToken()) {
                continue;
            }

            int insertIndex = -1;

            // Find tokens with the same name.
            for (int i = 0, n = allTokens.size(); i < n; i++) {
                final CardStack stack = allTokens.get(i);
                final CardPanel firstPanel = stack.get(0);
                final CardView firstCard = firstPanel.getCard();
                final CardStateView firstState = firstCard.getCurrentState();

                if (firstPanel.getCard().getCurrentState().getName().equals(state.getName())) {
                    if (!firstPanel.getAttachedPanels().isEmpty()) {
                        // Put this token to the left of tokens with the same
                        // name and attachments.
                        insertIndex = i;
                        break;
                    }

                    if (!panel.getAttachedPanels().isEmpty()
                            || !card.hasSameCounters(firstPanel.getCard())
                            || (card.isSick() != firstCard.isSick())
                            || (state.getPower() != firstState.getPower())
                            || (state.getToughness() != firstState.getToughness())
                            || !(card.getText().equals(firstCard.getText()))
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

    /*private final CardStackRow collectAllCreatures() {
        final CardStackRow allCreatures = new CardStackRow();
        outerLoop:
        //
        for (final CardPanel panel : this.getCardPanels()) {
            if (!panel.getCard().isCreature() || panel.getCard().isToken()) {
                continue;
            }

            int insertIndex = -1;

            // Find creatures with the same name.
            for (int i = 0, n = allCreatures.size(); i < n; i++) {
                final CardStack stack = allCreatures.get(i);
                final CardPanel firstPanel = stack.get(0);
                if (firstPanel.getCard().getName().equals(panel.getCard().getName())) {
                    if (!firstPanel.getAttachedPanels().isEmpty()) {
                        // Put this creature to the left of creatures with the same
                        // name and attachments.
                        insertIndex = i;
                        break;
                    }
                    if (!panel.getAttachedPanels().isEmpty()
                            || panel.getCard().isEnchanted()
                            || panel.getCard().isCloned()
                            || panel.getCard().isCopiedSpell()
                            || !panel.getCard().getCounters().equals(firstPanel.getCard().getCounters())
                            || (panel.getCard().isSick() != firstPanel.getCard().isSick())
                            || (panel.getCard().getNetPower() != firstPanel.getCard().getNetPower())
                            || (panel.getCard().getNetToughness() != firstPanel.getCard().getNetToughness())
                            || (stack.size() == creatureStackMax)) {
                        // If this creature has attachments or the stack is full,
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
            allCreatures.add(insertIndex == -1 ? allCreatures.size() : insertIndex, stack);
        }
        return allCreatures;
    }*/

    @Override
    public final CardPanel addCard(final CardView card) {
        final CardPanel placeholder = new CardPanel(getMatchUI(), card);
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
        //final CardStackRow creaturesRegular = new CardStackRow(this.getCardPanels(), RowType.CreatureNonToken);
        //final CardStackRow collectedCreatures = collectAllCreatures();
        final CardStackRow creatures = new CardStackRow(this.getCardPanels(), RowType.CreatureNonToken);
        final CardStackRow others = new CardStackRow(this.getCardPanels(), RowType.Other);
        
        if (!makeTokenRow) {
            for (CardStack s : tokens) {
                if (!s.isEmpty()) {
                    if (s.get(0).getCard().getCurrentState().isCreature()) {
                        creatures.add(s);
                    } else {
                        others.add(s);
                    }
                } 
            }
            tokens.clear();
        }

        /*if (FModel.getPreferences().getPrefBoolean(FPref.UI_STACK_CREATURES) && !collectedCreatures.isEmpty()) {
            creatures = collectedCreatures;
        } else {
            creatures = creaturesRegular;
        }*/
        
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
        if (null == lastTemplate) 
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

    // Position all card panels
    private void positionAllCards(List<CardStackRow> template)  {
        int x = 0;
        int y = PlayArea.GUTTER_Y;

        //System.out.println("-------- " + (mirror ? "^" : "_") + " (Positioning) Card width = " + cardWidth + ". Playarea = " + playAreaWidth + " x " + playAreaHeight);
        for (final CardStackRow row : template) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (int stackIndex = 0, stackCount = row.size(); stackIndex < stackCount; stackIndex++) {
                final CardStack stack = row.get(stackIndex);
                // Align others to the right.
                if (RowType.Other.isGoodFor(stack.get(0).getCard().getCurrentState())) {
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
                    //System.out.println("... placinng " + panel.getCard() + " @ (" + panelX + ", " + panelY + ")");
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

        //System.out.println("======== "  + (mirror ? "^" : "_") + " (try arrange) Card width = " + cardWidth + ". PlayArea = " + playAreaWidth + " x " + playAreaHeight + " ========");
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

        if (!landsFit || !creaturesFit || !tokensFit) { 
            return null;
        }
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
            if (nextOther == cntOthers) {
                return template; // everything was successfully placed
            }
        }

        template = templateCopy;
        // Try to put others on their own row(s)
        if (this.planRow(others, template, afterFirstRow)) {
            return template;
        }
        return null; // Cannot fit everything with that width;
    }

    // Won't modify the first parameter
    private boolean planRow(final CardStackRow sourceRow, final List<CardStackRow> template, final int insertIndex) {
        // The cards are sure to fit (with vertical scrolling) at the minimum
        // card width.
        final boolean isMinimalSize = this.getCardWidth() == this.getCardWidthMin();

        CardStackRow currentRow = new CardStackRow();
        for (final CardStack stack : sourceRow) {
            final int rowWidth = currentRow.getWidth();
            final int stackWidth = stack.getWidth();
            //System.out.printf("Adding %s (+%dpx), current row is %dpx and has %s \n", stack, stackWidth, rowWidth, currentRow); 
            // If the row is not empty and this stack doesn't fit, add the row.
            if (rowWidth + stackWidth > this.playAreaWidth && !currentRow.isEmpty()) {

                // Stop processing if the row is too wide or tall.
                if (rowWidth > this.playAreaWidth || this.getRowsHeight(template) + sourceRow.getHeight() > this.playAreaHeight) {
                    if (!isMinimalSize) 
                        return false;
                }

                if (insertIndex == -1)
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
                if (insertIndex == -1)
                    template.add(currentRow);
                else 
                    template.add(insertIndex, currentRow);
            } else return false;
        }
        //System.out.println("... row complete! " + currentRow.getWidth() + "px");
        return true;
    }

    private int planOthersRow(final List<CardStack> sourceRow, final int firstPile, final List<CardStackRow> template, final CardStackRow rowToFill) {
        int rowWidth = rowToFill.getWidth();

        //System.out.println("This row has:" + rowToFill + "; want to add:" + sourceRow);
        for (int i = firstPile; i < sourceRow.size(); i++) {
            CardStack stack = sourceRow.get(i);

            rowWidth += stack.getWidth();
            if (rowWidth > this.playAreaWidth) return i; // cannot add any more piles in a row 
            
            if (stack.getHeight() > rowToFill.getHeight()) { // if row becomes taller
                int newAllRowsHeight = this.getRowsHeight(template) - rowToFill.getHeight() + stack.getHeight();
                if (newAllRowsHeight > this.playAreaHeight) {
                    return i; // refuse to add here because it won't fit in height
                }
            }
            rowToFill.add(stack);
        }
        return sourceRow.size();
    }

    private int getRowsHeight(final List<CardStackRow> rows) {
        int height = 0;
        for (final CardStackRow row : rows) {
            height += row.getHeight();
        }
        return (height - this.cardSpacingY) + (PlayArea.GUTTER_Y * 2);
    }

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

    @Override
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().setCard(panel.getCard(), evt.isShiftDown());
        super.mouseOver(panel, evt);
    }

    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        selectCard(panel, new MouseTriggerEvent(evt), evt.isShiftDown()); //select entire stack if shift key down
        if ((panel.getTappedAngle() != 0) && (panel.getTappedAngle() != CardPanel.TAPPED_ANGLE)) {
            return;
        }
        super.mouseLeftClicked(panel, evt);
    }

    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        selectCard(panel, new MouseTriggerEvent(evt), evt.isShiftDown()); //select entire stack if shift key down
        super.mouseRightClicked(panel, evt);
    }

    private boolean selectCard(final CardPanel panel, final MouseTriggerEvent triggerEvent, final boolean selectEntireStack) {
        List<CardView> otherCardViewsToSelect = null;
        List<CardPanel> stack = panel.getStack();
        if (selectEntireStack) {
            if (stack != null) {
                for (CardPanel p : stack) {
                    if (p != panel && p.getCard() != null && p.getStack() == stack) {
                        if (otherCardViewsToSelect == null) {
                            otherCardViewsToSelect = new ArrayList<CardView>();
                        }
                        otherCardViewsToSelect.add(p.getCard());
                    }
                }
            }
        }
        if (getMatchUI().getGameController().selectCard(panel.getCard(), otherCardViewsToSelect, triggerEvent)) {
            return true;
        }
        //if panel can't do anything with card selection, try selecting previous panel in stack
        if (stack != null) {
            for (int i = stack.indexOf(panel) + 1; i < stack.size(); i++) { //looping forward since panels stored in reverse order
                CardPanel p = stack.get(i);
                if (p.getStack() == stack && selectCard(stack.get(i), triggerEvent, selectEntireStack)) {
                    return true;
                }
            }
        }
        //as a last resort try to select attached panels not in stack
        for (CardPanel p : panel.getAttachedPanels()) {
            if (p.getStack() != stack) { //ensure same panel not checked more than once
                if (selectCard(p, triggerEvent, selectEntireStack)) {
                    return true;
                }
            }
        }
        getMatchUI().flashIncorrectAction();
        return false;
    }

    public void update() {
        FThreads.assertExecutedByEdt(true);
        recalculateCardPanels(model, zone);
    }

    private void recalculateCardPanels(final PlayerView model, final ZoneType zone) {
        final List<CardView> modelCopy;
        synchronized (model) {
            Iterable<CardView> cards = model.getCards(zone);
            if (cards != null) {
                modelCopy = Lists.newArrayList(cards);
            }
            else {
                modelCopy = Lists.newArrayList();
            }
        }

        final List<CardView> oldCards = Lists.newArrayList();
        for (final CardPanel cpa : getCardPanels()) {
            oldCards.add(cpa.getCard());
        }

        final List<CardView> toDelete = Lists.newArrayList(oldCards);
        final List<CardView> notToDelete = Lists.newLinkedList();
        for (final CardView c : modelCopy) {
            for (int i = 0; i  < toDelete.size(); i++) {
                final CardView c2 = toDelete.get(i);
                if (c.getId() == c2.getId()) {
                    notToDelete.add(c2);
                }
            }
        }
        toDelete.removeAll(notToDelete);

        if (toDelete.size() == getCardPanels().size()) {
            clear();
        }
        else {
            for (final CardView card : toDelete) {
                removeCardPanel(getCardPanel(card.getId()));
            }
        }

        final List<CardView> toAdd = new ArrayList<CardView>(modelCopy);
        toAdd.removeAll(oldCards);

        final List<CardPanel> newPanels = new ArrayList<CardPanel>();
        for (final CardView card : toAdd) {
            final CardPanel placeholder = new CardPanel(getMatchUI(), card);
            placeholder.setDisplayEnabled(false);
            this.getCardPanels().add(placeholder);
            this.add(placeholder);
            newPanels.add(placeholder);
        }

        boolean needLayoutRefresh = !newPanels.isEmpty();
        for (final CardView card : modelCopy) {
            if (updateCard(card, true)) {
                needLayoutRefresh = true;
            }
        }
        if (needLayoutRefresh) {
            doLayout();
        }

        if (!newPanels.isEmpty()) {
            for (final CardPanel toPanel : newPanels) {
                scrollRectToVisible(new Rectangle(toPanel.getCardX(), toPanel.getCardY(), toPanel.getCardWidth(), toPanel.getCardHeight()));
                Animation.moveCard(toPanel);
            }
        }

        invalidate();
        repaint();
    }

    public boolean updateCard(final CardView card, boolean fromRefresh) {
        final CardPanel toPanel = getCardPanel(card.getId());
        if (toPanel == null) { return false; }

        boolean needLayoutRefresh = false;
        if (card.isTapped()) {
            toPanel.setTapped(true);
            toPanel.setTappedAngle(forge.view.arcane.CardPanel.TAPPED_ANGLE);
        }
        else {
            toPanel.setTapped(false);
            toPanel.setTappedAngle(0);
        }
        toPanel.getAttachedPanels().clear();

        if (card.isEnchanted()) {
            final Iterable<CardView> enchants = card.getEnchantedBy();
            for (final CardView e : enchants) {
                final CardPanel cardE = getCardPanel(e.getId());
                if (cardE != null) {
                    if (cardE.getAttachedToPanel() != toPanel) {
                        cardE.setAttachedToPanel(toPanel);
                        needLayoutRefresh = true; //ensure layout refreshed if any attachments change
                    }
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        if (card.isEquipped()) {
            final Iterable<CardView> equips = card.getEquippedBy();
            for (final CardView e : equips) {
                final CardPanel cardE = getCardPanel(e.getId());
                if (cardE != null) {
                    if (cardE.getAttachedToPanel() != toPanel) {
                        cardE.setAttachedToPanel(toPanel);
                        needLayoutRefresh = true; //ensure layout refreshed if any attachments change
                    }
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        if (card.isFortified()) {
            final Iterable<CardView> fortifications = card.getFortifiedBy();
            for (final CardView f : fortifications) {
                final CardPanel cardE = getCardPanel(f.getId());
                if (cardE != null) {
                    if (cardE.getAttachedToPanel() != toPanel) {
                        cardE.setAttachedToPanel(toPanel);
                        needLayoutRefresh = true; //ensure layout refreshed if any attachments change
                    }
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        CardPanel attachedToPanel;
        if (card.getEnchantingCard() != null) {
            attachedToPanel = getCardPanel(card.getEnchantingCard().getId());
        }
        else if (card.getEquipping() != null) {
            attachedToPanel = getCardPanel(card.getEquipping().getId());
        }
        else if (card.getFortifying() != null) {
            attachedToPanel = getCardPanel(card.getFortifying().getId());
        }
        else {
            attachedToPanel = null;
        }
        if (toPanel.getAttachedToPanel() != attachedToPanel) {
            toPanel.setAttachedToPanel(attachedToPanel);
            needLayoutRefresh = true; //ensure layout refreshed if any attachments change
        }

        toPanel.setCard(card);
        if (fromRefresh) {
            toPanel.updatePTOverlay(); //ensure PT Overlay updated on refresh
        }

        if (needLayoutRefresh && !fromRefresh) {
            doLayout(); //ensure layout refreshed here if not being called from a full refresh
        }
        return needLayoutRefresh;
    }

    private static enum RowType {
        Land,
        Creature,
        CreatureNonToken,
        Other;

        public boolean isGoodFor(final CardStateView stateView) {
            switch (this) {
            case Land:              return stateView.isLand();
            case Creature:          return stateView.isCreature();
            case CreatureNonToken:  return stateView.isCreature() && !stateView.getCard().isToken();
            case Other:             return !stateView.isLand() && !stateView.isCreature();
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
            if (type == RowType.Other) {
                this.addAllOthers(cardPanels, type);
            } else {
                this.addAll(cardPanels, type);
            }
        }

        private void addAll(final List<CardPanel> cardPanels, final RowType type) {
            for (final CardPanel panel : cardPanels) {
                if (!type.isGoodFor(panel.getCard().getCurrentState()) || (panel.getAttachedToPanel() != null)) {
                    continue;
                }
                final CardStack stack = new CardStack();
                stack.add(panel);
                this.add(stack);
            }
        }
        
        /**
         * This is an alternate method to addAll() that sorts "other" cards into stacks 
         * based on sickness, cloning, counters, and cards attached to them. All cards
         * that aren't equipped/enchanted/enchanting/equipping/etc that are otherwise 
         * the same get stacked.
         */
        private void addAllOthers(final List<CardPanel> cardPanels, final RowType type) {
            for (final CardPanel panel : cardPanels) {
                if (!type.isGoodFor(panel.getCard().getCurrentState()) || (panel.getAttachedToPanel() != null)) {
                    continue;
                }
                boolean stackable = false;
                for (final CardStack s : this) {
                    final CardView otherCard = s.get(0).getCard();
                    final CardStateView otherState = otherCard.getCurrentState();
                    final CardView thisCard = panel.getCard();
                    final CardStateView thisState = thisCard.getCurrentState();
                    if (otherState.getName().equals(thisState.getName()) && s.size() < othersStackMax) {
                        if (panel.getAttachedPanels().isEmpty()
                            && thisCard.hasSameCounters(otherCard)
                            && (thisCard.isSick() == otherCard.isSick())
                            && (thisCard.isCloned() == otherCard.isCloned())) {
                            s.add(panel);
                            stackable = true;
                        }
                    }
                }
                if (!stackable) {
                    final CardStack stack = new CardStack();
                    stack.add(panel);
                    this.add(stack);
                }
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
            if (panel.getCard() == null) {
                return false;
            }
            if (super.add(panel)) {
                panel.setStack(this);
                addAttachedPanels(panel);
                return true;
            }
            return false;
        }

        @Override
        public void add(final int index, final CardPanel panel) {
            super.add(index, panel);
            panel.setStack(this);
        }

        private void addAttachedPanels(final CardPanel panel) {
            for (final CardPanel attachedPanel : panel.getAttachedPanels()) {
                if (panel.getCard() != null && super.add(attachedPanel)) {
                    addAttachedPanels(attachedPanel);
                }
            }
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
