package forge.control.input;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputAutoPassPriority extends InputNonSyncBase {
    private static final long serialVersionUID = -7520803307255234647L;

    public InputAutoPassPriority(Player player) {
        super(player);
    }

    @Override
    public void showMessage() {
        passPriority();
    }

}
