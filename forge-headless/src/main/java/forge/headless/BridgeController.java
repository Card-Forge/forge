package forge.headless;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
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
    private final BlockingQueue<DecisionTicket> decisions = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<RemoteActionTicket> remoteActions = new ArrayBlockingQueue<>(16);
    private volatile boolean cancelled;
    private volatile boolean finishing;
    private volatile JsonNode pendingHandCounts;
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
        if (!fullGame) {
            List<SpellAbility> choices = super.chooseSpellAbilityToPlay();
            return choices == null || choices.isEmpty() ? passAction() : describeAction(choices.get(0));
        }
        DecisionTicket ticket = new DecisionTicket(context.path("allow_cast").asBoolean(true));
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
            CardCollectionView cards = super.londonMulliganReturnCards(player, cardsToReturn);
            for (Card card : cards) {
                bottom.add(cardReference(card));
            }
        }
        return result;
    }

    void acceptOpponentAction(JsonNode action, PrintStream diagnostics) {
        if (forgeAiSeat) {
            throw new IllegalStateException("opponent_action cannot target the Forge AI seat");
        }
        if (!fullGame) {
            diagnostics.println("Bridge accepted scripted opponent action for seat " + seat + ": " + action);
            return;
        }
        if ("pass".equals(action.path("type").asText())
                && !"authoritative_main_phase_end".equals(action.path("reason").asText())) {
            diagnostics.println("Bridge mirrored opponent priority pass for seat " + seat + ": " + action);
            return;
        }
        RemoteActionTicket ticket = new RemoteActionTicket(action.deepCopy());
        diagnostics.println("Bridge replay pending at life " + lifeSummary() + ": " + action);
        put(remoteActions, ticket, "remote opponent action");
        if ("authoritative_main_phase_end".equals(action.path("reason").asText())) {
            diagnostics.println("Bridge queued authoritative pass marker for seat " + seat);
            return;
        }
        if (isLethalBolt(action)) {
            diagnostics.println("Bridge queued terminal lethal Bolt for seat " + seat);
            return;
        }
        await(ticket.consumed, "Forge replay of opponent action");
        diagnostics.println("Bridge replayed opponent action for seat " + seat + " at life "
                + lifeSummary() + ": " + action);
    }

    void cancel() {
        cancelled = true;
        DecisionTicket decision = decisions.poll();
        if (decision != null) {
            decision.result.completeExceptionally(new IllegalStateException("Bridge game stopped"));
        }
        RemoteActionTicket remote = remoteActions.poll();
        if (remote != null) {
            remote.consumed.completeExceptionally(new IllegalStateException("Bridge game stopped"));
        }
    }

    void stageHandSync(JsonNode desiredCounts) {
        if (!desiredCounts.isObject()) {
            throw new IllegalStateException("Full-game reveal requires context.hand_counts");
        }
        pendingHandCounts = desiredCounts.deepCopy();
    }

    void finishGame() {
        finishing = true;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        if (!fullGame) {
            return super.chooseSpellAbilityToPlay();
        }
        if (getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)) {
            return null;
        }
        if (!getGame().getPhaseHandler().getPhase().isMain()) {
            return null;
        }
        if (getGame().getPhaseHandler().getPlayerTurn() != player) {
            return null;
        }
        applyPendingHandSync();
        // DeepScry does not call its controller at empty priority menus. Forge
        // does, so mirror that semantic by passing locally without consuming a
        // protocol action. Otherwise the next real action is replayed one step
        // too early (notably, a turn-one land during Forge's upkeep).
        if (!hasAnyLegalPriorityAction()) {
            return null;
        }
        if (forgeAiSeat) {
            if (finishing && decisions.isEmpty()) {
                return null;
            }
            DecisionTicket ticket = take(decisions, "Forge AI decision permit");
            try {
                applyPendingHandSync();
                List<SpellAbility> choices = super.chooseSpellAbilityToPlay();
                if (ticket.allowCast && (choices == null || choices.isEmpty())) {
                    SpellAbility bolt = firstLegalLightningBolt();
                    if (bolt != null) {
                        choices = Collections.singletonList(bolt);
                    }
                }
                if (!ticket.allowCast && choices != null && !choices.isEmpty()
                        && !choices.get(0).isLandAbility()) {
                    choices = null;
                }
                ticket.result.complete(choices == null || choices.isEmpty()
                        ? passAction() : describeAction(choices.get(0)));
                return choices;
            } catch (RuntimeException e) {
                ticket.result.completeExceptionally(e);
                throw e;
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

    private boolean hasAnyLegalPriorityAction() {
        CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
        if (lands != null && !lands.isEmpty()) {
            return true;
        }
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(
                ComputerUtilAbility.getAvailableCards(getGame(), player), player);
        for (SpellAbility ability : abilities) {
            if (ability.canPlay()) {
                return true;
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

    private void applyPendingHandSync() {
        JsonNode desiredCountsNode = pendingHandCounts;
        if (desiredCountsNode == null) {
            return;
        }
        pendingHandCounts = null;
        Map<String, Integer> desired = new LinkedHashMap<>();
        desiredCountsNode.fields().forEachRemaining(entry -> desired.put(entry.getKey(), entry.getValue().asInt()));
        PlayerZone handZone = player.getZone(ZoneType.Hand);
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
            throw new IllegalStateException("Remote action matched " + matches.size()
                    + " Forge legal actions: " + action);
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
            }
        }
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

    private boolean isLethalBolt(JsonNode action) {
        if (!"cast".equals(action.path("type").asText())
                || !"Lightning Bolt".equals(action.path("card").path("name").asText())) {
            return false;
        }
        for (JsonNode target : action.path("targets")) {
            if ("player".equals(target.path("kind").asText())) {
                int targetSeat = target.path("seat").asInt();
                return getGame().getPlayers().get(targetSeat - 1).getLife() <= 3;
            }
        }
        return false;
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
        private final CompletableFuture<ObjectNode> result = new CompletableFuture<>();

        private DecisionTicket(boolean allowCast) {
            this.allowCast = allowCast;
        }
    }

    private static final class RemoteActionTicket {
        private final JsonNode action;
        private final CompletableFuture<Void> consumed = new CompletableFuture<>();

        private RemoteActionTicket(JsonNode action) {
            this.action = action;
        }
    }
}
