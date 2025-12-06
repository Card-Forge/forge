package forge.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.GameEntity;

import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.zone.ZoneType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.WrappedAbility;
import forge.model.FModel;
import forge.gui.GuiBase;
import forge.util.Aggregates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Headless server for Forge.
 * Listens for HTTP requests to control the game state.
 */
public class ForgeHeadlessServer {
    private static final int PORT = 8080;
    private static final int HTTP_OK = 200;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;

    private static final int BUFFER_SIZE = 1024;
    private static final int WAIT_TIMEOUT_MS = 5000;
    private static final int SLEEP_INTERVAL_MS = 10;
    private static final int DECK_SIZE_SMALL = 20;

    private static volatile Game currentGame = null;
    private static volatile Thread currentGameThread = null;
    private static final BlockingQueue<String> ACTION_QUEUE = new LinkedBlockingQueue<>();
    private static final AtomicReference<JsonObject> LAST_GAME_STATE = new AtomicReference<>(new JsonObject());
    private static volatile boolean waitingForInput = false;
    private static volatile boolean shouldStopGame = false;
    private static volatile Deck player1InitialDeck = null;
    private static volatile Deck player2InitialDeck = null;

    // Store current combat actions so getPossibleActions can access them during
    // combat phases
    private static volatile JsonArray currentCombatActions = null;

    /**
     * Main entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        System.out.println("Starting ForgeHeadless Server on port " + PORT);

        // Initialize Forge Resources
        // Initialize Forge Resources
        // We need a HeadlessGui implementation. Since the one in ForgeHeadless might be
        // private/inaccessible,
        // we will define our own minimal one here.
        GuiBase.setInterface(new ServerHeadlessGui());
        FModel.initialize(null, null);

        startHttpServer();
    }

    /**
     * Starts the HTTP server to listen for game control requests.
     */
    private static void startHttpServer() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // POST /api/reset
            server.createContext("/api/reset", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, HTTP_METHOD_NOT_ALLOWED, "Method Not Allowed");
                    return;
                }

                System.out.println("Received /api/reset request");

                // Parse request body
                JsonObject options = new JsonObject();
                try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                        BufferedReader br = new BufferedReader(isr)) {
                    String value = br.lines().collect(Collectors.joining("\n"));
                    if (value != null && !value.isEmpty()) {
                        try {
                            options = JsonParser.parseString(value).getAsJsonObject();
                        } catch (Exception e) {
                            System.err.println("Failed to parse JSON body: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading request body: " + e.getMessage());
                }

                resetGame(options);

                // Wait for game to start and produce initial state
                waitForDecisionPoint();

                sendResponse(exchange, HTTP_OK, LAST_GAME_STATE.get().toString());
            });

            // POST /api/step
            server.createContext("/api/step", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, HTTP_METHOD_NOT_ALLOWED, "Method Not Allowed");
                    return;
                }

                // Read action from body
                try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                        BufferedReader br = new BufferedReader(isr)) {
                    String action = br.lines().collect(Collectors.joining("\n"));
                    System.out.println("Received /api/step action: " + action);

                    // Send action to game thread
                    ACTION_QUEUE.offer(action);

                    // Wait for next decision point
                    waitForDecisionPoint();

                    sendResponse(exchange, HTTP_OK, LAST_GAME_STATE.get().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "Internal Server Error");
                }
            });

            // GET /api/state
            server.createContext("/api/state", exchange -> {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, HTTP_METHOD_NOT_ALLOWED, "Method Not Allowed");
                    return;
                }
                sendResponse(exchange, HTTP_OK, LAST_GAME_STATE.get().toString());
            });

            server.setExecutor(null);
            server.start();
            System.out.println("HTTP Server started. Waiting for requests...");

        } catch (final IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Resets the current game and starts a new one in a separate thread.
     * 
     * @param options Configuration options for the new game (e.g. decks).
     */
    private static void resetGame(final JsonObject options) {
        // Stop existing game and thread if any
        if (currentGame != null) {
            System.out.println("Stopping existing game...");
            shouldStopGame = true;

            try {
                // Wait for the old game thread to finish (with timeout)
                if (currentGameThread != null && currentGameThread.isAlive()) {
                    System.out.println("Waiting for old game thread to finish...");
                    currentGameThread.join(2000); // Wait up to 2 seconds
                    if (currentGameThread.isAlive()) {
                        System.err.println("Old game thread didn't finish, interrupting...");
                        currentGameThread.interrupt();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error stopping game: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Clear everything - INCLUDING LAST_GAME_STATE to avoid returning stale
        // observations
        currentGame = null;
        currentGameThread = null;
        ACTION_QUEUE.clear();
        LAST_GAME_STATE.set(new JsonObject()); // CRITICAL: Clear old game state!
        waitingForInput = false;
        shouldStopGame = false;

        System.out.println("Starting new game thread...");

        // Generate decks BEFORE starting the thread
        // This ensures we fail fast if deck generation fails
        Deck deck1 = null;
        Deck deck2 = null;

        // Check for custom decks in options
        // TODO: Implement parsing of custom deck lists from JSON

        // Default to Random Standard Decks if not provided
        if (deck1 == null) {
            System.out.println("Generating Random Standard Deck for Player 1...");
            deck1 = generateRandomStandardDeck();
        }
        if (deck2 == null) {
            System.out.println("Generating Random Standard Deck for Player 2...");
            deck2 = generateRandomStandardDeck();
        }

        final Deck finalDeck1 = deck1;
        final Deck finalDeck2 = deck2;

        // Store the initial decks so we can include them in the observation
        player1InitialDeck = finalDeck1;
        player2InitialDeck = finalDeck2;

        // Start a new game thread
        currentGameThread = new Thread(() -> {
            try {
                final List<RegisteredPlayer> players = new ArrayList<>();
                players.add(new RegisteredPlayer(finalDeck1).setPlayer(new ServerPlayer("Player 1")));
                players.add(
                        new RegisteredPlayer(finalDeck2).setPlayer(new forge.ai.LobbyPlayerAi("AI Player 2", null)));

                final GameRules rules = new GameRules(GameType.Constructed);
                final Match match = new Match(rules, players, "Server Match");

                // Create the game - this is synchronous
                currentGame = match.createGame();
                System.out.println("Game created! Starting match...");

                // Start the game - this will block until game is over
                match.startGame(currentGame);
                System.out.println("Match finished");
            } catch (Exception e) {
                System.err.println("Error in game thread: " + e.getMessage());
                e.printStackTrace();
            }
        });
        currentGameThread.start();

        // STEP 1: Wait for the new game to actually be created
        System.out.println("Waiting for new game object to be created...");
        int waited = 0;
        while (currentGame == null && waited < 5000) {
            try {
                Thread.sleep(50);
                waited += 50;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (currentGame == null) {
            throw new RuntimeException("Failed to create new game within timeout!");
        }

        System.out.println("Game object created, now waiting for Turn 1...");

        // STEP 2: Wait for the new game to reach Turn 1
        // This ensures we don't return with stale state or Turn 0
        waited = 0;
        int maxWaitForTurn1 = 10000; // 10 seconds
        while (waited < maxWaitForTurn1) {
            try {
                if (currentGame != null &&
                        currentGame.getPhaseHandler() != null) {
                    int turn = currentGame.getPhaseHandler().getTurn();
                    if (turn >= 1) {
                        System.out.println("New game reached Turn " + turn + "! Reset complete.");
                        break;
                    }
                }
                Thread.sleep(100);
                waited += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Ignore exceptions during transition
            }
        }

        // STEP 3: Give the game a moment to stabilize and update state
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("New game is ready and initialized!");
    }

    private static Deck generateRandomStandardDeck() {
        try {
            int count = Aggregates.randomInt(1, 3);
            List<String> colors = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                colors.add("Random " + i);
            }
            Deck deck = DeckgenUtil.buildColorDeck(colors, FModel.getFormats().getStandard().getFilterPrinted(), true);

            // Validate the deck was actually generated with cards
            if (deck == null || deck.getMain().isEmpty()) {
                throw new RuntimeException("Generated deck is null or empty!");
            }

            System.out.println("Successfully generated random deck with " + deck.getMain().countAll() + " cards");
            return deck;
        } catch (Exception e) {
            // FAIL LOUD - Don't hide deck generation failures!
            System.err.println("FATAL ERROR: Failed to generate random deck!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            // Throw the exception to make the problem visible
            throw new RuntimeException("Deck generation failed - cannot start game without valid decks!", e);
        }
    }

    /**
     * Waits for the game thread to reach a decision point, indicated by
     * the {@code waitingForInput} flag, or for the game to end.
     */
    private static void waitForDecisionPoint() {
        int slept = 0;
        while (!waitingForInput && slept < WAIT_TIMEOUT_MS) {
            if (currentGame != null && currentGame.isGameOver()) {
                // Game is over, stop waiting
                break;
            }
            try {
                Thread.sleep(SLEEP_INTERVAL_MS);
                slept += SLEEP_INTERVAL_MS;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (slept >= WAIT_TIMEOUT_MS) {
            System.err.println("Timed out waiting for game decision point.");
        }

        // Always update game state after waiting (to capture game over state)
        if (currentGame != null) {
            updateGameState(currentGame);
        }
    }

    // --- Helper Classes ---

    /**
     * Custom player implementation for the server.
     */
    private static class ServerPlayer extends forge.ai.LobbyPlayerAi {
        /**
         * Constructs a ServerPlayer.
         * 
         * @param name The player's name.
         */
        ServerPlayer(final String name) {
            super(name, null);
        }

        @Override
        public Player createIngamePlayer(final Game game, final int id) {
            System.out.println("ServerPlayer.createIngamePlayer called for: " + getName());
            final Player player = new Player(getName(), game, id);
            // Install our custom controller that gives agent control
            player.setFirstController(new ServerPlayerController(game, player, this));
            System.out.println("ServerPlayer: Installed ServerPlayerController for " + getName());
            return player;
        }

        @Override
        public forge.ai.PlayerControllerAi createMindSlaveController(final Player master, final Player slave) {
            final Game game = slave.getGame();
            return new ServerPlayerController(game, slave, this);
        }
    }

    /**
     * Custom player controller for the server, intercepting decision points.
     */
    private static class ServerPlayerController extends forge.ai.PlayerControllerAi {
        /**
         * Constructs a ServerPlayerController.
         * 
         * @param game        The game instance.
         * @param player      The player this controller belongs to.
         * @param lobbyPlayer The lobby player associated with this controller.
         */
        ServerPlayerController(final Game game, final Player player, final forge.ai.LobbyPlayerAi lobbyPlayer) {
            super(game, player, lobbyPlayer);
        }

        // --- Hybrid Agent Interception Points ---

        public List<SpellAbility> chooseSpellAbilityToPlay() {
            // 1. Update Game State
            updateGameState(getGame());

            // 2. Ask Python for action
            // We expect "play_action <index>" or "pass_priority"
            final String action = askPython();

            if (action.startsWith("play_action")) {
                final int index = Integer.parseInt(action.split(" ")[1]);
                final JsonObject actions = getPossibleActions(player, getGame());
                final JsonArray list = actions.getAsJsonArray("actions");
                if (index >= 0 && index < list.size()) {
                    // In a real impl, we'd map this back to the actual SpellAbility object
                    // For now, let's just return null (pass priority) if it's the pass action
                    // This part needs the same logic as ForgeHeadless to map index -> SpellAbility
                    return null; // Placeholder
                }
            }
            return null; // Default to pass
        }

        @Override
        public void declareAttackers(final Player attacker, final Combat combat) {
            System.out.println("ServerPlayer: declareAttackers called - agent has control");

            // Get all creatures that could potentially attack
            final CardCollectionView potentialAttackers = attacker.getCreaturesInPlay();

            // Build options list with all possible attackers
            while (true) {
                final JsonArray attackerOptions = new JsonArray();

                // Add each potential attacker as an option
                for (final Card creature : potentialAttackers) {
                    if (combat.isAttacking(creature)) {
                        continue; // Already declared as attacker
                    }
                    // Use CombatUtil to check if creature can attack
                    if (!CombatUtil.canAttack(creature)) {
                        continue; // Can't attack (e.g., summoning sickness, tapped)
                    }

                    final JsonObject option = new JsonObject();
                    option.addProperty("type", "declare_attacker");
                    option.addProperty("creature", creature.toString());
                    option.addProperty("creature_id", creature.getId());
                    attackerOptions.add(option);
                }

                // Add "pass priority" option to signal done
                final JsonObject passOption = new JsonObject();
                passOption.addProperty("type", "pass_priority");
                attackerOptions.add(passOption);

                // Store combat actions so getPossibleActions can access them
                currentCombatActions = attackerOptions;

                // Wait for agent's choice
                waitingForInput = true;
                ForgeHeadlessServer.updateGameState(currentGame);

                final JsonObject actionPayload = ForgeHeadlessServer.waitForNextAction();
                waitingForInput = false;

                if (actionPayload == null) {
                    System.err.println("No action received, finishing attack declaration");
                    break;
                }

                // Check if agent passed priority (done declaring attackers)
                final String actionType = actionPayload.has("action") ? actionPayload.get("action").getAsString() : "";

                if ("pass_priority".equals(actionType)) {
                    System.out.println("Agent passed priority - done declaring attackers");
                    break;
                }

                // Apply attacker declaration
                if (actionPayload.has("index")) {
                    final int index = actionPayload.get("index").getAsInt();
                    if (index >= 0 && index < attackerOptions.size()) {
                        final JsonObject selectedOption = attackerOptions.get(index).getAsJsonObject();
                        if ("pass_priority".equals(selectedOption.get("type").getAsString())) {
                            System.out.println("Agent passed priority (via index) - done declaring attackers");
                            break;
                        }

                        // Find and declare the attacker
                        final int creatureId = selectedOption.get("creature_id").getAsInt();
                        for (final Card creature : potentialAttackers) {
                            if (creature.getId() == creatureId) {
                                // Declare this creature as an attacker
                                final GameEntity defender = combat.getDefenders().iterator().next();
                                combat.addAttacker(creature, defender);
                                System.out.println("Declared " + creature + " as attacker");
                                break;
                            }
                        }
                    }
                }
            }

            // Clear combat actions after finishing attacker declarations
            currentCombatActions = null;
        }

        @Override
        public void declareBlockers(final Player defender, final Combat combat) {
            System.out.println("ServerPlayer: declareBlockers called - agent has control");

            // Build list of possible blocker assignments
            // Define missing variables
            final CardCollectionView potentialBlockers = defender.getCreaturesInPlay();
            final CardCollectionView attackers = combat.getAttackers();

            while (true) {
                final JsonArray blockerOptions = new JsonArray();

                // For each potential blocker, show which attackers it can block
                for (final Card blocker : potentialBlockers) {
                    if (combat.isBlocking(blocker)) {
                        continue; // Already declared as blocker
                    }
                    if (blocker.isTapped()) {
                        continue; // Can't block (tapped)
                    }

                    // Check each attacker this blocker could block
                    for (final Card attacker : attackers) {
                        if (CombatUtil.canBlock(attacker, blocker, combat)) {
                            final JsonObject option = new JsonObject();
                            option.addProperty("type", "declare_blocker");
                            option.addProperty("blocker", blocker.toString());
                            option.addProperty("blocker_id", blocker.getId());
                            option.addProperty("attacker", attacker.toString());
                            option.addProperty("attacker_id", attacker.getId());
                            blockerOptions.add(option);
                        }
                    }
                }

                // Always add "pass priority" option to signal done (even if no legal blockers)
                final JsonObject passOption = new JsonObject();
                passOption.addProperty("type", "pass_priority");
                blockerOptions.add(passOption);

                // Store combat actions so getPossibleActions can access them
                currentCombatActions = blockerOptions;

                // Wait for agent's choice
                waitingForInput = true;
                ForgeHeadlessServer.updateGameState(currentGame);

                final JsonObject actionPayload = ForgeHeadlessServer.waitForNextAction();
                waitingForInput = false;

                if (actionPayload == null) {
                    System.err.println("No action received, finishing blocker declaration");
                    break;
                }

                // Check if agent passed priority (done declaring blockers)
                final String actionType = actionPayload.has("action") ? actionPayload.get("action").getAsString() : "";

                if ("pass_priority".equals(actionType)) {
                    System.out.println("Agent passed priority - done declaring blockers");
                    break;
                }

                // Apply blocker declaration
                if (actionPayload.has("index")) {
                    final int index = actionPayload.get("index").getAsInt();
                    if (index >= 0 && index < blockerOptions.size()) {
                        final JsonObject selectedOption = blockerOptions.get(index).getAsJsonObject();
                        if ("pass_priority".equals(selectedOption.get("type").getAsString())) {
                            System.out.println("Agent passed priority (via index) - done declaring blockers");
                            break;
                        }

                        // Find and declare the blocker
                        final int blockerId = selectedOption.get("blocker_id").getAsInt();
                        final int attackerId = selectedOption.get("attacker_id").getAsInt();

                        Card blockerCard = null;
                        Card attackerCard = null;

                        for (final Card blocker : potentialBlockers) {
                            if (blocker.getId() == blockerId) {
                                blockerCard = blocker;
                                break;
                            }
                        }

                        for (final Card attacker : attackers) {
                            if (attacker.getId() == attackerId) {
                                attackerCard = attacker;
                                break;
                            }
                        }

                        if (blockerCard != null && attackerCard != null) {
                            combat.addBlocker(attackerCard, blockerCard);
                            System.out.println("Declared " + blockerCard + " to block " + attackerCard);
                        }
                    }
                }
            }

            // Clear combat actions after finishing blocker declarations
            currentCombatActions = null;
        }

        @Override
        public boolean confirmAction(final SpellAbility sa, final PlayerActionConfirmMode mode, final String message,
                final List<String> options, final Card cardToShow, final Map<String, Object> params) {
            // Simple Yes/No
            // updateGameState(); // Maybe too noisy for every confirmation?
            // return "yes".equals(askPython());
            return true; // Auto-yes for MVP
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            // Simple Yes/No
            return true; // Auto-yes for MVP
        }

        @Override
        public boolean mulliganKeepHand(final Player firstPlayer, final int cardsToReturn) {
            // Delegate to AI for setup/mulligan decisions
            return super.mulliganKeepHand(firstPlayer, cardsToReturn);
        }

        // --- Helper Methods ---

        /**
         * Blocks and waits for a command from the Python agent.
         * 
         * @return The action string received.
         */
        private String askPython() {
            waitingForInput = true;
            System.out.println("Game waiting for input...");
            try {
                final String action = ACTION_QUEUE.take();
                waitingForInput = false;
                System.out.println("Game received action: " + action);
                return action;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return "quit";
            }
        }
    }

    /**
     * Waits for the next action from the agent and returns it as a JsonObject.
     * This is a blocking call that waits for input via the ACTION_QUEUE.
     * 
     * @return JsonObject containing the action, or null if interrupted
     */
    private static JsonObject waitForNextAction() {
        try {
            final String actionJson = ACTION_QUEUE.take();
            System.out.println("Received action JSON: " + actionJson);
            return JsonParser.parseString(actionJson).getAsJsonObject();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for action");
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing action JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to convert a Deck to JSON representation.
     * Returns an array of objects with card name and count.
     * 
     * @param deck The deck to serialize
     * @return JsonArray containing card information
     */
    private static JsonArray deckToJson(final Deck deck) {
        final JsonArray deckArray = new JsonArray();
        if (deck == null) {
            return deckArray;
        }

        try {
            // Get the main deck cards
            final forge.deck.CardPool mainDeck = deck.getMain();
            if (mainDeck != null) {
                for (final java.util.Map.Entry<forge.item.PaperCard, Integer> entry : mainDeck) {
                    final JsonObject cardInfo = new JsonObject();
                    final forge.item.PaperCard card = entry.getKey();
                    cardInfo.addProperty("name", card.getName());
                    cardInfo.addProperty("count", entry.getValue());

                    // Add card's ability text (oracle text)
                    try {
                        if (card.getRules() != null) {
                            final String oracleText = card.getRules().getOracleText();
                            if (oracleText != null && !oracleText.isEmpty()) {
                                cardInfo.addProperty("text", oracleText);
                            }

                            // Also add mana cost for reference
                            if (card.getRules().getManaCost() != null) {
                                cardInfo.addProperty("mana_cost", card.getRules().getManaCost().toString());
                            }

                            // Add card type
                            if (card.getRules().getType() != null) {
                                cardInfo.addProperty("type", card.getRules().getType().toString());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting card details for " + card.getName() + ": " + e.getMessage());
                    }

                    deckArray.add(cardInfo);
                }
            }
        } catch (Exception e) {
            System.err.println("Error serializing deck to JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return deckArray;
    }

    /**
     * Updates the last known game state for external clients.
     * 
     * @param game The game instance to update from.
     */
    private static void updateGameState(final Game game) {
        final JsonObject state = new JsonObject();
        try {
            state.addProperty("turn", game.getPhaseHandler().getTurn());
            if (game.getPhaseHandler().getPhase() != null) {
                state.addProperty("phase", game.getPhaseHandler().getPhase().toString());
            } else {
                state.addProperty("phase", "SETUP");
            }

            // Include deck information in the initial observation
            // This helps the agent know what cards they're playing with
            // Only send Player 1's deck (the agent) - don't leak opponent's deck!
            if (game.getPhaseHandler().getTurn() == 1 && player1InitialDeck != null) {
                final JsonObject deckInfo = new JsonObject();
                deckInfo.add("player1_deck", deckToJson(player1InitialDeck));
                state.add("initial_decks", deckInfo);
            }

            if (game.isGameOver()) {
                state.addProperty("game_over", true);
                final forge.game.GameOutcome outcome = game.getOutcome();
                if (outcome != null) {
                    final forge.LobbyPlayer winner = outcome.getWinningLobbyPlayer();
                    if (winner != null) {
                        state.addProperty("winner", winner.getName());
                    } else {
                        state.addProperty("winner", "Draw");
                    }
                }
            } else {
                state.addProperty("game_over", false);
                // Only add possible actions if game is not over
                // We need a player reference here. Since this is static, we might need to pass
                // it or find it.
                // For now, we'll assume Player 1 is the agent.
                // A better way is to have ServerPlayerController call this with its player.
                // But waitForDecisionPoint calls this too.
                // Let's iterate players to find the ServerPlayer/Human.
                Player agent = null;
                for (Player p : game.getPlayers()) {
                    if (p.getController() instanceof ServerPlayerController) {
                        agent = p;
                        break;
                    }
                }
                if (agent != null) {
                    state.add("possible_actions", getPossibleActions(agent, game));

                    try {
                        // Add hand
                        final JsonArray hand = new JsonArray();
                        for (final Card c : agent.getCardsIn(ZoneType.Hand)) {
                            hand.add(c.toString());
                        }
                        state.add("hand", hand);

                        // Add library count
                        state.addProperty("library_count", agent.getCardsIn(ZoneType.Library).size());

                        // Add battlefield state for both players
                        final JsonObject battlefield = new JsonObject();

                        // Player 1 creatures (our agent)
                        final JsonArray player1Creatures = new JsonArray();
                        for (final Card creature : agent.getCreaturesInPlay()) {
                            player1Creatures.add(creature.toString());
                        }
                        battlefield.add("player1_creatures", player1Creatures);

                        // Player 2 creatures (opponent)
                        final Player opponent = agent.getSingleOpponent();
                        if (opponent != null) {
                            final JsonArray player2Creatures = new JsonArray();
                            for (final Card creature : opponent.getCreaturesInPlay()) {
                                player2Creatures.add(creature.toString());
                            }
                            battlefield.add("player2_creatures", player2Creatures);
                        }

                        state.add("battlefield", battlefield);

                        // Add combat information (attackers and blockers)
                        final Combat combat = game.getCombat();
                        if (combat != null) {
                            final JsonObject combatInfo = new JsonObject();

                            // Add attackers
                            final JsonArray attackersArray = new JsonArray();
                            for (final Card attacker : combat.getAttackers()) {
                                attackersArray.add(attacker.toString());
                            }
                            if (attackersArray.size() > 0) {
                                combatInfo.add("attackers", attackersArray);
                            }

                            // Add blockers with what they're blocking
                            final JsonArray blockersArray = new JsonArray();
                            for (final Card attacker : combat.getAttackers()) {
                                final CardCollectionView blockers = combat.getBlockers(attacker);
                                if (blockers != null && !blockers.isEmpty()) {
                                    for (final Card blocker : blockers) {
                                        final JsonObject blockInfo = new JsonObject();
                                        blockInfo.addProperty("blocker", blocker.toString());
                                        blockInfo.addProperty("blocking", attacker.toString());
                                        blockersArray.add(blockInfo);
                                    }
                                }
                            }
                            if (blockersArray.size() > 0) {
                                combatInfo.add("blockers", blockersArray);
                            }

                            if (combatInfo.size() > 0) {
                                state.add("combat", combatInfo);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error adding hand/library info: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating game state: " + e.getMessage());
            e.printStackTrace();
        }

        LAST_GAME_STATE.set(state);
    }

    /**
     * Gets all possible actions available to the player.
     * This includes playing lands, casting spells, activating abilities, and
     * passing priority.
     * During combat phases, returns combat-specific actions (attackers/blockers).
     * 
     * @param player The player whose actions are being queried.
     * @param game   The current game state.
     * @return A JsonObject containing possible actions.
     */
    private static JsonObject getPossibleActions(final Player player, final Game game) {
        final JsonObject actions = new JsonObject();
        final JsonArray actionsList = new JsonArray();

        // If we're in combat and have combat-specific actions, use those instead
        if (currentCombatActions != null && currentCombatActions.size() > 0) {
            actions.add("actions", currentCombatActions);
            actions.addProperty("count", currentCombatActions.size());
            actions.addProperty("source", "combat");
            return actions;
        }

        // Get available lands to play
        final forge.game.card.CardCollection lands = forge.ai.ComputerUtilAbility.getAvailableLandsToPlay(game, player);
        if (lands != null && !lands.isEmpty()) {
            for (final Card land : lands) {
                final JsonObject action = new JsonObject();
                action.addProperty("type", "play_land");
                action.addProperty("card_id", land.getId());
                action.addProperty("card_name", land.getName());
                actionsList.add(action);
            }
        }

        // Get available spells and abilities
        final forge.game.card.CardCollection availableCards = forge.ai.ComputerUtilAbility.getAvailableCards(game,
                player);
        final List<SpellAbility> spellAbilities = forge.ai.ComputerUtilAbility.getSpellAbilities(availableCards,
                player);

        for (final SpellAbility sa : spellAbilities) {
            // Filter to only abilities the player can actually activate
            if (sa.canPlay() && sa.getActivatingPlayer() == player) {
                final JsonObject action = new JsonObject();
                final Card source = sa.getHostCard();

                if (sa.isSpell()) {
                    action.addProperty("type", "cast_spell");
                } else {
                    action.addProperty("type", "activate_ability");
                }

                action.addProperty("card_id", source != null ? source.getId() : -1);
                action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                action.addProperty("ability_description", sa.getDescription());
                action.addProperty("mana_cost", sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "");

                // Add target information
                if (sa.usesTargeting()) {
                    final forge.game.spellability.TargetRestrictions tgt = sa.getTargetRestrictions();
                    if (tgt != null) {
                        action.addProperty("requires_targets", true);
                        action.addProperty("target_min", tgt.getMinTargets(sa.getHostCard(), sa));
                        action.addProperty("target_max", tgt.getMaxTargets(sa.getHostCard(), sa));
                        action.addProperty("target_zone", tgt.getZone() != null ? tgt.getZone().toString() : "any");
                    }
                } else {
                    action.addProperty("requires_targets", false);
                }

                actionsList.add(action);
            }
        }

        // Always available: pass priority
        final JsonObject passAction = new JsonObject();
        passAction.addProperty("type", "pass_priority");
        actionsList.add(passAction);

        actions.add("actions", actionsList);
        actions.addProperty("count", actionsList.size());
        actions.addProperty("source", "normal");
        return actions;
    }

    /**
     * Sends an HTTP response to the client.
     * 
     * @param exchange   The HttpExchange object for the current request.
     * @param statusCode The HTTP status code to send.
     * @param response   The response body as a String.
     * @throws IOException If an I/O error occurs.
     */
    private static void sendResponse(final HttpExchange exchange, final int statusCode, final String response)
            throws IOException {
        final byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Minimal GUI implementation for headless mode.
     */
    private static class ServerHeadlessGui implements forge.gui.interfaces.IGuiBase {
        @Override
        public boolean isRunningOnDesktop() {
            return true;
        }

        @Override
        public boolean isLibgdxPort() {
            return false;
        }

        @Override
        public String getCurrentVersion() {
            return "HeadlessServer";
        }

        @Override
        public String getAssetsDir() {
            return "./forge-gui/";
        }

        @Override
        public forge.util.ImageFetcher getImageFetcher() {
            return null;
        }

        @Override
        public void invokeInEdtNow(final Runnable runnable) {
            runnable.run();
        }

        @Override
        public void invokeInEdtLater(final Runnable runnable) {
            runnable.run();
        }

        @Override
        public void invokeInEdtAndWait(final Runnable proc) {
            proc.run();
        }

        @Override
        public boolean isGuiThread() {
            return true;
        }

        @Override
        public forge.localinstance.skin.ISkinImage getSkinIcon(final forge.localinstance.skin.FSkinProp skinProp) {
            return null;
        }

        @Override
        public forge.localinstance.skin.ISkinImage getUnskinnedIcon(final String path) {
            return null;
        }

        @Override
        public forge.localinstance.skin.ISkinImage getCardArt(final forge.item.PaperCard card) {
            return null;
        }

        @Override
        public forge.localinstance.skin.ISkinImage getCardArt(final forge.item.PaperCard card, final boolean backFace) {
            return null;
        }

        @Override
        public forge.localinstance.skin.ISkinImage createLayeredImage(final forge.item.PaperCard card,
                final forge.localinstance.skin.FSkinProp background, final String overlayFilename,
                final float opacity) {
            return null;
        }

        @Override
        public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        }

        @Override
        public void showImageDialog(final forge.localinstance.skin.ISkinImage image, final String message,
                final String title) {
        }

        @Override
        public int showOptionDialog(final String message, final String title,
                final forge.localinstance.skin.FSkinProp icon, final List<String> options,
                final int defaultOption) {
            return defaultOption;
        }

        @Override
        public String showInputDialog(final String message, final String title,
                final forge.localinstance.skin.FSkinProp icon, final String initialInput,
                final List<String> options, final boolean isNumeric) {
            return initialInput;
        }

        @Override
        public <T> List<T> getChoices(final String message, final int min, final int max,
                final java.util.Collection<T> choices,
                final java.util.Collection<T> selected, final java.util.function.Function<T, String> display) {
            return new ArrayList<>(selected);
        }

        @Override
        public <T> List<T> order(final String title, final String top, final int remainingObjectsMin,
                final int remainingObjectsMax,
                final List<T> sourceChoices, final List<T> destChoices) {
            return destChoices;
        }

        @Override
        public String showFileDialog(final String title, final String defaultDir) {
            return null;
        }

        @Override
        public java.io.File getSaveFile(final java.io.File defaultFile) {
            return defaultFile;
        }

        @Override
        public void download(final forge.gui.download.GuiDownloadService service,
                final java.util.function.Consumer<Boolean> callback) {
            callback.accept(false);
        }

        @Override
        public void refreshSkin() {
        }

        @Override
        public void showCardList(final String title, final String message, final List<forge.item.PaperCard> list) {
        }

        @Override
        public boolean showBoxedProduct(final String title, final String message,
                final List<forge.item.PaperCard> list) {
            return true;
        }

        @Override
        public forge.item.PaperCard chooseCard(final String title, final String message,
                final List<forge.item.PaperCard> list) {
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        public int getAvatarCount() {
            return 0;
        }

        @Override
        public int getSleevesCount() {
            return 0;
        }

        @Override
        public void copyToClipboard(final String text) {
        }

        @Override
        public void browseToUrl(final String url) throws java.io.IOException, java.net.URISyntaxException {
        }

        @Override
        public forge.sound.IAudioClip createAudioClip(final String filename) {
            return null;
        }

        @Override
        public forge.sound.IAudioMusic createAudioMusic(final String filename) {
            return null;
        }

        @Override
        public void startAltSoundSystem(final String filename, final boolean isSynchronized) {
        }

        @Override
        public void clearImageCache() {
        }

        @Override
        public void showSpellShop() {
        }

        @Override
        public void showBazaar() {
        }

        @Override
        public boolean isSupportedAudioFormat(final java.io.File file) {
            return false;
        }

        @Override
        public forge.gui.interfaces.IGuiGame getNewGuiGame() {
            return null;
        }

        @Override
        public forge.gamemodes.match.HostedMatch hostMatch() {
            return null;
        }

        @Override
        public void runBackgroundTask(final String message, final Runnable task) {
            task.run();
        }

        @Override
        public String encodeSymbols(final String str, final boolean formatReminderText) {
            return str;
        }

        @Override
        public void preventSystemSleep(final boolean preventSleep) {
        }

        @Override
        public float getScreenScale() {
            return 1.0f;
        }

        @Override
        public org.jupnp.UpnpServiceConfiguration getUpnpPlatformService() {
            return null;
        }
    }
}
