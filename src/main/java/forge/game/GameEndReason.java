package forge.game;

public enum GameEndReason {
    AllOpponentsLost,
    // Noone won
    Draw,       // Having little idea how they can reach a draw, so I didn't enumerate possible reasons here
    // Special conditions, they force one player to win and thus end the game

    WinsGameSpellEffect // ones that could be both hardcoded (felidar) and scripted ( such as Mayael's Aria )
}
