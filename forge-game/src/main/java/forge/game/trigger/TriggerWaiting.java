package forge.game.trigger;

import java.util.Map;

/** 
 * TriggerWaiting is just a small object to keep track of things that occurred that need to be run.
 */
public class TriggerWaiting {
    private TriggerType mode;
    private Map<String, Object> params;

    public TriggerWaiting(TriggerType m,  Map<String, Object> p) {
        mode = m;
        params = p;
    }

    public TriggerType getMode() {
        return mode;
    }

    public Map<String, Object> getParams() {
        return params;
    }
    

    @Override
    public String toString() {
        return String.format("Waiting trigger: %s with %s", mode, params);
    }
}
