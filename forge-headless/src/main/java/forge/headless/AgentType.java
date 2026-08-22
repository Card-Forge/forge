package forge.headless;

/**
 * Defines the types of agents that can control a player in TUI mode.
 */
public enum AgentType {
    /**
     * Interactive text UI - reads choices from stdin
     */
    TUI,

    /**
     * AI-controlled player using Forge's built-in AI
     */
    AI,

    /**
     * Random agent - makes random valid choices
     */
    RANDOM,

    /**
     * Zero agent - always chooses option 0 (pass priority)
     */
    ZERO
}
