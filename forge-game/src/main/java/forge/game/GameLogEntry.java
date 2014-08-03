package forge.game;

import forge.game.io.GameStateDeserializer;
import forge.game.io.GameStateSerializer;
import forge.game.io.IGameStateObject;

public class GameLogEntry implements IGameStateObject {
    public final String message;
    public final GameLogEntryType type;
    // might add here date and some other fields

    GameLogEntry(final GameLogEntryType type0, final String messageIn) {
        type = type0;
        message = messageIn;
    }
    
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return type.getCaption() + ": " + message;
    }

    @Override
    public void loadState(GameStateDeserializer gsd) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void saveState(GameStateSerializer gss) {
        gss.write(type.name());
        gss.write(message);
    }
}