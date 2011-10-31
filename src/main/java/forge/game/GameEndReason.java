package forge.game;

/**
 * The Enum GameEndReason.
 */
public enum GameEndReason {
    /** The All opponents lost. */
    AllOpponentsLost,
    // Noone won
    /** The Draw. */
    Draw, // Having little idea how they can reach a draw, so I didn't enumerate
          // possible reasons here
    // Special conditions, they force one player to win and thus end the game

    /** The Wins game spell effect. */
 WinsGameSpellEffect // ones that could be both hardcoded (felidar) and
                        // scripted ( such as Mayael's Aria )
}
