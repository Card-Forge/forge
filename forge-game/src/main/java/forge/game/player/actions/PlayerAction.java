package forge.game.player.actions;

import forge.game.GameEntityView;
import forge.game.player.PlayerController;

public abstract class PlayerAction {
    protected String name;
    protected GameEntityView gameEntityView = null;

    public PlayerAction(GameEntityView cardView) {
        gameEntityView = cardView;
    }

    public void run(PlayerController controller) {
        // Turn this abstract soon
        // This should try to replicate the recorded macro action
    }

    public GameEntityView getGameEntityView() {
        return gameEntityView;
    }
}
