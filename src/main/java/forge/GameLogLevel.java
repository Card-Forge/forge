package forge;

public enum GameLogLevel {
    GAME_OUTCOME("Game outcome"),
    MATCH_RESULTS("Match result"),
    TURN("Turn"),
    MULLIGAN("Mulligan"),
    ANTE("Ante"),
    COMBAT("Combat"),
    EFFECT_REPLACED("ReplacementEffect"),
    LAND("Land"),
    STACK("Stack"),
    DAMAGE("Damage"),
    MANA("Mana"),
    PHASE("Phase");
    
    private final String caption; 
    private GameLogLevel(String name) {
        this.caption = name;
    }

    public String getCaption() {
        return caption;
    }
}