package forge.game.trigger;

import java.util.List;
import java.util.Map;

/** 
 * TriggerWaiting is just a small object to keep track of things that occurred that need to be run.
 */
public class TriggerWaiting {
    private TriggerType mode;
    private Map<String, Object> params;
    private List<Trigger> triggers = null;

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
    

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(final List<Trigger> triggers) {
        this.triggers = triggers;
    }

	@Override
    public String toString() {
        return String.format("Waiting trigger: %s with %s", mode, params);
    }
}
