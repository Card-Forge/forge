package forge.game;

public enum GameLogEntryType {
    GAME_OUTCOME("Game Outcome"),
    MATCH_RESULTS("Match Result"),
    TURN("Turn"),
    MULLIGAN("Mulligan"),
    ANTE("Ante"),
    DRAFT("Draft"),
    ZONE_CHANGE("Zone Change"),
    PLAYER_CONTROL("Player Control"),
    DAMAGE("Damage"),
    // Where's life loss?
    LAND("Land"),
    DISCARD("Discard"),
    COMBAT("Combat"),
    INFORMATION("Information"),
    STACK_RESOLVE("Resolve Stack"),
    STACK_ADD("Add To Stack"),
    EFFECT_REPLACED("Replacement Effect"),
    MANA("Mana"),
    PHASE("Phase");
    
    private final String caption; 
    GameLogEntryType(String name) {
        this.caption = name;
    }

    public String getCaption() {
        return caption;
    }

}