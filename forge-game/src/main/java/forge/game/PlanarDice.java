package forge.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import forge.game.ability.AbilityKey;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.trigger.TriggerType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Represents the planar dice for Planechase games.
 *
 */
public enum PlanarDice {
    Planeswalk,
    Chaos,
    Blank;

    public static PlanarDice roll(Player roller, PlanarDice riggedResult) {
        final Game game = roller.getGame();
        int rolls = 1;
        int ignore = 0;

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(roller);
        repParams.put(AbilityKey.Number, rolls);
        repParams.put(AbilityKey.Ignore, ignore);

        switch (game.getReplacementHandler().run(ReplacementType.RollPlanarDice, repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                rolls = (int) repParams.get(AbilityKey.Number);
                ignore = (int) repParams.get(AbilityKey.Ignore);
                break;
            }
        }

        List<PlanarDice> results = Lists.newArrayList();
        for (int r = 0; r < rolls; r++) {
            PlanarDice thisRoll = Blank;
            int i = forge.util.MyRandom.getRandom().nextInt(6);
            roller.roll();
            if (riggedResult != null)
                thisRoll = riggedResult;
            else if (i == 0)
                thisRoll = Planeswalk;
            else if (i == 1)
                thisRoll = Chaos;
            results.add(thisRoll);
        }

        for (int ig = 0; ig < ignore; ig++) {
            results.remove(roller.getController().choosePDRollToIgnore(results));
        }
        PlanarDice res = results.get(0);

        PlanarDice trigRes = res;

        final Map<AbilityKey, Object> resRepParams = AbilityKey.mapFromAffected(roller);
        resRepParams.put(AbilityKey.Result, res);

        switch (game.getReplacementHandler().run(ReplacementType.PlanarDiceResult, resRepParams)) {
            case NotReplaced:
                break;
            case Updated: {
                trigRes = (PlanarDice) resRepParams.get(AbilityKey.Result);
                break;
            }
        }

        Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(roller);
        runParams.put(AbilityKey.Result, trigRes);
        game.getTriggerHandler().runTrigger(TriggerType.PlanarDice, runParams,false);

        // Also run normal RolledDie and RolledDieOnce triggers
        for (int r = 0; r < rolls; r++) {
            runParams = AbilityKey.mapFromPlayer(roller);
            runParams.put(AbilityKey.Sides, 6);
            runParams.put(AbilityKey.Result, 0);
            roller.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);
        }

        runParams = AbilityKey.mapFromPlayer(roller);
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
