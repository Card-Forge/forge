package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class CopySpellAbilityEffect extends SpellAbilityEffect {

    // *************************************************************************
    // ************************* CopySpell *************************************
    // *************************************************************************

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<SpellAbility> tgtSpells = getTargetSpellAbilities(sa);

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
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa);
        }
        if (amount > 1) {
            sb.append(amount).append(" times");
        }
        sb.append(".");
        // TODO probably add an optional "You may choose new targets..."
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();
        Player controller = sa.getActivatingPlayer();

        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            controller = AbilityUtils.getDefinedPlayers(card, sa.getParam("Controller"), sa).get(0);
        }

        final List<SpellAbility> tgtSpells = getTargetSpellAbilities(sa);


        if (tgtSpells.size() == 0) {
            return;
        }

        if (sa.hasParam("CopyMultipleSpells")) {
            final int spellCount = Integer.parseInt(sa.getParam("CopyMultipleSpells"));
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
                    CardFactory.copySpellontoStack(card, chosenSAcopy.getSourceCard(), chosenSAcopy, true);
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
                CardFactory.copySpellontoStack(card, chosenSA.getSourceCard(), chosenSA, true);
            }
        }
    } // end resolve

}
