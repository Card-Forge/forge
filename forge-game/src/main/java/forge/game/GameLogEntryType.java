package forge.game;

public enum GameLogEntryType {
    GAME_OUTCOME("Game outcome"),
    MATCH_RESULTS("Match result"),
    TURN("Turn"),
    MULLIGAN("Mulligan"),
    ANTE("Ante"),
    ZONE_CHANGE("Zone Change"),
    PLAYER_CONROL("Player contol"),
    COMBAT("Combat"),
    DISCARD("Discard"),
    INFORMATION("Information"),
    EFFECT_REPLACED("Replacement Effect"),
    LAND("Land"),
    STACK_RESOLVE("Resolve stack"),
    STACK_ADD("Add to stack"),
    DAMAGE("Damage"),
    MANA("Mana"),
    PHASE("Phase");
    
    private final String caption; 
    private GameLogEntryType(String name) {
        this.caption = name;
    }

    public String getCaption() {
        return caption;
    }

}