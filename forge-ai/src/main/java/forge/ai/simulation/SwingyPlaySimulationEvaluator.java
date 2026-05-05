package forge.ai.simulation;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.IIdentifiable;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.IHasForgeLog;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SwingyPlaySimulationEvaluator implements IHasForgeLog {
    private static final int CLEAR_SCORE_GAIN = 50;
    private static final int CLEAR_SCORE_LOSS = 50;
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final ThreadLocal<Boolean> EVALUATING = ThreadLocal.withInitial(() -> false);

    private static final Map<String, AiAbilityDecision> CACHE = new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AiAbilityDecision> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private SwingyPlaySimulationEvaluator() {
    }

    public static AiAbilityDecision judge(Player ai, SpellAbility sa) {
        if (OnePlaySafetyChecker.isChecking() || EVALUATING.get() || needsTargetsButHasNone(sa)) {
            return null;
        }

        String key = makeCacheKey(ai, sa);
        synchronized (CACHE) {
            AiAbilityDecision cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        AiAbilityDecision result = simulate(ai, sa);
        if (result != null) {
            synchronized (CACHE) {
                CACHE.put(key, result);
            }
        }
        return result;
    }

    private static AiAbilityDecision simulate(Player ai, SpellAbility sa) {
        EVALUATING.set(true);
        try {
            SimulationController controller = new SimulationController(new Score(0)) {
                @Override
                public boolean shouldRecurse() {
                    return false;
                }
            };
            GameSimulator simulator = new GameSimulator(controller, ai.getGame(), ai, null);
            Score origScore = simulator.getScoreForOrigGame();
            Score resultScore = simulator.simulateSpellAbility(sa);
            if (resultScore.value == Integer.MIN_VALUE) {
                return new AiAbilityDecision(0, AiPlayDecision.CurseEffects);
            }
            if (resultScore.value == Integer.MAX_VALUE) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            int scoreChange = resultScore.value - origScore.value;
            if (scoreChange >= CLEAR_SCORE_GAIN) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            if (scoreChange <= -CLEAR_SCORE_LOSS) {
                return new AiAbilityDecision(0, AiPlayDecision.CurseEffects);
            }
            return null;
        } catch (RuntimeException ex) {
            logSimulationFailure(ai, sa, ex);
            return null;
        } finally {
            EVALUATING.set(false);
        }
    }

    private static boolean needsTargetsButHasNone(SpellAbility sa) {
        SpellAbility current = sa;
        while (current != null) {
            if (current.usesTargeting()) {
                TargetChoices targets = current.getTargets();
                if (targets == null || targets.isEmpty()) {
                    return true;
                }
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private static void logSimulationFailure(Player ai, SpellAbility sa, RuntimeException ex) {
        Game game = ai.getGame();
        aiLog.warn(ex, "SwingyPlaySimulationEvaluator simulation failure: player={}, phase={}, turn={}, host={}, api={}, ability={}, targets={}",
                ai,
                game.getPhaseHandler().getPhase(),
                game.getPhaseHandler().getPlayerTurn(),
                sa.getHostCard(),
                sa.getApi(),
                sa,
                makeTargetsFingerprint(sa));
    }

    private static String makeCacheKey(Player ai, SpellAbility sa) {
        Game game = ai.getGame();
        StringBuilder key = new StringBuilder();
        PhaseType phase = game.getPhaseHandler().getPhase();
        key.append("game=").append(game.getId());
        key.append("|phase=").append(phase);
        key.append("|turn=").append(game.getPhaseHandler().getPlayerTurn().getId());
        key.append("|player=").append(ai.getId());
        key.append("|life=").append(ai.getLife());
        key.append("|poison=").append(ai.getPoisonCounters());
        key.append("|hand=").append(ai.getCardsIn(ZoneType.Hand).size());
        key.append("|library=").append(ai.getCardsIn(ZoneType.Library).size());
        key.append("|battlefield=");
        game.getCardsIn(ZoneType.Battlefield).forEach(card -> key.append(card.getController().getId()).append(':')
                .append(card.getName()).append(':')
                .append(card.getGameTimestamp()).append(';'));
        key.append("|action=").append(sa.getHostCard().getName());
        key.append("|ability=").append(sa.getDescription());
        key.append("|x=").append(sa.getRootAbility().getXManaCostPaid());
        key.append("|targets=").append(makeTargetsFingerprint(sa));
        return key.toString();
    }

    private static String makeTargetsFingerprint(SpellAbility sa) {
        StringBuilder key = new StringBuilder();
        for (TargetChoices choices : sa.getAllTargetChoices()) {
            for (GameObject target : choices) {
                if (target instanceof IIdentifiable identifiable) {
                    key.append(identifiable.getId()).append(':');
                }
                key.append(target).append(';');
            }
        }
        return key.toString();
    }
}
