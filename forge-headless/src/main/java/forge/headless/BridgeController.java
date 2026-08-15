package forge.headless;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import forge.LobbyPlayer;
import forge.ai.ComputerUtilAbility;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/** Protocol-synchronized controller around Forge's real AI and remote replay seats. */
final class BridgeController extends PlayerControllerAi {
    private static final long WAIT_SECONDS = 30;

    private final int seat;
    private final boolean forgeAiSeat;
    private final boolean fullGame;
    private final int startingSeat;
    private final BlockingQueue<DecisionTicket> decisions = new ArrayBlockingQueue<>(16);
    private final BlockingQueue<RemoteActionTicket> remoteActions = new ArrayBlockingQueue<>(128);
    private final BlockingQueue<CombatActionTicket> combatActions = new ArrayBlockingQueue<>(16);
    private final BlockingQueue<CombatDecisionTicket> combatDecisions = new ArrayBlockingQueue<>(16);
    private volatile boolean cancelled;
    private volatile boolean finishing;
    private volatile int drainThroughTurn;
    private volatile int remoteDrainThroughTurn;
    private int forgeLandPlayedTurn = -1;
    private volatile boolean multiBlockerScenario;
    private volatile boolean multiBlockerDamageObserved;
    private volatile boolean instantPriority;
    private volatile JsonNode pendingHandCounts;
    private volatile int pendingHandTurn;
    private RemoteActionTicket deferredRemoteAction;

    BridgeController(Game game, Player player, LobbyPlayer lobbyPlayer, int seat, boolean forgeAiSeat,
            boolean fullGame, int startingSeat) {
        super(game, player, lobbyPlayer);
        this.seat = seat;
        this.forgeAiSeat = forgeAiSeat;
        this.fullGame = fullGame;
        this.startingSeat = startingSeat;
    }

    ObjectNode decidePriority(JsonNode context) {
        requireForgeAiSeat();
        multiBlockerScenario |= context.path("multi_blocker").asBoolean(false);
        instantPriority |= context.path("instant_priority").asBoolean(false);
        if (!fullGame) {
            List<SpellAbility> choices = super.chooseSpellAbilityToPlay();
            return choices == null || choices.isEmpty() ? passAction() : describeAction(choices.get(0));
        }
        boolean allowCast = context.path("allow_cast").asBoolean(true);
        int authoritativeTurn = context.path("turn").asInt();
        String authoritativePhase = context.path("phase").asText("");
        if (context.path("resolve_stack").asBoolean(false)) {
            put(decisions, new DecisionTicket(false, authoritativeTurn, authoritativePhase),
                    "Forge AI stack-resolution pass");
            return passAction();
        }
        // Drain permits commonly arrive while the shadow is still completing
        // the preceding remote turn. Keep those future permits queued; their
        // turn tag makes them safe to consume later. Only an already-past
        // permit is genuinely stale.
        if (!allowCast && getGame().getPhaseHandler().getTurn() > authoritativeTurn) {
            return passAction();
        }
        if (!allowCast) {
            drainThroughTurn = Math.max(drainThroughTurn, authoritativeTurn);
            // Also enqueue a sentinel to wake a game thread already blocked in
            // take(decisions). If no thread is blocked, the controller removes
            // this ticket locally before auto-passing the completed turn.
            put(decisions, new DecisionTicket(false, authoritativeTurn, authoritativePhase),
                    "Forge AI drain sentinel");
            return passAction();
        }
        DecisionTicket ticket = new DecisionTicket(allowCast, authoritativeTurn, authoritativePhase);
        put(decisions, ticket, "Forge AI decision permit");
        return await(ticket.result, "Forge AI priority decision");
    }

    ObjectNode decideMulligan(int cardsToReturn) {
        requireForgeAiSeat();
        boolean keep = fullGame || super.mulliganKeepHand(player, cardsToReturn);
        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        if (!keep) {
            result.put("type", "mulligan");
            return result;
        }

        result.put("type", "keep");
        ArrayNode bottom = result.putArray("bottom");
        if (!fullGame && cardsToReturn > 0) {
            CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
            CardCollectionView cards = super.tuckCardsViaMulligan(hand, cardsToReturn);
            for (Card card : cards) {
                bottom.add(cardReference(card));
            }
        }
        return result;
    }

    void queuePriorityHandoffPass(int turn, String phase) {
        requireForgeAiSeat();
        put(decisions, new DecisionTicket(false, turn, phase), "Forge AI priority handoff pass");
    }

    boolean hasPendingNonMainAction(int turn) {
        if (forgeAiSeat) {
            return false;
        }
        RemoteActionTicket head = deferredRemoteAction == null ? remoteActions.peek() : deferredRemoteAction;
        while (head != null && head.turn < turn
                && "pass".equals(head.action.path("type").asText())) {
            if (deferredRemoteAction == null) {
                remoteActions.poll();
            } else {
                deferredRemoteAction = null;
            }
            head.consumed.complete(null);
            head = deferredRemoteAction == null ? remoteActions.peek() : deferredRemoteAction;
        }
        return head != null && head.turn == turn
                && !"pass".equals(head.action.path("type").asText())
                && !"main1".equals(head.phase) && !"main2".equals(head.phase);
    }

    void awaitPendingNonMainAction(int turn) {
        if (!hasPendingNonMainAction(turn)) {
            return;
        }
        RemoteActionTicket head = deferredRemoteAction == null ? remoteActions.peek() : deferredRemoteAction;
        await(head.consumed, "non-main remote action handoff");
    }

    ObjectNode decideCombat(String kind, JsonNode context) {
        requireForgeAiSeat();
        multiBlockerScenario |= context.path("multi_blocker").asBoolean(false);
        instantPriority |= context.path("instant_priority").asBoolean(false);
        CombatDecisionTicket ticket = new CombatDecisionTicket(
                kind, context.path("allow_combat").asBoolean(true),
                context.path("multi_blocker").asBoolean(false));
        put(combatDecisions, ticket, "Forge AI " + kind + " decision");
        return await(ticket.result, "Forge AI " + kind + " decision");
    }

    void acceptOpponentAction(JsonNode action, JsonNode context, PrintStream diagnostics) {
        if (forgeAiSeat) {
            throw new IllegalStateException("opponent_action cannot target the Forge AI seat");
        }
        if (!fullGame) {
            diagnostics.println("Bridge accepted scripted opponent action for seat " + seat + ": " + action);
            return;
        }
        multiBlockerScenario |= context.path("multi_blocker").asBoolean(false);
        instantPriority |= context.path("instant_priority").asBoolean(false);
        int authoritativeTurn = context.path("turn").asInt();
        String authoritativePhase = context.path("phase").asText("");
        String actionType = action.path("type").asText();
        if ("declare_attackers".equals(actionType) || "declare_blockers".equals(actionType)) {
            put(combatActions, new CombatActionTicket(action.deepCopy(), authoritativeTurn),
                    "remote combat action");
            diagnostics.println("Bridge queued exact remote combat action for seat " + seat + ": " + action);
            return;
        }
        if ("pass".equals(action.path("type").asText())
                && "authoritative_turn_end".equals(action.path("reason").asText())) {
            remoteDrainThroughTurn = Math.max(remoteDrainThroughTurn, authoritativeTurn);
            put(remoteActions, new RemoteActionTicket(action.deepCopy(), authoritativeTurn, authoritativePhase),
                    "remote turn-end sentinel");
            diagnostics.println("Bridge reached authoritative remote turn end for seat " + seat
                    + " turn " + authoritativeTurn);
            return;
        }
        String passReason = action.path("reason").asText();
        boolean isBridgeBarrier = "authoritative_main_phase_end".equals(passReason)
                || "authoritative_stack_resolution".equals(passReason)
                || "authoritative_priority_pass".equals(passReason);
        if ("pass".equals(action.path("type").asText()) && !isBridgeBarrier) {
            diagnostics.println("Bridge mirrored opponent priority pass for seat " + seat + ": " + action);
            return;
        }
        RemoteActionTicket ticket = new RemoteActionTicket(action.deepCopy(), authoritativeTurn, authoritativePhase);
        diagnostics.println("Bridge replay pending at life " + lifeSummary() + ": " + action);
        put(remoteActions, ticket, "remote opponent action");
        if ("authoritative_main_phase_end".equals(action.path("reason").asText())) {
            diagnostics.println("Bridge queued authoritative pass marker for seat " + seat);
            return;
        }
        if ("authoritative_stack_resolution".equals(action.path("reason").asText())) {
            diagnostics.println("Bridge queued authoritative stack-resolution pass for seat " + seat);
            return;
        }
        if ("authoritative_priority_pass".equals(action.path("reason").asText())) {
            diagnostics.println("Bridge queued authoritative priority pass for seat " + seat);
            return;
        }
        diagnostics.println("Bridge queued exact replay action for seat " + seat);
    }

    void cancel() {
        cancelled = true;
        DecisionTicket decision;
        while ((decision = decisions.poll()) != null) {
            decision.result.completeExceptionally(new IllegalStateException("Bridge game stopped"));
        }
        RemoteActionTicket remote;
        while ((remote = remoteActions.poll()) != null) {
            remote.consumed.completeExceptionally(new IllegalStateException("Bridge game stopped"));
        }
        CombatDecisionTicket combatDecision;
        while ((combatDecision = combatDecisions.poll()) != null) {
            combatDecision.result.completeExceptionally(new IllegalStateException("Bridge game stopped"));
        }
    }

    void stageHandSync(JsonNode desiredCounts, int turn) {
        if (!desiredCounts.isObject()) {
            throw new IllegalStateException("Full-game reveal requires context.hand_counts");
        }
        pendingHandCounts = desiredCounts.deepCopy();
        pendingHandTurn = turn;
    }

    void finishGame() {
        finishing = true;
        if (forgeAiSeat) {
            DecisionTicket ticket;
            while ((ticket = decisions.poll()) != null) {
                ticket.result.complete(passAction());
            }
        }
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        if (!fullGame) {
            return super.chooseSpellAbilityToPlay();
        }
        boolean sharedActiveMain = getGame().getPhaseHandler().getPhase().isMain()
                && getGame().getPhaseHandler().getPlayerTurn() == player;
        if (!instantPriority) {
            if (getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)
                    || !sharedActiveMain) {
                return null;
            }
        } else if (!getGame().getStack().isEmpty()) {
            // The first instant increment exposes empty-stack windows (end step
            // and combat tricks). Both engines drain an already-cast spell
            // locally before accepting another bridge action.
            return null;
        }
        applyPendingHandSync();
        if (forgeAiSeat && instantPriority && !sharedActiveMain && decisions.peek() == null) {
            return null;
        }
        // DeepScry does not ask the Forge-controlled seat at empty menus, so its
        // AI can pass locally. The remote replay seat still consumes a tagged
        // pass/barrier here; that prevents Forge from crossing a resolution or
        // turn boundary before the corresponding digest has been sampled.
        if (forgeAiSeat && !hasAnyLegalPriorityAction()) {
            return null;
        }
        if (forgeAiSeat) {
            int currentTurn = getGame().getPhaseHandler().getTurn();
            DecisionTicket head = decisions.peek();
            while (head != null && head.turn < currentTurn) {
                decisions.poll().result.complete(passAction());
                head = decisions.peek();
            }
            if (instantPriority && !sharedActiveMain && head == null) {
                return null;
            }
            if (head != null && head.turn > currentTurn) {
                return null;
            }
            String currentPhase = currentBridgePhase();
            if (head != null && head.turn == currentTurn && !head.phase.isEmpty()
                    && !head.phase.equals(currentPhase)) {
                return null;
            }
            if (drainThroughTurn >= currentTurn && head != null
                    && !head.allowCast && head.turn <= currentTurn) {
                decisions.poll().result.complete(passAction());
                return null;
            }
            // A drain sentinel follows every real action for its authoritative
            // turn. Once it has been consumed, pass any extra Forge callbacks
            // locally until the shadow advances; never consume a future turn's
            // real decision early.
            if ((finishing || drainThroughTurn >= currentTurn)
                    && (head == null || head.turn > currentTurn)) {
                return null;
            }
            DecisionTicket ticket = take(decisions, "Forge AI decision permit");
            try {
                applyPendingHandSync();
                List<SpellAbility> choices = super.chooseSpellAbilityToPlay();
                if (instantPriority && ticket.allowCast) {
                    SpellAbility deterministic = firstLegalDeterministicAction();
                    choices = deterministic == null ? null : Collections.singletonList(deterministic);
                }
                if (!instantPriority && ticket.allowCast && (choices == null || choices.isEmpty())) {
                    SpellAbility bolt = firstLegalLightningBolt();
                    if (bolt != null) {
                        choices = Collections.singletonList(bolt);
                    }
                }
                if (ticket.allowCast && choices != null && !choices.isEmpty()) {
                    // One protocol decision describes one game action. Forge's
                    // stock AI may batch several abilities in this callback;
                    // executing that batch would mutate more than the single
                    // action exactly matched by DeepScry.
                    choices = Collections.singletonList(choices.get(0));
                }
                if (ticket.allowCast && multiBlockerScenario && choices != null && !choices.isEmpty()
                        && !choices.get(0).isLandAbility()) {
                    SpellAbility creature = firstLegalCoverageCreature();
                    choices = creature == null ? null : Collections.singletonList(creature);
                }
                if (!instantPriority && ticket.allowCast && choices != null && !choices.isEmpty()
                        && !choices.get(0).isLandAbility() && opponentControlsCreature()) {
                    SpellAbility bolt = firstLegalLightningBolt();
                    if (bolt != null) {
                        choices = Collections.singletonList(bolt);
                    }
                }
                if (ticket.allowCast && choices != null && !choices.isEmpty()
                        && getGame().getPhaseHandler().getPhase() == PhaseType.MAIN1
                        && choices.get(0).getHostCard().isCreature()
                        && controlsCreature()) {
                    choices = null;
                }
                if (!ticket.allowCast) {
                    choices = null;
                }
                if (choices != null && !choices.isEmpty() && choices.get(0).isLandAbility()) {
                    forgeLandPlayedTurn = currentTurn;
                }
                ticket.result.complete(choices == null || choices.isEmpty()
                        ? passAction() : describeAction(choices.get(0)));
                return choices;
            } catch (RuntimeException e) {
                ticket.result.completeExceptionally(e);
                throw e;
            }
        }

        if (instantPriority && !sharedActiveMain && !hasAnyLegalPriorityAction()) {
            return null;
        }
        int currentTurn = getGame().getPhaseHandler().getTurn();
        RemoteActionTicket head = deferredRemoteAction == null ? remoteActions.peek() : deferredRemoteAction;
        while (head != null && head.turn < currentTurn
                && "pass".equals(head.action.path("type").asText())) {
            if (deferredRemoteAction == null) {
                remoteActions.poll();
            } else {
                deferredRemoteAction = null;
            }
            head.consumed.complete(null);
            head = deferredRemoteAction == null ? remoteActions.peek() : deferredRemoteAction;
        }
        if (remoteDrainThroughTurn >= currentTurn && head != null && head.turn <= currentTurn
                && "authoritative_turn_end".equals(head.action.path("reason").asText())) {
            if (deferredRemoteAction == null) {
                remoteActions.poll();
            } else {
                deferredRemoteAction = null;
            }
            head.consumed.complete(null);
            return null;
        }
        if (remoteDrainThroughTurn >= currentTurn && (head == null || head.turn > currentTurn)) {
            return null;
        }
        if (instantPriority && head != null && head.turn > currentTurn) {
            return null;
        }
        if (instantPriority && head != null && head.turn < currentTurn
                && !"pass".equals(head.action.path("type").asText())) {
            throw new IllegalStateException("Missed remote action from turn " + head.turn
                    + " before Forge advanced to turn " + currentTurn + ": " + head.action);
        }
        if (instantPriority && !sharedActiveMain) {
            if (head == null || head.turn > currentTurn) {
                return null;
            }
            String currentPhase = currentBridgePhase();
            boolean mainPhaseTicket = "main1".equals(head.phase) || "main2".equals(head.phase);
            if (head.turn == currentTurn && !mainPhaseTicket && !"main1".equals(currentPhase)) {
                return null;
            }
            if (head.turn == currentTurn && mainPhaseTicket && !head.phase.equals(currentPhase)) {
                return null;
            }
        }
        RemoteActionTicket ticket = deferredRemoteAction == null
                ? (finishing ? remoteActions.poll() : take(remoteActions, "remote opponent action"))
                : deferredRemoteAction;
        if (ticket == null) {
            return null;
        }
        deferredRemoteAction = null;
        try {
            applyPendingHandSync();
            if ("play_land".equals(ticket.action.path("type").asText())
                    && !getGame().getPhaseHandler().getPhase().isMain()) {
                deferredRemoteAction = ticket;
                return null;
            }
            SpellAbility ability = matchRemoteAction(ticket.action);
            ticket.consumed.complete(null);
            return ability == null ? null : Collections.singletonList(ability);
        } catch (RuntimeException e) {
            ticket.consumed.completeExceptionally(e);
            throw e;
        }
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        if (!fullGame) {
            super.declareAttackers(attacker, combat);
            return;
        }
        if (forgeAiSeat) {
            CombatDecisionTicket ticket = take(combatDecisions, "Forge attacker decision permit");
            try {
                if (ticket.allowCombat && ticket.multiBlocker && !multiBlockerDamageObserved) {
                    declareCoverageAttacker(attacker, combat);
                } else if (ticket.allowCombat) {
                    super.declareAttackers(attacker, combat);
                }
                ticket.result.complete(describeAttackers(combat));
            } catch (RuntimeException e) {
                ticket.result.completeExceptionally(e);
                throw e;
            }
            return;
        }
        CombatActionTicket ticket = take(combatActions, "remote attacker declaration");
        applyRemoteAttackers(attacker, combat, ticket.action);
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        if (!fullGame) {
            super.declareBlockers(defender, combat);
            return;
        }
        if (forgeAiSeat) {
            CombatDecisionTicket ticket = take(combatDecisions, "Forge blocker decision permit");
            try {
                if (ticket.allowCombat && ticket.multiBlocker && !multiBlockerDamageObserved) {
                    declareCoverageBlockers(combat);
                } else if (ticket.allowCombat) {
                    super.declareBlockers(defender, combat);
                }
                ticket.result.complete(describeBlockers(combat));
            } catch (RuntimeException e) {
                ticket.result.completeExceptionally(e);
                throw e;
            }
            return;
        }
        CombatActionTicket ticket = take(combatActions, "remote blocker declaration");
        applyRemoteBlockers(combat, ticket.action);
    }

    private void declareCoverageAttacker(Player attacker, Combat combat) {
        for (Card card : CombatUtil.getPossibleAttackers(attacker)) {
            if (!"Hill Giant".equals(card.getName())) {
                continue;
            }
            for (Player candidate : getGame().getPlayers()) {
                if (candidate != attacker && countBattlefield(candidate, "Gray Ogre") >= 2
                        && CombatUtil.canAttack(card, candidate)) {
                    combat.addAttacker(card, candidate);
                    return;
                }
            }
        }
    }

    private void declareCoverageBlockers(Combat combat) {
        Card hillGiant = null;
        for (Card attacker : combat.getAttackers()) {
            if ("Hill Giant".equals(attacker.getName())) {
                hillGiant = attacker;
                break;
            }
        }
        if (hillGiant == null) {
            return;
        }
        int assigned = 0;
        for (Card blocker : player.getCardsIn(ZoneType.Battlefield)) {
            if ("Gray Ogre".equals(blocker.getName()) && CombatUtil.canBlock(hillGiant, blocker, combat)) {
                combat.addBlocker(hillGiant, blocker);
                if (++assigned == 2) {
                    return;
                }
            }
        }
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        if (blockers.size() > 1) {
            multiBlockerDamageObserved = true;
        }
        return super.orderBlockers(attacker, blockers);
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers,
            CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
        if (blockers.size() > 1) {
            multiBlockerDamageObserved = true;
        }
        return super.assignCombatDamage(attacker, blockers, remaining, damageDealt, defender, overrideOrder);
    }

    boolean requiresMultiBlockerCoverage() {
        return multiBlockerScenario;
    }

    boolean observedMultiBlockerDamage() {
        return multiBlockerDamageObserved;
    }

    private ObjectNode describeAttackers(Combat combat) {
        ObjectNode action = BridgeTransport.JSON.createObjectNode();
        action.put("type", "declare_attackers");
        ArrayNode assignments = action.putArray("assignments");
        for (Card attacker : combat.getAttackers()) {
            ObjectNode assignment = assignments.addObject();
            assignment.set("attacker", cardReference(attacker));
            GameEntity defender = combat.getDefenderByAttacker(attacker);
            if (!(defender instanceof Player defendingPlayer)) {
                throw new IllegalStateException("Task D only supports player combat defenders");
            }
            ObjectNode target = assignment.putObject("defender");
            target.put("kind", "player");
            target.put("seat", defendingPlayer.getId() + 1);
        }
        return action;
    }

    private ObjectNode describeBlockers(Combat combat) {
        ObjectNode action = BridgeTransport.JSON.createObjectNode();
        action.put("type", "declare_blockers");
        ArrayNode assignments = action.putArray("assignments");
        for (Card attacker : combat.getAttackers()) {
            for (Card blocker : combat.getBlockers(attacker)) {
                ObjectNode assignment = assignments.addObject();
                assignment.set("blocker", cardReference(blocker));
                assignment.set("attacker", cardReference(attacker));
            }
        }
        return action;
    }

    private void applyRemoteAttackers(Player attacker, Combat combat, JsonNode action) {
        if (!"declare_attackers".equals(action.path("type").asText())) {
            throw new IllegalStateException("Expected declare_attackers, got " + action);
        }
        CardCollection available = CombatUtil.getPossibleAttackers(attacker);
        for (JsonNode assignment : action.path("assignments")) {
            Card card = exactCombatCard(available, assignment.path("attacker"), "attacker");
            int defenderSeat = assignment.path("defender").path("seat").asInt();
            Player defendingPlayer = getGame().getPlayers().get(defenderSeat - 1);
            if (!CombatUtil.canAttack(card, defendingPlayer)) {
                throw new IllegalStateException("Remote attacker cannot legally attack: " + assignment);
            }
            combat.addAttacker(card, defendingPlayer);
            available.remove(card);
        }
    }

    private void applyRemoteBlockers(Combat combat, JsonNode action) {
        if (!"declare_blockers".equals(action.path("type").asText())) {
            throw new IllegalStateException("Expected declare_blockers, got " + action);
        }
        for (JsonNode assignment : action.path("assignments")) {
            Card blocker = exactCombatCard(player.getCardsIn(ZoneType.Battlefield),
                    assignment.path("blocker"), "blocker");
            Card attacker = exactCombatCard(combat.getAttackers(), assignment.path("attacker"), "blocked attacker");
            combat.addBlocker(attacker, blocker);
        }
    }

    private Card exactCombatCard(Iterable<Card> cards, JsonNode reference, String label) {
        String name = reference.path("name").asText();
        int wantedIndex = reference.path("idx").asInt(0);
        List<Card> matches = new ArrayList<>();
        for (Card card : cards) {
            if (card.getName().equals(name) && sameNameIndex(card) == wantedIndex) {
                matches.add(card);
            }
        }
        if (matches.size() != 1) {
            throw new IllegalStateException("Remote " + label + " matched " + matches.size()
                    + " Forge cards: " + reference);
        }
        return matches.get(0);
    }

    private boolean hasAnyLegalPriorityAction() {
        CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
        if (forgeLandPlayedTurn != getGame().getPhaseHandler().getTurn() && lands != null) {
            for (Card land : lands) {
                for (SpellAbility ability : land.getAllPossibleAbilities(player, true)) {
                    if (ability.isLandAbility() && ability.canPlay()) {
                        return true;
                    }
                }
            }
        }
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(
                ComputerUtilAbility.getAvailableCards(getGame(), player), player);
        for (SpellAbility ability : abilities) {
            Card host = ability.getHostCard();
            if (ability.isSpell() && host != null && host.isInZone(ZoneType.Hand) && ability.canPlay()) {
                return true;
            }
        }
        return false;
    }

    private boolean controlsCreature() {
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            if (card.isCreature()) {
                return true;
            }
        }
        return false;
    }

    private boolean opponentControlsCreature() {
        for (Player candidate : getGame().getPlayers()) {
            if (candidate == player) {
                continue;
            }
            for (Card card : candidate.getCardsIn(ZoneType.Battlefield)) {
                if (card.isCreature()) {
                    return true;
                }
            }
        }
        return false;
    }

    private SpellAbility firstLegalLightningBolt() {
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(
                ComputerUtilAbility.getAvailableCards(getGame(), player), player);
        for (SpellAbility ability : abilities) {
            Card host = ability.getHostCard();
            if (ability.isSpell() && host != null && "Lightning Bolt".equals(host.getName())
                    && ability.canPlay()) {
                ability.resetTargets();
                boolean targetedCreature = false;
                for (Player candidate : getGame().getPlayers()) {
                    if (candidate == player) {
                        continue;
                    }
                    for (Card card : candidate.getCardsIn(ZoneType.Battlefield)) {
                        if (card.isCreature() && ability.canTarget(card)) {
                            ability.getTargets().add(card);
                            targetedCreature = true;
                            break;
                        }
                    }
                    if (targetedCreature) {
                        break;
                    }
                }
                if (targetedCreature) {
                    return ability;
                }
                for (Player candidate : getGame().getPlayers()) {
                    if (candidate != player) {
                        ability.getTargets().add(candidate);
                        break;
                    }
                }
                return ability;
            }
        }
        return null;
    }

    private SpellAbility firstLegalDeterministicAction() {
        if (forgeLandPlayedTurn != getGame().getPhaseHandler().getTurn()) {
            CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
            if (lands != null) {
                for (Card land : lands) {
                    for (SpellAbility ability : land.getAllPossibleAbilities(player, true)) {
                        if (ability.isLandAbility() && ability.canPlay()) {
                            return ability;
                        }
                    }
                }
            }
        }
        for (String name : new String[] { "Shock", "Lightning Bolt", "Lightning Strike", "Incinerate" }) {
            SpellAbility burn = firstLegalNamedSpell(name);
            if (burn != null) {
                prepareDeterministicBurnTargets(burn, burnDamage(name));
                return burn;
            }
        }
        for (String name : new String[] { "Goblin Piker", "Gray Ogre", "Hill Giant" }) {
            SpellAbility creature = firstLegalNamedSpell(name);
            if (creature != null) {
                return creature;
            }
        }
        return null;
    }

    private SpellAbility firstLegalNamedSpell(String name) {
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(
                ComputerUtilAbility.getAvailableCards(getGame(), player), player);
        for (SpellAbility ability : abilities) {
            Card host = ability.getHostCard();
            if (ability.isSpell() && host != null && name.equals(host.getName()) && ability.canPlay()) {
                return ability;
            }
        }
        return null;
    }

    private void prepareDeterministicBurnTargets(SpellAbility ability, int damage) {
        ability.resetTargets();
        for (Player candidate : getGame().getPlayers()) {
            if (candidate == player) {
                continue;
            }
            for (Card card : candidate.getCardsIn(ZoneType.Battlefield)) {
                if (card.isCreature() && card.getNetToughness() <= damage && ability.canTarget(card)) {
                    ability.getTargets().add(card);
                    return;
                }
            }
        }
        for (Player candidate : getGame().getPlayers()) {
            if (candidate != player && ability.canTarget(candidate)) {
                ability.getTargets().add(candidate);
                return;
            }
        }
    }

    private static int burnDamage(String name) {
        return "Shock".equals(name) ? 2 : 3;
    }

    private SpellAbility firstLegalCoverageCreature() {
        String wanted = countBattlefield(player, "Gray Ogre") < 2
                ? "Gray Ogre"
                : countBattlefield(player, "Hill Giant") == 0 ? "Hill Giant" : "Gray Ogre";
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(
                ComputerUtilAbility.getAvailableCards(getGame(), player), player);
        for (SpellAbility ability : abilities) {
            Card host = ability.getHostCard();
            if (ability.isSpell() && host != null && wanted.equals(host.getName()) && ability.canPlay()) {
                return ability;
            }
        }
        return null;
    }

    private static int countBattlefield(Player owner, String name) {
        int count = 0;
        for (Card card : owner.getCardsIn(ZoneType.Battlefield)) {
            if (name.equals(card.getName())) {
                count++;
            }
        }
        return count;
    }

    private void applyPendingHandSync() {
        JsonNode desiredCountsNode = pendingHandCounts;
        if (desiredCountsNode == null || getGame().getPhaseHandler().getTurn() < pendingHandTurn) {
            return;
        }
        int desiredSize = 0;
        Iterator<JsonNode> desiredCounts = desiredCountsNode.elements();
        while (desiredCounts.hasNext()) {
            desiredSize += desiredCounts.next().asInt();
        }
        PlayerZone handZone = player.getZone(ZoneType.Hand);
        // A turn's reveal arrives before Forge has necessarily performed its
        // draw-step action. Defer reconciliation until the authoritative hand
        // size is reachable; instant-priority callbacks can occur in upkeep.
        if (handZone.size() != desiredSize) {
            return;
        }
        pendingHandCounts = null;
        Map<String, Integer> desired = new LinkedHashMap<>();
        desiredCountsNode.fields().forEachRemaining(entry -> desired.put(entry.getKey(), entry.getValue().asInt()));
        PlayerZone libraryZone = player.getZone(ZoneType.Library);
        List<Card> hand = new ArrayList<>();
        List<Card> library = new ArrayList<>();
        handZone.getCards().forEach(hand::add);
        libraryZone.getCards().forEach(library::add);
        for (Entry<String, Integer> target : desired.entrySet()) {
            while (countNamed(hand, target.getKey()) < target.getValue()) {
                Card incoming = firstNamed(library, target.getKey());
                Card outgoing = firstExcess(hand, desired);
                if (incoming == null || outgoing == null) {
                    throw new IllegalStateException("Cannot reconcile " + target.getKey()
                            + " into " + player.getName() + " hand");
                }
                hand.remove(outgoing);
                library.remove(incoming);
                hand.add(incoming);
                library.add(outgoing);
            }
        }
        handZone.setCards(hand);
        libraryZone.setCards(library);
    }

    private static int countNamed(List<Card> cards, String name) {
        int count = 0;
        for (Card card : cards) {
            if (card.getName().equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static Card firstNamed(List<Card> cards, String name) {
        for (Card card : cards) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        return null;
    }

    private static Card firstExcess(List<Card> hand, Map<String, Integer> desired) {
        Map<String, Integer> actual = new HashMap<>();
        for (Card card : hand) {
            actual.merge(card.getName(), 1, Integer::sum);
        }
        for (Card card : hand) {
            if (actual.get(card.getName()) > desired.getOrDefault(card.getName(), 0)) {
                return card;
            }
        }
        return null;
    }

    private String currentBridgePhase() {
        PhaseType phase = getGame().getPhaseHandler().getPhase();
        if (phase == PhaseType.END_OF_TURN) {
            return "end";
        }
        return phase.nameForScripts.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean mulliganKeepHand(Player mulliganingPlayer, int cardsToReturn) {
        return fullGame || super.mulliganKeepHand(mulliganingPlayer, cardsToReturn);
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
        if (fullGame) {
            return getGame().getPlayers().get(startingSeat - 1);
        }
        return super.chooseStartingPlayer(isFirstGame);
    }

    private SpellAbility matchRemoteAction(JsonNode action) {
        String type = action.path("type").asText();
        if ("pass".equals(type)) {
            return null;
        }
        JsonNode reference = "activate".equals(type) ? action.path("source") : action.path("card");
        String name = reference.path("name").asText();
        int wantedIndex = reference.path("idx").asInt(0);
        List<SpellAbility> matches = new ArrayList<>();

        if ("play_land".equals(type)) {
            CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
            if (lands != null) {
                for (Card land : lands) {
                    if (land.getName().equals(name) && sameNameIndex(land) == wantedIndex) {
                        for (SpellAbility ability : land.getAllPossibleAbilities(player, true)) {
                            if (ability.isLandAbility()) {
                                matches.add(ability);
                            }
                        }
                    }
                }
            }
        } else if ("cast".equals(type) || "activate".equals(type)) {
            List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(
                    ComputerUtilAbility.getAvailableCards(getGame(), player), player);
            for (SpellAbility ability : abilities) {
                boolean correctKind = "cast".equals(type) ? ability.isSpell() : !ability.isSpell();
                Card host = ability.getHostCard();
                if (correctKind && host != null && host.getName().equals(name)
                        && sameNameIndex(host) == wantedIndex && ability.canPlay()) {
                    matches.add(ability);
                }
            }
        } else {
            throw new IllegalStateException("Unsupported remote action type: " + type);
        }

        if (matches.size() != 1) {
            List<String> handState = new ArrayList<>();
            for (Card card : player.getCardsIn(ZoneType.Hand)) {
                handState.add(card.getName());
            }
            List<String> landState = new ArrayList<>();
            for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
                if (card.isLand()) {
                    landState.add(card.getName() + "(tapped=" + card.isTapped() + ")");
                }
            }
            throw new IllegalStateException("Remote action matched " + matches.size()
                    + " Forge legal actions: " + action + "; hand=" + handState + "; lands=" + landState
                    + "; phase=" + currentBridgePhase());
        }
        SpellAbility matched = matches.get(0);
        applyTargets(matched, action.path("targets"));
        return matched;
    }

    private void applyTargets(SpellAbility ability, JsonNode targets) {
        if (!targets.isArray() || targets.isEmpty()) {
            return;
        }
        ability.resetTargets();
        for (JsonNode target : targets) {
            if ("player".equals(target.path("kind").asText())) {
                int targetSeat = target.path("seat").asInt();
                ability.getTargets().add(getGame().getPlayers().get(targetSeat - 1));
            } else if ("card".equals(target.path("kind").asText())) {
                Card card = exactOpponentCard(target.path("card"), "spell target");
                if (!ability.canTarget(card)) {
                    throw new IllegalStateException("Remote spell target is not legal: " + target);
                }
                ability.getTargets().add(card);
            }
        }
    }

    private Card exactOpponentCard(JsonNode reference, String label) {
        String name = reference.path("name").asText();
        int wantedIndex = reference.path("idx").asInt(0);
        List<Card> matches = new ArrayList<>();
        for (Player candidate : getGame().getPlayers()) {
            if (candidate == player) {
                continue;
            }
            for (Card card : candidate.getCardsIn(ZoneType.Battlefield)) {
                if (card.getName().equals(name) && sameNameIndex(card) == wantedIndex) {
                    matches.add(card);
                }
            }
        }
        if (matches.size() != 1) {
            throw new IllegalStateException("Remote " + label + " matched " + matches.size()
                    + " opposing Forge cards: " + reference);
        }
        return matches.get(0);
    }

    private ObjectNode describeAction(SpellAbility ability) {
        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        if (ability.isLandAbility()) {
            result.put("type", "play_land");
            result.set("card", cardReference(ability.getHostCard()));
            return result;
        }

        result.put("type", ability.isSpell() ? "cast" : "activate");
        result.set(ability.isSpell() ? "card" : "source", cardReference(ability.getHostCard()));
        ArrayNode targets = result.putArray("targets");
        TargetChoices chosenTargets = ability.getTargets();
        for (Player target : chosenTargets.getTargetPlayers()) {
            ObjectNode targetNode = targets.addObject();
            targetNode.put("kind", "player");
            targetNode.put("seat", target.getId() + 1);
        }
        for (Card target : chosenTargets.getTargetCards()) {
            ObjectNode targetNode = targets.addObject();
            targetNode.put("kind", "card");
            targetNode.set("card", cardReference(target));
        }
        if (ability.isSpell() && "Lightning Bolt".equals(ability.getHostCard().getName())
                && targets.isEmpty()) {
            ObjectNode targetNode = targets.addObject();
            targetNode.put("kind", "player");
            targetNode.put("seat", seat == 1 ? 2 : 1);
        }
        result.putArray("modes");
        result.putNull("x");
        result.put("payment", "auto");
        return result;
    }

    private ObjectNode cardReference(Card card) {
        ObjectNode reference = BridgeTransport.JSON.createObjectNode();
        reference.put("name", card.getName());
        reference.put("zone", card.getZone() == null ? "none" : card.getZone().getZoneType().name().toLowerCase());
        reference.put("idx", sameNameIndex(card));
        if (card.isInZone(ZoneType.Battlefield)) {
            reference.put("controller", card.getController().getId() + 1);
            reference.put("object_id", "forge-" + card.getId());
        }
        return reference;
    }

    private int sameNameIndex(Card card) {
        if (card.getZone() == null) {
            return 0;
        }
        int index = 0;
        for (Card candidate : card.getZone().getCards()) {
            if (candidate == card) {
                return index;
            }
            if (candidate.getName().equals(card.getName())) {
                index++;
            }
        }
        return index;
    }

    private static ObjectNode passAction() {
        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        result.put("type", "pass");
        return result;
    }

    private String lifeSummary() {
        List<String> totals = new ArrayList<>();
        for (Player gamePlayer : getGame().getPlayers()) {
            totals.add(Integer.toString(gamePlayer.getLife()));
        }
        return String.join("/", totals);
    }


    private void requireForgeAiSeat() {
        if (!forgeAiSeat) {
            throw new IllegalStateException("Decision requested from mirrored opponent seat " + seat);
        }
    }

    private <T> void put(BlockingQueue<T> queue, T value, String operation) {
        try {
            if (cancelled || !queue.offer(value, WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out queuing " + operation);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while queuing " + operation, e);
        }
    }

    private <T> T take(BlockingQueue<T> queue, String operation) {
        try {
            T value = queue.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (cancelled || value == null) {
                throw new IllegalStateException("Timed out waiting for " + operation);
            }
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + operation, e);
        }
    }

    private <T> T await(CompletableFuture<T> future, String operation) {
        try {
            return future.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed waiting for " + operation, e);
        }
    }

    private static final class DecisionTicket {
        private final boolean allowCast;
        private final int turn;
        private final String phase;
        private final CompletableFuture<ObjectNode> result = new CompletableFuture<>();

        private DecisionTicket(boolean allowCast, int turn, String phase) {
            this.allowCast = allowCast;
            this.turn = turn;
            this.phase = phase;
        }
    }

    private static final class RemoteActionTicket {
        private final JsonNode action;
        private final int turn;
        private final String phase;
        private final CompletableFuture<Void> consumed = new CompletableFuture<>();

        private RemoteActionTicket(JsonNode action, int turn, String phase) {
            this.action = action;
            this.turn = turn;
            this.phase = phase;
        }
    }

    private static final class CombatActionTicket {
        private final JsonNode action;
        @SuppressWarnings("unused")
        private final int turn;

        private CombatActionTicket(JsonNode action, int turn) {
            this.action = action;
            this.turn = turn;
        }
    }

    private static final class CombatDecisionTicket {
        private final String kind;
        private final boolean allowCombat;
        private final boolean multiBlocker;
        private final CompletableFuture<ObjectNode> result = new CompletableFuture<>();

        private CombatDecisionTicket(String kind, boolean allowCombat, boolean multiBlocker) {
            this.kind = kind;
            this.allowCombat = allowCombat;
            this.multiBlocker = multiBlocker;
        }
    }
}
