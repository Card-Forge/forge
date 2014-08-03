package forge.game.io;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import forge.game.Game;
import forge.util.FileUtil;

public class GameStateDeserializer {
    private int index;
    private final String[] pieces;

    public static void loadGameState(Game game, String filename) {
        
    }

    private GameStateDeserializer(String filename) {
        pieces = FileUtil.readFileToString(filename).split(String.valueOf(GameStateSerializer.DELIMITER));
    }

    public String readString() {
        return pieces[index++];
    }
    public int readInt() {
        return Integer.parseInt(readString());
    }
    public long readLong() {
        return Long.parseLong(readString());
    }
    public float readFloat() {
        return Float.parseFloat(readString());
    }
    public double readDouble() {
        return Double.parseDouble(readString());
    }
    public Object deserialize() {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(readString().getBytes()));
            Object obj = ois.readObject();
            ois.close();
            return obj;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
