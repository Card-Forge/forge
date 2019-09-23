package forge.limited;

public enum LimitedPoolType { 
    Full("Full Cardpool"),
    Block("Block / Set"),
    FantasyBlock("Fantasy Block"),
    Custom("Custom Cube"),
    Chaos("Chaos Draft");
    
    private final String displayName;
    LimitedPoolType(String name) {
        displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}