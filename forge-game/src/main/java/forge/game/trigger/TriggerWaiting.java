package forge.game.trigger;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.ability.AbilityKey;
import forge.game.player.Player;
import forge.util.TextUtil;

/** 
 * TriggerWaiting is just a small object to keep track of things that occurred that need to be run.
 */
public class TriggerWaiting {
    private TriggerType mode;
    private Map<AbilityKey, Object> params;
    private Map<Trigger, Player> triggers;

    public TriggerWaiting(TriggerType m, Map<AbilityKey, Object> p) {
        mode = m;
        params = p;
    }

    public TriggerType getMode() {
        return mode;
    }

    public Map<AbilityKey, Object> getParams() {
        return params;
    }

    public Iterable<Trigger> getTriggers() {
        if (triggers == null) {
            return null;
        }
        return triggers.keySet();
    }

    public void setTriggers(final List<Trigger> trigs) {
        // Preserve the order of trigs: getTriggers() feeds the order simultaneous triggers are put
        // on the stack, and a plain HashMap would iterate in hashCode-bucket order, which varies
        // per JVM run (Trigger.hashCode folds in the identity hash of the Trigger class object).
        // LinkedHashMap keeps the deterministic zone-walk order from getActiveTrigger.
        this.triggers = Maps.newLinkedHashMap();
        for (Trigger t : trigs) {
            triggers.put(t, t.getHostCard().getController());
        }
    }

    public Player getController(Trigger t) {
        if (triggers == null) {
            return null;
        }
        return triggers.get(t);
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace("Waiting trigger:", mode.toString(),"with", params.toString());
    }
}
