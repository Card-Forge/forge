package forge.ai;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.tinylog.Logger;

// Heuristic: does the player have any playable action this priority window?
// Bounded by timeoutMs; returns true on expiry (false-positive — player is prompted).
public final class AvailableActions {

    private AvailableActions() {}

    public static boolean compute(Player player, long timeoutMs) {
        long deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L;
        for (Card card : player.getCardsIn(ZoneType.Hand)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (checkTimeout(deadlineNanos, timeoutMs)) return true;
                if (sa.isSpell()) {
                    if (canAfford(sa, player) && hasValidTargets(sa)) {
                        return true;
                    }
                } else if (sa.isLandAbility()) {
                    return true;
                }
            }
        }

        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (checkTimeout(deadlineNanos, timeoutMs)) return true;
                if (!sa.isManaAbility() && canAfford(sa, player) && hasValidTargets(sa)) {
                    return true;
                }
            }
        }

        for (ZoneType zone : new ZoneType[]{ZoneType.Graveyard, ZoneType.Exile, ZoneType.Command}) {
            for (Card card : player.getCardsIn(zone)) {
                for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                    if (checkTimeout(deadlineNanos, timeoutMs)) return true;
                    if (!sa.isManaAbility() && canAfford(sa, player) && hasValidTargets(sa)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean canAfford(SpellAbility sa, Player player) {
        if (sa.getPayCosts() == null || !sa.getPayCosts().hasManaCost()) {
            return true;
        }
        return ComputerUtilMana.canPayManaCost(sa, player, 0, false);
    }

    private static boolean hasValidTargets(SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return true;
        }
        return sa.getTargetRestrictions().hasCandidates(sa);
    }

    private static boolean checkTimeout(long deadlineNanos, long timeoutMs) {
        if (System.nanoTime() < deadlineNanos) {
            return false;
        }
        Logger.warn("AvailableActions: heuristic timed out after {}ms; returning true.", timeoutMs);
        return true;
    }
}
