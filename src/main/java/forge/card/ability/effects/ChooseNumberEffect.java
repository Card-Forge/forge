package forge.card.ability.effects;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;

import forge.Card;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.player.Player;
import forge.gui.GuiDialog;

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
        final Card card = sa.getSourceCard();
        //final int min = sa.containsKey("Min") ? Integer.parseInt(sa.get("Min")) : 0;
        //final int max = sa.containsKey("Max") ? Integer.parseInt(sa.get("Max")) : 99;
        final boolean random = sa.hasParam("Random");

        final String sMin = sa.getParamOrDefault("Min", "0");
        final int min = StringUtils.isNumeric(sMin) ? Integer.parseInt(sMin) : CardFactoryUtil.xCount(card, card.getSVar(sMin));
        final String sMax = sa.getParamOrDefault("Max", "99");
        final int max = StringUtils.isNumeric(sMax) ? Integer.parseInt(sMax) : CardFactoryUtil.xCount(card, card.getSVar(sMax)); 

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                int chosen;
                if (random) {
                    final Random randomGen = new Random();
                    chosen = randomGen.nextInt(max - min) + min;
                    final String message = "Randomly chosen number: " + chosen;
                    GuiDialog.message(message, card.toString());
                } else {
                    String title = sa.hasParam("ListTitle") ? sa.getParam("ListTitle") : "Choose a number";
                    chosen = p.getController().chooseNumber(sa, title, min, max);
                }
                card.setChosenNumber(chosen);
            }
        }
    }

}
