package forge;

public class GameLogEntry {
    public final String type;
    public final String message;
    public final GameLogLevel level;

    GameLogEntry(final String typeIn, final String messageIn, final GameLogLevel levelIn) {
        type = typeIn;
        message = messageIn;
        level = levelIn;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return type + ": " + message;
    }
}