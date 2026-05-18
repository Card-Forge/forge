package forge.ai;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.tinylog.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Heuristic: does the player have any playable action this priority window?
// Bounded by timeoutMs; FP-safe on expiry — unvisited cards are marked actionable.
public final class AvailableActions {

    private AvailableActions() {}

    /** Hand spells and lands; activated abilities on the battlefield; flashback spells. */
    private record ZoneScan(ZoneType zone, boolean sortByCmc, BiPredicate<Card, Player> hasActionable) {}

    private static final List<ZoneScan> SCANS = List.of(
            new ZoneScan(ZoneType.Hand,        true,  AvailableActions::cardHasActionableSpell),
            // Battlefield isn't sorted: activation costs are per-ability, not the permanent's CMC.
            new ZoneScan(ZoneType.Battlefield, false, AvailableActions::cardHasActionableActivated),
            new ZoneScan(ZoneType.Flashback,   true,  AvailableActions::cardHasActionableActivated));

    /** Boolean form: early-exits on the first actionable card. */
    public static boolean compute(Player player, long timeoutMs) {
        return withAiController(player, () -> !walk(player, timeoutMs, true).isEmpty());
    }

    /** Set form: walks every card so highlight consumers can mark the actionable subset. */
    public static Set<CardView> collectActionable(Player player, long timeoutMs) {
        return withAiController(player, () -> walk(player, timeoutMs, false));
    }

    /** Run the predictive sweep under an AI controller so cost-adjustment chooseX
     *  dispatches don't prompt (mirrors InputPayMana auto-pay). */
    private static <T> T withAiController(Player player, Supplier<T> body) {
        AtomicReference<T> result = new AtomicReference<>();
        player.runWithController(
                () -> result.set(body.get()),
                new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer()));
        return result.get();
    }

    private static Set<CardView> walk(Player player, long timeoutMs, boolean earlyExit) {
        long deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L;
        Set<CardView> actionable = new HashSet<>();
        Set<CardView> visited = new HashSet<>();

        for (ZoneScan scan : SCANS) {
            Iterable<Card> cards = scan.sortByCmc()
                    ? sortedCardsIn(player, scan.zone())
                    : player.getCardsIn(scan.zone());
            for (Card card : cards) {
                if (checkTimeout(deadlineNanos, timeoutMs)) {
                    addUnvisited(actionable, visited, player);
                    return actionable;
                }
                CardView cv = card.getView();
                visited.add(cv);
                if (scan.hasActionable().test(card, player)) {
                    actionable.add(cv);
                    if (earlyExit) return actionable;
                }
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

    /** Timeout fallback: mark only the cards we never got to evaluate (FP-safe).
     *  Cards we visited and ruled out keep their determination. */
    private static void addUnvisited(Set<CardView> actionable, Set<CardView> visited, Player player) {
        for (ZoneScan scan : SCANS) {
            for (Card c : player.getCardsIn(scan.zone())) {
                CardView cv = c.getView();
                if (!visited.contains(cv)) actionable.add(cv);
            }
        }
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
