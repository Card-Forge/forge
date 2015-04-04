package forge.match;

public enum MatchConstants {
    ALWAYSACCEPT  ("Always accept this trigger"),
    ALWAYSDECLINE ("Always decline this trigger"),
    ALWAYSASK     ("Always ask"),
    HUMANCOMMAND  ("Player's Command zone", "Command: ", "Player - View Command"),
    HUMANEXILED   ("Player's Exile", "Exile:", "Player - View Exile"),
    HUMANFLASHBACK("Play card with Flashback", "Flashback:", "Player - View Cards with Flashback"),
    HUMANGRAVEYARD("Player's Graveyard", "Graveyard:", "Player - View Graveyard"),
    HUMANHAND     ("Player's Hand", "Hand:", "Player - View Hand"),
    HUMANLIBRARY  ("Player's Library", "Library:", "Player - View Library");
    
    public final String title;
    public final String button;
    public final String menu;
    
    private MatchConstants(String title0) {
        title  = title0;
        button = title0;
        menu   = title0;
    }
    private MatchConstants(String title0, String button0, String menu0) {
        title  = title0;
        button = button0;
        menu   = menu0;
    }
}
