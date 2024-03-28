package forge.game;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.List;

public interface IEntityMap {
    public abstract Game getGame();

    public abstract GameObject map(GameObject o);

    public default Player map(final Player p) {
        return (Player) map((GameObject) p);
    }

    public default Card map(final Card c) {
        return (Card) map((GameObject) c);
    }

    public default GameEntity map(final GameEntity e) {
        return (GameEntity) map((GameObject) e);
    }

    public default CardCollection mapCollection(final CardCollectionView cards) {
        final CardCollection collection = new CardCollection();
        for (final Card c : cards) {
            collection.add(map(c));
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    public default <T extends GameObject> List<T> mapList(final List<T> objects) {
        final List<T> result = new ArrayList<>();
        for (final T o : objects) {
            result.add((T) map(o));
        }
        return result;
    }

}
