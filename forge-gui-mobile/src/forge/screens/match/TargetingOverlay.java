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
    private static FSkinColor friendColor, foeColor;

    public static void updateColors() {
        friendColor = FSkinColor.get(Colors.CLR_NORMAL_TARGETING_ARROW);
        if (friendColor.getAlpha() == 0) {
            friendColor = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(153f / 255f);
        }

        foeColor = FSkinColor.get(Colors.CLR_COMBAT_TARGETING_ARROW);
        if (foeColor.getAlpha() == 0) {
            foeColor = FSkinColor.getStandardColor(new Color(1, 0, 0, 153 / 255f));
        }
    }

    private TargetingOverlay() {
    }

    public static void drawArrow(Graphics g, CardView startCard, CardView endCard) {
        drawArrow(g, CardAreaPanel.get(startCard).getTargetingArrowOrigin(),
                    CardAreaPanel.get(endCard).getTargetingArrowOrigin(),
                    startCard.getOwner().isOpponentOf(endCard.getOwner()));
    }
    public static void drawArrow(Graphics g, Vector2 start, CardView targetCard, boolean connectsFoes) {
        drawArrow(g, start,
                CardAreaPanel.get(targetCard).getTargetingArrowOrigin(),
                connectsFoes);
    }
    public static void drawArrow(Graphics g, Vector2 start, PlayerView targetPlayer, boolean connectsFoes) {
        drawArrow(g, start,
                MatchController.getView().getPlayerPanel(targetPlayer).getAvatar().getTargetingArrowOrigin(),
                connectsFoes);
    }
    public static void drawArrow(Graphics g, FDisplayObject startCardDisplay, FDisplayObject endCardDisplay, boolean connectsFoes) {
        drawArrow(g, CardAreaPanel.getTargetingArrowOrigin(startCardDisplay, false),
                CardAreaPanel.getTargetingArrowOrigin(endCardDisplay, false),
                connectsFoes);
    }
    public static void drawArrow(Graphics g, Vector2 start, Vector2 end, boolean connectsFoes) {
        if (start == null || end == null) { return; }

        FSkinColor color = connectsFoes ? foeColor : friendColor;
        g.drawArrow(BORDER_THICKNESS, ARROW_THICKNESS, ARROW_SIZE, color, start.x, start.y, end.x, end.y);
    }
}
