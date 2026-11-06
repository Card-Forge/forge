package forge.ai.simulation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Yawgmoth's Will with something worth replaying in the graveyard (Time Walk, Ancestral Recall, Timetwister, Black Lotus,
 * a Mox, Mana Crypt, Sol Ring): the rollouts do not see the value of a permission, so the Will is cast in the main phase
 * when the mana for it and for the cheapest card worth replaying is there. The cards themselves are then cast by the
 * normal choice, as the Will lets them be cast from the graveyard this turn.
 */
public final class WillReplay {
    private static final Set<String> WORTH = new HashSet<>(Arrays.asList("Time Walk", "Ancestral Recall", "Timetwister",
            "Black Lotus", "Mox Pearl", "Mox Sapphire", "Mox Jet", "Mox Ruby", "Mox Emerald", "Mox Diamond", "Chrome Mox",
            "Mana Crypt", "Sol Ring", "Mana Vault", "Lion's Eye Diamond", "Time Vault", "Voltaic Key", "Manifold Key"));

    private WillReplay() {
    }

    static boolean isWill(final Card c) {
        return c != null && "Yawgmoth's Will".equals(c.getName());
    }

    /** The cost the Will and the card it brings back need together, 0 when the graveyard holds nothing worth it. */
    static int manaNeeded(final Player ai, final Card will) {
        int cheapest = -1;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (WORTH.contains(c.getName()) && (cheapest < 0 || c.getCMC() < cheapest)) {
                cheapest = c.getCMC();
            }
        }
        return cheapest < 0 ? 0 : will.getCMC() + cheapest;
    }

    public static SpellAbility sureCast(final Player ai, final List<SpellAbility> candidates) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !game.getStack().isEmpty() || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (!isWill(host) || !sa.isSpell() || !host.isInZone(ZoneType.Hand)) {
                continue;
            }
            final int needed = manaNeeded(ai, host);
            sa.setActivatingPlayer(ai);
            if (needed > 0 && ComputerUtilMana.getAvailableManaEstimate(ai) >= needed && sa.canPlay()
                    && ComputerUtilCost.canPayCost(sa, ai, false)) {
                return sa;
            }
        }
        return null;
    }
}
