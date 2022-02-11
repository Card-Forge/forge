package forge.adventure.util;

import com.badlogic.gdx.utils.Array;

/**
 * List of function points to inform all listeners, maybe redesign to a more java like approach
 */
public class SignalList extends Array<Runnable> {
    public void emit() {

        for(Runnable  signal:this) {
            try {
                signal.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
