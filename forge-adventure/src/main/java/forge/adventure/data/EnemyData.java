package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.adventure.util.Res;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;

import java.io.File;

public class EnemyData {
    public String name;
    public String sprite;
    public String deck;
    public float spawnRate;
    public float difficulty;
    public float speed;
    public int life;
    public Array<RewardData> rewards;

    private Deck deckObj;
    public Deck getDeck() {
        if(deckObj==null)
            deckObj= DeckSerializer.fromFile(new File(Res.CurrentRes.GetFilePath(deck)));
        return deckObj;
    }
}
