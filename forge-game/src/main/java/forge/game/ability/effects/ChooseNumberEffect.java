package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
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

        final String sMin = sa.getParamOrDefault("Min", "0");
        final int min = AbilityUtils.calculateAmount(card, sMin, sa); 
        final String sMax = sa.getParamOrDefault("Max", "99");
        final int max = AbilityUtils.calculateAmount(card, sMax, sa); 

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                int chosen;
                if (random) {
                    final Random randomGen = new Random();
                    chosen = randomGen.nextInt(max - min) + min;
                    p.getGame().getAction().nofityOfValue(sa, p, Integer.toString(chosen), null);
                } else {
                    String title = sa.hasParam("ListTitle") ? sa.getParam("ListTitle") : "Choose a number";
                    chosen = p.getController().chooseNumber(sa, title, min, max);
                    // don't notify here, because most scripts I've seen don't store that number in a long term
                }
                card.setChosenNumber(chosen);
                if (sa.hasParam("Notify")) {
                    p.getGame().getAction().nofityOfValue(sa, card, p.getName() + " picked " + chosen, p);
                }
            }
        }
    }

}
