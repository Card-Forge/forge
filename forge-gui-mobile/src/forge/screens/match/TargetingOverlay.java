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

import java.util.*;

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

    // assembleArrows collects arrows here, then draws them all with ONE Graphics.drawArrowBatch
    private static final class PendingArrow {
        Vector2 start, end;
        ArcConnection connects;
        boolean solid; // see the attacker -> player arrows in assembleArrows
    }
    private static final ArrayList<PendingArrow> pendingArrows = new ArrayList<>(); // reused every frame (UI thread only)
    private static int pendingCount = 0;

    private static void queueArrow(final Vector2 start, final Vector2 end, final ArcConnection connects, final boolean solid) {
        if (start == null || end == null) { return; }
        final PendingArrow a;
        if (pendingCount < pendingArrows.size()) {
            a = pendingArrows.get(pendingCount);
        } else {
            a = new PendingArrow();
            pendingArrows.add(a);
        }
        a.start = start;
        a.end = end;
        a.connects = connects;
        a.solid = solid;
        pendingCount++;
    }

    private static void flushArrows(final Graphics g) {
        final int count = pendingCount;
        if (count == 0) { return; }
        final String style = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_ARROW_OPTION); // read once, not per arrow per pass
        // the runnable is executed 2-3 times by drawArrowBatch, so it only draws from the prepared list
        g.drawArrowBatch(() -> {
            for (int i = 0; i < count; i++) {
                final PendingArrow a = pendingArrows.get(i);
                drawArrowStyled(g, a.start, a.end, a.connects, style, a.solid);
            }
        });
    }

    private static void clearPending() {
        // drop references to this frame's endpoints so nothing is kept alive between frames
        for (int i = 0; i < pendingCount; i++) {
            final PendingArrow a = pendingArrows.get(i);
            a.start = null;
            a.end = null;
            a.connects = null;
        }
        pendingCount = 0;
    }

    private static void addBlockPair(final Map<CardView, List<CardView>> pairs, final CardView key, final CardView blocker, final CardView attacker) {
        final List<CardView> list = pairs.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(blocker);
        list.add(attacker);
    }

    public static void assembleArrows(final Graphics g, final Set<CardView> cardsonBattlefield, final Map<Integer, Vector2> endpoints, final CombatView combat, final Set<PlayerView> playerViewSet) {
        if (cardsonBattlefield.isEmpty())
            return;
        try {
            // Snapshot the combat data ONCE per frame. CombatView.getAttackers() / getAttackersOf(p) build a new collection on every
            // call, and this used to run for every card on the battlefield (a JFR profile showed it at about a third of the render
            // thread's Java time). Same arrows come out, same numbers of them.
            final List<CardView> blockInfoAttackers = new ArrayList<>(); // attackers that have planned-blocker info (old "cards == null -> continue")
            final Map<CardView, List<CardView>> blockPairs = new HashMap<>(); // card -> blocker, attacker, blocker, attacker, ...
            final Map<PlayerView, Set<CardView>> attackersOfPlayer = new HashMap<>();
            if (null != combat) {
                for (final CardView attackingCard : combat.getAttackers()) {
                    final Iterable<CardView> blockers = combat.getPlannedBlockers(attackingCard);
                    if (blockers == null) continue;
                    blockInfoAttackers.add(attackingCard);
                    for (final CardView blockingCard : blockers) {
                        // as before, a blocker arrow is drawn once while visiting the attacker and once while visiting the blocker
                        addBlockPair(blockPairs, attackingCard, blockingCard, attackingCard);
                        if (!blockingCard.equals(attackingCard)) {
                            addBlockPair(blockPairs, blockingCard, blockingCard, attackingCard);
                        }
                    }
                }
                if (playerViewSet != null) {
                    for (final PlayerView p : playerViewSet) {
                        final Set<CardView> attackers = new HashSet<>();
                        for (final CardView a : combat.getAttackersOf(p)) {
                            attackers.add(a);
                        }
                        attackersOfPlayer.put(p, attackers);
                    }
                }
            }

            for (CardView c : cardsonBattlefield) {
                final CardView attachedTo = c.getAttachedTo();
                final CardView paired = c.getPairedWith();
                if (null != attachedTo) {
                    if (attachedTo.getController() != null && !attachedTo.getController().equals(c.getController())) {
                        queueArrow(endpoints.get(attachedTo.getId()), endpoints.get(c.getId()), ArcConnection.Friends, false);
                    }
                }
                if (null != attachedTo && c == attachedTo.getAttachedTo()) {
                    queueArrow(endpoints.get(attachedTo.getId()), endpoints.get(c.getId()), ArcConnection.Friends, false);
                }
                for (final CardView enc : c.getAttachedCards()) {
                    if (enc.getController() != null && !enc.getController().equals(c.getController())) {
                        queueArrow(endpoints.get(c.getId()), endpoints.get(enc.getId()), ArcConnection.Friends, false);
                    }
                }
                if (null != paired) {
                    queueArrow(endpoints.get(paired.getId()), endpoints.get(c.getId()), ArcConnection.Friends, false);
                }
                if (null != combat) {
                    final GameEntityView defender = combat.getDefender(c);
                    // if c is attacking a planeswalker or battle
                    if (defender instanceof CardView) {
                        queueArrow(endpoints.get(defender.getId()), endpoints.get(c.getId()), ArcConnection.FoesAttacking, false);
                    }
                    // if c is a planeswalker that's being attacked
                    for (final CardView pwAttacker : combat.getAttackersOf(c)) {
                        queueArrow(endpoints.get(c.getId()), endpoints.get(pwAttacker.getId()), ArcConnection.FoesAttacking, false);
                    }
                    // blocker -> attacker arrows that involve this card
                    final List<CardView> pairs = blockPairs.get(c);
                    if (pairs != null) {
                        for (int i = 0; i < pairs.size(); i += 2) {
                            queueArrow(endpoints.get(pairs.get(i).getId()), endpoints.get(pairs.get(i + 1).getId()), ArcConnection.FoesBlocking, false);
                        }
                    }
                }
            }

            // attacker -> player arrows do not depend on the card being iterated, so they are queued once
            if (null != combat && playerViewSet != null) {
                for (final CardView attackingCard : blockInfoAttackers) {
                    for (final PlayerView p : playerViewSet) {
                        if (attackersOfPlayer.get(p).contains(attackingCard)) {
                            final Vector2 vPlayer = MatchScreen.getPlayerPanel(p).getAvatar().getTargetingArrowOrigin();
                            // solid = true: the old repeated drawing stacked the translucent 0.8/0.9 alphas up to fully opaque,
                            // so drawing once at full alpha keeps the same look. Pass false for the intended translucent look.
                            queueArrow(endpoints.get(attackingCard.getId()), vPlayer, ArcConnection.FoesAttacking, true);
                        }
                    }
                }
            }

            flushArrows(g);
        } finally {
            clearPending(); // also covers an exception thrown half way through collecting
        }
    }

    // immediate mode, unchanged behavior: used by any other caller that draws a single arrow
    public static void drawArrow(final Graphics g, final Vector2 start, final Vector2 end, final ArcConnection connects) {
        if (start == null || end == null) { return; }
        drawArrowStyled(g, start, end, connects, FModel.getPreferences().getPref(ForgePreferences.FPref.UI_ARROW_OPTION), false);
    }

    private static void drawArrowStyled(final Graphics g, final Vector2 start, final Vector2 end, final ArcConnection connects, final String style, final boolean solid) {
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

        switch (style) {
            case "Point" -> g.drawCurvedLinePointer(Utils.scale(3), color.getColor(), Color.WHITE, start.x, start.y, end.x, end.y);
            case "Line" -> g.drawLinePointer(Utils.scale(3), color.getColor(), start.x, start.y, end.x, end.y);
            default -> g.drawCurvedArrow(Utils.scale(3),
                    solid ? color.alphaColor(1f).getColor() : color.alphaColor(0.8f).getColor(),
                    solid ? Color.WHITE : FSkinColor.getStandardColor(Color.WHITE).alphaColor(0.9f).getColor(),
                    start.x, start.y, end.x, end.y, ArcConnection.Friends.equals(connects));
        }
    }
}
