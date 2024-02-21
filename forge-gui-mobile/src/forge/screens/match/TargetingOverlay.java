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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Utils;

import java.util.Map;
import java.util.Set;

public class TargetingOverlay {
    private static final float BORDER_THICKNESS = Utils.scale(1);
    private static final float ARROW_THICKNESS = Utils.scale(5);
    private static final float ARROW_SIZE = 3 * ARROW_THICKNESS;
    private static FSkinColor friendColor, foeAtkColor, foeDefColor;

    public enum ArcConnection {
        Friends,
        FoesAttacking,
        FoesBlocking,
        FriendsStackTargeting,
        FoesStackTargeting
    }

    public static void updateColors() {
        friendColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_NORMAL_TARGETING_ARROW) : FSkinColor.get(Colors.CLR_NORMAL_TARGETING_ARROW);
        if (friendColor.getAlpha() == 0) {
            friendColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_ACTIVE).alphaColor(153f / 255f) : FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(153f / 255f);
        }

        foeDefColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_COMBAT_TARGETING_ARROW) : FSkinColor.get(Colors.CLR_COMBAT_TARGETING_ARROW);
        if (foeDefColor.getAlpha() == 0) {
            foeDefColor = FSkinColor.getStandardColor(new Color(1, 0, 0, 153 / 255f));
        }

        foeAtkColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_PWATTK_TARGETING_ARROW) : FSkinColor.get(Colors.CLR_PWATTK_TARGETING_ARROW);
        if (foeAtkColor.getAlpha() == 0) {
            foeAtkColor = FSkinColor.getStandardColor(new Color(255 / 255f, 138 / 255f, 1 / 255f, 153 / 255f));
        }
    }

    private TargetingOverlay() {
    }
    public static void assembleArrows(final Graphics g, final CardView c, final Map<Integer, Vector2> endpoints, final CombatView combat, final Set<PlayerView> playerViewSet) {
        final CardView attachedTo = c.getAttachedTo();
        final Iterable<CardView> attachedCards = c.getAttachedCards();
        final CardView paired = c.getPairedWith();
        if (null != attachedTo) {
            if (attachedTo.getController() != null && !attachedTo.getController().equals(c.getController())) {
                drawArrow(g, endpoints.get(attachedTo.getId()), endpoints.get(c.getId()), ArcConnection.Friends);
            }
        }
        if (null != attachedTo && c == attachedTo.getAttachedTo()) {
            drawArrow(g, endpoints.get(attachedTo.getId()), endpoints.get(c.getId()), ArcConnection.Friends);
        }
        if (null != attachedCards) {
            for (final CardView enc : attachedCards) {
                if (enc.getController() != null && !enc.getController().equals(c.getController())) {
                    drawArrow(g, endpoints.get(c.getId()), endpoints.get(enc.getId()), ArcConnection.Friends);
                }
            }
        }
        if (null != paired) {
            drawArrow(g, endpoints.get(paired.getId()), endpoints.get(c.getId()), ArcConnection.Friends);
        }
        if (null != combat) {
            final GameEntityView defender = combat.getDefender(c);
            // if c is attacking a planeswalker or battle
            if (defender instanceof CardView) {
                drawArrow(g, endpoints.get(defender.getId()), endpoints.get(c.getId()), ArcConnection.FoesAttacking);
            }
            // if c is a planeswalker that's being attacked
            for (final CardView pwAttacker : combat.getAttackersOf(c)) {
                drawArrow(g, endpoints.get(c.getId()), endpoints.get(pwAttacker.getId()), ArcConnection.FoesAttacking);
            }
            for (final CardView attackingCard : combat.getAttackers()) {
                final Iterable<CardView> cards = combat.getPlannedBlockers(attackingCard);
                if (cards == null) continue;
                for (final CardView blockingCard : cards) {
                    if (!attackingCard.equals(c) && !blockingCard.equals(c)) { continue; }
                    drawArrow(g, endpoints.get(attackingCard.getId()), endpoints.get(blockingCard.getId()), ArcConnection.FoesBlocking);
                }
                if (playerViewSet != null) {
                    for (final PlayerView p : playerViewSet) {
                        if (combat.getAttackersOf(p).contains(attackingCard)) {
                            final Vector2 vPlayer = MatchController.getView().getPlayerPanel(p).getAvatar().getTargetingArrowOrigin();
                            drawArrow(g, endpoints.get(attackingCard.getId()), vPlayer, TargetingOverlay.ArcConnection.FoesAttacking);
                        }
                    }
                }
            }
        }
    }
    public static void drawArrow(final Graphics g, final Vector2 start, final Vector2 end, final ArcConnection connects) {
        if (start == null || end == null) { return; }

        FSkinColor color = foeDefColor;

        switch (connects) {
            case Friends:
            case FriendsStackTargeting:
                color = friendColor;
                break;
            case FoesAttacking:
                color = foeAtkColor;
                break;
            case FoesBlocking:
            case FoesStackTargeting:
                color = foeDefColor;
        }

        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_USE_LASER_ARROWS))
            g.drawLineArrow(Utils.scale(3), color, start.x, start.y, end.x, end.y);
        else
            g.drawArrow(BORDER_THICKNESS, ARROW_THICKNESS, ARROW_SIZE, color, start.x, start.y, end.x, end.y);
    }
}
