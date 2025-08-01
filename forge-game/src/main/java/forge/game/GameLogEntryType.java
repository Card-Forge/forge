package forge.game;

public enum GameLogEntryType {
    GAME_OUTCOME("Game outcome"),
    MATCH_RESULTS("Match result"),
    TURN("Turn"),
    MULLIGAN("Mulligan"),
    ANTE("Ante"),
    DRAFT("Draft"),
    ZONE_CHANGE("Zone Change"),
    PLAYER_CONTROL("Player control"),
    DAMAGE("Damage"),
    // Where's life loss?
    LAND("Land"),
    DISCARD("Discard"),
    COMBAT("Combat"),
    INFORMATION("Information"),
    STACK_RESOLVE("Resolve stack"),
    STACK_ADD("Add to stack"),
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