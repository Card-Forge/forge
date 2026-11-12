package forge.ai;

import java.util.HashSet;
import java.util.Set;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * Where the copies of a spell or ability go. A copy chooses its targets the way the original did, so with a counterspell,
 * a tap, a bounce or a removal spell every copy went for the same target and only the first did anything. A copy now
 * leaves the targets that the original and the copies before it already have, when another legal one exists.
 */
public final class CopyTargets {
    private CopyTargets() {
    }

    /** An effect that does nothing the second time on one target: a counter, a tap, a bounce, a removal spell. */
    public static boolean repeatsDoNothing(final SpellAbility sa) {
        final ApiType api = sa == null ? null : sa.getApi();
        return api == ApiType.Counter || api == ApiType.Tap || api == ApiType.TapAll || api == ApiType.Untap
                || api == ApiType.ChangeZone || api == ApiType.Destroy || api == ApiType.Sacrifice;
    }

    /** What the other spells and abilities on the stack from the same source (the original, earlier copies) aim at. */
    static Set<GameObject> takenByTheOthers(final Player ai, final SpellAbility copy) {
        final Set<GameObject> taken = new HashSet<>();
        final Card host = copy.getHostCard();
        if (host == null) {
            return taken;
        }
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final SpellAbility top = si.getSpellAbility();
            if (top == null || top == copy || top.getHostCard() == null || !host.getName().equals(top.getHostCard().getName())) {
                continue;
            }
            for (SpellAbility s = top; s != null; s = s.getSubAbility()) {
                if (s.usesTargeting() && s.getTargets() != null) {
                    taken.addAll(s.getTargets());
                }
            }
        }
        return taken;
    }

    private static SpellAbility targetingPart(final SpellAbility sa) {
        SpellAbility found = null;
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (s.usesTargeting()) {
                if (found != null) {
                    return null; // several aimed parts: left as the AI chose them
                }
                found = s;
            }
        }
        return found;
    }

    /**
     * Moves a copy that aims at the same single target as another of its source to the best other legal target.
     *
     * @return true if the copy was moved
     */
    public static boolean awayFromTheOthers(final Player ai, final SpellAbility copy) {
        final SpellAbility ts = targetingPart(copy);
        if (ts == null || !repeatsDoNothing(ts) || ts.getTargets() == null || ts.getTargets().size() != 1) {
            return false;
        }
        final Set<GameObject> taken = takenByTheOthers(ai, copy);
        final GameObject mine = ts.getTargets().get(0);
        if (!taken.contains(mine)) {
            return false;
        }
        copy.setActivatingPlayer(ai);
        if (ts.getApi() == ApiType.Counter) {
            for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
                final SpellAbility spell = si.getSpellAbility();
                if (spell == null || taken.contains(spell) || spell.getHostCard() == null
                        || spell.getHostCard().getName().equals(copy.getHostCard().getName()) || !ts.canTarget(spell)) {
                    continue;
                }
                ts.resetTargets();
                return ts.getTargets().add(spell);
            }
            return false;
        }
        for (final GameObject other : FlatMonteCarloPicker.alternativeTargets(ai, ts, 30)) {
            if (!taken.contains(other) && FlatMonteCarloPicker.applyTarget(ts, other)) {
                return true;
            }
        }
        return false;
    }
}
