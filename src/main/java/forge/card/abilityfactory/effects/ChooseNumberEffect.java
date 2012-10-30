package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
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
            public String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a number.");
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /**
     * <p>
     * chooseNumberResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
            @Override
            public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        //final int min = params.containsKey("Min") ? Integer.parseInt(params.get("Min")) : 0;
        //final int max = params.containsKey("Max") ? Integer.parseInt(params.get("Max")) : 99;
        final boolean random = params.containsKey("Random");

        final int min;
        if (!params.containsKey("Min")) {
            min = Integer.parseInt("0");
        } else if (params.get("Min").matches("[0-9][0-9]?")) {
            min = Integer.parseInt(params.get("Min"));
        } else {
            min = CardFactoryUtil.xCount(card, card.getSVar(params.get("Min")));
        } // Allow variables for Min

        final int max;
        if (!params.containsKey("Max")) {
            max = Integer.parseInt("99");
        } else if (params.get("Max").matches("[0-9][0-9]?")) {
            max = Integer.parseInt(params.get("Max"));
        } else {
            max = CardFactoryUtil.xCount(card, card.getSVar(params.get("Max")));
        } // Allow variables for Max

        final String[] choices = new String[max + 1];
        if (!random) {
            // initialize the array
            for (int i = min; i <= max; i++) {
                choices[i] = Integer.toString(i);
            }
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    int chosen;
                    if (random) {
                        final Random randomGen = new Random();
                        chosen = randomGen.nextInt(max - min) + min;
                        final String message = "Randomly chosen number: " + chosen;
                        JOptionPane.showMessageDialog(null, message, "" + card, JOptionPane.PLAIN_MESSAGE);
                    } else if (params.containsKey("ListTitle")) {
                        final Object o = GuiChoose.one(params.get("ListTitle"), choices);
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

    // *************************************************************************
    // ************************* ChoosePlayer **********************************
    // *************************************************************************
}