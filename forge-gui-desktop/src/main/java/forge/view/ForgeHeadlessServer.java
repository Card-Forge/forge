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

import forge.game.combat.Combat;
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
    private static final BlockingQueue<String> ACTION_QUEUE = new LinkedBlockingQueue<>();
    private static final AtomicReference<JsonObject> LAST_GAME_STATE = new AtomicReference<>(new JsonObject());
    private static volatile boolean waitingForInput = false;

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
        // Stop existing game if any
        if (currentGame != null) {
            // Logic to stop game?
            currentGame = null;
        }
        ACTION_QUEUE.clear();

        // Start a new game thread
        new Thread(() -> {
            System.out.println("Starting new game thread...");

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

            final List<RegisteredPlayer> players = new ArrayList<>();
            players.add(new RegisteredPlayer(deck1).setPlayer(new ServerPlayer("Player 1")));
            players.add(new RegisteredPlayer(deck2).setPlayer(new forge.ai.LobbyPlayerAi("AI Player 2", null)));

            final GameRules rules = new GameRules(GameType.Constructed);
            final Match match = new Match(rules, players, "Server Match");
            currentGame = match.createGame();

            match.startGame(currentGame);
        }).start();
    }

    private static Deck generateRandomStandardDeck() {
        try {
            int count = Aggregates.randomInt(1, 3);
            List<String> colors = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                colors.add("Random " + i);
            }
            return DeckgenUtil.buildColorDeck(colors, FModel.getFormats().getStandard().getFilterPrinted(), true);
        } catch (Exception e) {
            System.err.println("Error generating random deck: " + e.getMessage());
            e.printStackTrace();
            // Fallback to simple deck
            final Deck deck = new Deck("Fallback Deck");
            for (int i = 0; i < DECK_SIZE_SMALL; i++) {
                deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Mountain"));
            }
            for (int i = 0; i < DECK_SIZE_SMALL; i++) {
                deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Shock"));
            }
            return deck;
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
            final Player ai = new Player(getName(), game, id);
            ai.setFirstController(new ServerPlayerController(game, ai, this));
            return ai;
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

        @Override
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
            updateGameState(getGame());
            // In a real impl, we would send the list of possible attackers
            // and wait for a list of indices to attack with.
            // For MVP, we'll just ask Python and if it says "attack_all", we attack with
            // all.
            // If it says "skip", we skip.
            final String action = askPython();
            if ("attack_all".equals(action)) {
                // combat.addAttacker(card, defender);
            }
            // For now, fall back to AI to keep the game moving if not fully implemented
            super.declareAttackers(attacker, combat);
        }

        @Override
        public void declareBlockers(final Player defender, final Combat combat) {
            updateGameState(getGame());
            final String action = askPython();
            // Fallback to AI
            super.declareBlockers(defender, combat);
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
     * Duplicates ForgeHeadless.getPossibleActions (simplified) to provide
     * available actions to the client.
     * 
     * @param player The player whose actions are being queried.
     * @param game   The current game state.
     * @return A JsonObject containing possible actions.
     */
    private static JsonObject getPossibleActions(final Player player, final Game game) {
        final JsonObject actions = new JsonObject();
        final JsonArray actionsList = new JsonArray();

        // Always available: pass priority
        final JsonObject passAction = new JsonObject();
        passAction.addProperty("type", "pass_priority");
        actionsList.add(passAction);

        actions.add("actions", actionsList);
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
                final List<String> inputOptions, final boolean isNumeric) {
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
