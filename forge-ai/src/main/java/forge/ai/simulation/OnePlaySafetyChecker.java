package forge.ai.simulation;

import forge.ai.AiPlayDecision;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.IIdentifiable;
import forge.game.ability.ApiType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.IHasForgeLog;

import java.util.HashMap;
import java.util.Map;

public class OnePlaySafetyChecker implements IHasForgeLog {
    private static final int CATASTROPHIC_SCORE_LOSS = 250;
    private static final int BAD_SCORE_LOSS = 50;
    private static final int DANGEROUS_LIFE_TOTAL = 5;
    private static final ThreadLocal<Boolean> CHECKING = ThreadLocal.withInitial(() -> false);

    private final Player player;
    private final Map<String, AiPlayDecision> cache = new HashMap<>();
    private final SafetyThreatMemory safetyThreatMemory;

    public OnePlaySafetyChecker(Player player) {
        this.player = player;
        this.safetyThreatMemory = new SafetyThreatMemory(player);
    }

    public static boolean isChecking() {
        return CHECKING.get();
    }

    public AiPlayDecision checkStatic(SpellAbility sa) {
        if (isOwnCommanderSpell(sa)) {
            return AiPlayDecision.WillPlay;
        }
        if (isStaticallyUnsafe(sa)) {
            safetyThreatMemory.rememberSuspectsForUnsafeAction(sa);
            return AiPlayDecision.CurseEffects;
        }
        return AiPlayDecision.WillPlay;
    }

    public AiPlayDecision checkDuringSpellSelection(SpellAbility sa) {
        if (CHECKING.get()) {
            return AiPlayDecision.WillPlay;
        }
        if (isOwnCommanderSpell(sa)) {
            return AiPlayDecision.WillPlay;
        }

        final String key = makeCacheKey(sa);
        AiPlayDecision cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        if (isStaticallyUnsafe(sa)) {
            safetyThreatMemory.rememberSuspectsForUnsafeAction(sa);
            cache.put(key, AiPlayDecision.CurseEffects);
            return AiPlayDecision.CurseEffects;
        }
        if (!shouldSimulateDuringSpellSelection(sa)) {
            return AiPlayDecision.WillPlay;
        }
        return check(sa);
    }

    public AiPlayDecision check(SpellAbility sa) {
        if (CHECKING.get()) {
            return AiPlayDecision.WillPlay;
        }
        if (isOwnCommanderSpell(sa)) {
            return AiPlayDecision.WillPlay;
        }

        final String key = makeCacheKey(sa);
        AiPlayDecision cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        if (isStaticallyUnsafe(sa)) {
            safetyThreatMemory.rememberSuspectsForUnsafeAction(sa);
            cache.put(key, AiPlayDecision.CurseEffects);
            return AiPlayDecision.CurseEffects;
        }
        if (sa instanceof AbilityStatic) {
            return AiPlayDecision.WillPlay;
        }
        if (sa.getApi() == ApiType.Charm) {
            return AiPlayDecision.WillPlay;
        }
        if (needsTargetsButHasNone(sa)) {
            return AiPlayDecision.WillPlay;
        }

        CHECKING.set(true);
        AiPlayDecision result = AiPlayDecision.WillPlay;
        try {
            SimulationController controller = new SimulationController(new Score(0)) {
                @Override
                public boolean shouldRecurse() {
                    return false;
                }
            };
            GameSimulator simulator = new GameSimulator(controller, player.getGame(), player, null);
            Score origScore = simulator.getScoreForOrigGame();
            Score resultScore = simulator.simulateSpellAbility(sa);
            if (isUnsafeResult(sa, simulator, origScore, resultScore)) {
                result = AiPlayDecision.CurseEffects;
                safetyThreatMemory.rememberSuspectsForUnsafeAction(sa);
            }
        } catch (RuntimeException ex) {
            logSimulationFailure(sa, ex);
            result = AiPlayDecision.WillPlay;
        } finally {
            CHECKING.set(false);
        }

        cache.put(key, result);
        return result;
    }

    public int getThreatAssessmentBonus(Card card) {
        return safetyThreatMemory.getThreatAssessmentBonus(card);
    }

    private boolean isOwnCommanderSpell(SpellAbility sa) {
        Card host = sa.getHostCard();
        return sa.isSpell() && host != null && host.isCommander() && host.getOwner().equals(player);
    }

    private void logSimulationFailure(SpellAbility sa, RuntimeException ex) {
        Game game = player.getGame();
        aiLog.warn(ex, "OnePlaySafetyChecker simulation failure: player={}, phase={}, turn={}, host={}, api={}, ability={}, targets={}",
                player,
                game.getPhaseHandler().getPhase(),
                game.getPhaseHandler().getPlayerTurn(),
                sa.getHostCard(),
                sa.getApi(),
                sa,
                makeTargetsFingerprint(sa));
    }

    private boolean isStaticallyUnsafe(SpellAbility sa) {
        if (hasActiveOpponentDrawDanger(player.getGame()) && drawsMultipleCardsForPlayer(sa)) {
            return true;
        }
        if (hasLatentOpponentDrawDanger(player.getGame())) {
            if (sa.isSpell() && hasSelfSpellCastDrawEngine(player)) {
                return true;
            }
            if (sa.isSpell() && hasSelfSpellCastDrawTrigger(sa.getHostCard())) {
                return true;
            }
            if (sa.isSpell() && hasSelfTurnDrawTrigger(sa.getHostCard())) {
                return true;
            }
        }
        return false;
    }

    private boolean needsTargetsButHasNone(SpellAbility sa) {
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

    private boolean shouldSimulateDuringSpellSelection(SpellAbility sa) {
        if (sa instanceof AbilityStatic) {
            return false;
        }
        if (sa.getApi() == ApiType.Charm) {
            return false;
        }
        if (needsTargetsButHasNone(sa)) {
            return false;
        }
        if (hasActiveOpponentDrawDanger(player.getGame()) && hasApi(sa, ApiType.Draw)) {
            return true;
        }
        if (hasMassBoardChangingApi(sa)) {
            return true;
        }
        return hasPotentialOpponentSafetyThreat() && hasReactiveApi(sa);
    }

    private boolean hasPotentialOpponentSafetyThreat() {
        for (Card card : player.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (card.getController().isOpponentOf(player) && SafetyThreatMemory.isPotentialSafetyThreat(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasApi(SpellAbility sa, ApiType api) {
        SpellAbility current = sa;
        while (current != null) {
            if (current.getApi() == api) {
                return true;
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private boolean hasMassBoardChangingApi(SpellAbility sa) {
        SpellAbility current = sa;
        while (current != null) {
            ApiType api = current.getApi();
            if (api == ApiType.DestroyAll
                    || api == ApiType.ChangeZoneAll
                    || api == ApiType.DamageAll
                    || api == ApiType.EachDamage
                    || api == ApiType.SacrificeAll
                    || api == ApiType.Balance) {
                return true;
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private boolean hasReactiveApi(SpellAbility sa) {
        SpellAbility current = sa;
        while (current != null) {
            ApiType api = current.getApi();
            if (api == ApiType.Destroy
                    || api == ApiType.ChangeZone
                    || api == ApiType.DealDamage
                    || api == ApiType.Sacrifice
                    || api == ApiType.Draw
                    || api == ApiType.Discard
                    || api == ApiType.Mill
                    || api == ApiType.Token
                    || api == ApiType.Fight
                    || api == ApiType.GainControl
                    || api == ApiType.ExchangeControl
                    || api == ApiType.Play
                    || api == ApiType.PlayLandVariant) {
                return true;
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private boolean isUnsafeResult(SpellAbility sa, GameSimulator simulator, Score origScore, Score resultScore) {
        if (resultScore.value == Integer.MIN_VALUE) {
            return true;
        }

        final int scoreLoss = origScore.value - resultScore.value;
        if (scoreLoss >= CATASTROPHIC_SCORE_LOSS) {
            return true;
        }

        Player simPlayer = simulator.getSimulatedGameState().getPlayer(player.getId());
        if (simPlayer == null || !simPlayer.isInGame()) {
            return true;
        }
        if (hasLatentOpponentDrawDanger(player.getGame())) {
            if (hasSelfSpellCastDrawEngine(simPlayer)
                    && (sa.isSpell() || addsSelfSpellCastDrawEngine(simPlayer, sa))) {
                return true;
            }
            if (addsSelfTurnDrawEngine(simPlayer, sa)) {
                return true;
            }
        }

        if (resultScore.value >= origScore.value) {
            return false;
        }

        final int lifeLost = player.getLife() - simPlayer.getLife();
        final int significantLifeLoss = Math.max(4, player.getLife() / 4);
        if (lifeLost >= significantLifeLoss) {
            return true;
        }
        if (lifeLost > 0 && simPlayer.getLife() <= DANGEROUS_LIFE_TOTAL) {
            return true;
        }

        return scoreLoss >= BAD_SCORE_LOSS;
    }

    private boolean drawsMultipleCardsForPlayer(SpellAbility sa) {
        SpellAbility current = sa;
        while (current != null) {
            if (current.getApi() == ApiType.Draw && affectsPlayer(current)) {
                if (getDrawAmount(current) >= 2) {
                    return true;
                }
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private boolean affectsPlayer(SpellAbility drawSa) {
        final String defined = drawSa.getParamOrDefault("Defined", "You");
        if ("You".equals(defined) || "Player".equals(defined)) {
            return true;
        }
        if (defined.startsWith("Targeted")) {
            if (drawSa.getTargets() == null) {
                return false;
            }
            for (Player target : drawSa.getTargets().getTargetPlayers()) {
                if (target.equals(player)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getDrawAmount(SpellAbility drawSa) {
        if (!drawSa.hasParam("NumCards")) {
            return 1;
        }
        try {
            return AbilityUtils.calculateAmount(drawSa.getHostCard(), drawSa.getParam("NumCards"), drawSa);
        } catch (RuntimeException ex) {
            return 2;
        }
    }

    private boolean hasLatentOpponentDrawDanger(Game game) {
        if (hasActiveOpponentDrawDanger(game)) {
            return true;
        }
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            if (card.getController().isOpponentOf(player) && hasDangerousOpponentDrawTrigger(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveOpponentDrawDanger(Game game) {
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            if (card.getController().isOpponentOf(player) && hasDangerousOpponentDrawTrigger(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDangerousOpponentDrawTrigger(Card card) {
        for (Trigger trigger : card.getTriggers()) {
            if (trigger.getMode() != TriggerType.Drawn) {
                continue;
            }
            SpellAbility triggerSa = trigger.ensureAbility();
            if (triggerSa == null) {
                continue;
            }
            ApiType api = triggerSa.getApi();
            if (api != ApiType.DealDamage && api != ApiType.LoseLife && api != ApiType.Token) {
                continue;
            }
            if (trigger.hasParam("ValidPlayer") && !trigger.matchesValidParam("ValidPlayer", player)) {
                continue;
            }
            if (trigger.hasParam("ValidCard")
                    && !player.getCardsIn(ZoneType.Library).isEmpty()
                    && !trigger.matchesValidParam("ValidCard", player.getCardsIn(ZoneType.Library).get(0))) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean addsSelfSpellCastDrawEngine(Player simPlayer, SpellAbility origSa) {
        Card simHost = findByName(simPlayer.getGame(), origSa.getHostCard().getName());
        return simHost != null && simHost.getController().equals(simPlayer) && hasSelfSpellCastDrawTrigger(simHost);
    }

    private boolean addsSelfTurnDrawEngine(Player simPlayer, SpellAbility origSa) {
        Card simHost = findByName(simPlayer.getGame(), origSa.getHostCard().getName());
        return simHost != null && simHost.getController().equals(simPlayer) && hasSelfTurnDrawTrigger(simHost);
    }

    private Card findByName(Game game, String name) {
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        return null;
    }

    private boolean hasSelfSpellCastDrawEngine(Player playerToCheck) {
        for (Card card : playerToCheck.getCardsIn(ZoneType.Battlefield)) {
            if (hasSelfSpellCastDrawTrigger(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSelfSpellCastDrawTrigger(Card card) {
        for (Trigger trigger : card.getTriggers()) {
            if (trigger.getMode() != TriggerType.SpellCast) {
                continue;
            }
            if (!trigger.matchesValidParam("ValidActivatingPlayer", card.getController())) {
                continue;
            }
            SpellAbility triggerSa = trigger.ensureAbility();
            while (triggerSa != null) {
                if (triggerSa.getApi() == ApiType.Draw) {
                    return true;
                }
                triggerSa = triggerSa.getSubAbility();
            }
        }
        return false;
    }

    private boolean hasSelfTurnDrawTrigger(Card card) {
        for (Trigger trigger : card.getTriggers()) {
            if (trigger.getMode() != TriggerType.Phase) {
                continue;
            }
            if (!"Draw".equalsIgnoreCase(trigger.getParamOrDefault("Phase", ""))) {
                continue;
            }
            if (!drawsForSelfFromTrigger(trigger.ensureAbility())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean drawsForSelfFromTrigger(SpellAbility triggerSa) {
        while (triggerSa != null) {
            if (triggerSa.getApi() == ApiType.Draw) {
                String defined = triggerSa.getParamOrDefault("Defined", "You");
                if ("You".equals(defined) || "TriggeredPlayer".equals(defined) || "Player".equals(defined)) {
                    return true;
                }
            }
            triggerSa = triggerSa.getSubAbility();
        }
        return false;
    }

    private String makeCacheKey(SpellAbility sa) {
        StringBuilder key = new StringBuilder();
        Game game = player.getGame();
        PhaseType phase = game.getPhaseHandler().getPhase();
        key.append("game=").append(game.getId());
        key.append("|phase=").append(phase);
        key.append("|turn=").append(game.getPhaseHandler().getPlayerTurn().getId());
        key.append("|life=").append(player.getLife());
        key.append("|poison=").append(player.getPoisonCounters());
        key.append("|library=").append(player.getCardsIn(ZoneType.Library).size());
        key.append("|hand=").append(player.getCardsIn(ZoneType.Hand).size());
        key.append("|battlefield=").append(makeBattlefieldRulesFingerprint(game));
        key.append("|command=").append(makeCommandRulesFingerprint(game));
        key.append("|action=").append(sa.getHostCard().getName());
        key.append("|ability=").append(sa.getDescription());
        key.append("|x=").append(sa.getRootAbility().getXManaCostPaid());
        key.append("|targets=").append(makeTargetsFingerprint(sa));
        return key.toString();
    }

    private String makeBattlefieldRulesFingerprint(Game game) {
        StringBuilder key = new StringBuilder();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            if (card.getTriggers().isEmpty() && card.getReplacementEffects().isEmpty() && card.getStaticAbilities().isEmpty()) {
                continue;
            }
            key.append(card.getController().getId()).append(':')
                    .append(card.getName()).append(':')
                    .append(card.getGameTimestamp()).append(';');
        }
        return key.toString();
    }

    private String makeCommandRulesFingerprint(Game game) {
        StringBuilder key = new StringBuilder();
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            if (card.getTriggers().isEmpty() && card.getReplacementEffects().isEmpty() && card.getStaticAbilities().isEmpty()) {
                continue;
            }
            key.append(card.getController().getId()).append(':')
                    .append(card.getName()).append(':')
                    .append(card.getGameTimestamp()).append(';');
        }
        return key.toString();
    }

    private String makeTargetsFingerprint(SpellAbility sa) {
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
