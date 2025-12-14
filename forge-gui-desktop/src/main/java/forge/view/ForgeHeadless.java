package forge.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileSection;
import forge.util.FileUtil;
import java.io.File;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.ai.AIAgentClient;

import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.util.ImageFetcher;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.item.PaperCard;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiGame;
import forge.gamemodes.match.HostedMatch;
import org.jupnp.UpnpServiceConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import forge.game.event.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.card.CardLists;
import forge.game.GameEntity;
import forge.util.collect.FCollectionView;
import forge.game.player.DelayedReveal;
import java.util.Map;
import java.util.UUID;

import forge.StaticData;

import forge.deck.DeckgenUtil;
import forge.util.Aggregates;

public class ForgeHeadless {
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

    // Server & State
    private static final int PORT = 8081;
    private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private static volatile Game currentGame = null;
    private static volatile String currentPromptType = "none"; // "action", "target", "none"
    private static volatile JsonObject currentPromptData = new JsonObject();

    // AI Agent Configuration
    private static volatile String aiAgentEndpoint = null;
    private static volatile String gameId = null;
    private static volatile AIAgentClient aiAgentClient = null;
    private static volatile boolean condensedLogging = false;
    private static volatile boolean useMyrDeck = false;
    private static final java.util.Set<String> knownCardNames = new java.util.HashSet<>();

    public static void main(String[] args) {
        System.err.println("DEBUG: ForgeHeadless main started");

        // Parse command-line arguments
        boolean player1IsHuman = true; // default
        boolean player2IsHuman = false; // default
        boolean verboseLogging = false; // default

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
            } else if (arg.equals("--condensed-log")) {
                condensedLogging = true;
            } else if (arg.equals("--use-myr")) {
                useMyrDeck = true;
            } else if (arg.equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (arg.equals("--ai-endpoint") && i + 1 < args.length) {
                aiAgentEndpoint = args[++i];
            } else if (arg.startsWith("--ai-endpoint=")) {
                aiAgentEndpoint = arg.substring("--ai-endpoint=".length());
            } else if (arg.equals("--game-id") && i + 1 < args.length) {
                gameId = args[++i];
            } else if (arg.startsWith("--game-id=")) {
                gameId = arg.substring("--game-id=".length());
            }
        }

        // Initialize AI Agent Client if endpoint is configured
        if (aiAgentEndpoint != null && !aiAgentEndpoint.isEmpty()) {
            aiAgentClient = new AIAgentClient(aiAgentEndpoint);
            System.out.println("AI Agent mode enabled. Endpoint: " + aiAgentEndpoint);
            if (gameId == null) {
                gameId = UUID.randomUUID().toString();
            }
            System.out.println("Game ID: " + gameId);
        }

        // Start HTTP Server (still needed for fallback and monitoring)
        startHttpServer();

        GuiBase.setInterface(new HeadlessGui());
        FModel.initialize(null, null);

        // Generate Decks
        Deck deck1 = null;
        if (useMyrDeck) {
            System.out.println("Forcing use of 'Myr of Mirrodin' deck for Player 1");
            deck1 = loadPreconstructedDeck("Myr of Mirrodin");
            if (deck1 == null) {
                System.err.println("WARNING: Could not load 'Myr of Mirrodin' deck. Falling back to random.");
            }
        }
        
        if (deck1 == null) {
            deck1 = loadRandomPrecon();
        }
        
        if (deck1 == null) {
            System.err.println("FATAL ERROR: Could not load any preconstructed decks from " + ForgeConstants.QUEST_PRECON_DIR);
            System.exit(1);
        }
        System.out.println("Loaded Deck 1: " + deck1.getName());

        Deck deck2 = loadRandomPrecon();
        if (deck2 == null) {
            System.err.println("FATAL ERROR: Could not load any preconstructed decks from " + ForgeConstants.QUEST_PRECON_DIR);
            System.exit(1);
        }
        System.out.println("Loaded Deck 2: " + deck2.getName());

        // Setup Players based on configuration
        List<RegisteredPlayer> players = new ArrayList<>();

        if (player1IsHuman) {
            RegisteredPlayer rp1 = new RegisteredPlayer(deck1).setPlayer(new HeadlessLobbyPlayer("Player 1"));
            // rp1.setStartingLife(1000); // Only for debugging
            players.add(rp1);
        } else {
            RegisteredPlayer rp1 = new RegisteredPlayer(deck1)
                    .setPlayer(new forge.ai.LobbyPlayerAi("AI Player 1", null));
            players.add(rp1);
        }

        if (player2IsHuman) {
            RegisteredPlayer rp2 = new RegisteredPlayer(deck2).setPlayer(new HeadlessLobbyPlayer("Player 2"));
            players.add(rp2);
        } else {
            RegisteredPlayer rp2 = new RegisteredPlayer(deck2)
                    .setPlayer(new forge.ai.LobbyPlayerAi("AI Player 2", null));
            players.add(rp2);
        }

        System.err.println("DEBUG: Player 1 - " + (player1IsHuman ? "Human" : "AI"));
        System.err.println("DEBUG: Player 2 - " + (player2IsHuman ? "Human" : "AI"));

        // Setup Match
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Headless Match");
        Game game = match.createGame();
        currentGame = game;

        runGame(match, game, player1IsHuman, player2IsHuman, verboseLogging);
    }

    private static void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // GET /state
            server.createContext("/state", exchange -> {
                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 204, "");
                    return;
                }
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                if (currentGame == null) {
                    sendResponse(exchange, 503, "Game not started");
                    return;
                }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String response = gson.toJson(extractGameState(currentGame));
                sendResponse(exchange, 200, response);
            });

            // GET /input
            server.createContext("/input", exchange -> {
                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 204, "");
                    return;
                }
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                JsonObject response = new JsonObject();
                response.addProperty("type", currentPromptType);
                response.add("data", currentPromptData);
                sendResponse(exchange, 200, response.toString());
            });

            // POST /action
            server.createContext("/action", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("index")) {
                        int index = json.get("index").getAsInt();
                        inputQueue.offer("play_action " + index);
                        sendResponse(exchange, 200, "Action queued");
                    } else {
                        sendResponse(exchange, 400, "Missing 'index' field");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON");
                }
            });

            // POST /target
            server.createContext("/target", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("index")) {
                        int index = json.get("index").getAsInt();
                        inputQueue.offer(String.valueOf(index));
                        sendResponse(exchange, 200, "Target selection queued");
                    } else {
                        sendResponse(exchange, 400, "Missing 'index' field");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON");
                }
            });

            // POST /control
            server.createContext("/control", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("command")) {
                        String cmd = json.get("command").getAsString();
                        inputQueue.offer(cmd);
                        sendResponse(exchange, 200, "Command queued");
                    } else {
                        sendResponse(exchange, 400, "Missing 'command' field");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON");
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("HTTP Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to start HTTP server. Exiting.");
            System.exit(1);
        }
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
                System.err.println("Generated deck is null or empty!");
                return null;
            }

            System.out.println("Successfully generated random deck with " + deck.getMain().countAll() + " cards");
            return deck;
        } catch (Exception e) {
            System.err.println("Error generating random deck: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static Deck loadPreconstructedDeck(String deckName) {
        if (deckName == null) return null;
        
        File dir = new File(ForgeConstants.QUEST_PRECON_DIR);
        if (!dir.exists() || !dir.isDirectory()) return null;
        
        // Simple search in top level
        File[] files = dir.listFiles((d, name) -> name.endsWith(".dck"));
        if (files != null) {
            for (File f : files) {
                if (f.getName().equalsIgnoreCase(deckName) || f.getName().equalsIgnoreCase(deckName + ".dck")) {
                    return DeckSerializer.fromSections(FileSection.parseSections(FileUtil.readFile(f)));
                }
            }
        }
        return null;
    }

    private static Deck loadRandomPrecon() {
        File dir = new File(ForgeConstants.QUEST_PRECON_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
             System.err.println("Precon directory not found: " + dir.getAbsolutePath());
             return null;
        }
        
        // Recursive search for all .dck files
        List<File> allDecks = new ArrayList<>();
        findDeckFiles(dir, allDecks);
        
        if (allDecks.isEmpty()) {
             System.err.println("No .dck files found in " + dir.getAbsolutePath());
             return null;
        }
        
        // Try up to 10 times to find a valid constructed deck (>= 60 cards)
        for (int i = 0; i < 10; i++) {
            File randomDeckFile = allDecks.get(Aggregates.randomInt(0, allDecks.size() - 1));
            try {
                Deck d = DeckSerializer.fromSections(FileSection.parseSections(FileUtil.readFile(randomDeckFile)));
                // Ensure deck is a valid constructed deck (approx 60 cards)
                // Exclude Draft/Sealed (40) and Commander (100)
                if (d != null && d.getMain().countAll() >= 60 && d.getMain().countAll() <= 80) {
                    d.setName(randomDeckFile.getName().replace(".dck", ""));
                    return d;
                }
            } catch (Exception e) {
                System.err.println("Error loading deck " + randomDeckFile.getName() + ": " + e.getMessage());
            }
        }
        
        // Fallback: just return the last attempted valid deck even if small, or null
        return null;
    }

    private static void findDeckFiles(File dir, List<File> results) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    findDeckFiles(f, results);
                } else if (f.getName().endsWith(".dck")) {
                    results.add(f);
                }
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        // Add CORS headers to allow browser access from file:// and other origins
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        }
    }

    private static void printUsage() {
        System.out.println("ForgeHeadless - Headless Magic: The Gathering Game Engine");
        System.out.println("\nUsage: java -cp <jar> forge.view.ForgeHeadless [options]");
        System.out.println("\nPlayer Options:");
        System.out.println("  --both-human    Both players are human-controlled (interactive)");
        System.out.println("  --both-ai       Both players are AI-controlled (simulation mode)");
        System.out.println("  --p1-ai         Player 1 is AI-controlled (default: human)");
        System.out.println("  --p2-human      Player 2 is human-controlled (default: AI)");
        System.out.println("\nAI Agent Options:");
        System.out.println("  --ai-endpoint <url>   URL of external AI agent for decision-making");
        System.out.println("  --game-id <id>        Unique game ID for tracking (auto-generated if not provided)");
        System.out.println("\nOther Options:");
        System.out.println("  --verbose       Enable verbose logging of game events");
        System.out.println("  --condensed-log Enable condensed logging (actions, state, decisions)");
        System.out.println("  --use-myr       Force Player 1 to use 'Myr of Mirrodin' deck");
        System.out.println("  --help          Show this help message");
        System.out.println("\nHTTP Server running on port " + PORT);
        System.out.println("\nAI Agent Mode:");
        System.out.println("  When --ai-endpoint is provided, the game will call out to the specified");
        System.out.println("  endpoint for all player decisions instead of waiting for HTTP input.");
        System.out.println("  The endpoint receives game state + action options and returns decisions.");
    }

    private static void initialize() {
        // FModel.initialize() is called in main
    }

    private static void runGame(Match match, Game game, boolean player1IsHuman, boolean player2IsHuman,
            boolean verboseLogging) {
        // Start Game
        if (verboseLogging) {
            HeadlessGameObserver observer = new HeadlessGameObserver();
            match.subscribeToEvents(observer);
            game.subscribeToEvents(observer);
        }
        match.startGame(game);
    }

    private static JsonObject getCardDefinition(String cardName) {
        JsonObject def = new JsonObject();
        PaperCard pc = StaticData.instance().getCommonCards().getCard(cardName);
        if (pc == null) {
            def.addProperty("name", cardName);
            def.addProperty("error", "Card not found in DB");
            return def;
        }

        def.addProperty("name", pc.getName());
        def.addProperty("mana_cost", pc.getRules().getManaCost().toString());
        def.addProperty("type", pc.getRules().getType().toString());
        def.addProperty("oracle_text", pc.getRules().getOracleText());
        
        if (pc.getRules().getType().isCreature()) {
            def.addProperty("power", pc.getRules().getPower().toString());
            def.addProperty("toughness", pc.getRules().getToughness().toString());
        }

        return def;
    }

    private static void addVisibleCardsToKnownSet(Game game, Player player) {
        // Player 1 Zones (Our Agent)
        for (Card c : player.getCardsIn(ZoneType.Hand)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Graveyard)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Exile)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Command)) knownCardNames.add(c.getName());

        // Player 2 Zones (Visible)
        Player opponent = player.getSingleOpponent(); 
        if (opponent != null) {
            for (Card c : opponent.getCardsIn(ZoneType.Graveyard)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Battlefield)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Exile)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Command)) knownCardNames.add(c.getName());
        }

        // Stack
        for (forge.game.spellability.SpellAbilityStackInstance stackItem : game.getStack()) {
            Card source = stackItem.getSpellAbility().getHostCard();
            if (source != null) {
                knownCardNames.add(source.getName());
            }
        }
    }

    private static JsonObject extractGameState(Game game) {
        // Find Player 1 (the one we are controlling/reporting for)
        Player player = null;
        for (Player p : game.getPlayers()) {
            if (p.getController() instanceof HeadlessPlayerController) {
                player = p;
                break;
            }
        }
        
        if (player != null) {
            addVisibleCardsToKnownSet(game, player);
        }

        JsonObject state = new JsonObject();

        // Add definitions for all known cards
        JsonObject cardDefinitions = new JsonObject();
        for (String cardName : knownCardNames) {
            cardDefinitions.add(cardName, getCardDefinition(cardName));
        }
        state.add("card_definitions", cardDefinitions);

        // General Game Info
        state.addProperty("turn", game.getPhaseHandler().getTurn());
        state.addProperty("phase", game.getPhaseHandler().getPhase().toString());
        state.addProperty("activePlayerId", game.getPhaseHandler().getPlayerTurn().getId());
        state.addProperty("priorityPlayerId", game.getPhaseHandler().getPlayerTurn().getId()); // Approximate

        // Stack - show what spells/abilities are on the stack
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
            // Filter to only abilities the player can actually activate
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

                // Add target information
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

    private static class HeadlessLobbyPlayer extends forge.ai.LobbyPlayerAi {
        public HeadlessLobbyPlayer(String name) {
            super(name, null);
        }

        @Override
        public Player createIngamePlayer(Game game, final int id) {
            Player ai = new Player(getName(), game, id);
            ai.setFirstController(new HeadlessPlayerController(game, ai, this));
            return ai;
        }
    }

    private static class HeadlessPlayerController extends forge.ai.PlayerControllerAi {
        public HeadlessPlayerController(Game game, Player player, forge.ai.LobbyPlayerAi lobbyPlayer) {
            super(game, player, lobbyPlayer);
        }

        @Override
        public boolean mulliganKeepHand(Player player, int cardsToReturn) {
            return true; // Always keep hand
        }

        @Override
        public void declareAttackers(Player attacker, Combat combat) {
            if (aiAgentClient != null) {
                try {
                    JsonObject gameState = extractGameState(currentGame);
                    JsonObject actionState = new JsonObject();

                    // Attackers
                    JsonArray attackersJson = new JsonArray();
                    CardCollection potentialAttackers = CardLists.filter(attacker.getCreaturesInPlay(),
                            c -> CombatUtil.canAttack(c));
                    for (int i = 0; i < potentialAttackers.size(); i++) {
                        Card c = potentialAttackers.get(i);
                        JsonObject att = new JsonObject();
                        att.addProperty("index", i);
                        att.addProperty("id", c.getId());
                        att.addProperty("name", c.getName());
                        att.addProperty("power", c.getNetPower());
                        att.addProperty("toughness", c.getNetToughness());
                        attackersJson.add(att);
                    }
                    actionState.add("attackers", attackersJson);

                    // Defenders
                    JsonArray defendersJson = new JsonArray();
                    List<GameEntity> defenders = new ArrayList<GameEntity>();
                    for (GameEntity d : combat.getDefenders()) {
                        defenders.add(d);
                    }
                    for (int i = 0; i < defenders.size(); i++) {
                        GameEntity d = defenders.get(i);
                        JsonObject def = new JsonObject();
                        def.addProperty("index", i);
                        def.addProperty("id", d.getId());
                        def.addProperty("name", d.getName());
                        def.addProperty("type", d instanceof Player ? "Player" : "Planeswalker");
                        defendersJson.add(def);
                    }
                    actionState.add("defenders", defendersJson);

                    JsonObject context = new JsonObject();
                    context.addProperty("requestType", "declare_attackers");
                    context.addProperty("phase", currentGame.getPhaseHandler().getPhase().toString());
                    context.addProperty("turn", currentGame.getPhaseHandler().getTurn());
                    context.addProperty("playerName", player.getName());

                    AIAgentClient.AIAgentRequest request = new AIAgentClient.AIAgentRequest(
                            gameId, "declare_attackers", gameState, actionState, context);

                    System.out.println("Calling AI agent for declare_attackers...");
                    AIAgentClient.AIAgentResponse response = aiAgentClient.requestDecision(request);

                    if ("declare_attackers".equals(response.getDecisionType())) {
                        JsonArray attackersDec = response.getAttackers();
                        if (attackersDec != null) {
                            for (int i = 0; i < attackersDec.size(); i++) {
                                JsonObject dec = attackersDec.get(i).getAsJsonObject();
                                int attIdx = dec.get("attacker_index").getAsInt();
                                int defIdx = dec.get("defender_index").getAsInt();

                                if (attIdx >= 0 && attIdx < potentialAttackers.size() &&
                                        defIdx >= 0 && defIdx < defenders.size()) {
                                    Card attackerCard = potentialAttackers.get(attIdx);
                                    GameEntity defenderEntity = defenders.get(defIdx);
                                    combat.addAttacker(attackerCard, defenderEntity);
                                    System.out.println("AI declared attacker: " + attackerCard.getName() + " -> "
                                            + defenderEntity.getName());
                                }
                            }
                        }
                    }
                    return;
                } catch (Exception e) {
                    System.err.println("AI agent error in declareAttackers: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            // Fallback to default AI
            super.declareAttackers(attacker, combat);
        }

        @Override
        public void declareBlockers(Player defender, Combat combat) {
            if (aiAgentClient != null) {
                try {
                    JsonObject gameState = extractGameState(currentGame);
                    JsonObject actionState = new JsonObject();

                    // Attackers (to be blocked)
                    JsonArray attackersJson = new JsonArray();
                    CardCollection attackers = combat.getAttackers();
                    for (int i = 0; i < attackers.size(); i++) {
                        Card c = attackers.get(i);
                        JsonObject att = new JsonObject();
                        att.addProperty("index", i);
                        att.addProperty("id", c.getId());
                        att.addProperty("name", c.getName());
                        att.addProperty("power", c.getNetPower());
                        att.addProperty("toughness", c.getNetToughness());
                        GameEntity attacked = combat.getDefenderByAttacker(c);
                        att.addProperty("attacking", attacked != null ? attacked.getName() : "Unknown");
                        attackersJson.add(att);
                    }
                    actionState.add("attackers", attackersJson);

                    // Blockers
                    JsonArray blockersJson = new JsonArray();
                    CardCollection potentialBlockers = CardLists.filter(defender.getCreaturesInPlay(),
                            c -> CombatUtil.canBlock(c));
                    for (int i = 0; i < potentialBlockers.size(); i++) {
                        Card c = potentialBlockers.get(i);
                        JsonObject blk = new JsonObject();
                        blk.addProperty("index", i);
                        blk.addProperty("id", c.getId());
                        blk.addProperty("name", c.getName());
                        blk.addProperty("power", c.getNetPower());
                        blk.addProperty("toughness", c.getNetToughness());
                        blockersJson.add(blk);
                    }
                    actionState.add("blockers", blockersJson);

                    JsonObject context = new JsonObject();
                    context.addProperty("requestType", "declare_blockers");
                    context.addProperty("phase", currentGame.getPhaseHandler().getPhase().toString());
                    context.addProperty("turn", currentGame.getPhaseHandler().getTurn());
                    context.addProperty("playerName", player.getName());

                    AIAgentClient.AIAgentRequest request = new AIAgentClient.AIAgentRequest(
                            gameId, "declare_blockers", gameState, actionState, context);

                    System.out.println("Calling AI agent for declare_blockers...");
                    AIAgentClient.AIAgentResponse response = aiAgentClient.requestDecision(request);

                    if ("declare_blockers".equals(response.getDecisionType())) {
                        JsonArray blocksDec = response.getBlocks();
                        if (blocksDec != null) {
                            for (int i = 0; i < blocksDec.size(); i++) {
                                JsonObject dec = blocksDec.get(i).getAsJsonObject();
                                int blkIdx = dec.get("blocker_index").getAsInt();
                                int attIdx = dec.get("attacker_index").getAsInt();

                                if (blkIdx >= 0 && blkIdx < potentialBlockers.size() &&
                                        attIdx >= 0 && attIdx < attackers.size()) {
                                    Card blockerCard = potentialBlockers.get(blkIdx);
                                    Card attackerCard = attackers.get(attIdx);
                                    combat.addBlocker(attackerCard, blockerCard);
                                    System.out.println("AI declared blocker: " + blockerCard.getName() + " -> "
                                            + attackerCard.getName());
                                }
                            }
                        }
                    }
                    return;
                } catch (Exception e) {
                    System.err.println("AI agent error in declareBlockers: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            // Fallback to default AI
            super.declareBlockers(defender, combat);
        }
        // To support manual combat, we need to override declareAttackers,
        // declareBlockers, etc.

        private <T extends GameEntity> JsonObject createTargetOptionsJson(FCollectionView<T> optionList, int min,
                int max, String title) {
            JsonObject result = new JsonObject();
            result.addProperty("min", min);
            result.addProperty("max", max);
            result.addProperty("title", title);

            JsonArray options = new JsonArray();
            int index = 0;
            for (T target : optionList) {
                JsonObject option = new JsonObject();
                option.addProperty("index", index++);
                option.addProperty("type", target.getClass().getSimpleName());
                option.addProperty("name", target.getName());
                option.addProperty("id", target.getId());

                if (target instanceof Player) {
                    option.addProperty("life", ((Player) target).getLife());
                }

                options.add(option);
            }
            result.add("targets", options);
            return result;
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

            List<T> selected = new ArrayList<>();
            List<T> options = new ArrayList<>();
            for (T t : optionList)
                options.add(t);

            if (options.isEmpty()) {
                currentPromptType = "none";
                return selected;
            }

            // Create action state for target selection
            JsonObject actionState = createTargetOptionsJson(optionList, min, max, title);

            // If AI agent is configured, call out to it for decision
            if (aiAgentClient != null) {
                try {
                    JsonObject gameState = extractGameState(currentGame);
                    JsonObject context = new JsonObject();
                    context.addProperty("requestType", "target");
                    context.addProperty("spellName",
                            sa != null && sa.getHostCard() != null ? sa.getHostCard().getName() : "Unknown");
                    context.addProperty("spellDescription", sa != null ? sa.getDescription() : "");

                    AIAgentClient.AIAgentRequest request = new AIAgentClient.AIAgentRequest(
                            gameId, "target", gameState, actionState, context);

                    System.out.println("Calling AI agent for target selection...");
                    AIAgentClient.AIAgentResponse response = aiAgentClient.requestDecision(request);

                    // Handle multi-select responses
                    if (response.getIndices() != null) {
                        for (int idx : response.getIndices()) {
                            if (idx >= 0 && idx < options.size() && selected.size() < max) {
                                T target = options.get(idx);
                                if (!selected.contains(target)) {
                                    selected.add(target);
                                }
                            }
                        }
                    } else if (response.getIndex() >= 0 && response.getIndex() < options.size()) {
                        selected.add(options.get(response.getIndex()));
                    }

                    System.out.println("AI agent selected " + selected.size() + " target(s)");
                    return selected;

                } catch (AIAgentClient.AIAgentException e) {
                    System.err.println("AI agent error, falling back to HTTP input: " + e.getMessage());
                    // Fall through to HTTP input below
                }
            }

            // Fallback: Update global prompt state for HTTP input
            currentPromptType = "target";
            currentPromptData = actionState;

            System.out.println("Waiting for target selection via HTTP...");

            while (selected.size() < max) {
                try {
                    // Block waiting for input
                    String input = inputQueue.take();
                    int index = Integer.parseInt(input.trim());

                    if (index == -1) {
                        if (selected.size() >= min)
                            break;
                        System.out.println("Must select at least " + min + " targets.");
                        continue;
                    }

                    if (index >= 0 && index < options.size()) {
                        T target = options.get(index);
                        if (!selected.contains(target)) {
                            selected.add(target);
                            if (selected.size() == max)
                                break;
                        } else {
                            System.out.println("Already selected.");
                        }
                    } else {
                        System.out.println("Invalid index.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return selected;
                }
            }
            return selected;
        }

        private List<SpellAbility> getPossibleSpellAbilities() {
            // Always regenerate the list of actions
            List<SpellAbility> allAbilities = new ArrayList<>();

            // Get available lands to play
            CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
            if (lands != null && !lands.isEmpty()) {
                for (Card land : lands) {
                    SpellAbility sa = land.getSpellPermanent();
                    if (sa != null) {
                        allAbilities.add(sa);
                    }
                }
            }

            // Get available spells and abilities
            CardCollection availableCards = ComputerUtilAbility.getAvailableCards(getGame(), player);
            List<SpellAbility> spellAbilities = ComputerUtilAbility.getSpellAbilities(availableCards, player);

            for (SpellAbility sa : spellAbilities) {
                // Filter to only abilities the player can actually activate
                if (sa.canPlay() && sa.getActivatingPlayer() == player && ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                    allAbilities.add(sa);
                }
            }

            return allAbilities;
        }

        private JsonObject getPossibleActionsJson() {
            List<SpellAbility> actions = getPossibleSpellAbilities();
            JsonObject result = new JsonObject();
            JsonArray actionsList = new JsonArray();

            for (SpellAbility sa : actions) {
                JsonObject action = new JsonObject();
                Card source = sa.getHostCard();

                // Determine action type
                if (source != null && source.isLand() && sa.isSpell()) {
                    action.addProperty("type", "play_land");
                    action.addProperty("card_id", source.getId());
                    action.addProperty("card_name", source.getName());
                } else if (sa.isSpell()) {
                    action.addProperty("type", "cast_spell");
                    action.addProperty("card_id", source != null ? source.getId() : -1);
                    action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                    action.addProperty("ability_description", sa.getDescription());
                    action.addProperty("mana_cost", sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "");

                    // Add target information
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
                } else {
                    action.addProperty("type", "activate_ability");
                    action.addProperty("card_id", source != null ? source.getId() : -1);
                    action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                    action.addProperty("ability_description", sa.getDescription());
                    action.addProperty("mana_cost",
                            sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "no cost");
                    action.addProperty("requires_targets", sa.usesTargeting());
                }

                actionsList.add(action);
            }

            // Always available: pass priority
            JsonObject passAction = new JsonObject();
            passAction.addProperty("type", "pass_priority");
            actionsList.add(passAction);

            result.add("actions", actionsList);
            result.addProperty("count", actionsList.size());
            return result;
        }

        @Override
        public boolean chooseTargetsFor(SpellAbility sa) {
            System.out.println("DEBUG: chooseTargetsFor called for: " + sa.getDescription());
            if (!sa.usesTargeting()) {
                System.out.println("DEBUG: No targeting needed");
                return true;
            }

            forge.game.spellability.TargetRestrictions tgt = sa.getTargetRestrictions();
            List<GameEntity> candidates = tgt.getAllCandidates(sa, true);

            forge.util.collect.FCollection<GameEntity> optionList = new forge.util.collect.FCollection<>();
            optionList.addAll(candidates);

            int min = tgt.getMinTargets(sa.getHostCard(), sa);
            int max = tgt.getMaxTargets(sa.getHostCard(), sa);
            String title = "Select targets for " + sa.getHostCard().getName();

            System.out.println("DEBUG: Choosing targets (min=" + min + ", max=" + max + ")");
            List<GameEntity> chosen = chooseEntitiesForEffect(optionList, min, max, null, sa, title, player, null);
            System.out.println("DEBUG: chosen entities count: " + chosen.size());

            if (chosen.size() < min) {
                System.out.println("DEBUG: chosen size (" + chosen.size() + ") < min (" + min + ")");
                return false;
            }

            for (GameEntity entity : chosen) {
                sa.getTargets().add(entity);
            }

            return true;
        }

        @Override
        public boolean playChosenSpellAbility(SpellAbility sa) {
            System.out.println("DEBUG: Executing playChosenSpellAbility for: " + sa.getDescription());
            boolean result = false;
            try {
                if (!sa.setupTargets()) {
                    System.out.println("DEBUG: setupTargets failed for: " + sa.getDescription());
                    return false;
                }
                result = super.playChosenSpellAbility(sa);
                System.out.println("DEBUG: super.playChosenSpellAbility returned: " + result);
            } catch (Exception e) {
                System.err.println("DEBUG: Exception in playChosenSpellAbility: " + e.getMessage());
                e.printStackTrace();
            }
            return result;
        }

        private void printCondensedLog(JsonObject gameState, JsonObject actionState) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n--- CONDENSED LOG ---\n");

            // Possible Actions
            sb.append("POSSIBLE ACTIONS:\n");
            JsonArray actions = actionState.getAsJsonArray("actions");
            for (int i = 0; i < actions.size(); i++) {
                JsonObject a = actions.get(i).getAsJsonObject();
                sb.append("  ").append(i).append(": ").append(a.get("type").getAsString());
                if (a.has("card_name"))
                    sb.append(" - ").append(a.get("card_name").getAsString());
                if (a.has("ability_description"))
                    sb.append(" (").append(a.get("ability_description").getAsString()).append(")");
                sb.append("\n");
            }

            // Battlefields
            sb.append("BATTLEFIELDS: \n");
            JsonArray players = gameState.getAsJsonArray("players");
            for (int i = 0; i < players.size(); i++) {
                JsonObject p = players.get(i).getAsJsonObject();
                sb.append("  ").append(p.get("name").getAsString()).append(": ");
                JsonArray bf = p.getAsJsonArray("battlefield");
                List<String> cards = new ArrayList<>();
                for (int j = 0; j < bf.size(); j++)
                    cards.add(bf.get(j).getAsJsonObject().get("name").getAsString());
                sb.append(cards).append("\n");
            }

            // Hand
            sb.append("HAND: ");
            for (int i = 0; i < players.size(); i++) {
                JsonObject p = players.get(i).getAsJsonObject();
                if (p.get("name").getAsString().equals(player.getName())) {
                    JsonArray hand = p.getAsJsonArray("hand");
                    List<String> cards = new ArrayList<>();
                    if (hand != null) {
                        for (int j = 0; j < hand.size(); j++)
                            cards.add(hand.get(j).getAsJsonObject().get("name").getAsString());
                    } else {
                         System.err.println("DEBUG: Hand is null in JSON for player " + p.get("name").getAsString());
                    }
                    sb.append(cards);
                }
            }
            sb.append("\n");

            sb.append("--> OUT");
            System.out.println(sb.toString());
        }

        @Override
        public java.util.List<forge.game.spellability.SpellAbility> chooseSpellAbilityToPlay() {
            System.out.println("Phase: " + getGame().getPhaseHandler().getPhase());
            System.out.println("DEBUG: Controller Game Hash: " + System.identityHashCode(getGame()));
            System.out.println("DEBUG: Global Game Hash: " + System.identityHashCode(currentGame));
            System.out.println("DEBUG: Controller Player Hand Size: " + player.getCardsIn(ZoneType.Hand).size());

            // Build action state
            JsonObject actionState = getPossibleActionsJson();
            List<SpellAbility> actions = getPossibleSpellAbilities();

            if (condensedLogging) {
                printCondensedLog(extractGameState(getGame()), actionState);
            }

            // OPTIMIZATION: If only one action is available (which is always "pass
            // priority"),
            // automatically take it without calling the AI agent.
            if (actions.isEmpty()) { // Should not happen given getPossibleActionsJson adds pass_priority
                // But if getPossibleSpellAbilities returns empty, it means only pass is
                // available
                // Actually getPossibleSpellAbilities does NOT include pass_priority,
                // getPossibleActionsJson does.
                // Let's check the JSON count or just trust the logic.
                // The actions list from getPossibleSpellAbilities only contains
                // spells/abilities.
                // If it is empty, it means the only thing we can do is pass.
                // However, we should check if we can play lands too.
                // Let's look at getPossibleActionsJson() implementation again.
                // It adds lands, spells, abilities, and THEN pass_priority.
                // So if actions list is empty AND no lands to play...
                // Actually, let's rely on the JSON count since that aggregates everything.
                if (actionState.has("count") && actionState.get("count").getAsInt() == 1) {
                    // The only action is "pass_priority" (or technically a single forced action,
                    // but usually pass)
                    // Let's verify it is pass_priority just to be safe, although currently it's
                    // always added last.
                    JsonArray actionsList = actionState.getAsJsonArray("actions");
                    if (actionsList.size() > 0) {
                        JsonObject firstAction = actionsList.get(0).getAsJsonObject();
                        if ("pass_priority".equals(firstAction.get("type").getAsString())) {
                            System.out.println("Auto-passing priority (only option)...");
                            return null;
                        }
                    }
                }
            }

            // If AI agent is configured, call out to it for decision
            if (aiAgentClient != null) {
                try {
                    JsonObject gameState = extractGameState(currentGame);
                    JsonObject context = new JsonObject();
                    context.addProperty("requestType", "action");
                    context.addProperty("phase", currentGame.getPhaseHandler().getPhase().toString());
                    context.addProperty("turn", currentGame.getPhaseHandler().getTurn());
                    context.addProperty("playerName", player.getName());

                    AIAgentClient.AIAgentRequest request = new AIAgentClient.AIAgentRequest(
                            gameId, "action", gameState, actionState, context);

                    System.out.println("Calling AI agent for action decision...");
                    AIAgentClient.AIAgentResponse response = aiAgentClient.requestDecision(request);

                    // Handle pass decision (explicit)
                    if (response.isPass()) {
                        System.out.println("AI agent decided to pass priority");
                        if (condensedLogging)
                            System.out.println("DECISION --> IN: pass");
                        return null;
                    }

                    // Handle action selection
                    int actionIndex = response.getIndex();

                    // TRANSLATION LAYER: Check if the selected index corresponds to "pass_priority"
                    JsonArray actionsList = actionState.getAsJsonArray("actions");
                    if (actionIndex >= 0 && actionIndex < actionsList.size()) {
                        JsonObject selectedActionJson = actionsList.get(actionIndex).getAsJsonObject();
                        if ("pass_priority".equals(selectedActionJson.get("type").getAsString())) {
                            System.out.println("AI agent selected pass_priority via index " + actionIndex);
                            if (condensedLogging)
                                System.out.println("DECISION --> IN: pass (via index " + actionIndex + ")");
                            return null;
                        }
                    }

                    if (actionIndex >= 0 && actionIndex < actions.size()) {
                        SpellAbility chosenAbility = actions.get(actionIndex);
                        List<SpellAbility> result = new ArrayList<>();
                        result.add(chosenAbility);
                        String actionDesc = (chosenAbility.getHostCard() != null ? chosenAbility.getHostCard().getName()
                                : "Unknown");
                        System.out.println("AI agent selected action: " + actionDesc);
                        if (condensedLogging)
                            System.out.println("DECISION --> IN: action " + actionIndex + " (" + actionDesc + ")");
                        return result;
                    } else {
                        System.err.println("AI agent returned invalid action index: " + actionIndex);
                        // Fall through to HTTP input
                    }

                } catch (AIAgentClient.AIAgentException e) {
                    System.err.println("AI agent error, falling back to HTTP input: " + e.getMessage());
                    // Fall through to HTTP input below
                }
            }

            // Fallback: Update global prompt state for HTTP input
            currentPromptType = "action";
            currentPromptData = actionState;

            System.out.println("Waiting for action via HTTP...");

            while (true) {
                try {
                    String input = inputQueue.take();

                    if (input.trim().isEmpty())
                        continue;

                    String[] parts = input.split(" ");
                    String command = parts[0];

                    if (command.equals("play_action") || command.equals("play")) {
                        if (parts.length < 2) {
                            System.out.println("Usage: play_action|play <index>");
                            continue;
                        }

                        try {
                            int actionIndex = Integer.parseInt(parts[1]);

                            if (actionIndex < 0 || actionIndex >= actions.size()) {
                                System.out.println("Invalid action index: " + actionIndex);
                                continue;
                            }

                            SpellAbility chosenAbility = actions.get(actionIndex);
                            List<SpellAbility> result = new ArrayList<>();
                            result.add(chosenAbility);

                            currentPromptType = "none";
                            return result;

                        } catch (NumberFormatException e) {
                            System.out.println("Invalid action index.");
                        }
                    } else if (command.equals("pass_priority") || command.equals("pp") || command.equals("pass")) {
                        currentPromptType = "none";
                        return null; // Pass priority
                    } else if (command.equals("concede") || command.equals("c")) {
                        System.exit(0);
                        return null;
                    } else {
                        System.out.println("Unknown command: " + command);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
            // System.out.println(message); // Uncomment to also see in console
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
