package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventRollDie;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Localizer;
import forge.util.MyRandom;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class RollDiceEffect extends SpellAbilityEffect {

    public static String makeFormatedDescription(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        final String key = "ResultSubAbilities";
        if (sa.hasParam(key)) {
            String [] diceAbilities = sa.getParam(key).split(",");
            for (String ab : diceAbilities) {
                String [] kv = ab.split(":");
                String desc = sa.getAdditionalAbility(kv[0]).getDescription();
                if (!desc.isEmpty()) {
                    sb.append("\n").append(desc);
                }
            }
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final PlayerCollection player = getTargetPlayers(sa);

        StringBuilder stringBuilder = new StringBuilder();
        if (player.size() == 1 && player.get(0).equals(sa.getActivatingPlayer())) {
            stringBuilder.append("Roll ");
        } else {
            stringBuilder.append(player).append(" rolls ");
        }
        stringBuilder.append(sa.getParamOrDefault("Amount", "a")).append(" d");
        stringBuilder.append(sa.getParamOrDefault("Sides", "6"));
        if (sa.hasParam("IgnoreLower")) {
            stringBuilder.append(" and ignore the lower roll");
        }
        stringBuilder.append(".");
        return stringBuilder.toString();
    }

    public static int rollDiceForPlayer(SpellAbility sa, Player player, int amount, int sides) {
        return rollDiceForPlayer(sa, player, amount, sides, 0, 0, null);
    }
    private static int rollDiceForPlayer(SpellAbility sa, Player player, int amount, int sides, int ignore, int modifier, List<Integer> rollsResult) {
        Map<Player, Integer> ignoreChosenMap = Maps.newHashMap();

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(player);
        repParams.put(AbilityKey.Number, amount);
        repParams.put(AbilityKey.Ignore, ignore);
        repParams.put(AbilityKey.IgnoreChosen, ignoreChosenMap);

        switch (player.getGame().getReplacementHandler().run(ReplacementType.RollDice, repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                amount = (int) repParams.get(AbilityKey.Number);
                ignore = (int) repParams.get(AbilityKey.Ignore);
                ignoreChosenMap = (Map<Player, Integer>) repParams.get(AbilityKey.IgnoreChosen);
                break;
            }
        }

        if (amount == 0) {
            return 0;
        }
        int total = 0;
        int countMaxRolls = 0;
        List<Integer> naturalRolls = (rollsResult == null ? new ArrayList<>() : rollsResult);

        for (int i = 0; i < amount; i++) {
            int roll = MyRandom.getRandom().nextInt(sides) + 1;
            // Play the die roll sound
            player.getGame().fireEvent(new GameEventRollDie());
            player.roll();
            naturalRolls.add(roll);
            total += roll;
        }

        naturalRolls.sort(null);

        List<Integer> ignored = new ArrayList<>();
        // Ignore lowest rolls
        if (ignore > 0) {
            for (int i = ignore - 1; i >= 0; --i) {
                total -= naturalRolls.get(i);
                ignored.add(naturalRolls.get(i));
                naturalRolls.remove(i);
            }
        }
        // Player chooses to ignore rolls
        for (Player chooser : ignoreChosenMap.keySet()) {
            for (int ig = 0; ig < ignoreChosenMap.get(chooser); ig++) {
                Integer ign = chooser.getController().chooseRollToIgnore(naturalRolls);
                total -= ign;
                ignored.add(ign);
                naturalRolls.remove(ign);
            }
        }

        //Notify of results
        if (amount > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(Localizer.getInstance().getMessage("lblPlayerRolledResult", player,
                    StringUtils.join(naturalRolls, ", ")));
            if (!ignored.isEmpty()) {
                sb.append("\r\n").append(Localizer.getInstance().getMessage("lblIgnoredRolls",
                        StringUtils.join(ignored, ", ")));
            }
            player.getGame().getAction().notifyOfValue(sa, player, sb.toString(), null);
        }

        List<Integer> rolls = Lists.newArrayList();
        int oddResults = 0;
        int evenResults = 0;
        int differentResults = 0;
        for (Integer i : naturalRolls) {
            final int modifiedRoll = i + modifier;
            if (!rolls.contains(modifiedRoll)) {
                differentResults++;
            }
            rolls.add(modifiedRoll);
            if (modifiedRoll % 2 == 0) {
                evenResults++;
            } else {
                oddResults++;
            }
            if (i == sides) {
                countMaxRolls++;
            }
        }
        if (sa.hasParam("EvenOddResults")) {
            sa.setSVar("EvenResults", Integer.toString(evenResults));
            sa.setSVar("OddResults", Integer.toString(oddResults));
        }
        if (sa.hasParam("DifferentResults")) {
            sa.setSVar("DifferentResults", Integer.toString(differentResults));
        }
        if (sa.hasParam("MaxRollsResults")) {
            sa.setSVar("MaxRolls", Integer.toString(countMaxRolls));
        }
        total += modifier;

        // Run triggers
        int rollNum = 1;
        for (Integer roll : rolls) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
            runParams.put(AbilityKey.Sides, sides);
            runParams.put(AbilityKey.Modifier, modifier);
            runParams.put(AbilityKey.Result, roll);
            runParams.put(AbilityKey.Number, player.getNumRollsThisTurn() - amount + rollNum);
            player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);
            rollNum++;
        }
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
        runParams.put(AbilityKey.Sides, sides);
        runParams.put(AbilityKey.Result, rolls);
        player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDieOnce, runParams, false);

        return total;
    }

    private static void resolveSub(SpellAbility sa, int num) {
        Map<String, SpellAbility> diceAbilities = sa.getAdditionalAbilities();
        SpellAbility resultAbility = null;
        for (Map.Entry<String, SpellAbility> e : diceAbilities.entrySet()) {
            String diceKey = e.getKey();
            if (diceKey.contains("-")) {
                String[] ranges = diceKey.split("-");
                if (Integer.parseInt(ranges[0]) <= num && Integer.parseInt(ranges[1]) >= num) {
                    resultAbility = e.getValue();
                    break;
                }
            } else if (StringUtils.isNumeric(diceKey) && Integer.parseInt(diceKey) == num) {
                resultAbility = e.getValue();
                break;
            }
        }
        if (resultAbility != null) {
            AbilityUtils.resolve(resultAbility);

        } else if (sa.hasAdditionalAbility("Else")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("Else"));
        }
    }

    private int rollDice(SpellAbility sa, Player player, int amount, int sides) {
        final Card host = sa.getHostCard();
        final int modifier = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Modifier", "0"), sa);
        final int ignore = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("IgnoreLower", "0"), sa);

        List<Integer> rolls = new ArrayList<>();
        int total = rollDiceForPlayer(sa, player, amount, sides, ignore, modifier, rolls);

        if (sa.hasParam("StoreResults")) {
            host.addStoredRolls(rolls);
        }
        if (sa.hasParam("ResultSVar")) {
            sa.setSVar(sa.getParam("ResultSVar"), Integer.toString(total));
        }
        if (sa.hasParam("ChosenSVar")) {
            int chosen = player.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblChooseAResult"), rolls, player);
            String message = Localizer.getInstance().getMessage("lblPlayerChooseValue", player, chosen);
            player.getGame().getAction().notifyOfValue(sa, player, message, player);
            sa.setSVar(sa.getParam("ChosenSVar"), Integer.toString(chosen));
            if (sa.hasParam("OtherSVar")) {
                int other = rolls.get(0);
                for (int i = 1; i < rolls.size(); ++i) {
                    if (rolls.get(i) != chosen) {
                        other = rolls.get(i);
                        break;
                    }
                }
                sa.setSVar(sa.getParam("OtherSVar"), Integer.toString(other));
            }
        }
        if (sa.hasParam("UseHighestRoll")) {
            total = Collections.max(rolls);
        }

        if (sa.hasParam("SubsForEach")) {
            for (Integer roll : rolls) {
                resolveSub(sa, roll);
            }
        } else {
            resolveSub(sa, total);
        }

        if (sa.hasParam("NoteDoubles")) {
            Set<Integer> unique = new HashSet<>();
            for (Integer roll : rolls) {
                if (!unique.add(roll)) {
                    sa.setSVar("Doubles", "1");
                }
            }
        }

        return total;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "1"), sa);
        int sides = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Sides", "6"), sa);
        boolean rememberHighest = sa.hasParam("RememberHighestPlayer");

        final PlayerCollection playersToRoll = getTargetPlayers(sa);
        List<Integer> results = new ArrayList<>(playersToRoll.size());

        for (Player player : playersToRoll) {
            if (sa.hasParam("RerollResults")) {
                rerollDice(sa, host, player, sides);
            } else {
                int result = rollDice(sa, player, amount, sides);
                results.add(result);
            }
        }
        if (rememberHighest) {
            int highest = 0;
            for (int i = 0; i < results.size(); ++i) {
                if (highest < results.get(i)) {
                    highest = results.get(i);
                }
            }
            for (int i = 0; i < results.size(); ++i) {
                if (highest == results.get(i)) {
                    host.addRemembered(playersToRoll.get(i));
                }
            }
        }
    }

    private void rerollDice(SpellAbility sa, Card host, Player roller, int sides) {
        List<Integer> toReroll = Lists.newArrayList();

        for (Integer storedResult : host.getStoredRolls()) {
            if (roller.getController().confirmAction(sa, null,
                    Localizer.getInstance().getMessage("lblRerollResult", storedResult), null)) {
                toReroll.add(storedResult);
            }
        }

        Map<Integer, Integer> replaceMap = Maps.newHashMap();
        for (Integer old : toReroll) {
            int newRoll = rollDice(sa, roller, 1, sides);
            replaceMap.put(old, newRoll);
        }
        host.replaceStoredRoll(replaceMap);
    }
}
