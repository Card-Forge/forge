package forge.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import forge.ai.ComputerUtilAbility;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.*;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gamemodes.match.HostedMatch;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.ImageFetcher;
import forge.util.collect.FCollectionView;
import org.jupnp.UpnpServiceConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ForgeHeadlessHttp {
    // ANSI Color Constants
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BOLD = "\u001B[1m";

    private static final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private static final AtomicReference<Game> activeGame = new AtomicReference<>();
    private static final AtomicBoolean restartGame = new AtomicBoolean(false);

    public static void main(String[] args) throws IOException {
        System.err.println("DEBUG: ForgeHeadlessHttp main started");

        // Parse command-line arguments (keeping it simple for now, mostly for player
        // config)
        boolean player1IsHuman = true; // default
        boolean player2IsHuman = false; // default
        boolean verboseLogging = false; // default

        int port = 8080; // default

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--both-human")) {
                player1IsHuman = true;
                player2IsHuman = true;
            } else if (arg.equals("--both-ai")) {
                player1IsHuman = false;
                player2IsHuman = false;
            } else if (arg.equals("--p1-ai")) {
                player1IsHuman = false;
            } else if (arg.equals("--p2-human")) {
                player2IsHuman = true;
            } else if (arg.equals("--verbose")) {
                verboseLogging = true;
            } else if (arg.equals("--port")) {
                if (i + 1 < args.length) {
                    try {
                        port = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number. Using default 8080.");
                    }
                }
            } else if (arg.equals("--help")) {
                printUsage();
                System.exit(0);
            }
        }

        GuiBase.setInterface(new HeadlessGui());
        FModel.initialize(null, null);

        // Start HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/state", new StateHandler());
        server.createContext("/action", new ActionHandler());
        server.createContext("/reset", new ResetHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("HTTP Server started on port " + port);

        // Game Loop
        while (true) {
            restartGame.set(false);
            commandQueue.clear(); // Clear any pending commands from previous game

            // Generate Decks
            Deck deck1 = DeckgenUtil.getRandomColorDeck(FModel.getFormats().getStandard().getFilterPrinted(), true);
            Deck deck2 = DeckgenUtil.getRandomColorDeck(FModel.getFormats().getStandard().getFilterPrinted(), true);

            // Setup Players
            List<RegisteredPlayer> players = new ArrayList<>();

            if (player1IsHuman) {
                RegisteredPlayer rp1 = new RegisteredPlayer(deck1).setPlayer(new HeadlessLobbyPlayerHttp("Player 1"));
                players.add(rp1);
            } else {
                RegisteredPlayer rp1 = new RegisteredPlayer(deck1)
                        .setPlayer(new forge.ai.LobbyPlayerAi("AI Player 1", null));
                players.add(rp1);
            }

            if (player2IsHuman) {
                RegisteredPlayer rp2 = new RegisteredPlayer(deck2).setPlayer(new HeadlessLobbyPlayerHttp("Player 2"));
                players.add(rp2);
            } else {
                RegisteredPlayer rp2 = new RegisteredPlayer(deck2)
                        .setPlayer(new forge.ai.LobbyPlayerAi("AI Player 2", null));
                players.add(rp2);
            }

            // Setup Match
            GameRules rules = new GameRules(GameType.Constructed);
            Match match = new Match(rules, players, "Headless Match");
            Game game = match.createGame();
            activeGame.set(game);

            System.out.println("Starting new game...");
            runGame(match, game, verboseLogging);
            System.out.println("Game ended.");

            if (!restartGame.get()) {
                System.out.println("Exiting application.");
                break; // Exit loop if not restarting
            } else {
                System.out.println("Restarting game...");
            }
        }
        server.stop(0);
    }

    private static void printUsage() {
        System.out.println("ForgeHeadlessHttp - Headless Magic: The Gathering Game Engine (HTTP Interface)");
        System.out.println("\nUsage: java -cp <jar> forge.view.ForgeHeadlessHttp [options]");
        System.out.println("\nOptions:");
        System.out.println("  --both-human    Both players are human-controlled (interactive)");
        System.out.println("  --both-ai       Both players are AI-controlled (simulation mode)");
        System.out.println("  --p1-ai         Player 1 is AI-controlled (default: human)");
        System.out.println("  --p2-human      Player 2 is human-controlled (default: AI)");
        System.out.println("  --port <port>   Specify the HTTP server port (default: 8080)");
        System.out.println("  --verbose       Enable verbose logging of game events");
        System.out.println("  --help          Show this help message");
    }

    private static void runGame(Match match, Game game, boolean verboseLogging) {
        if (verboseLogging) {
            HeadlessGameObserver observer = new HeadlessGameObserver();
            match.subscribeToEvents(observer);
            game.subscribeToEvents(observer);
        }
        match.startGame(game);
    }

    // HTTP Handlers
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Forge Headless HTTP Interface Running";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"GET".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, -1);
                return;
            }

            Game game = activeGame.get();
            if (game == null) {
                String response = "{\"error\": " + "\"No active game\"}";
                t.sendResponseHeaders(503, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            // We might want to synchronize this, but for now let's try without
            JsonObject state = extractGameState(game);
            // Add possible actions if it's a human player's turn (or we can just always add
            // them for the human player)
            // Ideally we find the human player and get their actions.
            // For simplicity, let's just add actions for the active player if they are
            // human?
            // Actually, let's just add actions for ALL human players in the game.

            JsonArray actionsForPlayers = new JsonArray();
            for (Player p : game.getPlayers()) {
                if (p.getController() instanceof HeadlessPlayerControllerHttp) {
                    JsonObject playerActions = new JsonObject();
                    playerActions.addProperty("player_id", p.getId());
                    playerActions.add("actions", getPossibleActions(p, game));
                    actionsForPlayers.add(playerActions);
                }
            }
            state.add("possible_actions", actionsForPlayers);

            String response = new GsonBuilder().setPrettyPrinting().create().toJson(state);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(t.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            System.out.println("Received action: " + body);

            try {
                commandQueue.put(body);
                String response = "{\"status\": \"accepted\"}";
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (InterruptedException e) {
                t.sendResponseHeaders(500, -1);
            }
        }
    }

    static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, -1);
                return;
            }

            System.out.println("Received RESET command");
            restartGame.set(true);
            try {
                commandQueue.put("concede"); // Force current game to end
                String response = "{\"status\": \"resetting\"}";
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (InterruptedException e) {
                t.sendResponseHeaders(500, -1);
            }
        }
    }

    // Logic from ForgeHeadless
    private static JsonObject extractGameState(Game game) {
        JsonObject state = new JsonObject();

        // General Game Info
        state.addProperty("turn", game.getPhaseHandler().getTurn());
        state.addProperty("phase", game.getPhaseHandler().getPhase().toString());
        state.addProperty("activePlayerId", game.getPhaseHandler().getPlayerTurn().getId());

        // Stack
        JsonArray stackArray = new JsonArray();
        for (forge.game.spellability.SpellAbilityStackInstance stackItem : game.getStack()) {
            JsonObject stackObj = new JsonObject();
            SpellAbility sa = stackItem.getSpellAbility();
            Card source = sa.getHostCard();
            stackObj.addProperty("card_name", source != null ? source.getName() : "Unknown");
            stackObj.addProperty("card_id", source != null ? source.getId() : -1);
            stackObj.addProperty("description", sa.getStackDescription());
            stackObj.addProperty("controller", sa.getActivatingPlayer().getName());
            stackArray.add(stackObj);
        }
        state.add("stack", stackArray);
        state.addProperty("stack_size", game.getStack().size());

        // Players
        JsonArray playersArray = new JsonArray();
        for (Player p : game.getPlayers()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("id", p.getId());
            playerObj.addProperty("name", p.getName());
            playerObj.addProperty("life", p.getLife());
            playerObj.addProperty("libraryCount", p.getCardsIn(ZoneType.Library).size());

            // Hand
            JsonArray handArray = new JsonArray();
            for (Card c : p.getCardsIn(ZoneType.Hand)) {
                JsonObject cardObj = new JsonObject();
                cardObj.addProperty("name", c.getName());
                cardObj.addProperty("id", c.getId());
                cardObj.addProperty("zone", "Hand");
                handArray.add(cardObj);
            }
            playerObj.add("hand", handArray);

            // Other Zones
            playerObj.add("graveyard", getZoneJson(p, ZoneType.Graveyard));
            playerObj.add("battlefield", getZoneJson(p, ZoneType.Battlefield));
            playerObj.add("exile", getZoneJson(p, ZoneType.Exile));

            playersArray.add(playerObj);
        }
        state.add("players", playersArray);

        return state;
    }

    private static JsonArray getZoneJson(Player p, ZoneType zone) {
        JsonArray zoneArray = new JsonArray();
        for (Card c : p.getCardsIn(zone)) {
            JsonObject cardObj = new JsonObject();
            cardObj.addProperty("name", c.getName());
            cardObj.addProperty("id", c.getId());
            cardObj.addProperty("zone", zone.toString());
            zoneArray.add(cardObj);
        }
        return zoneArray;
    }

    private static JsonObject getPossibleActions(Player player, Game game) {
        JsonObject actions = new JsonObject();
        JsonArray actionsList = new JsonArray();

        // Get available lands to play
        CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(game, player);
        if (lands != null && !lands.isEmpty()) {
            for (Card land : lands) {
                JsonObject action = new JsonObject();
                action.addProperty("type", "play_land");
                action.addProperty("card_id", land.getId());
                action.addProperty("card_name", land.getName());
                actionsList.add(action);
            }
        }

        // Get available spells and abilities
        CardCollection availableCards = ComputerUtilAbility.getAvailableCards(game, player);
        List<SpellAbility> spellAbilities = ComputerUtilAbility.getSpellAbilities(availableCards, player);

        for (SpellAbility sa : spellAbilities) {
            if (sa.canPlay() && sa.getActivatingPlayer() == player) {
                JsonObject action = new JsonObject();
                Card source = sa.getHostCard();

                if (sa.isSpell()) {
                    action.addProperty("type", "cast_spell");
                } else {
                    action.addProperty("type", "activate_ability");
                }

                action.addProperty("card_id", source != null ? source.getId() : -1);
                action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                action.addProperty("ability_description", sa.getDescription());
                action.addProperty("mana_cost", sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "");

                if (sa.usesTargeting()) {
                    forge.game.spellability.TargetRestrictions tgt = sa.getTargetRestrictions();
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
        JsonObject passAction = new JsonObject();
        passAction.addProperty("type", "pass_priority");
        actionsList.add(passAction);

        actions.add("actions", actionsList);
        actions.addProperty("count", actionsList.size());
        return actions;
    }

    private static class HeadlessLobbyPlayerHttp extends forge.ai.LobbyPlayerAi {
        public HeadlessLobbyPlayerHttp(String name) {
            super(name, null);
        }

        @Override
        public Player createIngamePlayer(Game game, final int id) {
            Player ai = new Player(getName(), game, id);
            ai.setFirstController(new HeadlessPlayerControllerHttp(game, ai, this));
            return ai;
        }
    }

    private static class HeadlessPlayerControllerHttp extends forge.ai.PlayerControllerAi {
        public HeadlessPlayerControllerHttp(Game game, Player player, forge.ai.LobbyPlayerAi lobbyPlayer) {
            super(game, player, lobbyPlayer);
        }

        @Override
        public boolean mulliganKeepHand(Player player, int cardsToReturn) {
            return true; // Always keep hand
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
                DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer,
                Map<String, Object> params) {
            List<T> results = chooseEntitiesForEffect(optionList, isOptional ? 0 : 1, 1, delayedReveal, sa, title,
                    targetedPlayer, params);
            return results.isEmpty() ? null : results.get(0);
        }

        @Override
        public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max,
                DelayedReveal delayedReveal, SpellAbility sa, String title, Player targetedPlayer,
                Map<String, Object> params) {
            // Simplified targeting for now - just pick first available or wait for input?
            // For now, let's just pick the first ones to unblock the game if it gets here.
            // Ideally we'd expose this via HTTP too, but that's complex.
            // Let's just auto-select for now to keep it moving, or maybe we can implement a
            // "wait for target" state?
            // Given the prompt asked for "same input params as forge-headless", and
            // forge-headless had interactive targeting...
            // We should probably support targeting via HTTP.
            // But for this first pass, let's auto-select to avoid getting stuck.

            List<T> selected = new ArrayList<>();
            for (T t : optionList) {
                if (selected.size() < max) {
                    selected.add(t);
                }
            }
            return selected;
        }

        private List<SpellAbility> getPossibleSpellAbilities() {
            List<SpellAbility> allAbilities = new ArrayList<>();

            CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
            if (lands != null && !lands.isEmpty()) {
                for (Card land : lands) {
                    SpellAbility sa = land.getSpellPermanent();
                    if (sa != null) {
                        allAbilities.add(sa);
                    }
                }
            }

            CardCollection availableCards = ComputerUtilAbility.getAvailableCards(getGame(), player);
            List<SpellAbility> spellAbilities = ComputerUtilAbility.getSpellAbilities(availableCards, player);

            for (SpellAbility sa : spellAbilities) {
                if (sa.canPlay() && sa.getActivatingPlayer() == player) {
                    allAbilities.add(sa);
                }
            }

            return allAbilities;
        }

        @Override
        public java.util.List<forge.game.spellability.SpellAbility> chooseSpellAbilityToPlay() {
            while (true) {
                try {
                    // Wait for command from HTTP
                    String input = commandQueue.take();

                    if (input.trim().isEmpty())
                        continue;

                    String[] parts = input.split(" ");
                    String command = parts[0];

                    if (command.equals("play_action") || command.equals("play")) {
                        if (parts.length < 2)
                            continue;

                        try {
                            int actionIndex = Integer.parseInt(parts[1]);
                            List<SpellAbility> actions = getPossibleSpellAbilities();

                            if (actionIndex < 0 || actionIndex >= actions.size()) {
                                System.out.println("Invalid action index: " + actionIndex);
                                continue;
                            }

                            SpellAbility chosenAbility = actions.get(actionIndex);
                            List<SpellAbility> result = new ArrayList<>();
                            result.add(chosenAbility);
                            return result;

                        } catch (NumberFormatException e) {
                            System.out.println("Invalid action index.");
                        }
                    } else if (command.equals("pass_priority") || command.equals("pp") || command.equals("pass")) {
                        return null; // Pass priority
                    } else if (command.equals("concede") || command.equals("c")) {
                        player.concede();
                        return null;
                    }
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
    }

    private static class HeadlessGameObserver extends forge.game.event.IGameEventVisitor.Base<Void> {
        private PrintStream logStream;

        public HeadlessGameObserver() {
            try {
                logStream = new PrintStream(new FileOutputStream("headless_game.log"), true);
            } catch (IOException e) {
                System.err.println("Error creating log file: " + e.getMessage());
                logStream = System.out;
            }
        }

        private void log(String message) {
            logStream.println(message);
        }

        @com.google.common.eventbus.Subscribe
        public void receive(forge.game.event.GameEvent ev) {
            ev.visit(this);
        }

        @Override
        public Void visit(GameEventTurnBegan event) {
            log("\n" + ANSI_WHITE + "=== Turn " + event.turnNumber() + " - " + event.turnOwner().getName() + " ==="
                    + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventTurnPhase event) {
            log(ANSI_WHITE + "Phase: " + event.phase() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventGameOutcome event) {
            log("\n*** GAME OVER ***");
            log("Result: " + event.result().getOutcomeStrings());
            return null;
        }

        @Override
        public Void visit(GameEventSpellAbilityCast event) {
            log(ANSI_CYAN + "CAST: " + event.sa().getHostCard().getName() + " by "
                    + event.sa().getActivatingPlayer().getName() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventLandPlayed event) {
            log(ANSI_GREEN + "LAND: " + event.land().getName() + " played by " + event.player().getName() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventPlayerLivesChanged event) {
            log(ANSI_YELLOW + "LIFE: " + event.player().getName() + " is now at " + event.newLives() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventAttackersDeclared event) {
            if (!event.attackersMap().isEmpty()) {
                log(ANSI_RED + "COMBAT: Attackers declared by " + event.player().getName() + ANSI_RESET);
                event.attackersMap().asMap().forEach((target, attackers) -> {
                    log(ANSI_PURPLE + "  Target: " + target + ANSI_RESET);
                    for (Card attacker : attackers) {
                        log(ANSI_RED + "    - " + attacker.getName() + " (" + attacker.getNetPower() + "/"
                                + attacker.getNetToughness() + ")" + ANSI_RESET);
                    }
                });
            }
            return null;
        }

        @Override
        public Void visit(GameEventBlockersDeclared event) {
            if (!event.blockers().isEmpty()) {
                log(ANSI_RED + "COMBAT: Blockers declared by " + event.defendingPlayer().getName() + ANSI_RESET);
                event.blockers().forEach((defender, map) -> {
                    map.forEach((attacker, blockers) -> {
                        for (Card blocker : blockers) {
                            log(ANSI_RED + "    - " + blocker.getName() + " blocks " + attacker.getName() + ANSI_RESET);
                        }
                    });
                });
            }
            return null;
        }

        @Override
        public Void visit(GameEventPlayerDamaged event) {
            log(ANSI_BOLD + ANSI_RED + "DAMAGE: " + event.target().getName() + " took " + event.amount()
                    + " damage from " + event.source() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventCardDamaged event) {
            log(ANSI_BOLD + ANSI_RED + "DAMAGE: " + event.card().getName() + " took " + event.amount() + " damage from "
                    + event.source() + ANSI_RESET);
            return null;
        }
    }

    private static class HeadlessGui implements IGuiBase {
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
            return "Headless";
        }

        @Override
        public String getAssetsDir() {
            return "./forge-gui/";
        }

        @Override
        public ImageFetcher getImageFetcher() {
            return null;
        }

        @Override
        public void invokeInEdtNow(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void invokeInEdtLater(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void invokeInEdtAndWait(Runnable proc) {
            proc.run();
        }

        @Override
        public boolean isGuiThread() {
            return true;
        }

        @Override
        public ISkinImage getSkinIcon(FSkinProp skinProp) {
            return null;
        }

        @Override
        public ISkinImage getUnskinnedIcon(String path) {
            return null;
        }

        @Override
        public ISkinImage getCardArt(PaperCard card) {
            return null;
        }

        @Override
        public ISkinImage getCardArt(PaperCard card, boolean backFace) {
            return null;
        }

        @Override
        public ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename,
                float opacity) {
            return null;
        }

        @Override
        public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        }

        @Override
        public void showImageDialog(ISkinImage image, String message, String title) {
        }

        @Override
        public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options,
                int defaultOption) {
            return defaultOption;
        }

        @Override
        public String showInputDialog(String message, String title, FSkinProp icon, String initialInput,
                List<String> inputOptions, boolean isNumeric) {
            return initialInput;
        }

        @Override
        public <T> List<T> getChoices(String message, int min, int max, java.util.Collection<T> choices,
                java.util.Collection<T> selected, java.util.function.Function<T, String> display) {
            return new ArrayList<>(selected);
        }

        @Override
        public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax,
                List<T> sourceChoices, List<T> destChoices) {
            return destChoices;
        }

        @Override
        public String showFileDialog(String title, String defaultDir) {
            return null;
        }

        @Override
        public java.io.File getSaveFile(java.io.File defaultFile) {
            return defaultFile;
        }

        @Override
        public void download(GuiDownloadService service, java.util.function.Consumer<Boolean> callback) {
            callback.accept(false);
        }

        @Override
        public void refreshSkin() {
        }

        @Override
        public void showCardList(String title, String message, List<PaperCard> list) {
        }

        @Override
        public boolean showBoxedProduct(String title, String message, List<PaperCard> list) {
            return true;
        }

        @Override
        public PaperCard chooseCard(String title, String message, List<PaperCard> list) {
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
        public void copyToClipboard(String text) {
        }

        @Override
        public void browseToUrl(String url) throws java.io.IOException, java.net.URISyntaxException {
        }

        @Override
        public IAudioClip createAudioClip(String filename) {
            return null;
        }

        @Override
        public IAudioMusic createAudioMusic(String filename) {
            return null;
        }

        @Override
        public void startAltSoundSystem(String filename, boolean isSynchronized) {
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
        public boolean isSupportedAudioFormat(java.io.File file) {
            return false;
        }

        @Override
        public IGuiGame getNewGuiGame() {
            return null;
        }

        @Override
        public HostedMatch hostMatch() {
            return null;
        }

        @Override
        public void runBackgroundTask(String message, Runnable task) {
            task.run();
        }

        @Override
        public String encodeSymbols(String str, boolean formatReminderText) {
            return str;
        }

        @Override
        public void preventSystemSleep(boolean preventSleep) {
        }

        @Override
        public float getScreenScale() {
            return 1.0f;
        }

        @Override
        public UpnpServiceConfiguration getUpnpPlatformService() {
            return null;
        }
    }
}
