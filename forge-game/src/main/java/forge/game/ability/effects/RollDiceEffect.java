package forge.game.ability.effects;

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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RollDiceEffect extends SpellAbilityEffect {

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
        stringBuilder.append(sa.getParamOrDefault("Amt", "a")).append(" ");
        stringBuilder.append(sa.getParamOrDefault("Sides", "6")).append("-sided ");
        if (sa.getParamOrDefault("Amt", "1").equals("1")) {
            stringBuilder.append("die.");
        } else {
            stringBuilder.append("dice.");
        }
        return stringBuilder.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amt", "1"), sa);
        int sides = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Sides", "6"), sa);

        final PlayerCollection playersToRoll = getTargetPlayers(sa);

        for(Player player : playersToRoll) {
            int total = 0;
            List<Integer> rolls = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                int roll = MyRandom.getRandom().nextInt(sides) + 1;
                rolls.add(roll);

                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Player, player);
                runParams.put(AbilityKey.Result, roll);
                player.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);

                total += roll;
            }

            if (amount > 0) {
                String message = Localizer.getInstance().getMessage("lblPlayerRolledResult", player, StringUtils.join(rolls, ", "));
                player.getGame().getAction().notifyOfValue(sa, player, message, null);
            }

            if (sa.hasParam("ResultSVar")) {
                host.setSVar(sa.getParam("ResultSVar"), ""+total);
            }
            if (sa.hasAdditionalAbility("OnDoubles") && rolls.get(0).equals(rolls.get(1))) {
                AbilityUtils.resolve(sa.getAdditionalAbility("OnDoubles"));
            }
            if (sa.hasAdditionalAbility("On"+total)) {
                AbilityUtils.resolve(sa.getAdditionalAbility("On"+total));
            } else if (sa.hasAdditionalAbility("Else")) {
                AbilityUtils.resolve(sa.getAdditionalAbility("Else"));
            }
        }
    }
}