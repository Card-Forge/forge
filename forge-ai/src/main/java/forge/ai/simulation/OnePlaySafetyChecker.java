package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class OnePlaySafetyChecker {
    private static final ThreadLocal<Boolean> CHECKING = ThreadLocal.withInitial(() -> false);

    public static boolean isAcceptable(Player player, SpellAbility sa) {
        // Forge keeps the parent ability on the stack while it resolves. Score actions offered
        // during that resolution incrementally; priority responses need two full stack-resolution
        // branches and are not supported yet.
        if (sa == null || CHECKING.get()
                || (!player.getGame().getStack().isEmpty()
                && !player.getGame().getStack().isResolving())) {
            return true;
        }

        CHECKING.set(true);
        try {
            GameSimulator simulator = new GameSimulator(
                    new SimulationController(new Score(0), 0),
                    player.getGame(), player, null);
            Score originalScore = simulator.getScoreForOrigGame();
            Game simulatedGame = simulator.getSimulatedGameState();
            Set<Card> existingCommandCards =
                    new HashSet<>(simulatedGame.getCardsIn(ZoneType.Command));
            Set<Trigger> existingDelayedTriggers =
                    new HashSet<>(simulatedGame.getTriggerHandler().getDelayedTriggers());
            Score scoreAfterCosts = simulator.simulateSpellAbility(sa, false);
            Player simulatedPlayer = (Player) simulator.getGameCopier().find(player);

            if (simulatedPlayer == null || scoreAfterCosts.value == Integer.MIN_VALUE) {
                return true;
            }
            GameSimulator.resolveStack(simulatedGame, simulatedPlayer.getWeakestOpponent());
            if (simulatedPlayer.hasLost()) {
                return false;
            }
            resolveScheduledReturns(simulatedGame, simulatedPlayer,
                    existingCommandCards, existingDelayedTriggers);
            if (simulatedPlayer.hasLost()) {
                return false;
            }
            Score resultScore =
                    new GameStateEvaluator().getScoreForGameState(simulatedGame, simulatedPlayer);
            // A land is already on the battlefield in the post-cost snapshot, so its
            // enters-the-battlefield drawback must be weighed against the pre-play state.
            Score minimumScore = sa.isLandAbility() ? originalScore : scoreAfterCosts;
            return resultScore.value == Integer.MIN_VALUE
                    || resultScore.value >= minimumScore.value
                    || hasUnsimulatedNextTurnEffect(sa);
        } finally {
            CHECKING.remove();
        }
    }

    private static boolean hasUnsimulatedNextTurnEffect(SpellAbility sa) {
        for (SpellAbility current = sa; current != null; current = current.getSubAbility()) {
            if (current.getApi() == ApiType.DelayedTrigger
                    && current.hasParam("NextTurn")) {
                return true;
            }
        }
        return false;
    }

    private static void resolveScheduledReturns(Game game, Player player,
            Set<Card> existingCommandCards, Set<Trigger> existingDelayedTriggers) {
        Set<Trigger> triggers =
                new LinkedHashSet<>(game.getTriggerHandler().getDelayedTriggers());
        triggers.removeAll(existingDelayedTriggers);
        game.getCardsIn(ZoneType.Command).stream()
                .filter(effect -> !existingCommandCards.contains(effect))
                .flatMap(effect -> effect.getTriggers().stream())
                .filter(trigger -> trigger.getMode() == TriggerType.Phase
                        && trigger.hasParam("OneOff"))
                .forEach(triggers::add);

        boolean resolved = false;
        for (Trigger trigger : triggers) {
            SpellAbility ability = trigger.ensureAbility();
            if (!isMandatoryReturn(trigger, ability)) {
                continue;
            }
            if (ability.getActivatingPlayer() == null) {
                ability.setActivatingPlayer(trigger.getHostCard().getController());
            }
            AbilityUtils.resolve(ability);
            resolved = true;
        }
        if (resolved && !game.isGameOver()) {
            GameSimulator.resolveStack(game, player.getWeakestOpponent());
        }
    }

    private static boolean isMandatoryReturn(Trigger trigger, SpellAbility ability) {
        if (trigger.getMode() != TriggerType.Phase || ability == null
                || trigger.hasParam("OptionalDecider")) {
            return false;
        }
        boolean returnsCard = false;
        for (SpellAbility current = ability; current != null; current = current.getSubAbility()) {
            if (current.hasParam("UnlessCost") || current.hasParam("OptionalDecider")
                    || "True".equalsIgnoreCase(current.getParam("Optional"))) {
                return false;
            }
            if (current.getApi() == ApiType.ChangeZone
                    || current.getApi() == ApiType.ChangeZoneAll) {
                ZoneType destination = ZoneType.smartValueOf(current.getParam("Destination"));
                returnsCard |= destination == ZoneType.Hand || destination == ZoneType.Battlefield;
            }
        }
        return returnsCard;
    }
}
