package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Localizer;
import forge.util.MyRandom;

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
                    sb.append("\n\n").append(desc);
                }
            }
        }

        return sb.toString();
    }

    private static int getRollAdvange(final Player player) {
        String str = "If you would roll one or more dice, instead roll that many dice plus one and ignore the lowest roll.";
        return player.getKeywords().getAmount(str);
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
        stringBuilder.append(sa.getParamOrDefault("Amount", "a")).append(" ");
        stringBuilder.append(sa.getParamOrDefault("Sides", "6")).append("-sided ");
        if (sa.getParamOrDefault("Amount", "1").equals("1")) {
            stringBuilder.append("die.");
        } else {
            stringBuilder.append("dice.");
        }
        return stringBuilder.toString();
    }

    private void rollDice(SpellAbility sa, Player player, int amount, int sides) {
        final Card host = sa.getHostCard();
        final int modifier = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Modifier", "0"), sa);
        final int advantage = getRollAdvange(player);
        amount += advantage;
        int total = 0;
        List<Integer> rolls = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            int roll = MyRandom.getRandom().nextInt(sides) + 1;
            rolls.add(roll);
            total += roll;
        }

        if (amount > 0) {
            String message = Localizer.getInstance().getMessage("lblPlayerRolledResult", player, StringUtils.join(rolls, ", "));
            player.getGame().getAction().notifyOfValue(sa, player, message, null);
        }

        // Ignore lowest rolls
        if (advantage > 0) {
            rolls.sort(null);
            for (int i = advantage - 1; i >= 0; --i) {
                total -= rolls.get(i);
                rolls.remove(i);
            }
        }

        // Run triggers
        for (Integer roll : rolls) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Player, player);
            runParams.put(AbilityKey.Result, roll);
            player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);
        }
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, player);
        runParams.put(AbilityKey.Result, rolls);
        player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDieOnce, runParams, false);

        total += modifier;
        if (sa.hasParam("ResultSVar")) {
            host.setSVar(sa.getParam("ResultSVar"), Integer.toString(total));
        }

        Map<String, SpellAbility> diceAbilities = sa.getAdditionalAbilities();
        SpellAbility resultAbility = null;
        for (Map.Entry<String, SpellAbility> e: diceAbilities.entrySet()) {
            String diceKey = e.getKey();
            if (diceKey.contains("-")) {
                String [] ranges = diceKey.split("-");
                if (Integer.parseInt(ranges[0]) <= total && Integer.parseInt(ranges[1]) >= total) {
                    resultAbility = e.getValue();
                    break;
                }
            } else if (StringUtils.isNumeric(diceKey) && Integer.parseInt(diceKey) == total) {
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

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "1"), sa);
        int sides = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Sides", "6"), sa);

        final PlayerCollection playersToRoll = getTargetPlayers(sa);

        for (Player player : playersToRoll) {
            rollDice(sa, player, amount, sides);
        }
    }
}
