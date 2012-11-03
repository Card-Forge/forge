package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class CopySpellEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        Player controller = sa.getActivatingPlayer();

        int amount = 1;
        if (params.containsKey("Amount")) {
            amount = AbilityFactory.calculateAmount(card, params.get("Amount"), sa);
        }

        if (params.containsKey("Controller")) {
            controller = AbilityFactory.getDefinedPlayers(card, params.get("Controller"), sa).get(0);
        }

        ArrayList<SpellAbility> tgtSpells;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtSpells = tgt.getTargetSAs();
        } else {
            tgtSpells = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtSpells.size() == 0) {
            return;
        }

        if (params.containsKey("CopyMultipleSpells")) {
            final int spellCount = Integer.parseInt(params.get("CopyMultipleSpells"));
            ArrayList<SpellAbility> chosenSAs = new ArrayList<SpellAbility>();
            SpellAbility chosenSAtmp = null;
            for (int multi = 0; multi < spellCount; multi++) {
                if (tgtSpells.size() == 1) {
                    chosenSAs.addAll(tgtSpells);
                } else if (sa.getActivatingPlayer().isHuman()) {
                    String num = "";
                    if (multi == 1 - 1) {
                        num = "first";
                    }
                    else if (multi == 2 - 1) {
                        num = "second";
                    }
                    else if (multi == 3 - 1) {
                        num = "third";
                    } else {
                        num = Integer.toString(multi - 1) + "th";
                    }
                    chosenSAtmp = GuiChoose.one("Select " + num + " spell to copy to stack", tgtSpells);
                    chosenSAs.add(chosenSAtmp);
                    tgtSpells.remove(chosenSAtmp);
                } else {
                    chosenSAs.add(tgtSpells.get(multi));
                }
            }

            for (final SpellAbility chosenSAcopy : chosenSAs) {
                chosenSAcopy.setActivatingPlayer(controller);
                for (int i = 0; i < amount; i++) {
                    Singletons.getModel().getCardFactory().copySpellontoStack(card, chosenSAcopy.getSourceCard(), chosenSAcopy, true);
                }
            }
        }
        else {
            SpellAbility chosenSA = null;
            if (tgtSpells.size() == 1) {
                chosenSA = tgtSpells.get(0);
            } else if (sa.getActivatingPlayer().isHuman()) {
                chosenSA = GuiChoose.one("Select a spell to copy", tgtSpells);
            } else {
                chosenSA = tgtSpells.get(0);
            }

            chosenSA.setActivatingPlayer(controller);
            for (int i = 0; i < amount; i++) {
                Singletons.getModel().getCardFactory().copySpellontoStack(card, chosenSA.getSourceCard(), chosenSA, true);
            }
        }
    } // end resolve

    /**
     * <p>
     * copySpellStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        ArrayList<SpellAbility> tgtSpells;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtSpells = tgt.getTargetSAs();
        } else {
            tgtSpells = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        sb.append("Copy ");
        // TODO Someone fix this Description when Copying Charms
        final Iterator<SpellAbility> it = tgtSpells.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getSourceCard());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        int amount = 1;
        if (params.containsKey("Amount")) {
            amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Amount"), sa);
        }
        if (amount > 1) {
            sb.append(amount).append(" times");
        }
        sb.append(".");
        // TODO probably add an optional "You may choose new targets..."
        return sb.toString();
    }

} // end class AbilityFactory_Copy