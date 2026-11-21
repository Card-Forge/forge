package forge.ai.simulation;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * A spell that needs one target more for every time it was kicked (Comet Storm: "choose another target for each time
 * this spell was kicked") went on the stack with the one target the copied choice had, and failed ("Couldn't add to
 * stack, failed to target"). The missing targets are added here: the opponents first, then their permanents, then the
 * rest. Only when at least one target is there already and the spell asks for more.
 */
public final class TargetTopUp {
    private TargetTopUp() {
    }

    public static void apply(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || !sa.usesTargeting() || sa.getTargets() == null || sa.getTargets().isEmpty()) {
            return;
        }
        try {
            final int min = sa.getMinTargets();
            if (sa.getTargets().size() >= min) {
                return;
            }
            final java.util.List<GameEntity> candidates = new java.util.ArrayList<>(
                    sa.getTargetRestrictions().getAllCandidates(sa));
            candidates.sort((a, b) -> Integer.compare(rank(ai, a), rank(ai, b)));
            for (final GameEntity e : candidates) {
                if (sa.getTargets().size() >= min) {
                    break;
                }
                if (!sa.getTargets().contains(e) && sa.canTarget(e)) {
                    sa.getTargets().add(e);
                }
            }
        } catch (final RuntimeException e) {
            // the cast fails as it did before
        }
    }

    private static int rank(final Player ai, final GameEntity e) {
        if (e instanceof Player) {
            return ai.isOpponentOf((Player) e) ? 0 : 3;
        }
        final Card c = (Card) e;
        return ai.isOpponentOf(c.getController()) ? 1 : 2;
    }
}
