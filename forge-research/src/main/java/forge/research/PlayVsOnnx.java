package forge.research;

import forge.GuiDesktop;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.error.ExceptionHandler;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.player.GamePlayerUtil;
import forge.research.onnx.OnnxInferenceEngine;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * Launches the Forge desktop GUI for a human player vs an ONNX-trained agent.
 *
 * Usage: java -cp ... forge.research.PlayVsOnnx <onnx_model_path> <human_deck.dck> <ai_deck.dck>
 */
public class PlayVsOnnx {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: PlayVsOnnx <onnx_model.onnx> <your_deck.dck> <ai_deck.dck>");
            System.out.println("  onnx_model.onnx - Path to the ONNX model file");
            System.out.println("  your_deck.dck   - Path to the human player's deck file");
            System.out.println("  ai_deck.dck     - Path to the ONNX agent's deck file");
            System.exit(1);
        }

        String onnxModelPath = args[0];
        String humanDeckPath = args[1];
        String aiDeckPath = args[2];

        // Validate files exist
        for (String[] entry : new String[][]{
            {onnxModelPath, "ONNX model"}, {humanDeckPath, "Human deck"}, {aiDeckPath, "AI deck"}
        }) {
            if (!new File(entry[0]).exists()) {
                System.err.println(entry[1] + " not found: " + entry[0]);
                System.exit(1);
            }
        }

        // Standard Forge desktop initialization
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        System.setProperty("sun.java2d.d3d", "false");

        // Use a custom GuiDesktop that suppresses BugReportDialog during init
        // (gauntlet data loading can crash with XStream errors, blocking the EDT)
        GuiBase.setInterface(new GuiDesktop() {
            @Override
            public void showBugReportDialog(String title, String text, boolean exit) {
                System.err.println("Suppressed bug report dialog: " + title);
            }
        });
        ExceptionHandler.registerErrorHandling();

        // Full Forge initialization (model, view, control)
        Singletons.initializeOnce(true);
        Singletons.getControl().initialize();

        // Schedule game start after GUI is fully initialized.
        // FControl.initialize() schedules FView.initialize() on EDT, which in turn
        // makes the frame visible at the end. We queue our game start after that.
        SwingUtilities.invokeLater(() -> {
            // This runs after FView.initialize() completes on the EDT
            try {
                startGame(onnxModelPath, humanDeckPath, aiDeckPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void startGame(String onnxModelPath, String humanDeckPath,
            String aiDeckPath) throws Exception {
        // Load decks
        Deck humanDeck = DeckSerializer.fromFile(new File(humanDeckPath));
        Deck aiDeck = DeckSerializer.fromFile(new File(aiDeckPath));

        if (humanDeck == null) {
            throw new RuntimeException("Failed to load human deck: " + humanDeckPath);
        }
        if (aiDeck == null) {
            throw new RuntimeException("Failed to load AI deck: " + aiDeckPath);
        }

        // Load ONNX engine
        OnnxInferenceEngine engine = new OnnxInferenceEngine(onnxModelPath);

        // Create players
        RegisteredPlayer humanRp = new RegisteredPlayer(humanDeck);
        humanRp.setPlayer(GamePlayerUtil.getGuiPlayer());

        OnnxLobbyPlayer onnxLobby = new OnnxLobbyPlayer("ONNX Agent", engine, 1);
        RegisteredPlayer aiRp = new RegisteredPlayer(aiDeck);
        aiRp.setPlayer(onnxLobby);

        List<RegisteredPlayer> players = new ArrayList<>();
        players.add(humanRp);
        players.add(aiRp);

        // Create GUI for the human player
        IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
        Map<RegisteredPlayer, IGuiGame> guis = new HashMap<>();
        guis.put(humanRp, gui);

        // Host and start match
        HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Constructed, null, players, guis);

        System.out.println("Game started: " + humanDeck.getName()
                + " vs ONNX Agent (" + aiDeck.getName() + ")");
    }
}
