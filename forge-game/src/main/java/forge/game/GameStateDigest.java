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
package forge.game;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/**
 * A canonical, order-sensitive dump of a game state, and its digest.
 *
 * <p>The AI performance plan makes exact state identity the pass criterion for behaviour-equivalent
 * optimisations, and requires a fixture's state to be verified before it is timed. This class
 * supplies both: {@link #canonicalDump(Game)} produces text that {@code diff} can explain, and
 * {@link #digest(Game)} reduces it to a short hash for cheap comparison in assertions and logs.</p>
 *
 * <h2>What is included</h2>
 * <p>Turn, phase, active and priority player; each player's life and counters; every zone's contents
 * <em>in zone order</em> — including hidden zones, because library order changes later decisions;
 * per-card identity, name, owner, controller, tapped/sick/face-down/phased-out status, timestamp and
 * counters; the stack from top to bottom; and the current combat's attackers, defenders and ordered
 * blockers.</p>
 *
 * <h2>What is deliberately not included</h2>
 * <p>Derived characteristics such as computed power/toughness, keyword sets and applied continuous
 * effects. Reading those can trigger recalculation and view updates, and a diagnostic must not be
 * able to change the thing it observes. They are also redundant: they are a function of the state
 * that <em>is</em> captured, so a divergence in them implies a divergence here.</p>
 *
 * <p>Every accessor used is read-only. Computing a digest never mutates the game.</p>
 */
public final class GameStateDigest {
    private GameStateDigest() {
    }

    /** SHA-256 of {@link #canonicalDump(Game)}, lower-case hex. */
    public static String digest(final Game game) {
        return digestOf(canonicalDump(game));
    }

    /** SHA-256 of arbitrary text, lower-case hex. Falls back to a string hash if SHA-256 is absent. */
    public static String digestOf(final String text) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha.digest(text.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            // Every supported platform provides SHA-256; this keeps the harness usable if one does not.
            return "nohash-" + Integer.toHexString(text.hashCode());
        }
    }

    /** The full canonical text form of {@code game}. Lines are ordered deterministically. */
    public static String canonicalDump(final Game game) {
        final StringBuilder sb = new StringBuilder(4096);
        if (game == null) {
            return "game null\n";
        }

        final PhaseHandler phase = game.getPhaseHandler();
        sb.append("game turn=").append(phase == null ? -1 : phase.getTurn());
        sb.append(" phase=").append(phase == null || phase.getPhase() == null ? "-" : phase.getPhase().name());
        sb.append(" active=").append(GameTraceDescriptors.describe(phase == null ? null : phase.getPlayerTurn()));
        sb.append(" priority=").append(GameTraceDescriptors.describe(phase == null ? null : phase.getPriorityPlayer()));
        sb.append(" over=").append(game.isGameOver());
        sb.append('\n');

        for (final Player player : game.getPlayers()) {
            appendPlayer(sb, player);
        }

        appendStack(sb, game);
        appendCombat(sb, game);
        return sb.toString();
    }

    private static void appendPlayer(final StringBuilder sb, final Player player) {
        sb.append("player ").append(GameTraceDescriptors.describe(player));
        sb.append(" life=").append(player.getLife());
        sb.append(" counters=").append(GameTraceDescriptors.describeCounters(player.getCounters()));
        sb.append('\n');

        for (final ZoneType zoneType : ZoneType.values()) {
            if (zoneType == ZoneType.Stack) {
                // The stack is game-wide and dumped once, in resolution order.
                continue;
            }
            final PlayerZone zone = player.getZone(zoneType);
            if (zone == null) {
                continue;
            }
            int index = 0;
            for (final Card card : zone.getCards()) {
                sb.append("  ").append(zoneType.name()).append(' ').append(index++).append(' ');
                appendCard(sb, card);
                sb.append('\n');
            }
        }
    }

    private static void appendCard(final StringBuilder sb, final Card card) {
        if (card == null) {
            sb.append('-');
            return;
        }
        sb.append(GameTraceDescriptors.describe(card));
        sb.append(" owner=").append(GameTraceDescriptors.describe(card.getOwner()));
        sb.append(" controller=").append(GameTraceDescriptors.describe(card.getController()));
        sb.append(" tapped=").append(card.isTapped());
        sb.append(" sick=").append(card.isSick());
        sb.append(" faceDown=").append(card.isFaceDown());
        sb.append(" phasedOut=").append(card.isPhasedOut());
        sb.append(" ts=").append(card.getGameTimestamp());
        sb.append(" counters=").append(GameTraceDescriptors.describeCounters(card.getCounters()));
    }

    private static void appendStack(final StringBuilder sb, final Game game) {
        int index = 0;
        for (final SpellAbilityStackInstance si : game.getStack()) {
            sb.append("stack ").append(index++).append(' ')
                    .append(GameTraceDescriptors.describe(si.getSpellAbility())).append('\n');
        }
    }

    private static void appendCombat(final StringBuilder sb, final Game game) {
        final Combat combat = game.getCombat();
        if (combat == null) {
            return;
        }
        sb.append("combat attacking=").append(GameTraceDescriptors.describe(combat.getAttackingPlayer())).append('\n');
        for (final Card attacker : combat.getAttackers()) {
            sb.append("  attacker ").append(GameTraceDescriptors.describe(attacker));
            sb.append(" -> ").append(GameTraceDescriptors.describe(combat.getDefenderByAttacker(attacker)));
            sb.append(" blockers=[");
            boolean first = true;
            for (final Card blocker : combat.getBlockers(attacker)) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(GameTraceDescriptors.describe(blocker));
            }
            sb.append("]\n");
        }
    }
}
