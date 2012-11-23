package forge.game.event;

public class SetTappedEvent extends Event {
    public final boolean Tapped;
    
    public SetTappedEvent(boolean tapped) {
        Tapped = tapped;
    }
}
