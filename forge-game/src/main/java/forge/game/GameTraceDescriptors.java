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

import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Multiset;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;

/**
 * Stable text descriptors for game objects, used by decision traces and state digests.
 *
 * <p>The plan's parity criterion is exact trace identity between two builds, so descriptors must
 * never contain an object hash code, an identity-ordered map dump, a localised string, or anything
 * else that can differ between two runs of the same fixture. Every method here is read-only: a
 * descriptor must not tap a card, set an activating player, or trigger a rules recalculation, since
 * that would make observing a decision change it.</p>
 */
public final class GameTraceDescriptors {
    private GameTraceDescriptors() {
    }

    /** A player as {@code p<id>:<name>}, or {@code -} when null. */
    public static String describe(final Player player) {
        if (player == null) {
            return "-";
        }
        return "p" + player.getId() + ':' + player.getName();
    }

    /** A card as {@code c<id>:<name>}, or {@code -} when null. */
    public static String describe(final Card card) {
        if (card == null) {
            return "-";
        }
        return "c" + card.getId() + ':' + card.getName();
    }

    /** Any game entity; players and cards use their dedicated forms. */
    public static String describe(final GameEntity entity) {
        if (entity == null) {
            return "-";
        }
        if (entity instanceof Player) {
            return describe((Player) entity);
        }
        if (entity instanceof Card) {
            return describe((Card) entity);
        }
        return "e" + entity.getId() + ':' + entity.getName();
    }

    /** Any targetable object, including entities and abilities on the stack. */
    public static String describe(final GameObject object) {
        if (object == null) {
            return "-";
        }
        if (object instanceof GameEntity) {
            return describe((GameEntity) object);
        }
        if (object instanceof SpellAbility) {
            return describe((SpellAbility) object);
        }
        return object.getClass().getSimpleName();
    }

    /**
     * An ability as {@code <host>|<api>|<description>|tgt=[...]}. The description is the card
     * script's own text, which is stable for a given card revision; targets are listed in the order
     * the ability holds them, because target <em>order</em> is itself part of AI behaviour.
     */
    public static String describe(final SpellAbility sa) {
        if (sa == null) {
            return "-";
        }
        final StringBuilder sb = new StringBuilder(64);
        sb.append(describe(sa.getHostCard()));
        sb.append('|').append(sa.getApi() == null ? "none" : sa.getApi().toString());
        final String description = sa.getDescription();
        sb.append('|').append(description == null ? "" : description);
        final TargetChoices targets = sa.getTargets();
        if (targets != null && !targets.isEmpty()) {
            sb.append("|tgt=[");
            boolean first = true;
            for (final GameObject target : targets) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(describe(target));
            }
            sb.append(']');
        }
        return sb.toString();
    }

    /**
     * Counters as {@code [NAME:count,...]}, sorted by counter name so that the underlying multiset's
     * iteration order cannot leak into the descriptor.
     */
    public static String describeCounters(final Multiset<CounterType> counters) {
        if (counters == null || counters.isEmpty()) {
            return "[]";
        }
        final Map<String, Integer> sorted = new TreeMap<>();
        for (final Multiset.Entry<CounterType> entry : counters.entrySet()) {
            sorted.merge(entry.getElement().getName(), entry.getCount(), Integer::sum);
        }
        final StringBuilder sb = new StringBuilder(16);
        sb.append('[');
        boolean first = true;
        for (final Map.Entry<String, Integer> entry : sorted.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return sb.append(']').toString();
    }
}
