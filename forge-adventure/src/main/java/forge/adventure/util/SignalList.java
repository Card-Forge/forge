package forge.adventure.util;

import com.badlogic.gdx.utils.Array;

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
