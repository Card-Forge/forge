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

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Launches the Forge desktop GUI for hotseat play (two human players).
 * With --logged flag, wraps both players in LoggingPlayerControllerHuman
 * to record every decision in RL action-space format.
 *
 * Usage: java -cp ... forge.research.PlayHotseat [--logged] <deck_a.dck> <deck_b.dck>
 *
 * Default decks: mono_red_pingers.dck vs caw_gates.dck
 */
public class PlayHotseat {

    private static volatile List<Map<String, Object>> sharedLog;

    public static void main(String[] args) throws Exception {
        // Parse --logged flag
        List<String> positionalArgs = new ArrayList<>();
        boolean logged = false;
        for (String arg : args) {
            if ("--logged".equals(arg)) {
                logged = true;
            } else {
                positionalArgs.add(arg);
            }
        }

        String deckPathA = positionalArgs.size() > 0 ? positionalArgs.get(0)
                : "src/main/resources/decks/mono_red_pingers.dck";
        String deckPathB = positionalArgs.size() > 1 ? positionalArgs.get(1)
                : "src/main/resources/decks/caw_gates.dck";

        for (String path : new String[]{deckPathA, deckPathB}) {
            if (!new File(path).exists()) {
                System.err.println("Deck file not found: " + path);
                System.exit(1);
            }
        }

        // Standard Forge desktop initialization
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        System.setProperty("sun.java2d.d3d", "false");

        GuiBase.setInterface(new GuiDesktop() {
            @Override
            public void showBugReportDialog(String title, String text, boolean exit) {
                System.err.println("Suppressed bug report dialog: " + title);
            }
        });
        ExceptionHandler.registerErrorHandling();

        Singletons.initializeOnce(true);
        Singletons.getControl().initialize();

        // If logged, create shared log and register shutdown hook
        if (logged) {
            sharedLog = Collections.synchronizedList(new ArrayList<>());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> saveLog(sharedLog)));
        }

        final String dA = deckPathA;
        final String dB = deckPathB;
        final boolean isLogged = logged;

        SwingUtilities.invokeLater(() -> {
            try {
                startGame(dA, dB, isLogged);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void startGame(String deckPathA, String deckPathB, boolean logged) {
        Deck deckA = DeckSerializer.fromFile(new File(deckPathA));
        Deck deckB = DeckSerializer.fromFile(new File(deckPathB));

        if (deckA == null || deckB == null) {
            throw new RuntimeException("Failed to load decks");
        }

        // Both players are human, sharing the same GUI
        IGuiGame gui = GuiBase.getInterface().getNewGuiGame();

        RegisteredPlayer rpA = new RegisteredPlayer(deckA);
        RegisteredPlayer rpB = new RegisteredPlayer(deckB);

        if (logged) {
            // P1 = logged human (GUI), P2 = stock Forge AI
            LoggingLobbyPlayer player1 = new LoggingLobbyPlayer("Human", sharedLog, 0);
            rpA.setPlayer(player1);
            rpB.setPlayer(GamePlayerUtil.createAiPlayer("AI Opponent"));
        } else {
            rpA.setPlayer(GamePlayerUtil.getGuiPlayer());
            rpB.setPlayer(new forge.player.LobbyPlayerHuman("Player 2"));
        }

        List<RegisteredPlayer> players = new ArrayList<>();
        players.add(rpA);
        players.add(rpB);

        // Map human player(s) to the GUI
        Map<RegisteredPlayer, IGuiGame> guis = new HashMap<>();
        guis.put(rpA, gui);
        if (!logged) {
            guis.put(rpB, gui); // hotseat: both share GUI
        }

        HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Constructed, null, players, guis);

        System.out.println("Game started: " + deckA.getName()
                + " vs " + deckB.getName());
        if (logged) {
            System.out.println("RL action logging ENABLED (Human vs AI) — decisions saved on exit.");
        } else {
            System.out.println("Hotseat mode (both human). No logging.");
        }
    }

    private static void saveLog(List<Map<String, Object>> log) {
        if (log == null || log.isEmpty()) {
            System.out.println("No decisions logged — skipping save.");
            return;
        }

        // Save to plays/ directory (relative to working dir)
        File playsDir = new File("plays");
        if (!playsDir.exists()) {
            playsDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File outFile = new File(playsDir, "gui_game_" + timestamp + ".json");

        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write(toJson(log));
            System.out.println("Saved " + log.size() + " decisions to " + outFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Minimal JSON serializer (no external dependency needed)
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        toJson(obj, sb, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void toJson(Object obj, StringBuilder sb, int indent) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String) {
            sb.append('"');
            escapeJson((String) obj, sb);
            sb.append('"');
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj);
        } else if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                indent(sb, indent + 1);
                sb.append('"');
                escapeJson(entry.getKey(), sb);
                sb.append("\": ");
                toJson(entry.getValue(), sb, indent + 1);
                if (++i < map.size()) sb.append(',');
                sb.append('\n');
            }
            indent(sb, indent);
            sb.append('}');
        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(sb, indent + 1);
                toJson(list.get(i), sb, indent + 1);
                if (i < list.size() - 1) sb.append(',');
                sb.append('\n');
            }
            indent(sb, indent);
            sb.append(']');
        } else {
            sb.append('"');
            escapeJson(obj.toString(), sb);
            sb.append('"');
        }
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) sb.append("  ");
    }

    private static void escapeJson(String s, StringBuilder sb) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
    }
}
