package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.event.GameEventRollDie;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerController;
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

    public static class DieRollResult {
        private int naturalValue;
        private int modifiedValue;

        public DieRollResult(int naturalValue, int modifiedValue) {
            this.naturalValue = naturalValue;
            this.modifiedValue = modifiedValue;
        }
        // Getters
        public int getNaturalValue() {
            return naturalValue;
        }
        public int getModifiedValue() {
            return modifiedValue;
        }
        // Setters
        public void setNaturalValue(int naturalValue) {
            this.naturalValue = naturalValue;
        }
        public void setModifiedValue(int modifiedValue) {
            this.modifiedValue = modifiedValue;
        }

        @Override
        public String toString() {
            return String.valueOf(modifiedValue);
        }
    }

    public static List<DieRollResult> getResultsList(List<Integer> naturalResults) {
        List<DieRollResult> results = new ArrayList<>();
        for (int r : naturalResults) {
            results.add(new DieRollResult(r, r));
        }
        return results;
    }

    public static List<Integer> getNaturalResults(List<DieRollResult> results) {
        List<Integer> naturalResults = new ArrayList<>();
        for (DieRollResult r : results) {
            naturalResults.add(r.getNaturalValue());
        }
        return naturalResults;
    }

    public static List<Integer> getFinalResults(List<DieRollResult> results) {
        List<Integer> naturalResults = new ArrayList<>();
        for (DieRollResult r : results) {
            naturalResults.add(r.getModifiedValue());
        }
        return naturalResults;
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
        Set<Card> dicePTExchanges = new HashSet<>();

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(player);
        List<Integer> ignored = new ArrayList<>();
        List<Integer> naturalRolls = rollAction(amount, sides, ignore, rollsResult, ignored, ignoreChosenMap, dicePTExchanges, player, repParams);

        if (sa != null && sa.hasParam("UseHighestRoll")) {
            naturalRolls.subList(0, naturalRolls.size() - 1).clear();
        }

        // Reroll Phase:
        String monitorKeyword = "Once each turn, you may pay {1} to reroll one or more dice you rolled.";
        CardCollection canRerollDice = getRerollCards(player, monitorKeyword);
        while (!canRerollDice.isEmpty()) {
            List<Integer> diceToReroll = player.getController().chooseDiceToReroll(naturalRolls);
            if (diceToReroll.isEmpty()) {break;}

            String message = Localizer.getInstance().getMessage("lblChooseRerollCard");
            Card c = player.getController().chooseSingleEntityForEffect(canRerollDice, sa, message, null);

            String[] parts = c.getSVar("ModsThisTurn").split("\\$");
            int activationsThisTurn = Integer.parseInt(parts[1]);
            SpellAbility modifierSA = c.getFirstSpellAbility();
            Cost cost = new Cost(c.getSVar("RollRerollCost"), false);
            boolean paid = player.getController().payCostDuringRoll(cost, modifierSA, null);
            if (paid) {
                for (Integer roll : diceToReroll) {
                    naturalRolls.remove(roll);
                }
                int amountToReroll = diceToReroll.size();
                List<Integer> rerolls = rollAction(amountToReroll, sides, 0, null, ignored, Maps.newHashMap(), dicePTExchanges, player, repParams);
                naturalRolls.addAll(rerolls);
                activationsThisTurn += 1;
                c.setSVar("ModsThisTurn", "Number$" + activationsThisTurn);
                canRerollDice.remove(c);
            }
        }

        // Modification Phase:
        List<DieRollResult> resultsList = new ArrayList<>();
        Integer rollToModify;
        String xenoKeyword = "After you roll a die, you may remove a +1/+1 counter from Xenosquirrels. If you do, increase or decrease the result by 1.";
        String nightShiftKeyword = "After you roll a die, you may pay 1 life. If you do, increase or decrease the result by 1. Do this only once each turn.";
        List<Card> canIncrementDice = getIncrementCards(player, xenoKeyword, nightShiftKeyword);
        boolean hasBeenModified = false;

        if (!canIncrementDice.isEmpty()) {
            do {
                rollToModify = player.getController().chooseRollToModify(naturalRolls);
                if (rollToModify == null) {break;}

                boolean modified = false;
                DieRollResult dieResult = new DieRollResult(rollToModify, rollToModify);
                // canIncrementThisRoll won't be empty the first iteration because canIncrementDice wasn't empty
                CardCollection canIncrementThisRoll = new CardCollection(canIncrementDice);
                Card c;
                do {
                    String message = Localizer.getInstance().getMessage("lblChooseRollIncrementCard", rollToModify);
                    c = player.getController().chooseSingleEntityForEffect(canIncrementThisRoll, sa, message, null);

                    String[] parts = c.getSVar("ModsThisTurn").split("\\$");
                    int activationsThisTurn = Integer.parseInt(parts[1]);
                    SpellAbility modifierSA = c.getFirstSpellAbility();
                    String costString = c.getSVar("RollModifyCost");
                    Cost cost = new Cost(costString, false);
                    boolean paid = player.getController().payCostDuringRoll(cost, modifierSA,  null);
                    if (paid) {
                        message = Localizer.getInstance().getMessage("lblChooseRollIncrement", rollToModify);
                        boolean isPositive = player.getController().chooseBinary(sa, message, PlayerController.BinaryChoiceType.IncreaseOrDecrease);
                        int increment = isPositive ? 1 : -1;
                        if (!modified) {naturalRolls.remove(rollToModify); modified = true;}
                        rollToModify += increment;
                        activationsThisTurn += 1;
                        c.setSVar("ModsThisTurn", "Number$" + activationsThisTurn);
                        canIncrementThisRoll.remove(c);
                    }
                } while (!canIncrementThisRoll.isEmpty());
                if (modified) {
                    dieResult.setModifiedValue(rollToModify);
                    resultsList.add(dieResult);
                    hasBeenModified = true;
                }
                canIncrementDice = getIncrementCards(player, xenoKeyword, nightShiftKeyword);
            } while (!naturalRolls.isEmpty() && !canIncrementDice.isEmpty());
        }

        // finish roll list
        for (Integer unmodified : naturalRolls) {
            // Add all the unmodified rolls into the results
            resultsList.add(new DieRollResult(unmodified, unmodified));
        }

        // Vedalken Exchange
        CardCollection vedalkenSwaps = new CardCollection(dicePTExchanges);
        if (!vedalkenSwaps.isEmpty()) {
            DieRollResult rollToSwap;
            do {
                rollToSwap = player.getController().chooseRollToSwap(resultsList);
                if (rollToSwap == null) {break;}

                String message = Localizer.getInstance().getMessage("lblChooseCardToDiceSwap", rollToSwap.getModifiedValue());
                Card c = player.getController().chooseSingleEntityForEffect(vedalkenSwaps, sa, message, null);
                int cPower = c.getCurrentPower();
                int cToughness = c.getCurrentToughness();
                String labelPower = Localizer.getInstance().getMessage("lblPower");
                String labelToughness = Localizer.getInstance().getMessage("lblToughness");
                List<String> choices = Arrays.asList(labelPower, labelToughness);
                String powerOrToughness = player.getController().chooseRollSwapValue(choices, rollToSwap.getModifiedValue(), cPower, cToughness);
                if (powerOrToughness != null) {
                    int tempRollValue = rollToSwap.getModifiedValue();
                    if (powerOrToughness.equals(labelPower)) {
                        rollToSwap.setModifiedValue(cPower);
                        c.addNewPT(tempRollValue, cToughness, player.getGame().getNextTimestamp(), 0);
                    } else if (powerOrToughness.equals(labelToughness)) {
                        rollToSwap.setModifiedValue(cToughness);
                        c.addNewPT(cPower, tempRollValue, player.getGame().getNextTimestamp(), 0);
                    } else {
                        throw new IllegalStateException("Unexpected value: " + powerOrToughness);
                    }
                    vedalkenSwaps.remove(c);
                }
            } while (!vedalkenSwaps.isEmpty());
        }

        //Notify of results
        if (amount > 0) {
            StringBuilder sb = new StringBuilder();
            String rollResults = StringUtils.join(getFinalResults(resultsList), ", ");
            String resultMessage = toVisitAttractions ? "lblAttractionRollResult" : "lblPlayerRolledResult";
            sb.append(Localizer.getInstance().getMessage(resultMessage, player, rollResults));
            if (!ignored.isEmpty()) {
                sb.append("\r\n").append(Localizer.getInstance().getMessage("lblIgnoredRolls",
                        StringUtils.join(ignored, ", ")));
            }
            if (hasBeenModified) {
                sb.append("\r\n").append(Localizer.getInstance().getMessage("lblNaturalRolls",
                        StringUtils.join(getNaturalResults(resultsList), ", ")));
            }
            player.getGame().getAction().notifyOfValue(sa, player, sb.toString(), null);
            player.addDieRollThisTurn(getFinalResults(resultsList));
        }

        List<Integer> rolls = Lists.newArrayList();
        int oddResults = 0;
        int evenResults = 0;
        int differentResults = 0;
        int countMaxRolls = 0;
        for (DieRollResult i : resultsList) {
            int naturalRoll = i.getNaturalValue();
            final int modifiedRoll = i.getModifiedValue() + modifier;

            i.setModifiedValue(modifiedRoll);

            if (!rolls.contains(modifiedRoll)) {
                differentResults++;
            }
            rolls.add(modifiedRoll);
            if (modifiedRoll % 2 == 0) {
                evenResults++;
            } else {
                oddResults++;
            }
            if (naturalRoll == sides) {
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
        for (DieRollResult roll : resultsList) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
            runParams.put(AbilityKey.Sides, sides);
            runParams.put(AbilityKey.Result, roll.getModifiedValue());
            runParams.put(AbilityKey.NaturalResult, roll.getNaturalValue());
            runParams.put(AbilityKey.RolledToVisitAttractions, toVisitAttractions);
            runParams.put(AbilityKey.Number, player.getNumRollsThisTurn() - amount + rollNum);
            player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);
            rollNum++;
        }
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
        runParams.put(AbilityKey.Sides, sides);
        runParams.put(AbilityKey.Result, getFinalResults(resultsList));
        runParams.put(AbilityKey.RolledToVisitAttractions, toVisitAttractions);
        player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDieOnce, runParams, false);

        return getFinalResults(resultsList).stream().reduce(0, Integer::sum);
    }

    /**
     * Gets a list of cards that can reroll dice roll results for a given player.
     * This is currently only Monitor Monitor
     *
     * @param player            The player whose battlefield is being checked for cards that can modify dice rolls
     * @param monitorKeyword       The keyword text identifying Monitor Monitor cards
     * @return A list of cards that are currently able to reroll dice
     */
    public static CardCollection getRerollCards(Player player, String monitorKeyword) {
        CardCollection monitors = CardLists.getKeyword(player.getCardsIn(ZoneType.Battlefield), monitorKeyword);
        return monitors.filter(card -> {
            String activationLimit = card.getSVar("RollModificationsLimit");
            String[] parts = card.getSVar("ModsThisTurn").split("\\$");
            int activationsThisTurn = Integer.parseInt(parts[1]);
            return (activationLimit.equals("None") || activationsThisTurn < Integer.parseInt(activationLimit));
        });
    }

    /**
     * Gets a list of cards that can modify dice roll results for a given player.
     * This includes both Xenosquirrels (which can remove +1/+1 counters to modify rolls)
     * and Night Shift cards (which can pay life to modify rolls once per turn).
     *
     * @param player            The player whose battlefield is being checked for cards that can modify dice rolls
     * @param xenoKeyword       The keyword text identifying Xenosquirrel cards
     * @param nightShiftKeyword The keyword text identifying Night Shift cards
     * @return A list of cards that are currently able to modify dice roll results
     */
    public static List<Card> getIncrementCards(Player player, String xenoKeyword, String nightShiftKeyword) {
        CardCollection xenosquirrels = CardLists.getKeyword(player.getCardsIn(ZoneType.Battlefield), xenoKeyword);
        CardCollection nightShifts = CardLists.getKeyword(player.getCardsIn(ZoneType.Battlefield), nightShiftKeyword);
        List<Card> canIncrementDice = new ArrayList<>();
        for (Card c : xenosquirrels) {
            // Xenosquirrels must have a P1P1 counter on it to remove in order to modify
            Integer P1P1Counters = c.getCounters().get(CounterEnumType.P1P1);
            if (P1P1Counters != null && P1P1Counters > 0 && c.canRemoveCounters(CounterEnumType.P1P1)) {
                canIncrementDice.add(c);
            }
        }
        for (Card c : nightShifts) {
            // Night Shift of the Living Dead has a limit of once per turn, player must be able to pay the 1 life cost
            String activationLimit = c.getSVar("RollModificationsLimit");
            String[] parts = c.getSVar("ModsThisTurn").split("\\$");
            int activationsThisTurn = Integer.parseInt(parts[1]);
            if ((activationLimit.equals("None") || activationsThisTurn < Integer.parseInt(activationLimit)) && player.canPayLife(1, true, c.getFirstSpellAbility())) {
                canIncrementDice.add(c);
            }
        }
        return canIncrementDice;
    }

    /**
     * Performs the dice rolling action with support for replacements, ignoring rolls, and tracking results.
     *
     * @param amount          number of dice to roll
     * @param sides           number of sides on each die
     * @param ignore          number of lowest rolls to automatically ignore
     * @param rollsResult     optional list to store roll results, if null a new list will be created
     * @param ignored         list to store ignored roll results
     * @param ignoreChosenMap mapping of players to number of rolls they can choose to ignore
     * @param player          the player performing the roll
     * @param repParams       replacement effect parameters
     * @return list of final roll results after applying ignores and replacements, sorted in ascending order
     */
    @SuppressWarnings("unchecked")
    private static List<Integer> rollAction(int amount, int sides, int ignore, List<Integer> rollsResult, List<Integer> ignored, Map<Player, Integer> ignoreChosenMap, Set<Card> dicePTExchanges, Player player, Map<AbilityKey, Object> repParams) {

        repParams.put(AbilityKey.Sides, sides);
        repParams.put(AbilityKey.Number, amount);
        repParams.put(AbilityKey.Ignore, ignore);
        repParams.put(AbilityKey.DicePTExchanges, dicePTExchanges);
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
            default:
                break;
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
