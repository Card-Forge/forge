package forge.game.event;

public class GameEventCardTapped extends GameEvent {
    public final boolean Tapped;

    public GameEventCardTapped(boolean tapped) {
        Tapped = tapped;
    }
}
