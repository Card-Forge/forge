package forge.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;

public abstract class GameObjectMap {
    public abstract Game getGame();

    public abstract GameObject map(GameObject o);

    public Player map(Player p) {
        return (Player) map((GameObject) p);
    }
 
    public Card map(Card c) {
        return (Card) map((GameObject) c);
    }

    public GameEntity map(GameEntity e) {
        return (GameEntity) map((GameObject) e);
    }

    public CardCollectionView mapCollection(CardCollectionView cards) {
        CardCollection collection = new CardCollection();
        for (Card c : cards) {
            collection.add(map(c));
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    public <T extends GameObject> List<T> mapList(List<T> objects) {
        ArrayList<T> result = new ArrayList<T>();
        for (T o : objects) {
            result.add((T) map(o));
        }
        return result;
    }

    public <K extends GameObject, V> void fillKeyedMap(Map<K, V> dest, Map<K, V> src) {
        for (Map.Entry<K, V> entry : src.entrySet()) {
            dest.put(entry.getKey(), entry.getValue());
        }
    }
}
