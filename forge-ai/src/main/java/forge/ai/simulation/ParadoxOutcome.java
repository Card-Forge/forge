package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Paradoxical Outcome: return any number of your nonland, nontoken permanents to your hand and draw a card for each.
 * The Moxen and the other cheap artifacts come back for nothing, so the more of them are in play the more the spell
 * draws and the less it costs: it is cast when it draws enough cards, with everything that can safely be returned as
 * its targets (the rollouts only ever choose a few). Not what the loop needs on the battlefield: Time Vault, The One
 * Ring, planeswalkers and anything with counters stay.
 */
public final class ParadoxOutcome {
    /** Fewer returned permanents than this and the spell is not worth its mana and a card. */
    static final int MIN_TARGETS = 4;
    /** The library must outlast the draw by this many cards. */
    static final int LIBRARY_MARGIN = 6;

    private ParadoxOutcome() {
    }

    /** Whether returning this permanent loses nothing the deck wants to keep. */
    public static boolean returnable(final Card c) {
        return !c.isLand() && !c.isToken() && !c.isPlaneswalker() && !VaultLoop.isVault(c)
                && !"The One Ring".equals(c.getName()) && !c.hasCounters();
    }

    /** The permanents it returns, in the order they were put into play. */
    static List<Card> targets(final Player ai) {
        final List<Card> list = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (returnable(c)) {
                list.add(c);
            }
        }
        return list;
    }

    /** The cast of Paradoxical Outcome with its targets set, or null when it is not the time. */
    public static SpellAbility sureCast(final Player ai, final List<SpellAbility> candidates) {
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !game.getStack().isEmpty() || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final List<Card> returned = targets(ai);
        if (returned.size() < MIN_TARGETS || ai.getCardsIn(ZoneType.Library).size() < returned.size() + LIBRARY_MARGIN) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || sa.getApi() != ApiType.ChangeZone
                    || !"Hand".equals(sa.getParamOrDefault("Destination", "")) || !sa.usesTargeting()
                    || !sa.getParamOrDefault("ValidTgts", "").contains("YouCtrl")) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            for (final Card c : returned) {
                sa.getTargets().add(c);
            }
            if (sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false)) {
                return sa;
            }
            sa.resetTargets();
        }
        return null;
    }
}
