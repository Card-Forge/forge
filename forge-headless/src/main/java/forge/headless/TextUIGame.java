package forge.headless;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.*;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.FileUtil;
import forge.util.FileSection;
import forge.util.MyRandom;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Text UI game mode for Forge.
 * Allows interactive gameplay through a text-based interface.
 */
public class TextUIGame {

    public static void run(String[] args) {
        // Strip the "tui" command from args - Main.java passes it but picocli doesn't need it
        String[] tuiArgs = new String[args.length - 1];
        System.arraycopy(args, 1, tuiArgs, 0, args.length - 1);

        // Use picocli to parse command line arguments
        TUICommand command = new TUICommand();
        CommandLine cmd = new CommandLine(command);
        cmd.setUnmatchedArgumentsAllowed(false);

        int exitCode = cmd.execute(tuiArgs);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Run the game with parsed command-line options.
     * Called by TUICommand after picocli parses the arguments.
     */
    public static void runGame(TUICommand cmd) {
        System.out.println("=== Forge Text UI Mode ===");

        // Extract parsed options from command
        String humanDeckName = cmd.deck1;
        String aiDeckName = cmd.deck2;
        String gameTypeStr = cmd.gameType;
        AgentType player1Agent = cmd.player1Agent;
        AgentType player2Agent = cmd.player2Agent;
        boolean askMana = cmd.askMana;
        boolean numericChoices = cmd.numericChoices;
        Long seed = cmd.seed;
        String startStatePath = cmd.startStatePath;

        // Set random seed if provided (must be done BEFORE creating the game)
        if (seed != null) {
            System.out.println("Setting random seed: " + seed);
            MyRandom.setRandom(new Random(seed));
        }

        // NOW load the card database after all arguments have been validated
        // Use lazy loading to only load cards from the decks being played
        System.out.println("Initializing Forge (lazy card loading enabled)...");
        Thread cardLoadingThread = new Thread(() -> {
            FModel.initialize(null, null);
        }, "CardLoadingThread");
        cardLoadingThread.start();

        // Install TUI GUI base which intercepts game log messages
        TUIGuiBase.install();

        // Wait for card loading to complete
        try {
            cardLoadingThread.join();
        } catch (InterruptedException e) {
            System.err.println("Card loading interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return;
        }
        System.out.println("Card database loaded successfully.");

        // Now parse GameType (requires FModel to be initialized)
        GameType type;
        try {
            type = GameType.valueOf(gameTypeStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid game type: " + gameTypeStr);
            System.err.println("Valid options: Constructed, Commander, etc.");
            return;
        }

        // Load decks
        Deck humanDeck = loadDeck(humanDeckName, type);
        Deck aiDeck = loadDeck(aiDeckName, type);

        if (humanDeck == null || aiDeck == null) {
            System.out.println("Failed to load decks. Exiting.");
            return;
        }

        System.out.println("Starting game: Player 1 (" + humanDeck.getName() + ", " + player1Agent + ") vs Player 2 (" + aiDeck.getName() + ", " + player2Agent + ")");
        System.out.println();

        // Create players
        List<RegisteredPlayer> players = new ArrayList<>();

        // Player 1
        RegisteredPlayer player1;
        if (type.equals(GameType.Commander)) {
            player1 = RegisteredPlayer.forCommander(humanDeck);
        } else {
            player1 = new RegisteredPlayer(humanDeck);
        }
        // Create LobbyPlayer based on agent type
        LobbyPlayer player1Lobby;
        if (player1Agent == AgentType.AI) {
            player1Lobby = GamePlayerUtil.createAiPlayer("AI-" + humanDeck.getName(), 0);
        } else {
            player1Lobby = GamePlayerUtil.getGuiPlayer("Player 1", 0, 0, false);
        }
        player1.setPlayer(player1Lobby);
        players.add(player1);

        // Player 2
        RegisteredPlayer player2;
        if (type.equals(GameType.Commander)) {
            player2 = RegisteredPlayer.forCommander(aiDeck);
        } else {
            player2 = new RegisteredPlayer(aiDeck);
        }
        // Create LobbyPlayer based on agent type
        LobbyPlayer player2Lobby;
        if (player2Agent == AgentType.AI) {
            player2Lobby = GamePlayerUtil.createAiPlayer("AI-" + aiDeck.getName(), 0);
        } else {
            player2Lobby = GamePlayerUtil.getGuiPlayer("Player 2", 1, 1, false);
        }
        player2.setPlayer(player2Lobby);
        players.add(player2);

        // Create and start the match
        GameRules rules = new GameRules(type);
        Match match = new Match(rules, players, "TUI Game");

        Game game = match.createGame();

        // Replace player controllers with TUI controllers
        // We need to do this after game creation but BEFORE startGame
        // because startGame calls prepareAllZones which may call controller methods
        Player player1GamePlayer = null;
        Player player2GamePlayer = null;

        for (Player p : game.getPlayers()) {
            if (p.getLobbyPlayer() == player1Lobby) {
                player1GamePlayer = p;
                System.out.println("Found player 1: " + p.getName());
            } else if (p.getLobbyPlayer() == player2Lobby) {
                player2GamePlayer = p;
                System.out.println("Found player 2: " + p.getName());
            }
        }

        // Install controller for player 1 based on agent type
        if (player1GamePlayer != null && player1Agent != AgentType.AI) {
            installController(player1GamePlayer, player1Lobby, game, player1Agent, askMana, numericChoices);
        }

        // Install controller for player 2 based on agent type
        if (player2GamePlayer != null && player2Agent != AgentType.AI) {
            installController(player2GamePlayer, player2Lobby, game, player2Agent, askMana, numericChoices);
        }

        // Set the current game for log monitoring
        TUIGuiBase.setCurrentGame(game);

        // Load game state from .pzl file if provided
        if (startStatePath != null) {
            System.out.println("Loading game state from: " + startStatePath);
            if (!loadGameState(game, startStatePath)) {
                System.err.println("Failed to load game state. Exiting.");
                return;
            }
        }

        // Start the game
        System.out.println("Game starting...");
        System.out.println("=".repeat(60));

        if (startStatePath != null) {
            // Game state was loaded, so the game is already set up
            // Just run the game loop
            game.getAction().invoke(() -> {
                game.getPhaseHandler().devModeSet(game.getPhaseHandler().getPhase(),
                                                   game.getPhaseHandler().getPlayerTurn(),
                                                   game.getPhaseHandler().getTurn());
            });
        } else {
            // Normal game start
            match.startGame(game);
        }

        // Game is over
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("GAME OVER");

        if (game.getOutcome().isDraw()) {
            System.out.println("Result: Draw!");
        } else {
            System.out.println("Winner: " + game.getOutcome().getWinningLobbyPlayer().getName());
        }

        // Print choice statistics if available
        System.out.println();
        System.out.println("=== Choice Statistics ===");

        if (player1GamePlayer != null && player1GamePlayer.getController() instanceof PlayerControllerTUI) {
            PlayerControllerTUI tuiController1 = (PlayerControllerTUI) player1GamePlayer.getController();
            System.out.println("Player 1 (" + player1GamePlayer.getName() + "):");
            System.out.println("  Total choices made: " + tuiController1.getTotalChoicesMade());
            System.out.println("  Total options presented: " + tuiController1.getTotalChoiceOptions());
            if (tuiController1.getTotalChoicesMade() > 0) {
                double avgOptions = (double) tuiController1.getTotalChoiceOptions() / tuiController1.getTotalChoicesMade();
                System.out.printf("  Average options per choice: %.2f%n", avgOptions);
            }
        }

        if (player2GamePlayer != null && player2GamePlayer.getController() instanceof PlayerControllerTUI) {
            PlayerControllerTUI tuiController2 = (PlayerControllerTUI) player2GamePlayer.getController();
            System.out.println("Player 2 (" + player2GamePlayer.getName() + "):");
            System.out.println("  Total choices made: " + tuiController2.getTotalChoicesMade());
            System.out.println("  Total options presented: " + tuiController2.getTotalChoiceOptions());
            if (tuiController2.getTotalChoicesMade() > 0) {
                double avgOptions = (double) tuiController2.getTotalChoiceOptions() / tuiController2.getTotalChoicesMade();
                System.out.printf("  Average options per choice: %.2f%n", avgOptions);
            }
        }
    }

    /**
     * Helper method to install a controller for a player using reflection.
     */
    private static void installController(Player gamePlayer, LobbyPlayer lobbyPlayer, Game game,
                                          AgentType agentType, boolean askMana, boolean numericChoices) {
        try {
            forge.game.player.PlayerController controller;

            switch (agentType) {
                case TUI:
                    controller = new PlayerControllerTUI(game, gamePlayer, lobbyPlayer, askMana, numericChoices);
                    break;
                case ZERO:
                    controller = new PlayerControllerZero(game, gamePlayer, lobbyPlayer, askMana, numericChoices);
                    break;
                case RANDOM:
                    controller = new PlayerControllerRandom(game, gamePlayer, lobbyPlayer, askMana, numericChoices);
                    break;
                case AI:
                    // AI controller is already installed, no need to replace
                    return;
                default:
                    System.err.println("Unknown agent type: " + agentType);
                    return;
            }

            java.lang.reflect.Field controllerField = Player.class.getDeclaredField("controller");
            controllerField.setAccessible(true);
            controllerField.set(gamePlayer, controller);
            System.out.println(agentType + " Controller installed for player: " + gamePlayer.getName());
        } catch (Exception e) {
            System.err.println("Failed to install controller: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Deck loadDeck(String deckName, GameType type) {
        int dotPos = deckName.lastIndexOf('.');
        if (dotPos > 0 && dotPos == deckName.length() - 4) {
            // It's a file - try to resolve it in this order:
            // 1. As-is (absolute or relative to current directory)
            // 2. Relative to the base deck directory
            File f = new File(deckName);

            // If not found as-is, try with the base directory
            if (!f.exists()) {
                String baseDir = type.equals(GameType.Commander) ?
                        ForgeConstants.DECK_COMMANDER_DIR : ForgeConstants.DECK_CONSTRUCTED_DIR;
                f = new File(baseDir, deckName);
            }

            if (!f.exists()) {
                System.out.println("Deck file not found: " + deckName);
                System.out.println("  Tried as: " + new File(deckName).getAbsolutePath());
                String baseDir = type.equals(GameType.Commander) ?
                        ForgeConstants.DECK_COMMANDER_DIR : ForgeConstants.DECK_CONSTRUCTED_DIR;
                System.out.println("  Tried in: " + new File(baseDir, deckName).getAbsolutePath());
                return null;
            }

            return DeckSerializer.fromFile(f);
        }

        // It's a deck name
        if (type.equals(GameType.Commander)) {
            return FModel.getDecks().getCommander().get(deckName);
        } else {
            return FModel.getDecks().getConstructed().get(deckName);
        }
    }

    /**
     * Loads a game state from a .pzl file and applies it to the game.
     * This is a simplified implementation that directly manipulates zones.
     *
     * @param game The game to apply the state to
     * @param puzzleFilePath Path to the .pzl file
     * @return true if successful, false otherwise
     */
    private static boolean loadGameState(Game game, String puzzleFilePath) {
        try {
            File puzzleFile = new File(puzzleFilePath);
            if (!puzzleFile.exists()) {
                System.err.println("Puzzle file not found: " + puzzleFilePath);
                return false;
            }

            // Read and parse the puzzle file
            List<String> pfData = FileUtil.readFile(puzzleFilePath);
            Map<String, List<String>> puzzleSections = FileSection.parseSections(pfData);

            // Get the [state] section
            List<String> stateLines = puzzleSections.get("state");
            if (stateLines == null || stateLines.isEmpty()) {
                System.err.println("No [state] section found in puzzle file");
                return false;
            }

            // Parse state into a map
            Map<String, String> stateMap = new java.util.HashMap<>();
            for (String line : stateLines) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    stateMap.put(parts[0].trim(), parts[1].trim());
                }
            }

            // Apply state to game within the game's action thread
            final Map<String, String> finalStateMap = stateMap;
            game.getAction().invoke(() -> applyPuzzleState(game, finalStateMap));

            System.out.println("Game state loaded successfully from: " + puzzleFilePath);
            return true;
        } catch (Exception e) {
            System.err.println("Error loading game state from puzzle file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Applies the parsed puzzle state to the game.
     * Must be called within game.getAction().invoke()
     */
    private static void applyPuzzleState(Game game, Map<String, String> state) {
        try {
            List<Player> players = game.getPlayers();
            if (players.size() < 2) {
                throw new RuntimeException("Game must have at least 2 players");
            }

            Player humanPlayer = players.get(0);
            Player aiPlayer = players.get(1);

            // Set life totals
            if (state.containsKey("humanlife")) {
                humanPlayer.setLife(Integer.parseInt(state.get("humanlife")), null);
            }
            if (state.containsKey("ailife")) {
                aiPlayer.setLife(Integer.parseInt(state.get("ailife")), null);
            }

            // Clear all zones first by removing cards one by one
            clearZone(game, humanPlayer, ZoneType.Hand);
            clearZone(game, humanPlayer, ZoneType.Library);
            clearZone(game, humanPlayer, ZoneType.Battlefield);
            clearZone(game, humanPlayer, ZoneType.Graveyard);

            clearZone(game, aiPlayer, ZoneType.Hand);
            clearZone(game, aiPlayer, ZoneType.Library);
            clearZone(game, aiPlayer, ZoneType.Battlefield);
            clearZone(game, aiPlayer, ZoneType.Graveyard);

            // Add cards to zones
            addCardsToZone(game, humanPlayer, ZoneType.Hand, state.get("humanhand"));
            addCardsToZone(game, humanPlayer, ZoneType.Library, state.get("humanlibrary"));
            addCardsToZone(game, humanPlayer, ZoneType.Battlefield, state.get("humanbattlefield"));
            addCardsToZone(game, humanPlayer, ZoneType.Graveyard, state.get("humangraveyard"));

            addCardsToZone(game, aiPlayer, ZoneType.Hand, state.get("aihand"));
            addCardsToZone(game, aiPlayer, ZoneType.Library, state.get("ailibrary"));
            addCardsToZone(game, aiPlayer, ZoneType.Battlefield, state.get("aibattlefield"));
            addCardsToZone(game, aiPlayer, ZoneType.Graveyard, state.get("aigraveyard"));

            // Set turn and phase
            int turn = state.containsKey("turn") ? Integer.parseInt(state.get("turn")) : 1;
            String activePlayerStr = state.get("activeplayer");
            Player activePlayer = "ai".equalsIgnoreCase(activePlayerStr) ? aiPlayer : humanPlayer;

            String phaseStr = state.get("activephase");
            PhaseType phase = phaseStr != null ? PhaseType.smartValueOf(phaseStr) : PhaseType.MAIN1;

            game.getPhaseHandler().devModeSet(phase, activePlayer, turn);

            System.out.println("Puzzle state applied: Turn " + turn + ", " + activePlayer.getName() + "'s " + phase);
        } catch (Exception e) {
            System.err.println("Error applying puzzle state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clears all cards from a zone
     */
    private static void clearZone(Game game, Player player, ZoneType zone) {
        // Make a copy of the card list to avoid concurrent modification
        List<Card> cards = new ArrayList<Card>();
        for (Card card : player.getZone(zone).getCards()) {
            cards.add(card);
        }
        for (Card card : cards) {
            game.getAction().exile(card, null, null);
        }
    }

    /**
     * Adds cards to a zone from a semicolon-separated string
     */
    private static void addCardsToZone(Game game, Player player, ZoneType zone, String cardListStr) {
        if (cardListStr == null || cardListStr.trim().isEmpty()) {
            return;
        }

        String[] cardNames = cardListStr.split(";");
        for (String cardName : cardNames) {
            cardName = cardName.trim();
            if (!cardName.isEmpty()) {
                try {
                    // Create a card from the name
                    PaperCard paperCard = FModel.getMagicDb().getCommonCards().getCard(cardName);
                    if (paperCard == null) {
                        System.err.println("Warning: Card not found: " + cardName);
                        continue;
                    }

                    Card card = Card.fromPaperCard(paperCard, player);

                    // Add to the appropriate zone
                    player.getZone(zone).add(card);
                } catch (Exception e) {
                    System.err.println("Error adding card " + cardName + " to " + zone + ": " + e.getMessage());
                }
            }
        }
    }
}
