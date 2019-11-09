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
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

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
        friendColor = FSkinColor.get(Colors.CLR_NORMAL_TARGETING_ARROW);
        if (friendColor.getAlpha() == 0) {
            friendColor = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(153f / 255f);
        }

        foeDefColor = FSkinColor.get(Colors.CLR_COMBAT_TARGETING_ARROW);
        if (foeDefColor.getAlpha() == 0) {
            foeDefColor = FSkinColor.getStandardColor(new Color(1, 0, 0, 153 / 255f));
        }

        foeAtkColor = FSkinColor.get(Colors.CLR_PWATTK_TARGETING_ARROW);
        if (foeAtkColor.getAlpha() == 0) {
            foeAtkColor = FSkinColor.getStandardColor(new Color(255 / 255f, 138 / 255f, 1 / 255f, 153 / 255f));
        }
    }

    private TargetingOverlay() {
    }

    public static void drawArrow(Graphics g, CardView startCard, CardView endCard) {
        ArcConnection connects;
        if (startCard.getOwner().isOpponentOf((endCard.getOwner()))) {
            if (startCard.isAttacking()) {
                connects = ArcConnection.FoesAttacking;
            } else {
                connects = ArcConnection.FoesBlocking;
            }
        } else {
            connects = ArcConnection.Friends;
        }

        drawArrow(g, CardAreaPanel.get(startCard).getTargetingArrowOrigin(),
                    CardAreaPanel.get(endCard).getTargetingArrowOrigin(),
                    connects);
    }
    public static void drawArrow(Graphics g, CardView startCard, PlayerView targetPlayer, int numplayers) {
        drawArrow(g, CardAreaPanel.get(startCard).getTargetingArrowOrigin(),
                MatchController.getView().getPlayerPanel(targetPlayer).getAvatar().getTargetingArrowOrigin(numplayers),
                ArcConnection.FoesAttacking);
    }
    public static void drawArrow(Graphics g, Vector2 start, CardView targetCard, ArcConnection connects) {
        drawArrow(g, start,
                CardAreaPanel.get(targetCard).getTargetingArrowOrigin(),
                connects);
    }
    public static void drawArrow(Graphics g, Vector2 start, PlayerView targetPlayer, ArcConnection connects) {
        drawArrow(g, start,
                MatchController.getView().getPlayerPanel(targetPlayer).getAvatar().getTargetingArrowOrigin(),
                connects);
    }
    public static void drawArrow(Graphics g, FDisplayObject startCardDisplay, FDisplayObject endCardDisplay, ArcConnection connects) {
        drawArrow(g, CardAreaPanel.getTargetingArrowOrigin(startCardDisplay, false),
                CardAreaPanel.getTargetingArrowOrigin(endCardDisplay, false),
                connects);
    }
    public static void drawArrow(Graphics g, Vector2 start, Vector2 end, ArcConnection connects) {
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

        g.drawArrow(BORDER_THICKNESS, ARROW_THICKNESS, ARROW_SIZE, color, start.x, start.y, end.x, end.y);
    }
}
