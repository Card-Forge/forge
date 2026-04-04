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
import java.util.*;
import java.util.function.BiPredicate;

import com.google.common.collect.Lists;

import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.util.collect.FCollection;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.util.SGuiChoose;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.util.Localizer;
import forge.model.FModel;
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

    private static final int STACK_MAX_CREATURES = 4;
    private static final int STACK_MAX_LANDS = 5;
    private static final int STACK_MAX_TOKENS = 5;
    private static final int STACK_MAX_CONTRAPTIONS = 5;
    private static final int STACK_MAX_OTHERS = 4;

    private final boolean mirror;

    // Cards visually split from their group. Split and non-split cards
    // can't share a stack. doUpdateCard removes entries on tapped state
    // changes, but this does NOT cover undeclare during InputAttack since
    // cards aren't actually tapped yet — click handlers must clean up.
    private final Set<Integer> splitCardIds = new HashSet<>();
    // Combat pairing map; rebuilt each doLayout() from CombatView.
    private Map<Integer, Integer> combatAssignments = Collections.emptyMap();
    // Coalesces multiple invokeLater(doLayout) calls within a single EDT cycle.
    private boolean layoutPending;

    // Computed in layout.
    private List<CardStackRow> rows = new ArrayList<>();
    private int cardWidth, cardHeight;
    private int playAreaWidth, playAreaHeight;
    private int extraCardSpacingX, cardSpacingX, cardSpacingY;
    private int stackSpacingX, stackSpacingY;

    private final PlayerView model;
    private final ZoneType zone;

    private boolean makeTokenRow = true;
    private boolean stackCreatures = false;
    private boolean groupTokensAndCreatures;
    private boolean groupAll;
    private boolean grouping; // groupTokensAndCreatures || groupAll

    public PlayArea(final CMatchUI matchUI, final FScrollPane scrollPane, final boolean mirror, final PlayerView player, final ZoneType zone) {
        super(matchUI, scrollPane);
        this.setBackground(Color.white);
        this.mirror = mirror;
        this.model = player;
        this.zone = zone;
        updateGroupScope();
    }

    private void updateGroupScope() {
        String groupScope = FModel.getPreferences().getPref(FPref.UI_GROUP_PERMANENTS);
        this.stackCreatures = "stack".equals(groupScope);
        this.groupTokensAndCreatures = "group_creatures".equals(groupScope) || "group_all".equals(groupScope);
        this.groupAll = "group_all".equals(groupScope);
        this.grouping = groupTokensAndCreatures || groupAll;
    }

    private CardStackRow collectAllLands(List<CardPanel> remainingPanels) {
        return collectStacked(remainingPanels, RowType.Land,
                (card, first) -> card.hasSameCounters(first)
                        && (!groupAll || card.isTapped() == first.isTapped())
                        && (!groupAll || card.getDamage() == first.getDamage()),
                STACK_MAX_LANDS, groupAll);
    }

    private CardStackRow collectAllTokens(List<CardPanel> remainingPanels) {
        return collectStacked(remainingPanels, RowType.Token,
                (card, first) -> card.hasSameCounters(first)
                        && card.isSick() == first.isSick()
                        && card.hasSamePT(first)
                        && card.getText().equals(first.getText())
                        && (!groupTokensAndCreatures || card.isTapped() == first.isTapped())
                        && (!groupTokensAndCreatures || card.getDamage() == first.getDamage()),
                STACK_MAX_TOKENS, groupTokensAndCreatures);
    }

    private CardStackRow collectAllCreatures(List<CardPanel> remainingPanels) {
        if (!this.stackCreatures && !this.groupTokensAndCreatures) {
            return collectUnstacked(remainingPanels, RowType.Creature);
        }
        return collectStacked(remainingPanels, RowType.Creature,
                (card, first) -> !card.isCloned()
                        && card.hasSameCounters(first)
                        && card.isSick() == first.isSick()
                        && card.hasSamePT(first)
                        && (!groupTokensAndCreatures || card.isTapped() == first.isTapped())
                        && (!groupTokensAndCreatures || card.getDamage() == first.getDamage())
                        && (!groupTokensAndCreatures || card.getText().equals(first.getText())),
                STACK_MAX_CREATURES, groupTokensAndCreatures);
    }

    private CardStackRow collectStacked(List<CardPanel> remainingPanels, RowType type,
            BiPredicate<CardView, CardView> isCompatible, int stackMax, boolean unlimitedGrouping) {
        final CardStackRow out = new CardStackRow();
        outerLoop:
        for (Iterator<CardPanel> iterator = remainingPanels.iterator(); iterator.hasNext(); ) {
            CardPanel panel = iterator.next();
            final CardView card = panel.getCard();
            final CardStateView state = card.getCurrentState();
            if (!type.isGoodFor(state)) {
                continue;
            }
            int insertIndex = -1;
            for (int i = 0, n = out.size(); i < n; i++) {
                final CardStack stack = out.get(i);
                final CardPanel firstPanel = stack.get(0);
                final CardView firstCard = firstPanel.getCard();
                if (!firstCard.getCurrentState().getOracleName().equals(state.getOracleName())) {
                    if (insertIndex != -1) { break; }
                    continue;
                }
                // First card in stack has attachments — insert before this stack
                if (!firstPanel.getAttachedPanels().isEmpty() || firstCard.hasCardAttachments()) {
                    insertIndex = i;
                    break;
                }
                // Split cards can't group with non-split cards
                if (splitCardIds.contains(card.getId()) != splitCardIds.contains(firstCard.getId())) {
                    insertIndex = i + 1;
                    continue;
                }
                // Cards with different combat pairings can't group together.
                // Must unbox to int — != on boxed Integer compares references,
                // not values, for IDs >= 128.
                int cardAssign = combatAssignments.getOrDefault(card.getId(), 0);
                int firstAssign = combatAssignments.getOrDefault(firstCard.getId(), 0);
                if (cardAssign != firstAssign) {
                    insertIndex = i + 1;
                    continue;
                }
                if (!panel.getAttachedPanels().isEmpty()
                        || !isCompatible.test(card, firstCard)
                        || (!unlimitedGrouping && stack.size() >= stackMax)) {
                    insertIndex = i + 1;
                    continue;
                }
                stack.add(0, panel);
                iterator.remove();
                continue outerLoop;
            }
            final CardStack stack = new CardStack();
            stack.add(panel);
            iterator.remove();
            out.add(insertIndex == -1 ? out.size() : insertIndex, stack);
        }
        return out;
    }

    private CardStackRow collectAllContraptions(List<CardPanel> remainingPanels) {
        final CardStackRow allContraptions = new CardStackRow();
        Map<Integer, List<CardStack>> stacksBySprocket = new HashMap<>();
        outerLoop:
        for (Iterator<CardPanel> iterator = remainingPanels.iterator(); iterator.hasNext(); ) {
            CardPanel panel = iterator.next();
            final CardView card = panel.getCard();
            final CardStateView state = card.getCurrentState();

            if (!RowType.Contraption.isGoodFor(state)) {
                continue;
            }

            //Attractions go on 4, anything on -1 (i.e. pending new sprocket) goes on 0.
            int sprocket = state.isAttraction() ? 4 : Math.max(card.getSprocket(), 0);

            List<CardStack> sprocketStacks = stacksBySprocket.computeIfAbsent(sprocket, k -> new ArrayList<>());

            if(panel.getAttachedPanels().isEmpty()) {
                for (final CardStack stack : sprocketStacks) {
                    if (stack.size() >= STACK_MAX_CONTRAPTIONS)
                        continue;
                    final CardPanel firstPanel = stack.get(0);
                    if (!firstPanel.getAttachedPanels().isEmpty())
                        continue;

                    // Add to stack.
                    stack.add(0, panel);
                    iterator.remove();
                    continue outerLoop;
                }
            }

            final CardStack stack = new CardStack();
            stack.alignRight = true;
            stack.add(panel);
            iterator.remove();
            sprocketStacks.add(stack);
        }
        //Take the lists of stacks, make sure they're arranged by sprocket, flatten them into a
        //big list of card stacks, and dump them into our output CardStackRow.
        stacksBySprocket.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .forEach(allContraptions::add);
        return allContraptions;
    }

    private CardStackRow collectUnstacked(List<CardPanel> remainingPanels, RowType type) {
        CardStackRow out = new CardStackRow();
        for (Iterator<CardPanel> iterator = remainingPanels.iterator(); iterator.hasNext(); ) {
            CardPanel p = iterator.next();
            if (!type.isGoodFor(p.getCard().getCurrentState())) {
                continue;
            }
            final CardStack stack = new CardStack();
            stack.add(p);
            iterator.remove();
            out.add(stack);
        }
        return out;
    }

    private CardStackRow collectAllOthers(final List<CardPanel> remainingPanels) {
        CardStackRow out = collectStacked(remainingPanels, RowType.Other,
                (card, first) -> card.hasSameCounters(first)
                        && card.isSick() == first.isSick()
                        && card.isCloned() == first.isCloned()
                        && (!groupAll || card.isTapped() == first.isTapped())
                        && (!groupAll || card.getDamage() == first.getDamage()),
                STACK_MAX_OTHERS, groupAll);
        for (CardStack stack : out) {
            stack.alignRight = true;
        }
        return out;
    }

    @Override
    public final CardPanel addCard(final CardView card) {
        final CardPanel placeholder = new CardPanel(getMatchUI(), card);
        placeholder.setDisplayEnabled(false);
        this.getCardPanels().add(placeholder);
        this.add(placeholder);
        return placeholder;
    }

    // Builds a combat-assignment map used to prevent grouping of cards with
    // different combat pairings. Maps blocker→attacker ID (so blockers of
    // different attackers stay separate), attacker→defender+blockerHash (so
    // attackers assigned to different defenders or with different blocker
    // sets stay separate).
    private Map<Integer, Integer> buildCombatAssignments() {
        if (getMatchUI().getGameView() == null) { return Collections.emptyMap(); }
        CombatView combat = getMatchUI().getGameView().getCombat();
        if (combat == null) { return Collections.emptyMap(); }
        Map<Integer, Integer> assignments = new HashMap<>();
        for (CardView attacker : combat.getAttackers()) {
            // High bit ensures combat assignments are always negative, never
            // colliding with the default 0 for non-combat cards
            GameEntityView defender = combat.getDefender(attacker);
            int assignment = (defender != null ? defender.getId() : 0) | Integer.MIN_VALUE;

            FCollection<CardView> blockers = combat.getPlannedBlockers(attacker);
            if (blockers != null && !blockers.isEmpty()) {
                for (CardView blocker : blockers) {
                    assignments.put(blocker.getId(), attacker.getId() | Integer.MIN_VALUE);
                }
                // Multiplicative hash, not XOR — XOR of certain ID
                // combinations cancels to 0
                int blockerHash = 1;
                for (CardView blocker : blockers) {
                    blockerHash = 31 * blockerHash + blocker.getId();
                }
                assignment ^= blockerHash;
            }
            assignments.put(attacker.getId(), assignment);
        }
        return assignments;
    }

    @Override
    public final void doLayout() {
        this.makeTokenRow = FModel.getPreferences().getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW);
        updateGroupScope();
        combatAssignments = buildCombatAssignments();
        final Rectangle rect = this.getScrollPane().getVisibleRect();

        this.playAreaWidth = rect.width;
        this.playAreaHeight = rect.height;

        List<CardPanel> unsorted = new LinkedList<>(this.getCardPanels());
        unsorted.removeIf(p -> p.getAttachedToPanel() != null);

        final CardStackRow lands = collectAllLands(unsorted);
        final CardStackRow tokens = collectAllTokens(unsorted);
        final CardStackRow creatures = collectAllCreatures(unsorted);
        final CardStackRow contraptions = collectAllContraptions(unsorted);
        final CardStackRow others = collectAllOthers(unsorted);

        if (!makeTokenRow) {
            for (CardStack s : tokens) {
                if (s.isEmpty())
                    continue;
                CardStateView state = s.get(0).getCard().getCurrentState();
                if (RowType.Creature.isGoodFor(state)) {
                    creatures.add(s);
                }
                else {
                    s.alignRight = true;
                    others.add(s);
                }
            }
            tokens.clear();
        }

        if(!contraptions.isEmpty()) {
            contraptions.stream()
                    .filter(s -> !s.isEmpty())
                    .forEachOrdered(others::add);
        }

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
        assert(lastTemplate != null); //Layout failure at every possible size?

        this.rows = lastTemplate;
        // Get size of all the rows.
        int x, y = PlayArea.GUTTER_Y;
        int maxRowWidth = 0;
        for (final CardStackRow row : this.rows) {
            int rowBottom = 0;
            x = PlayArea.GUTTER_X;
            for (final CardStack stack : row) {
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
            maxRowWidth = Math.max(maxRowWidth, x);
        }
        this.setPreferredSize(new Dimension(maxRowWidth - this.cardSpacingX, y - this.cardSpacingY));
        this.revalidate();
        positionAllCards(lastTemplate);
        repaint();

        super.doLayout();
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
                if (stack.alignRight) {
                    x = (this.playAreaWidth - PlayArea.GUTTER_X) + this.extraCardSpacingX;
                    for (int i = stackIndex, n = row.size(); i < n; i++) {
                        CardStack r = row.get(i);
                        x -= r.getWidth();
                    }
                }
                int maxVisible = 4;

                for (CardPanel p : stack) { p.setGroupCount(0); }

                for (int panelIndex = 0, panelCount = stack.size(); panelIndex < panelCount; panelIndex++) {
                    final CardPanel panel = stack.get(panelIndex);
                    this.setComponentZOrder(panel, panelIndex);

                    int visualPos;
                    boolean hidden;
                    if (grouping && panelCount > maxVisible) {
                        if (panelIndex < maxVisible) {
                            visualPos = maxVisible - 1 - panelIndex;
                            hidden = false;
                        } else {
                            visualPos = 0;
                            hidden = true;
                        }
                    } else {
                        visualPos = panelCount - panelIndex - 1;
                        hidden = false;
                    }

                    final int panelX = x + (visualPos * this.stackSpacingX);
                    final int panelY = y + (visualPos * this.stackSpacingY);
                    panel.setCardBounds(panelX, panelY, this.getCardWidth(), this.cardHeight);
                    panel.setDisplayEnabled(!hidden);
                }
                // Exclude attached panels (equipment/auras) — they're pulled
                // into the stack by addAttachedPanels but aren't grouped cards
                if (grouping) {
                    int groupCount = (int) stack.stream()
                            .filter(p -> p.getAttachedToPanel() == null).count();
                    if (groupCount > 1) {
                        stack.get(0).setGroupCount(groupCount);
                    }
                }
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
            }
            y = rowBottom;
        }
    }

    private List<CardStackRow> tryArrangePilesOfWidth(final CardStackRow lands, final CardStackRow tokens, final CardStackRow creatures, CardStackRow others) {
        List<CardStackRow> template = new ArrayList<>();
        
        int afterFirstRow;

        //System.out.println("======== "  + (mirror ? "^" : "_") + " (try arrange) Card width = " + cardWidth + ". PlayArea = " + playAreaWidth + " x " + playAreaHeight + " ========");
        boolean allFit = true;
        if (this.mirror) {
            // Wrap all creatures and lands.
            allFit &= this.planRow(lands, template, -1);
            afterFirstRow = template.size();
            allFit &= this.planRow(tokens, template, afterFirstRow);
            allFit &= this.planRow(creatures, template, template.size());
        } else {
            // Wrap all creatures and lands.
            allFit &= this.planRow(creatures, template, -1);
            afterFirstRow = template.size();
            allFit &= this.planRow(tokens, template, afterFirstRow);
            allFit &= this.planRow(lands, template, template.size());
        }

        if (!allFit) {
            return null;
        }
        // Other cards may be stored at end of usual rows or on their own row.
        int cntOthers = others.size();

        // Copy the template for the case 1st approach won't work
        final List<CardStackRow> templateCopy = new ArrayList<>(template.size());
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
        if(sourceRow.isEmpty())
            return true;
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
        boolean isLocal = getMatchUI().isLocalPlayer(model);
        boolean selectAll = evt.isShiftDown();
        if (!selectAll && panel.getGroupCount() >= 2) {
            selectAll = panel.isBadgeHit(evt.getX(), evt.getY());
        }
        // Badge click on tapped cards — undo once per card to reverse a batch
        // mana tap, rather than trying to activate (which shows disabled abilities).
        if (selectAll && panel.getCard() != null && panel.getCard().isTapped()
                && panel.getStack() != null) {
            for (int i = 0; i < panel.getStack().size(); i++) {
                getMatchUI().getGameController().undoLastAction();
            }
            doLayout();
            return;
        }
        if (!selectAll && panel.getCard() != null) {
            if (splitCardIds.contains(panel.getCard().getId())) {
                // Re-clicking a split card un-splits it (merges back into group)
                // and undeclares via selectCard in one operation.
                splitCardIds.remove(panel.getCard().getId());
                selectCard(panel, new MouseTriggerEvent(evt), false);
                doLayout();
                if ((panel.getTappedAngle() != 0) && (panel.getTappedAngle() != CardPanel.TAPPED_ANGLE)) {
                    return;
                }
                super.mouseLeftClicked(panel, evt);
                return;
            }
            List<CardPanel> stack = panel.getStack();
            if (stack != null && stack.size() >= (grouping ? 2 : 5)
                    && isLocal) {
                // Split from group, then check if the game accepts this card.
                // If accepted, doUpdateCard will remove from splitCardIds when
                // the card's tapped state changes, allowing it to regroup.
                splitCardIds.add(panel.getCard().getId());
                if (getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt))) {
                    doLayout();
                    if ((panel.getTappedAngle() != 0) && (panel.getTappedAngle() != CardPanel.TAPPED_ANGLE)) {
                        return;
                    }
                    super.mouseLeftClicked(panel, evt);
                    return;
                }
                splitCardIds.remove(panel.getCard().getId());
            }
        }
        boolean selected = selectCard(panel, new MouseTriggerEvent(evt), selectAll);
        // If this individual card was accepted (e.g. declared as attacker), mark it
        // as split so it can merge with other split cards from the same group.
        // Only split on the local player's own field — clicking opponent's cards
        // (e.g. selecting an attacker during declare blockers) should not split.
        if (selected && !selectAll && panel.getCard() != null && isLocal) {
            splitCardIds.add(panel.getCard().getId());
        }
        doLayout();
        if ((panel.getTappedAngle() != 0) && (panel.getTappedAngle() != CardPanel.TAPPED_ANGLE)) {
            return;
        }
        super.mouseLeftClicked(panel, evt);
    }

    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        if (panel.getGroupCount() >= 2 && panel.isBadgeHit(evt.getX(), evt.getY())) {
            List<CardPanel> stack = panel.getStack();
            if (stack != null && stack.size() >= 2) {
                handleBadgeRightClick(stack, evt);
                return;
            }
        }
        boolean selected = selectCard(panel, new MouseTriggerEvent(evt), evt.isShiftDown());
        // Right-click undeclare: remove from splitCardIds so card merges back
        if (selected && panel.getCard() != null) {
            splitCardIds.remove(panel.getCard().getId());
            if (evt.isShiftDown() && panel.getStack() != null) {
                for (CardPanel p : panel.getStack()) {
                    if (p.getCard() != null) {
                        splitCardIds.remove(p.getCard().getId());
                    }
                }
            }
            doLayout();
        }
        super.mouseRightClicked(panel, evt);
    }

    private void handleBadgeRightClick(final List<CardPanel> stack, final MouseEvent evt) {
        CardView primary = stack.get(0).getCard();
        String activateDesc = primary != null
                ? getMatchUI().getGameController().getActivateDescription(primary) : null;
        if (activateDesc == null) {
            getMatchUI().flashIncorrectAction();
            return;
        }
        Localizer loc = Localizer.getInstance();
        boolean alreadyInCombat = primary.isAttacking() || primary.isBlocking();
        String prompt;
        if (alreadyInCombat) {
            prompt = loc.getMessage("lblGroupHowManyRemove");
        } else if (activateDesc.equals(loc.getMessage("lblAttackWithCard"))) {
            prompt = loc.getMessage("lblGroupHowManyAttack");
        } else if (activateDesc.equals(loc.getMessage("lblBlockWithCard"))) {
            prompt = loc.getMessage("lblGroupHowManyBlock");
        } else {
            prompt = loc.getMessage("lblGroupHowManySelect");
        }
        Integer count = SGuiChoose.getInteger(prompt, 1, stack.size());
        if (count == null) {
            return; // cancelled
        }
        List<CardView> selected = new ArrayList<>();
        selected.add(primary);
        for (int i = 1; i < count && i < stack.size(); i++) {
            CardPanel p = stack.get(i);
            if (p.getCard() != null) {
                selected.add(p.getCard());
            }
        }
        List<CardView> others = selected.size() > 1 ? selected.subList(1, selected.size()) : null;
        boolean isCombat = alreadyInCombat
                || activateDesc.equals(loc.getMessage("lblAttackWithCard"))
                || activateDesc.equals(loc.getMessage("lblBlockWithCard"));
        if (alreadyInCombat) {
            MouseTriggerEvent rightClickTrigger = new MouseTriggerEvent(3, evt.getX(), evt.getY());
            getMatchUI().getGameController().selectCard(primary, others, rightClickTrigger);
        } else if (isCombat) {
            MouseTriggerEvent leftClickTrigger = new MouseTriggerEvent(1, evt.getX(), evt.getY());
            getMatchUI().getGameController().selectCard(primary, others, leftClickTrigger);
        } else {
            // Non-combat inputs (sacrifice, targeting) ignore otherCardsToSelect
            MouseTriggerEvent leftClickTrigger = new MouseTriggerEvent(1, evt.getX(), evt.getY());
            for (CardView cv : selected) {
                getMatchUI().getGameController().selectCard(cv, null, leftClickTrigger);
            }
        }
        // Update splitCardIds: clear whole stack, then mark the subset that
        // should remain visually separate.
        for (CardPanel p : stack) {
            if (p.getCard() != null) {
                splitCardIds.remove(p.getCard().getId());
            }
        }
        if (alreadyInCombat) {
            // Undeclaring: selected cards merge back. Non-selected stay split.
            Set<Integer> selectedIds = new HashSet<>();
            for (CardView cv : selected) {
                selectedIds.add(cv.getId());
            }
            for (CardPanel p : stack) {
                if (p.getCard() != null && !selectedIds.contains(p.getCard().getId())) {
                    splitCardIds.add(p.getCard().getId());
                }
            }
        } else {
            // Declaring/selecting: selected cards split from the rest.
            for (CardView cv : selected) {
                splitCardIds.add(cv.getId());
            }
        }
        doLayout();
    }

    private boolean selectCard(final CardPanel panel, final MouseTriggerEvent triggerEvent, final boolean selectEntireStack) {
        List<CardView> otherCardViewsToSelect = null;
        List<CardPanel> stack = panel.getStack();
        if (selectEntireStack) {
            if (stack != null) {
                for (CardPanel p : stack) {
                    if (p != panel && p.getCard() != null && p.getStack() == stack) {
                        if (otherCardViewsToSelect == null) {
                            otherCardViewsToSelect = new ArrayList<>();
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
        splitCardIds.clear();
        // Don't clear combatAssignments — doLayout() rebuilds from CombatView.
        // Clearing here races with network play: updateZones() runs AFTER
        // showCombat() in the same processEvents pass, wiping assignments
        recalculateCardPanels(model, zone);
    }

    private void recalculateCardPanels(final PlayerView model, final ZoneType zone) {
        final List<CardView> modelCopy;
        synchronized (model) {
            Iterable<CardView> cards = model.getCards(zone);
            if (cards != null) {
                modelCopy = Lists.newArrayList(cards);
            } else {
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
            for (final CardView c2 : toDelete) {
                if (c.getId() == c2.getId()) {
                    notToDelete.add(c2);
                }
            }
        }
        toDelete.removeAll(notToDelete);

        if (toDelete.size() == getCardPanels().size()) {
            clear(false);
        } else {
            for (final CardView card : toDelete) {
                removeCardPanel(getCardPanel(card.getId()),false);
            }
        }

        final List<CardView> toAdd = new ArrayList<>(modelCopy);
        toAdd.removeAll(oldCards);

        final List<CardPanel> newPanels = new ArrayList<>();
        for (final CardView card : toAdd) {
            final CardPanel placeholder = new CardPanel(getMatchUI(), card);
            placeholder.setDisplayEnabled(false);
            this.getCardPanels().add(placeholder);
            this.add(placeholder);
            newPanels.add(placeholder);
        }

        boolean needLayoutRefresh = !newPanels.isEmpty() || !toDelete.isEmpty();
        for (final CardView card : modelCopy) {
            if (doUpdateCard(card, true)) {
                needLayoutRefresh = true;
            }
        }
        if (needLayoutRefresh) {
            doLayout();
        }

        invalidate(); //pfps do the extra invalidate before any scrolling 
        if (!newPanels.isEmpty()) {
            int i = newPanels.size();
            for (final CardPanel toPanel : newPanels) {
                if ( --i == 0 ) { // only scroll to last panel to be added
                    scrollRectToVisible(new Rectangle(toPanel.getCardX(), toPanel.getCardY(), toPanel.getCardWidth(), toPanel.getCardHeight()));
                }
                Animation.moveCard(toPanel);
            }
        }
        repaint();
    }

    public boolean updateCard(final CardView card, boolean fromRefresh) {
        FThreads.assertExecutedByEdt(true);
        boolean result = doUpdateCard(card, fromRefresh);
        repaint();
        return result;
    }

    private boolean doUpdateCard(final CardView card, boolean fromRefresh) {
        final CardPanel toPanel = getCardPanel(card.getId());
        if (toPanel == null) { return false; }

        boolean needLayoutRefresh = false;
        boolean tappedStateChanged = card.isTapped() != toPanel.isTapped();
        if (tappedStateChanged) {
            splitCardIds.remove(card.getId());
        }
        if (card.isTapped()) {
            toPanel.setTapped(true);
            toPanel.setTappedAngle(forge.view.arcane.CardPanel.TAPPED_ANGLE);
        } else {
            toPanel.setTapped(false);
            toPanel.setTappedAngle(0);
        }
        toPanel.getAttachedPanels().clear();

        if (card.hasAnyCardAttachments()) {
            final Iterable<CardView> enchants = card.getAllAttachedCards();
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

        CardPanel attachedToPanel;
        if (card.getAttachedTo() != null) {
            if (card != card.getAttachedTo().getAttachedTo())
                attachedToPanel = getCardPanel(card.getAttachedTo().getId());
            else {
                toPanel.getAttachedPanels().remove(getCardPanel(card.getAttachedTo().getId()));
                CardPanel panel = getCardPanel(card.getAttachedTo().getId());
                if (panel != null)
                    panel.setAttachedToPanel(null);
                attachedToPanel = null;
            }
        } else {
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
        } else if (tappedStateChanged && !fromRefresh) {
            // Deferred layout for tapped state changes - allows split cards to
            // regroup with other tapped attackers after async state updates.
            // Coalesce via layoutPending so multiple rapid tapped-state changes
            // (e.g. declaring 5 attackers at once) only trigger one layout pass.
            if (!layoutPending) {
                layoutPending = true;
                javax.swing.SwingUtilities.invokeLater(() -> { layoutPending = false; doLayout(); });
            }
        }
        return needLayoutRefresh || tappedStateChanged;
    }

    private enum RowType {
        Land,
        Token,
        Creature,
        Contraption,
        Other;

        public boolean isGoodFor(final CardStateView stateView) {
            return switch (this) {
                case Land -> stateView.isLand() && !stateView.isCreature();
                case Token -> stateView.getCard().isToken();
                case Creature -> stateView.isCreature();
                case Contraption -> stateView.isContraption() || stateView.isAttraction();
                case Other -> true;
            };
        }
    }
    
    private class CardStackRow extends ArrayList<CardStack> {
        private static final long serialVersionUID = 716489891951011846L;

        public CardStackRow() {
            super(16);
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

        private boolean alignRight = false;

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
            int visualCount = PlayArea.this.grouping
                ? Math.min(this.size(), 4) : this.size();
            return PlayArea.this.cardWidth + ((visualCount - 1) * PlayArea.this.stackSpacingX)
                    + PlayArea.this.cardSpacingX;
        }

        private int getHeight() {
            int visualCount = PlayArea.this.grouping
                ? Math.min(this.size(), 4) : this.size();
            return PlayArea.this.cardHeight + ((visualCount - 1) * PlayArea.this.stackSpacingY)
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
