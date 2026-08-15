package forge.headless;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command-line interface definition for the Text UI mode using picocli.
 */
@Command(name = "tui",
         description = "Text UI Mode - Interactive Forge Gameplay",
         mixinStandardHelpOptions = true,
         version = "Forge TUI 2.0")
public class TUICommand implements Runnable {

    @Parameters(index = "0",
                description = "Deck file (.dck) or deck name. " +
                             "Paths can be absolute or relative to current directory.")
    String deck1;

    @Parameters(index = "1",
                arity = "0..1",
                description = "(Optional) Deck for player 2. If omitted, same deck as player 1.")
    String deck2;

    @Option(names = {"-f", "--format"},
            description = "Game format (default: Constructed)")
    String gameType = "Constructed";

    @Option(names = {"--p1", "--p1-agent"},
            description = "Player 1 agent type: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
            converter = CaseInsensitiveAgentTypeConverter.class)
    AgentType player1Agent = AgentType.TUI;

    @Option(names = {"--p2", "--p2-agent"},
            description = "Player 2 agent type: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
            converter = CaseInsensitiveAgentTypeConverter.class)
    AgentType player2Agent = AgentType.AI;

    @Option(names = "--askmana",
            description = "Prompt for mana abilities (default: ${DEFAULT-VALUE})")
    boolean askMana = false;

    @Option(names = "--numeric-choices",
            description = "Use numeric-only input (no text commands)")
    boolean numericChoices = false;

    @Option(names = "--seed",
            description = "Set random seed for deterministic testing")
    Long seed;

    @Option(names = "--start-state",
            description = "Load game state from .pzl file")
    String startStatePath;

    // Legacy support
    @Option(names = "--player2-tui",
            hidden = true,
            description = "Deprecated: use --p2=tui instead")
    boolean player2Tui = false;

    @Override
    public void run() {
        // Handle legacy flag
        if (player2Tui) {
            System.out.println("Warning: --player2-tui is deprecated, use --p2=tui instead");
            player2Agent = AgentType.TUI;
        }

        // If deck2 not provided, use deck1 for both players
        if (deck2 == null || deck2.isEmpty()) {
            deck2 = deck1;
        }

        // Delegate to the actual game logic
        TextUIGame.runGame(this);
    }

    /**
     * Case-insensitive converter for AgentType enum.
     */
    static class CaseInsensitiveAgentTypeConverter implements picocli.CommandLine.ITypeConverter<AgentType> {
        @Override
        public AgentType convert(String value) throws Exception {
            return AgentType.valueOf(value.toUpperCase());
        }
    }
}
