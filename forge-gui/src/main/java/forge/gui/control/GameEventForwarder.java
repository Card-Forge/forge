package forge.gui.control;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.interfaces.IGuiGame;

public class GameEventForwarder {
    private final IGuiGame gui;
    public GameEventForwarder(IGuiGame gui) { this.gui = gui; }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) { gui.handleGameEvent(ev); }
}
