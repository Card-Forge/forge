package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.Localizer;
import forge.util.MyRandom;

public class ChooseNumberEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a number.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        final boolean random = sa.hasParam("Random");
        final boolean anyNumber = sa.hasParam("ChooseAnyNumber");
        final boolean secretlyChoose = sa.hasParam("SecretlyChoose");

        final String sMin = sa.getParamOrDefault("Min", "0");
        final int min = AbilityUtils.calculateAmount(card, sMin, sa); 
        final String sMax = sa.getParamOrDefault("Max", "99");
        final int max = AbilityUtils.calculateAmount(card, sMax, sa); 

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Map<Player, Integer> chooseMap = Maps.newHashMap(); 

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                int chosen;
                if (random) {
                    chosen = MyRandom.getRandom().nextInt(max - min) + min;
                    p.getGame().getAction().notifyOfValue(sa, p, Integer.toString(chosen), null);
                } else {
                    String title = sa.hasParam("ListTitle") ? sa.getParam("ListTitle") : Localizer.getInstance().getMessage("lblChooseNumber");
                    if (anyNumber) {
                        Integer value = p.getController().announceRequirements(sa, title);
                        chosen = (value == null ? 0 : value);
                    } else {
                        chosen = p.getController().chooseNumber(sa, title, min, max);
                    }
                    // don't notify here, because most scripts I've seen don't store that number in a long term
                }
                if (secretlyChoose) {
                    chooseMap.put(p, chosen);
                } else {
                    card.setChosenNumber(chosen);
                }
                if (sa.hasParam("Notify")) {
                    p.getGame().getAction().notifyOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), chosen), p);
                }
            }
        }
        if (secretlyChoose) {
            StringBuilder sb = new StringBuilder();
            List<Player> highestNum = Lists.newArrayList();
            List<Player> lowestNum = Lists.newArrayList();
            int highest = 0;
            int lowest = Integer.MAX_VALUE;
            for (Entry<Player, Integer> ev : chooseMap.entrySet()) {
                int num = ev.getValue();
                Player player = ev.getKey();
                sb.append(Localizer.getInstance().getMessage("lblPlayerChoseNum", player.getName(), String.valueOf(num)));
                sb.append("\r\n");
                if (num > highest) {
                    highestNum.clear();
                    highest = num;
                }
                if (num == highest) {
                    highestNum.add(player);
                }
                if (num < lowest) {
                    lowestNum.clear();
                    lowest = num;
                }
                if (num == lowest) {
                    lowestNum.add(player);
                }
            }
            card.getGame().getAction().notifyOfValue(sa, card, sb.toString(), null);
            if (sa.hasParam("ChooseNumberSubAbility")) {
                SpellAbility sub = sa.getAdditionalAbility("ChooseNumberSubAbility");
                
                for (Player p : chooseMap.keySet()) {
                    card.addRemembered(p);
                    card.setChosenNumber(chooseMap.get(p));
                    AbilityUtils.resolve(sub);
                    card.clearRemembered();
                }
            }
            
            if (sa.hasParam("Lowest")) {
                SpellAbility sub = sa.getAdditionalAbility("Lowest");

                for (Player p : lowestNum) {
                    card.addRemembered(p);
                    card.setChosenNumber(lowest);
                    AbilityUtils.resolve(sub);
                    card.clearRemembered();
                }
            }

            if (sa.hasParam("NotLowest")) {
                List<Player> notLowestNum = Lists.newArrayList();
                for (Player p : chooseMap.keySet()) {
                    if (!lowestNum.contains(p)) {
                        notLowestNum.add(p);
                    }
                }
                SpellAbility sub = sa.getAdditionalAbility("NotLowest");

                for (Player p : notLowestNum) {
                    card.addRemembered(p);
                    AbilityUtils.resolve(sub);
                    card.clearRemembered();
                }
            }

            if (sa.hasParam("Highest")) {
                SpellAbility sub = sa.getAdditionalAbility("Highest");

                for (Player p : highestNum) {
                    card.addRemembered(p);
                    card.setChosenNumber(highest);
                    AbilityUtils.resolve(sub);
                    card.clearRemembered();
                }
                if (sa.hasParam("RememberHighest")) {
                    card.addRemembered(highestNum);
                }
            }
        }
    }

}
