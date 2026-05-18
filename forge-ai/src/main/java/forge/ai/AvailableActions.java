package forge.ai;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.tinylog.Logger;

import java.util.stream.Collectors;

// Heuristic: does the player have any playable action this priority window?
// Bounded by timeoutMs; returns true on expiry (false-positive — player is prompted).
public final class AvailableActions {

    private AvailableActions() {}

    public static boolean compute(Player player, long timeoutMs) {
        long deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L;

        // Run the predictive sweep under an AI controller so cost-adjustment chooseX dispatches don't prompt (mirrors InputPayMana auto-pay).
        boolean[] result = {false};
        player.runWithController(
                () -> result[0] = scan(player, deadlineNanos, timeoutMs),
                new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer()));
        return result[0];
    }

    private static boolean scan(Player player, long deadlineNanos, long timeoutMs) {
        for (Card card : sortedCardsIn(player, ZoneType.Hand)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (checkTimeout(deadlineNanos, timeoutMs)) return true;
                if (sa.isSpell()) {
                    if (canAfford(sa, player) && ComputerUtilAbility.isFullyTargetable(sa)) {
                        return true;
                    }
                } else if (sa.isLandAbility()) {
                    return true;
                }
            }
        }

        // Not sorted: activation costs are per-ability, not the permanent's CMC.
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (checkTimeout(deadlineNanos, timeoutMs)) return true;
                if (!sa.isManaAbility() && canAfford(sa, player) && ComputerUtilAbility.isFullyTargetable(sa)) {
                    return true;
                }
            }
        }

        for (Card card : sortedCardsIn(player, ZoneType.Flashback)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (checkTimeout(deadlineNanos, timeoutMs)) return true;
                if (!sa.isManaAbility() && canAfford(sa, player) && ComputerUtilAbility.isFullyTargetable(sa)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Sort cheap cards first so cheap-to-validate matches early-exit
    private static Iterable<Card> sortedCardsIn(Player player, ZoneType zone) {
        return player.getCardsIn(zone).stream().sorted(CardLists.CmcComparator).collect(Collectors.toList());
    }

    private static boolean canAfford(SpellAbility sa, Player player) {
        if (sa.getPayCosts() == null || !sa.getPayCosts().hasManaCost()) {
            return true;
        }
        return ComputerUtilMana.canPayManaCost(sa, player, 0, false);
    }

    private static boolean checkTimeout(long deadlineNanos, long timeoutMs) {
        if (System.nanoTime() < deadlineNanos) {
            return false;
        }
        Logger.warn("AvailableActions: heuristic timed out after {}ms; returning true.", timeoutMs);
        return true;
    }
}
