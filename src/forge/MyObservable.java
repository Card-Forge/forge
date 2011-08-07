package forge;

import java.util.Observable;

/**
 * <p>MyObservable class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class MyObservable extends Observable {
    /**
     * <p>updateObservers.</p>
     */
    public final void updateObservers() {
        this.setChanged();
        this.notifyObservers();

        if (AllZone.getPhase() != null && AllZone.getPhase().isNeedToNextPhase()) {
            if (AllZone.getPhase().isNeedToNextPhaseInit()) {
                // this is used.
                AllZone.getPhase().setNeedToNextPhase(false);
                AllZone.getPhase().nextPhase();
            }
        }
    }
}

