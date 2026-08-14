package forge.headless;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.phase.PhaseType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.MyRandom;

/** Owns one bridge process lifecycle and its local mirrored Forge game. */
final class BridgeSession {
    private static final int PROTOCOL_VERSION = 1;
    private static final int HELLO_REQUEST_ID = 1;

    private final BridgeOptions options;
    private final BridgeTransport transport;
    private final PrintStream diagnostics;
    private final Map<Integer, LobbyPlayerBridge> lobbyPlayers = new HashMap<>();

    private Game game;
    private List<Player> seatPlayers;
    private Match match;
    private Thread gameThread;
    private volatile Throwable gameThreadFailure;
    private String gameId;
    private long lastSequence;
    private boolean shutdownRequested;

    BridgeSession(BridgeOptions options, BridgeTransport transport, PrintStream diagnostics) {
        this.options = options;
        this.transport = transport;
        this.diagnostics = diagnostics;
    }

    void run() throws IOException {
        sendHello();
        acceptHelloResponse();

        JsonNode message;
        while ((message = transport.receive()) != null) {
            try {
                if (message.has("method")) {
                    handleCall(message);
                    if (shutdownRequested) {
                        return;
                    }
                } else {
                    throw new BridgeFailure("invalid_request", "Expected a JSON-RPC method");
                }
            } catch (BridgeFailure e) {
                sendErrorNotification(e.code, e.getMessage());
                if (message.has("id")) {
                    sendErrorResponse(message.get("id"), -32000, e.getMessage());
                }
                throw e;
            }
        }
        diagnostics.println("Bridge input closed; session exiting");
    }

    private void sendHello() throws IOException {
        ObjectNode params = BridgeTransport.JSON.createObjectNode();
        params.put("protocol_version", PROTOCOL_VERSION);
        params.put("engine", "forge");
        ArrayNode capabilities = params.putArray("capabilities");
        capabilities.add("decision.priority");
        capabilities.add("decision.mulligan");
        capabilities.add("event.reveal.library_bag");
        capabilities.add("event.opponent_action.replay");
        capabilities.add("mode.full_game_loop");

        ObjectNode request = rpcMessage();
        request.put("id", HELLO_REQUEST_ID);
        request.put("method", "hello");
        request.set("params", params);
        transport.send(request);
    }

    private void acceptHelloResponse() throws IOException {
        JsonNode response = transport.receive();
        if (response == null) {
            throw new BridgeFailure("handshake_eof", "Input closed before hello response");
        }
        if (!response.path("jsonrpc").asText().equals("2.0")
                || response.path("id").asInt(-1) != HELLO_REQUEST_ID) {
            throw new BridgeFailure("handshake_invalid", "Invalid hello response envelope");
        }
        if (response.has("error")) {
            throw new BridgeFailure("handshake_rejected", response.get("error").toString());
        }
        JsonNode result = response.path("result");
        if (!result.path("accepted").asBoolean(false)
                || result.path("protocol_version").asInt(-1) != PROTOCOL_VERSION) {
            throw new BridgeFailure("version_mismatch", "Peer rejected bridge protocol version " + PROTOCOL_VERSION);
        }
        diagnostics.println("Bridge hello accepted by " + result.path("engine").asText("peer"));
    }

    private void handleCall(JsonNode message) throws IOException {
        throwIfGameThreadFailed();
        if (!"2.0".equals(message.path("jsonrpc").asText())) {
            throw new BridgeFailure("invalid_request", "jsonrpc must be 2.0");
        }
        String method = message.path("method").asText();
        JsonNode params = message.path("params");
        JsonNode id = message.get("id");

        switch (method) {
        case "game_start":
            requireRequest(id, method);
            sendResult(id, startGame(params));
            break;
        case "decision":
            requireRequest(id, method);
            sendResult(id, handleDecision(params));
            break;
        case "reveal":
            requireNotification(id, method);
            handleReveal(params);
            break;
        case "opponent_action":
            requireNotification(id, method);
            handleOpponentAction(params);
            break;
        case "state_digest":
            requireNotification(id, method);
            handleStateDigest(params);
            break;
        case "game_end":
            requireNotification(id, method);
            handleGameEnd(params);
            break;
        case "error":
            requireNotification(id, method);
            diagnostics.println("Peer bridge error: " + params);
            break;
        case "shutdown":
            requireRequest(id, method);
            stopGameThread();
            ObjectNode shutdown = BridgeTransport.JSON.createObjectNode();
            shutdown.put("accepted", true);
            sendResult(id, shutdown);
            diagnostics.println("Bridge shutdown requested");
            shutdownRequested = true;
            break;
        default:
            if (id != null) {
                sendErrorResponse(id, -32601, "Method not found: " + method);
            }
            diagnostics.println("Ignoring unsupported bridge notification: " + method);
            break;
        }

    }

    private ObjectNode startGame(JsonNode params) {
        if (game != null) {
            throw new BridgeFailure("game_already_started", "A local Forge game already exists");
        }
        gameId = requireText(params, "game_id");
        int yourSeat = requireInt(params, "your_seat");
        if (yourSeat != options.getSeat()) {
            throw new BridgeFailure("seat_mismatch", "game_start your_seat does not match --seat");
        }
        JsonNode seedNode = params.path("rng").get("seed");
        if (seedNode != null && !seedNode.isNull() && seedNode.asLong() != options.getSeed()) {
            throw new BridgeFailure("seed_mismatch", "game_start RNG seed does not match --seed");
        }

        int startingSeat = requireInt(params, "starting_player");
        ArrayNode protocolPlayers = requireArray(params, "players");
        if (protocolPlayers.size() != options.getDecks().size()) {
            throw new BridgeFailure("player_count_mismatch", "game_start player count does not match -d decks");
        }

        List<RegisteredPlayer> registeredPlayers = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        for (int index = 0; index < protocolPlayers.size(); index++) {
            int seat = index + 1;
            JsonNode protocolPlayer = protocolPlayers.get(index);
            if (requireInt(protocolPlayer, "seat") != seat) {
                throw new BridgeFailure("seat_order", "Players must be supplied in contiguous seat order");
            }
            File deckFile = options.getDecks().get(index);
            Deck deck = DeckSerializer.fromFile(deckFile);
            if (deck == null) {
                throw new BridgeFailure("deck_load", "Could not load deck " + deckFile);
            }
            validateDecklist(deck, requireArray(protocolPlayer, "decklist"), seat);
            decks.add(deck);

            LobbyPlayerBridge lobbyPlayer = new LobbyPlayerBridge(requireText(protocolPlayer, "name"), seat,
                    seat == yourSeat, !options.isSkeleton(), startingSeat);
            lobbyPlayers.put(seat, lobbyPlayer);
            RegisteredPlayer registeredPlayer = new RegisteredPlayer(deck);
            registeredPlayer.setPlayer(lobbyPlayer);
            registeredPlayer.setId(index);
            registeredPlayers.add(registeredPlayer);
        }

        GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.Constructed));
        match = new Match(rules, registeredPlayers, "Bridge-" + gameId);
        game = match.createGame();
        seatPlayers = new ArrayList<>(game.getPlayers());
        if (options.isSkeleton()) {
            initializeDeckZones(decks);
            Player startingPlayer = playerForSeat(startingSeat);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, startingPlayer);
        }

        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        result.put("accepted", true);
        result.put("game_id", gameId);
        result.put("state", "ready");
        ArrayNode libraries = result.putArray("library_counts");
        ArrayNode hands = result.putArray("hand_counts");
        for (int index = 0; index < game.getPlayers().size(); index++) {
            if (options.isSkeleton()) {
                Player player = game.getPlayers().get(index);
                libraries.add(player.getZone(ZoneType.Library).size());
                hands.add(player.getZone(ZoneType.Hand).size());
            } else {
                libraries.add(decks.get(index).getMain().countAll());
                hands.add(0);
            }
        }
        diagnostics.println("Constructed local Forge bridge game " + gameId + " with "
                + registeredPlayers.size() + " seats");
        if (!options.isSkeleton()) {
            startGameThread();
            result.put("state", "running");
        }
        return result;
    }

    private void validateDecklist(Deck deck, ArrayNode protocolDecklist, int seat) {
        Map<String, Integer> expected = new LinkedHashMap<>();
        for (Entry<PaperCard, Integer> entry : deck.getMain()) {
            expected.merge(entry.getKey().getName(), entry.getValue(), Integer::sum);
        }
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (JsonNode card : protocolDecklist) {
            actual.merge(requireText(card, "name"), requireInt(card, "count"), Integer::sum);
        }
        if (!expected.equals(actual)) {
            throw new BridgeFailure("decklist_mismatch", "Protocol decklist differs from -d deck for seat " + seat);
        }
    }

    private void initializeDeckZones(List<Deck> decks) {
        for (int index = 0; index < decks.size(); index++) {
            Player player = game.getPlayers().get(index);
            CardPool main = decks.get(index).get(DeckSection.Main);
            List<Card> library = new ArrayList<>();
            for (Entry<PaperCard, Integer> entry : main) {
                for (int copy = 0; copy < entry.getValue(); copy++) {
                    Card card = Card.fromPaperCard(entry.getKey(), player);
                    card.setCollectible(true);
                    library.add(card);
                }
            }
            Collections.shuffle(library, MyRandom.getRandom());
            player.getZone(ZoneType.Library).setCards(library);
            player.drawCards(player.getStartingHandSize());
        }
    }

    private ObjectNode handleDecision(JsonNode params) {
        requireGame();
        checkSequence(params);
        BridgeController controller = lobbyPlayers.get(options.getSeat()).getController();
        String kind = requireText(params, "kind");
        switch (kind) {
        case "priority":
            return controller.decidePriority(params.path("context"));
        case "declare_attackers":
        case "declare_blockers":
            return controller.decideCombat(kind, params.path("context"));
        case "mulligan":
            int cardsToReturn = params.path("context").path("cards_to_return").asInt(0);
            return controller.decideMulligan(cardsToReturn);
        default:
            return passWithReason("unsupported_decision_kind");
        }
    }

    private void handleReveal(JsonNode params) {
        requireGame();
        checkSequence(params);
        int seat = requireInt(params, "seat");
        Player player = playerForSeat(seat);
        String zoneFrom = requireText(params, "zone_from");
        if (!"library".equals(zoneFrom)) {
            throw new BridgeFailure("unsupported_reveal_zone", "Task B reveal only supports zone_from=library");
        }
        String cardName = requireText(params.path("card"), "name");
        if (!options.isSkeleton()) {
            JsonNode context = params.path("context");
            lobbyPlayers.get(seat).getController().stageHandSync(context.path("hand_counts"),
                    requireInt(context, "turn"));
            diagnostics.println("Staged revealed hand for seat " + seat + ": " + cardName);
            return;
        }
        PlayerZone library = player.getZone(ZoneType.Library);
        List<Card> reordered = new ArrayList<>();
        for (Card card : library.getCards()) {
            reordered.add(card);
        }
        Card revealed = null;
        for (Card candidate : reordered) {
            if (candidate.getName().equals(cardName)) {
                revealed = candidate;
                break;
            }
        }
        if (revealed == null) {
            throw new BridgeFailure("reveal_card_missing", "No " + cardName + " remains in seat " + seat + " library");
        }
        reordered.remove(revealed);
        reordered.add(0, revealed);
        library.setCards(reordered);

        JsonNode zoneTo = params.get("zone_to");
        if (zoneTo != null && "hand".equals(zoneTo.asText())) {
            player.drawCards(1);
        }
        diagnostics.println("Forced reveal for seat " + seat + ": " + cardName + " from library");
    }

    private void handleOpponentAction(JsonNode params) {
        requireGame();
        checkSequence(params);
        int seat = requireInt(params, "seat");
        if (seat == options.getSeat()) {
            throw new BridgeFailure("opponent_seat", "opponent_action named the Forge AI seat");
        }
        lobbyPlayers.get(seat).getController().acceptOpponentAction(
                params.path("action"), params.path("context"), diagnostics);
    }

    private void handleStateDigest(JsonNode params) {
        requireGame();
        checkSequence(params);
        ArrayNode life = requireArray(params, "life");
        ArrayNode hands = requireArray(params, "hand_counts");
        ArrayNode permanents = requireArray(params, "permanents_count");
        ArrayNode creatureCounts = requireArray(params, "creature_counts");
        ArrayNode creaturePower = requireArray(params, "creature_power");
        ArrayNode creatures = requireArray(params, "creatures");
        if (life.size() != seatPlayers.size() || hands.size() != seatPlayers.size()
                || permanents.size() != seatPlayers.size()
                || creatureCounts.size() != seatPlayers.size()
                || creaturePower.size() != seatPlayers.size()
                || creatures.size() != seatPlayers.size()) {
            throw new BridgeFailure("state_digest_shape", "Digest player arrays do not match Forge seats");
        }
        List<Integer> actualLife = null;
        List<Integer> actualHands = null;
        List<Integer> actualPermanents = null;
        List<Integer> actualCreatureCounts = null;
        List<Integer> actualCreaturePower = null;
        List<List<String>> actualCreatures = null;
        int expectedTurn = params.path("turn").asInt();
        int actualTurn = 0;
        boolean matched = false;
        for (int attempt = 0; attempt < 500 && !matched; attempt++) {
            actualLife = new ArrayList<>();
            actualHands = new ArrayList<>();
            actualPermanents = new ArrayList<>();
            actualCreatureCounts = new ArrayList<>();
            actualCreaturePower = new ArrayList<>();
            actualCreatures = new ArrayList<>();
            actualTurn = game.getPhaseHandler().getTurn();
            boolean crossedIntoNextDraw = actualTurn == expectedTurn + 1;
            matched = actualTurn == expectedTurn || crossedIntoNextDraw;
            int nextTurnSeatIndex = (actualTurn - 1) % seatPlayers.size();
            for (int index = 0; index < seatPlayers.size(); index++) {
                Player player = seatPlayers.get(index);
                actualLife.add(player.getLife());
                int handSize = player.getZone(ZoneType.Hand).size();
                // With no remaining legal Main2 menu, Forge may execute the
                // next turn's mandatory draw before this notification thread
                // samples the resolved action. Normalize precisely that one
                // known transition back to the requested barrier state.
                if (crossedIntoNextDraw && index == nextTurnSeatIndex) {
                    handSize--;
                }
                actualHands.add(handSize);
                actualPermanents.add(player.getCardsIn(ZoneType.Battlefield).size());
                int playerCreaturePower = 0;
                List<String> playerCreatures = new ArrayList<>();
                for (Card card : player.getCardsIn(ZoneType.Battlefield).threadSafeIterable()) {
                    if (card.isCreature()) {
                        playerCreaturePower += card.getNetPower();
                        playerCreatures.add(card.getName());
                    }
                }
                Collections.sort(playerCreatures);
                actualCreatureCounts.add(playerCreatures.size());
                actualCreaturePower.add(playerCreaturePower);
                actualCreatures.add(playerCreatures);
                matched &= life.get(index).asInt() == actualLife.get(index)
                        && hands.get(index).asInt() == actualHands.get(index)
                        && permanents.get(index).asInt() == actualPermanents.get(index)
                        && creatureCounts.get(index).asInt() == actualCreatureCounts.get(index)
                        && creaturePower.get(index).asInt() == actualCreaturePower.get(index)
                        && jsonStrings(creatures.get(index)).equals(playerCreatures);
            }
            if (!matched) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BridgeFailure("state_digest_interrupted", "Interrupted awaiting digest state");
                }
            }
        }
        if (!matched) {
            throw new BridgeFailure("state_desync", "Digest mismatch at turn " + expectedTurn
                    + ": expected life/hand/permanents/creatures/power/identity="
                    + life + "/" + hands + "/" + permanents + "/" + creatureCounts + "/" + creaturePower
                    + "/" + creatures + ", Forge=" + actualLife + "/" + actualHands + "/" + actualPermanents
                    + "/" + actualCreatureCounts + "/" + actualCreaturePower + "/" + actualCreatures
                    + " at turn=" + actualTurn + " phase=" + game.getPhaseHandler().getPhase());
        }
        String actualDigest = "t" + params.path("turn").asInt()
                + "|l" + actualLife.get(0) + "," + actualLife.get(1)
                + "|h" + actualHands.get(0) + "," + actualHands.get(1)
                + "|p" + actualPermanents.get(0) + "," + actualPermanents.get(1)
                + "|c" + actualCreatureCounts.get(0) + "," + actualCreatureCounts.get(1)
                + "|pow" + actualCreaturePower.get(0) + "," + actualCreaturePower.get(1)
                + "|ci" + String.join("+", actualCreatures.get(0)) + ","
                + String.join("+", actualCreatures.get(1));
        if (!actualDigest.equals(requireText(params, "digest"))) {
            throw new BridgeFailure("state_digest_value", "Digest string mismatch: " + actualDigest);
        }
        diagnostics.println("State digest matched: " + actualDigest);
    }

    private static List<String> jsonStrings(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            values.add(value.asText());
        }
        return values;
    }

    private void handleGameEnd(JsonNode params) {
        requireGame();
        if (params.has("seq")) {
            checkSequence(params);
        }
        diagnostics.println("Bridge game ended: result=" + params.path("result").asText()
                + ", winner=" + params.path("winner").asText("none")
                + ", reason=" + params.path("reason").asText());
        if (!options.isSkeleton()) {
            for (LobbyPlayerBridge lobbyPlayer : lobbyPlayers.values()) {
                lobbyPlayer.getController().finishGame();
            }
            awaitGameThread();
        }
    }

    private void startGameThread() {
        gameThread = new Thread(() -> {
            try {
                match.startGame(game);
            } catch (Throwable failure) {
                gameThreadFailure = failure;
                diagnostics.println("Forge game thread failure: " + failure);
                failure.printStackTrace(diagnostics);
            } finally {
                diagnostics.println("Forge game thread stopped at turn " + game.getPhaseHandler().getTurn()
                        + " phase " + game.getPhaseHandler().getPhase() + " gameOver=" + game.isGameOver());
                for (LobbyPlayerBridge lobbyPlayer : lobbyPlayers.values()) {
                    lobbyPlayer.getController().cancel();
                }
            }
        }, "forge-bridge-game-loop");
        gameThread.start();
    }

    private void awaitGameThread() {
        try {
            gameThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BridgeFailure("game_thread_interrupted", "Interrupted waiting for Forge game completion");
        }
        if (gameThread.isAlive()) {
            throw new BridgeFailure("game_thread_timeout", "Forge game did not finish with the authoritative game");
        }
        throwIfGameThreadFailed();
    }

    private void stopGameThread() {
        if (gameThread == null || !gameThread.isAlive()) {
            return;
        }
        for (LobbyPlayerBridge lobbyPlayer : lobbyPlayers.values()) {
            lobbyPlayer.getController().cancel();
        }
        gameThread.interrupt();
    }

    private void throwIfGameThreadFailed() {
        if (gameThreadFailure != null) {
            throw new BridgeFailure("forge_game_failure", gameThreadFailure.toString());
        }
    }

    private void checkSequence(JsonNode params) {
        long sequence = params.path("seq").asLong(-1);
        if (sequence <= lastSequence) {
            throw new BridgeFailure("sequence_error", "Expected seq greater than " + lastSequence + ", got " + sequence);
        }
        lastSequence = sequence;
    }

    private Player playerForSeat(int seat) {
        if (seat < 1 || seat > game.getPlayers().size()) {
            throw new BridgeFailure("invalid_seat", "No player at seat " + seat);
        }
        return game.getPlayers().get(seat - 1);
    }

    private void requireGame() {
        if (game == null) {
            throw new BridgeFailure("game_not_started", "game_start must precede this message");
        }
    }

    private static void requireRequest(JsonNode id, String method) {
        if (id == null || id.isNull()) {
            throw new BridgeFailure("request_required", method + " requires a JSON-RPC id");
        }
    }

    private static void requireNotification(JsonNode id, String method) {
        if (id != null) {
            throw new BridgeFailure("notification_required", method + " must not include a JSON-RPC id");
        }
    }

    private void sendResult(JsonNode id, JsonNode result) throws IOException {
        ObjectNode response = rpcMessage();
        response.set("id", id);
        response.set("result", result);
        transport.send(response);
    }

    private void sendErrorResponse(JsonNode id, int code, String detail) throws IOException {
        ObjectNode response = rpcMessage();
        response.set("id", id == null ? NullNode.instance : id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", detail);
        transport.send(response);
    }

    private void sendErrorNotification(String code, String detail) throws IOException {
        ObjectNode notification = rpcMessage();
        notification.put("method", "error");
        ObjectNode params = notification.putObject("params");
        params.put("code", code);
        params.put("detail", detail);
        transport.send(notification);
    }

    private static ObjectNode rpcMessage() {
        ObjectNode message = BridgeTransport.JSON.createObjectNode();
        message.put("jsonrpc", "2.0");
        return message;
    }

    private static ObjectNode passWithReason(String reason) {
        ObjectNode pass = BridgeTransport.JSON.createObjectNode();
        pass.put("type", "pass");
        pass.put("reason", reason);
        return pass;
    }

    private static String requireText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual() || value.asText().isEmpty()) {
            throw new BridgeFailure("invalid_params", "Expected non-empty string field " + field);
        }
        return value.asText();
    }

    private static int requireInt(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw new BridgeFailure("invalid_params", "Expected integer field " + field);
        }
        return value.asInt();
    }

    private static ArrayNode requireArray(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (!(value instanceof ArrayNode)) {
            throw new BridgeFailure("invalid_params", "Expected array field " + field);
        }
        return (ArrayNode) value;
    }

    private static class BridgeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String code;

        BridgeFailure(String code, String message) {
            super(message);
            this.code = code;
        }
    }

}
