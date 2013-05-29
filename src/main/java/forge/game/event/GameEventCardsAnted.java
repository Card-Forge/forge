package forge.game.event;

import org.apache.commons.lang3.tuple.Pair;

import forge.Card;
import forge.game.player.Player;

public class GameEventCardsAnted extends GameEvent {
    public final Iterable<Pair<Player,Card>> cards;
    public GameEventCardsAnted(Iterable<Pair<Player,Card>> cardz) {
        cards = cardz;
    }
    
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}