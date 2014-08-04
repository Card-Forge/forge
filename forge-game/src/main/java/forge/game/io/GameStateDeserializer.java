package forge.game.io;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Collection;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.item.PaperCard;
import forge.util.FileUtil;

public class GameStateDeserializer {
    private int index;
    private final String[] pieces;

    public static void loadGameState(Game game, String filename) {
        GameStateDeserializer deserializer = new GameStateDeserializer(filename);
        deserializer.readObject(game);
    }

    private GameStateDeserializer(String filename) {
        pieces = FileUtil.readFileToString(filename).split(String.valueOf(GameStateSerializer.DELIMITER));
    }

    public String readString() {
        return pieces[index++];
    }
    public boolean readBoolean() {
        return readString().equals("1");
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
    public IGameStateObject readObject() {
        IGameStateObject obj = null;
        try {
            obj = (IGameStateObject)Class.forName(readString()).newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (obj != null) {
            obj.loadState(this);
        }
        return obj;
    }
    public void readObject(IGameStateObject obj) { //read into existing object
        if (obj != null && obj.getClass().getName().equals(readString())) {
            obj.loadState(this);
        }
    }
    public Card readCard() {
        return null;
    }
    public PaperCard readPaperCard() {
        return null;
    }
    public Player readPlayer() {
        return null;
    }
    public void readCardList(Collection<Card> value) {
        if (value == null) { return; }
        value.clear();
        int count = readInt();
        for (int i = 0; i < count; i++) {
            value.add(readCard());
        }
    }
    public void readPaperCardList(Collection<PaperCard> value) {
        if (value == null) { return; }
        value.clear();
        int count = readInt();
        for (int i = 0; i < count; i++) {
            value.add(readPaperCard());
        }
    }
    public void readPlayerList(Collection<Player> value) {
        if (value == null) { return; }
        value.clear();
        int count = readInt();
        for (int i = 0; i < count; i++) {
            value.add(readPlayer());
        }
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
