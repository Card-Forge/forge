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
    private List<Trigger> triggers = null;
    private Map<Trigger, Player> controllers = null;

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

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(final List<Trigger> triggers) {
        this.triggers = triggers;
        if (!triggers.isEmpty()) {
            controllers = Maps.newHashMap();
            for (Trigger t : triggers) {
                controllers.put(t, t.getHostCard().getController());
            }
        }
    }

    public Player getController(Trigger t) {
        if (controllers == null) {
            return null;
        }
        return controllers.get(t);
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace("Waiting trigger:", mode.toString(),"with", params.toString());
    }
}
