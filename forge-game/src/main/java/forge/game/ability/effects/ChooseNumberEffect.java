package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

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
        //final int min = sa.containsKey("Min") ? Integer.parseInt(sa.get("Min")) : 0;
        //final int max = sa.containsKey("Max") ? Integer.parseInt(sa.get("Max")) : 99;
        final boolean random = sa.hasParam("Random");
        final boolean anyNumber = sa.hasParam("ChooseAnyNumber");
        final boolean secretlyChoose = sa.hasParam("SecretlyChoose");

        final String sMin = sa.getParamOrDefault("Min", "0");
        final int min = AbilityUtils.calculateAmount(card, sMin, sa); 
        final String sMax = sa.getParamOrDefault("Max", "99");
        final int max = AbilityUtils.calculateAmount(card, sMax, sa); 

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Map<Player, Integer> chooseMap = new HashMap<Player, Integer>(); 

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                int chosen;
                if (random) {
                    final Random randomGen = new Random();
                    chosen = randomGen.nextInt(max - min) + min;
                    p.getGame().getAction().nofityOfValue(sa, p, Integer.toString(chosen), null);
                } else {
                    String title = sa.hasParam("ListTitle") ? sa.getParam("ListTitle") : "Choose a number";
                    if (anyNumber) {
                        Integer value = p.getController().announceRequirements(sa, title, true);
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
                    p.getGame().getAction().nofityOfValue(sa, card, p.getName() + " picked " + chosen, p);
                }
            }
        }
        if (secretlyChoose) {
            StringBuilder sb = new StringBuilder();
            List<Player> highestNum = new ArrayList<Player>();
            List<Player> lowestNum = new ArrayList<Player>();
            int highest = 0;
            int lowest = Integer.MAX_VALUE;
            for (Entry<Player, Integer> ev : chooseMap.entrySet()) {
                int num = ev.getValue();
                Player player = ev.getKey();
                sb.append(player).append(" chose ").append(num);
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
            card.getGame().getAction().nofityOfValue(sa, card, sb.toString(), null);
            if (sa.hasParam("ChooseNumberSubAbility")) {
                SpellAbility sub = AbilityFactory.getAbility(card.getSVar(sa.getParam("ChooseNumberSubAbility")), card);
                if (sa.isIntrinsic()) {
                	sub.setIntrinsic(true);
                	sub.changeText();
                }
                sub.setActivatingPlayer(sa.getActivatingPlayer());
                ((AbilitySub) sub).setParent(sa);
                for (Player p : chooseMap.keySet()) {
                    card.addRemembered(p);
                    card.setChosenNumber(chooseMap.get(p));
                    AbilityUtils.resolve(sub);
                    card.clearRemembered();
                }
            }
            
            if (sa.hasParam("Lowest")) {
                SpellAbility action = AbilityFactory.getAbility(card.getSVar(sa.getParam("Lowest")), card);
                if (sa.isIntrinsic()) {
                    action.setIntrinsic(true);
                    action.changeText();
                }
                action.setActivatingPlayer(sa.getActivatingPlayer());
                ((AbilitySub) action).setParent(sa);
                for (Player p : lowestNum) {
                    card.addRemembered(p);
                    card.setChosenNumber(lowest);
                    AbilityUtils.resolve(action);
                    card.clearRemembered();
                }
            }
            if (sa.hasParam("Highest")) {
                SpellAbility action = AbilityFactory.getAbility(card.getSVar(sa.getParam("Highest")), card);
                if (sa.isIntrinsic()) {
                    action.setIntrinsic(true);
                    action.changeText();
                }
                action.setActivatingPlayer(sa.getActivatingPlayer());
                ((AbilitySub) action).setParent(sa);
                for (Player p : highestNum) {
                    card.addRemembered(p);
                    card.setChosenNumber(highest);
                    AbilityUtils.resolve(action);
                    card.clearRemembered();
                }
                if (sa.hasParam("RememberHighest")) {
                    card.addRemembered(highestNum);
                }
            }
        }
    }

}
