package forge.ai.simulation;

import forge.ai.ComputerUtilCard;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.cost.CostPart;
import forge.game.cost.CostTapType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Forecast: an ability of a card in the hand that can only be used in the owner's upkeep, once each turn (Sky Hussar,
 * Govern the Guildless, Piercing Rays, Proclamation of Rebirth).
 * <p>
 * Monte Carlo decides in the main phases, so the upkeep is not for it; what the upkeep needs is the one thing it may
 * do there. Everything else is held for the main phase (see AiController), and the forecast ability is left to what the
 * heuristics say about its effect, with a check on its cost that the heuristics do not make: it is used when the mana
 * is spare for the spells in the hand, and when the creatures it taps are ones that would not attack this turn anyway.
 */
public final class Forecast {
    private Forecast() {
    }

    public static boolean isForecast(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && sa.hasParam("Forecast") && sa.isActivatedAbility() && host.isInZone(ZoneType.Hand);
    }

    /** The AI's upkeep, with the stack empty: before the first main phase of its own turn. */
    public static boolean beforeOwnMain(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ai.getGame().getStack().isEmpty() && ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.MAIN1);
    }

    /** Whether the cost of the forecast ability is one the AI can afford in the upkeep without a loss. */
    public static boolean worthIt(final Player ai, final SpellAbility sa) {
        if (!isForecast(sa)) {
            return false;
        }
        try {
            if (!FlatMonteCarloPicker.sparesMana(ai, sa)) {
                return false; // the mana is for a spell of the first main phase
            }
            if (sa.getPayCosts() == null) {
                return true;
            }
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (!(part instanceof CostTapType)) {
                    continue;
                }
                final Integer amount = part.convertAmount();
                if (free(ai, part, sa) < (amount == null ? 1 : amount)) {
                    return false; // the creatures it taps would have attacked
                }
            }
            return true;
        } catch (final RuntimeException e) {
            return false; // no way to tell: the main phase is where the AI decides
        }
    }

    /** How many creatures that the cost may tap are not needed this turn: summoning sick, or not attacking. */
    private static int free(final Player ai, final CostPart part, final SpellAbility sa) {
        final String type = part.getType().split("/")[0].replace(';', ',');
        int free = 0;
        for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type, ai, sa.getHostCard(), sa)) {
            if (c.isTapped()) {
                continue;
            }
            if (c.isSick() || !ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, c)) {
                free++;
            }
        }
        return free;
    }
}
