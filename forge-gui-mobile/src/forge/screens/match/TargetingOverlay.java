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
package forge.screens.match;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VStack;
import forge.screens.match.views.VStack.StackInstanceDisplay;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TargetingOverlay {
    private static final float ARROW_THICKNESS = Utils.scaleMax(5);
    private static final float ARROW_SIZE = 3 * ARROW_THICKNESS;

    private final List<Arrow> combatArrows = new ArrayList<Arrow>();
    private final List<Arrow> targetArrows = new ArrayList<Arrow>();
    private final List<Arrow> pairArrows = new ArrayList<Arrow>();

    public TargetingOverlay() {
    }

    private void refreshCombatArrows() {
        combatArrows.clear();

        final Combat combat = FControl.getGame().getCombat();
        if (combat != null) {
            //connect each attacker with planeswalker it's attacking if applicable
            for (Card planeswalker : combat.getDefendingPlaneswalkers()) {
                for (Card attacker : combat.getAttackersOf(planeswalker)) {
                    combatArrows.add(new Arrow(attacker, planeswalker));
                }
            }

            //connect each blocker with the attacker it's blocking
            for (Card blocker : combat.getAllBlockers()) {
                for (Card attacker : combat.getAttackersBlockedBy(blocker)) {
                    combatArrows.add(new Arrow(blocker, attacker));
                }
            }
        }
    }

    private void refreshTargetArrows() {
        targetArrows.clear();

        VStack stack = FControl.getView().getStack();
        if (stack.isVisible()) {
            for (FDisplayObject child : stack.getChildren()) {
                Vector2 arrowOrigin = new Vector2(
                        VStack.CARD_WIDTH * FCardPanel.TARGET_ORIGIN_FACTOR_X + 2 * VStack.PADDING,
                        VStack.CARD_HEIGHT * FCardPanel.TARGET_ORIGIN_FACTOR_Y + 2 * VStack.PADDING);

                float y = child.getTop() + arrowOrigin.y;
                if (y < 0) {
                    continue; //don't draw arrow scrolled off top
                }
                if (y > child.getHeight()) {
                    break; //don't draw arrow scrolled off bottom
                }

                SpellAbilityStackInstance stackInstance = ((StackInstanceDisplay)child).getStackInstance();
                TargetChoices targets = stackInstance.getSpellAbility().getTargets();
                arrowOrigin = arrowOrigin.add(stack.getScreenPosition());

                for (Card c : targets.getTargetCards()) {
                    targetArrows.add(new Arrow(arrowOrigin, c));
                }
                for (Player p : targets.getTargetPlayers()) {
                    targetArrows.add(new Arrow(arrowOrigin, p));
                }
            }
        }
    }

    private void refreshPairArrows() {
        pairArrows.clear();

        HashSet<Card> pairedCards = new HashSet<Card>();
        for (VPlayerPanel playerPanel : FControl.getView().getPlayerPanels().values()) {
            for (Card card : playerPanel.getField().getRow1().getOrderedCards()) {
                if (pairedCards.contains(card)) { continue; } //prevent arrows going both ways

                Card paired = card.getPairedWith();
                if (paired != null) {
                    pairArrows.add(new Arrow(card, paired));
                    pairedCards.add(paired);
                }
            }
        }
    }

    public void draw(final Graphics g) {
        refreshPairArrows(); //TODO: Optimize so these don't need to be refreshed every render
        if (!pairArrows.isEmpty()) {
            FSkinColor color = FSkinColor.get(Colors.CLR_NORMAL_TARGETING_ARROW);
            if (color.getAlpha() == 0) {
                color = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(153f / 255f);
            }
            for (Arrow arrow : pairArrows) {
                arrow.draw(g, color);
            }
        }

        refreshCombatArrows();
        if (!combatArrows.isEmpty()) {
            FSkinColor color = FSkinColor.get(Colors.CLR_COMBAT_TARGETING_ARROW);
            if (color.getAlpha() == 0) {
                color = FSkinColor.getStandardColor(new Color(1, 0, 0, 153 / 255f));
            }
            for (Arrow arrow : combatArrows) {
                arrow.draw(g, color);
            }
        }

        refreshTargetArrows();
        if (!targetArrows.isEmpty()) {
            FSkinColor color = FSkinColor.get(Colors.CLR_COMBAT_TARGETING_ARROW); //TODO: Consider a different color for targeting
            if (color.getAlpha() == 0) {
                color = FSkinColor.getStandardColor(new Color(1, 0, 0, 153 / 255f));
            }
            for (Arrow arrow : targetArrows) {
                arrow.draw(g, color);
            }
        }
    }

    private static class Arrow {
        private final Vector2 start, end;

        private Arrow(Card startCard, Card endCard) {
            this(CardAreaPanel.get(startCard).getTargetingArrowOrigin(), CardAreaPanel.get(endCard).getTargetingArrowOrigin());
        }
        private Arrow(Card card, Player player) {
            this(CardAreaPanel.get(card).getTargetingArrowOrigin(), FControl.getPlayerPanel(player).getAvatar().getTargetingArrowOrigin());
        }
        private Arrow(Vector2 start0, Card targetCard) {
            this(start0, CardAreaPanel.get(targetCard).getTargetingArrowOrigin());
        }
        private Arrow(Vector2 start0, Player targetPlayer) {
            this(start0, FControl.getPlayerPanel(targetPlayer).getAvatar().getTargetingArrowOrigin());
        }
        private Arrow(Vector2 start0, Vector2 end0) {
            start = start0;
            end = end0;
        }
        
        private void draw(Graphics g, FSkinColor color) {
            if (start == null || end == null) { return; }

            g.drawArrow(ARROW_THICKNESS, ARROW_SIZE, color, start.x, start.y, end.x, end.y);
        }
    }
}
