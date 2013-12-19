package forge.limited;

public enum LimitedPoolType { 
    Full("Full Cardpool"),
    Block("Block / Set"),
    FantasyBlock("Fantasy Block"),
    Custom("Custom");
    
    private final String displayName;
    private LimitedPoolType(String name) {
        displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}