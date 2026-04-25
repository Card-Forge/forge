package forge.ai;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;
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

    /**
     * Full-scan variant of {@link #compute} that returns the set of card
     * views with at least one actionable SA. No early-exit: we walk
     * every card so downstream highlight code can reuse the same
     * per-card answers the APINA boolean is derived from.
     *
     * Timeout behavior: on expiry, remaining unvisited cards are added
     * to the set (FP-safe — the player is shown extra highlights rather
     * than missing some). The APINA boolean can be derived as
     * {@code !result.isEmpty()}.
     */
    public static Set<CardView> collectActionable(Player player, long timeoutMs) {
        long deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L;
        Set<CardView> actionable = new HashSet<>();

        for (Card card : sortedCardsIn(player, ZoneType.Hand)) {
            if (checkTimeout(deadlineNanos, timeoutMs)) {
                addAllRemaining(actionable, player);
                return actionable;
            }
            if (cardHasActionableSpell(card, player)) {
                actionable.add(card.getView());
            }
        }
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            if (checkTimeout(deadlineNanos, timeoutMs)) {
                addAllRemaining(actionable, player);
                return actionable;
            }
            if (cardHasActionableActivated(card, player)) {
                actionable.add(card.getView());
            }
        }
        for (Card card : sortedCardsIn(player, ZoneType.Flashback)) {
            if (checkTimeout(deadlineNanos, timeoutMs)) {
                addAllRemaining(actionable, player);
                return actionable;
            }
            if (cardHasActionableActivated(card, player)) {
                actionable.add(card.getView());
            }
        }
        return actionable;
    }

    private static boolean cardHasActionableSpell(Card card, Player player) {
        for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
            if (sa.isSpell()) {
                if (canAfford(sa, player) && ComputerUtilAbility.isFullyTargetable(sa)) {
                    return true;
                }
            } else if (sa.isLandAbility()) {
                return true;
            }
        }
        return false;
    }

    private static boolean cardHasActionableActivated(Card card, Player player) {
        for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
            if (!sa.isManaAbility() && canAfford(sa, player)
                    && ComputerUtilAbility.isFullyTargetable(sa)) {
                return true;
            }
        }
        return false;
    }

    /** FP-safe timeout fallback: mark every card in a scannable zone as
     *  actionable. The player sees extra highlights rather than missing
     *  playable cards. */
    private static void addAllRemaining(Set<CardView> actionable, Player player) {
        for (Card c : player.getCardsIn(ZoneType.Hand)) actionable.add(c.getView());
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) actionable.add(c.getView());
        for (Card c : player.getCardsIn(ZoneType.Flashback)) actionable.add(c.getView());
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
