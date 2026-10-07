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
    // only the ones that trigger additional times
    private Map<Trigger, Integer> additionalTriggers;

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
        // keySet() drives simultaneous-trigger stacking, so preserve collection order
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

    /**
     * How many additional times the trigger triggers (e.g. because of Panharmonicon).
     * Like the controller this is determined when the triggers are collected,
     * because it must not be affected by anything that happens before they are put on the stack
     */
    public int getAdditionalTriggers(Trigger t) {
        if (additionalTriggers == null) {
            return 0;
        }
        return additionalTriggers.getOrDefault(t, 0);
    }

    public void setAdditionalTriggers(Trigger t, int amount) {
        if (amount > 0) {
            if (additionalTriggers == null) {
                additionalTriggers = Maps.newHashMap();
            }
            additionalTriggers.put(t, amount);
        }
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace("Waiting trigger:", mode.toString(),"with", params.toString());
    }
}
