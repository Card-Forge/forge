package forge.game;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import forge.game.ability.AbilityKey;
import forge.game.player.Player;
import forge.game.trigger.TriggerType;

/**
 * Represents the planar dice for Planechase games.
 *
 */
public enum PlanarDice {
    Planeswalk,
    Chaos,
    Blank;

    public static PlanarDice roll(Player roller, PlanarDice riggedResult)
    {
        PlanarDice res = Blank;
        int i = forge.util.MyRandom.getRandom().nextInt(6);
        if (riggedResult != null)
            res = riggedResult;
        else if (i == 0)
            res = Planeswalk;
        else if (i == 1)
            res = Chaos;


        PlanarDice trigRes = res;

        if(roller.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.blankIsChaos)
                && res == Blank)
        {
            trigRes = Chaos;
        }

        Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, roller);
        runParams.put(AbilityKey.Result, trigRes);
        roller.getGame().getTriggerHandler().runTrigger(TriggerType.PlanarDice, runParams,false);

        // Also run normal RolledDie and RolledDieOnce triggers
        runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, roller);
        runParams.put(AbilityKey.Sides, 6);
        runParams.put(AbilityKey.Result, 0);
        roller.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);

        runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, roller);
        runParams.put(AbilityKey.Sides, 6);
        runParams.put(AbilityKey.Result, Arrays.asList(0));
        roller.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDieOnce, runParams, false);

        return res;
    }

    /**
     * Parses a string into an enum member.
     * @param string to parse
     * @return enum equivalent
     */
    public static PlanarDice smartValueOf(String value) {

        final String valToCompate = value.trim();
        for (final PlanarDice v : PlanarDice.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in PlanarDice enum");
    }

    public static final ImmutableList<PlanarDice> values = ImmutableList.copyOf(values());
}
