package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.event.GameEventRollDie;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
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

        if(sa.hasParam("ToVisitYourAttractions")) {
            if (player.size() == 1 && player.get(0).equals(sa.getActivatingPlayer()))
                return "Roll to Visit Your Attractions.";
            else
                return String.format("%s %s to visit their Attractions.", Lang.joinHomogenous(player), Lang.joinVerb(player, "roll"));
        }

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
        boolean toVisitAttractions = sa != null && sa.hasParam("ToVisitYourAttractions");
        return rollDiceForPlayer(sa, player, amount, sides, 0, 0, null, toVisitAttractions);
    }
    public static int rollDiceForPlayerToVisitAttractions(Player player) {
        return rollDiceForPlayer(null, player, 1, 6, 0, 0, null, true);
    }
    private static int rollDiceForPlayer(SpellAbility sa, Player player, int amount, int sides, int ignore, int modifier, List<Integer> rollsResult, boolean toVisitAttractions) {
        if (amount == 0) {
            return 0;
        }

        Map<Player, Integer> ignoreChosenMap = Maps.newHashMap();
        Card squirrelWhacker = null;

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(player);
        List<Integer> ignored = new ArrayList<>();
        List<Integer> naturalRolls = rollAction(amount, sides, ignore, rollsResult, ignored, ignoreChosenMap, player, repParams);

        if (sa != null && sa.hasParam("UseHighestRoll")) {
            naturalRolls.subList(0, naturalRolls.size() - 1).clear();
        }

        // Reroll Phase:

        CardCollection canRerollDice = CardLists.getKeyword(player.getCardsIn(ZoneType.Battlefield), "RerollDieRoll");

        for (Card c : canRerollDice) {
            List<Integer> diceToReroll = player.getController().chooseDiceToReroll(naturalRolls);
            if (!diceToReroll.isEmpty()) {
                SpellAbility modifierSA = c.getFirstSpellAbility();
                Cost cost = new Cost(c.getSVar("RollRerollCost"), false);
                boolean paid = player.getController().payCostToPreventEffect(cost, modifierSA, false, null); //change to payCostDuringRoll
                if (paid) {
                    naturalRolls.removeAll(diceToReroll);
                    int amountToReroll = diceToReroll.size();
                    List<Integer> rerolls = rollAction(amountToReroll, sides, 0, null, ignored, Maps.newHashMap(), player, repParams);
                    naturalRolls.addAll(rerolls);
                }
            } else {break;}
        }

        // Modification Phase:
        List<Integer> unmodifiedRolls = new ArrayList<>(naturalRolls);
        List<Integer> modifiedRolls = new ArrayList<>();
        List<Integer> finalResults = new ArrayList<>();
        Integer rollToModify;
        String xenoKeyword = "After you roll a die, you may remove a +1/+1 counter from Xenosquirrels. If you do, increase or decrease the result by 1.";
        String nightShiftKeyword = "After you roll a die, you may pay 1 life. If you do, increase or decrease the result by 1. Do this only once each turn.";
        List<Card> canIncrementDice = getIncrementCards(player, xenoKeyword, nightShiftKeyword);

        if (!canIncrementDice.isEmpty()) {
            do {
                rollToModify = player.getController().chooseRollToModify(unmodifiedRolls);
                if (rollToModify != null) {
                    canIncrementDice = getIncrementCards(player, xenoKeyword, nightShiftKeyword);
                    boolean modified = false;
                    for (Card c : canIncrementDice) {
                        String[] parts = c.getSVar("ModsThisTurn").split("\\$");
                        int activationsThisTurn = Integer.parseInt(parts[1]);
                        SpellAbility modifierSA = c.getFirstSpellAbility();
                        String costString = c.getSVar("RollModifyCost");
                        System.out.println(costString);
                        Cost cost = new Cost(costString, false);
                        boolean paid = player.getController().payCostDuringRoll(cost, modifierSA,  null); //change to payCostDuringRoll
                        if (paid) {
                            Integer increment = player.getController().chooseRollIncrement(List.of(1, -1), rollToModify);
                            if (!modified) {unmodifiedRolls.remove(rollToModify); modified = true;}
                            rollToModify += increment;
                            activationsThisTurn += 1;
                            c.setSVar("ModsThisTurn", "Number$" + activationsThisTurn);
                            System.out.println("paid cost for modification");
                            System.out.println("unmodified:" + unmodifiedRolls);
                            System.out.println("modified:" + modifiedRolls);
                        }
                    }
                    if (modified) {modifiedRolls.add(rollToModify);}
                }
            } while (rollToModify != null && !unmodifiedRolls.isEmpty());
            finalResults.addAll(modifiedRolls);
            finalResults.addAll(unmodifiedRolls);
            //TODO this does not work with critical hit, we need to make sure to treat natural rolls differently just for that card
            naturalRolls = finalResults;
        }

        //Notify of results
        if (amount > 0) {
            StringBuilder sb = new StringBuilder();
            String rollResults = StringUtils.join(naturalRolls, ", ");
            String resultMessage = toVisitAttractions ? "lblAttractionRollResult" : "lblPlayerRolledResult";
            sb.append(Localizer.getInstance().getMessage(resultMessage, player, rollResults));
            if (!ignored.isEmpty()) {
                sb.append("\r\n").append(Localizer.getInstance().getMessage("lblIgnoredRolls",
                        StringUtils.join(ignored, ", ")));
            }
            player.getGame().getAction().notifyOfValue(sa, player, sb.toString(), null);
            player.addDieRollThisTurn(naturalRolls);
        }

        List<Integer> rolls = Lists.newArrayList();
        int oddResults = 0;
        int evenResults = 0;
        int differentResults = 0;
        int countMaxRolls = 0;
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
        if (sa != null) {
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
        }

        int rollNum = 1;
        for (Integer roll : rolls) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
            runParams.put(AbilityKey.Sides, sides);
            runParams.put(AbilityKey.Modifier, modifier);
            runParams.put(AbilityKey.Result, roll);
            runParams.put(AbilityKey.RolledToVisitAttractions, toVisitAttractions);
            runParams.put(AbilityKey.Number, player.getNumRollsThisTurn() - amount + rollNum);
            player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);
            rollNum++;
        }
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
        runParams.put(AbilityKey.Sides, sides);
        runParams.put(AbilityKey.Result, rolls);
        runParams.put(AbilityKey.RolledToVisitAttractions, toVisitAttractions);
        player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDieOnce, runParams, false);

        return rolls.stream().reduce(0, Integer::sum);
    }

    public static List<Card> getIncrementCards(Player player, String xenoKeyword, String nightShiftKeyword) {
        CardCollection xenosquirrels = CardLists.getKeyword(player.getCardsIn(ZoneType.Battlefield), xenoKeyword);
        CardCollection nightShifts = CardLists.getKeyword(player.getCardsIn(ZoneType.Battlefield), nightShiftKeyword);
        List<Card> canIncrementDice = new ArrayList<>(xenosquirrels);
        for (Card c : nightShifts) {
            String activationLimit = c.getSVar("RollModificationsLimit");
            String[] parts = c.getSVar("ModsThisTurn").split("\\$");
            int activationsThisTurn = Integer.parseInt(parts[1]);
            System.out.println("LIMIT: " + activationLimit);
            System.out.println("CURRENT: " + activationsThisTurn);
            if (activationLimit.equals("None") || activationsThisTurn < Integer.parseInt(activationLimit)) {
                canIncrementDice.add(c);
            }
        }
        return canIncrementDice;
    }

    private static List<Integer> rollAction(int amount, int sides, int ignore, List<Integer> rollsResult, List<Integer> ignored, Map<Player, Integer> ignoreChosenMap, Player player, Map<AbilityKey, Object> repParams) {

        repParams.put(AbilityKey.Number, amount);
        repParams.put(AbilityKey.Ignore, ignore);
        repParams.put(AbilityKey.IgnoreChosen, ignoreChosenMap);
        switch (player.getGame().getReplacementHandler().run(ReplacementType.RollDice, repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                amount = (int) repParams.get(AbilityKey.Number);
                ignore = (int) repParams.get(AbilityKey.Ignore);
                //noinspection unchecked
                ignoreChosenMap = (Map<Player, Integer>) repParams.get(AbilityKey.IgnoreChosen);
                break;
            }
        }

        List<Integer> naturalRolls = (rollsResult == null ? new ArrayList<>() : rollsResult);

        for (int i = 0; i < amount; i++) {
            int roll = MyRandom.getRandom().nextInt(sides) + 1;
            // Play the die roll sound
            player.getGame().fireEvent(new GameEventRollDie());
            player.roll();
            naturalRolls.add(roll);
        }

        naturalRolls.sort(null);

        // Ignore lowest rolls
        if (ignore > 0) {
            for (int i = ignore - 1; i >= 0; --i) {
                ignored.add(naturalRolls.get(i));
                naturalRolls.remove(i);
            }
        }
        // Player chooses to ignore rolls
        for (Player chooser : ignoreChosenMap.keySet()) {
            for (int ig = 0; ig < ignoreChosenMap.get(chooser); ig++) {
                Integer ign = chooser.getController().chooseRollToIgnore(naturalRolls);
                ignored.add(ign);
                naturalRolls.remove(ign);
            }
        }

        return naturalRolls;
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
        int total = rollDiceForPlayer(sa, player, amount, sides, ignore, modifier, rolls, sa.hasParam("ToVisitYourAttractions"));

        if (sa.hasParam("UseDifferenceBetweenRolls")) {
            total = Collections.max(rolls) - Collections.min(rolls);
        }

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
                if (sa.hasParam("ToVisitYourAttractions")) {
                    player.visitAttractions(result);
                }
            }
        }
        if (rememberHighest) {
            int highest = 0;
            for (Integer result : results) {
                if (highest < result) {
                    highest = result;
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
