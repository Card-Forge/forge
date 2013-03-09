package forge.game;

import forge.error.BugReporter;
import forge.gui.match.controllers.CMessage;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public final class GameInputUpdatesThread extends Thread {
    private final MatchController match;
    private final GameState game;
    private boolean wasChangedRecently;

    /**
     * TODO: Write javadoc for Constructor.
     * @param match
     * @param game
     */
    public GameInputUpdatesThread(MatchController match, GameState game) {
        this.match = match;
        this.game = game;
    }

    public void run(){
        while(!game.isGameOver()) {
            boolean needsNewInput = CMessage.SINGLETON_INSTANCE.getInputControl().isValid() == false;
            if ( needsNewInput ) {
                match.getInput().setNewInput(game);
                wasChangedRecently = true;
            }
            try {
                Thread.sleep(wasChangedRecently ? 2 : 40);
                wasChangedRecently = false;
            } catch (InterruptedException e) {
                BugReporter.reportException(e);
                break;
            }
        }
    }
}