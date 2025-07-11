package forge.game;

public enum GameLogEntryType {
    GAME_OUTCOME("Game outcome"),
    MATCH_RESULTS("Match result"),
    TURN("Turn"),
    MULLIGAN("Mulligan"),
    ANTE("Ante"),
    ZONE_CHANGE("Zone Change"),
    PLAYER_CONTROL("Player control"),
    DAMAGE("Damage"),
    // Where's life loss?
    LAND("Land"),
    DISCARD("Discard"),
    COMBAT("Combat"),
    INFORMATION("Information"),
    EFFECT_REPLACED("Replacement Effect"),
    STACK_RESOLVE("Resolve stack"),
    STACK_ADD("Add to stack"),
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