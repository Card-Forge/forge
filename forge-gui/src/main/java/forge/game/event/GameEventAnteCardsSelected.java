package forge.game.event;

import org.apache.commons.lang3.tuple.Pair;

import forge.Card;
import forge.game.player.Player;

public class GameEventAnteCardsSelected extends GameEvent {
    public final Iterable<Pair<Player,Card>> cards;
    public GameEventAnteCardsSelected(Iterable<Pair<Player,Card>> cardz) {
        cards = cardz;
    }
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}