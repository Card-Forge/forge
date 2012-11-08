package forge.card.abilityfactory.effects;

import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import forge.Card;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class ChooseNumberEffect extends SpellEffect
{
    
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
        
        final int min;
        if (!sa.hasParam("Min")) {
            min = Integer.parseInt("0");
        } else if (sa.getParam("Min").matches("[0-9][0-9]?")) {
            min = Integer.parseInt(sa.getParam("Min"));
        } else {
            min = CardFactoryUtil.xCount(card, card.getSVar(sa.getParam("Min")));
        } // Allow variables for Min
        
        final int max;
        if (!sa.hasParam("Max")) {
            max = Integer.parseInt("99");
        } else if (sa.getParam("Max").matches("[0-9][0-9]?")) {
            max = Integer.parseInt(sa.getParam("Max"));
        } else {
            max = CardFactoryUtil.xCount(card, card.getSVar(sa.getParam("Max")));
        } // Allow variables for Max
        
        final String[] choices = new String[max + 1];
        if (!random) {
            // initialize the array
            for (int i = min; i <= max; i++) {
                choices[i] = Integer.toString(i);
            }
        }
        
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final Target tgt = sa.getTarget();
        
        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    int chosen;
                    if (random) {
                        final Random randomGen = new Random();
                        chosen = randomGen.nextInt(max - min) + min;
                        final String message = "Randomly chosen number: " + chosen;
                        JOptionPane.showMessageDialog(null, message, "" + card, JOptionPane.PLAIN_MESSAGE);
                    } else if (sa.hasParam("ListTitle")) {
                        final Object o = GuiChoose.one(sa.getParam("ListTitle"), choices);
                        if (null == o) {
                            return;
                        }
                        chosen = Integer.parseInt((String) o);
                    } else {
                        final Object o = GuiChoose.one("Choose a number", choices);
                        if (null == o) {
                            return;
                        }
                        chosen = Integer.parseInt((String) o);
                    }
                    card.setChosenNumber(chosen);
                    
                } else {
                    // TODO - not implemented
                }
            }
        }
    }

}