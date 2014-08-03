package forge.game.io;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;

public class GameStateSerializer {
    public final static char DELIMITER = (char)5;

    private final Map<Integer, Card> cards = new HashMap<Integer, Card>();
    private final List<Player> players = new ArrayList<Player>();
    private final BufferedWriter bw;

    public static void saveGameState(Game game, String filename) {
        GameStateSerializer serializer = null;
        try {
            serializer = new GameStateSerializer(filename);
            game.saveState(serializer);
            serializer.writeEndOfFile();
            serializer.bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GameStateSerializer(String filename) throws IOException {
        bw = new BufferedWriter(new FileWriter(filename));
    }

    private void writeEndOfFile() {
        
    }

    public void write(String value) {
        try {
            bw.write(value + DELIMITER);
        }
        catch (Exception e) {}
    }
    public void write(IGameStateObject value) {
        if (value == null) { return; }
        write(value.getClass().getName()); //needed so state object can be initialized
        value.saveState(this);
    }
    public void write(Card card) {
        if (card == null) { return; }

        int key = card.getUniqueNumber();
        cards.put(key, card);
        write(key); //only write info for each card once at end of file
    }
    public void write(Player player) {
        if (player == null) { return; }

        int key = players.indexOf(player);
        if (key == -1) {
            key = players.size();
            players.add(player);
        }
        write(key); //only write info for each player once at end of file
    }
    public void write(boolean value) {
        write(value ? "1" : "0");
    }
    public void write(int value) {
        write(String.valueOf(value));
    }
    public void write(long value) {
        write(String.valueOf(value));
    }
    public void write(float value) {
        write(String.valueOf(value));
    }
    public void write(double value) {
        write(String.valueOf(value));
    }
    public void writeList(Collection<? extends IGameStateObject> value) {
        if (value == null) { return; }
        write(value.size()); //must write size first
        for (IGameStateObject obj : value) {
            write(obj);
        }
    }
    public void writeCardList(Collection<Card> value) {
        if (value == null) { return; }
        write(value.size()); //must write size first
        for (Card card : value) {
            write(card);
        }
    }
    public void writePlayerList(Collection<Player> value) {
        if (value == null) { return; }
        write(value.size()); //must write size first
        for (Player player : value) {
            write(player);
        }
    }
    public void serialize(Serializable obj) {
        String str;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            str = baos.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            str = ""; //ensure placeholder include in output
        }
        write(str);
    }
}
