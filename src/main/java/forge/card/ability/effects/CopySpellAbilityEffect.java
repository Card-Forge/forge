package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardLists;
import forge.ITargetable;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.Lang;

public class CopySpellAbilityEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<SpellAbility> tgtSpells = getTargetSpells(sa);

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

        final List<SpellAbility> tgtSpells = getTargetSpells(sa);


        if (tgtSpells.size() == 0) {
            return;
        }

        boolean mayChoseNewTargets = true;
        List<SpellAbility> copies = new ArrayList<SpellAbility>();
        
        if (sa.hasParam("CopyMultipleSpells")) {
            final int spellCount = Integer.parseInt(sa.getParam("CopyMultipleSpells"));

            for (int multi = 0; multi < spellCount && !tgtSpells.isEmpty(); multi++) {
                String prompt = "Select " + Lang.getOrdinal(multi) + " spell to copy to stack";
                SpellAbility chosen = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa, prompt);
                copies.add(CardFactory.copySpellAbilityAndSrcCard(card, chosen.getSourceCard(), chosen, true));
                tgtSpells.remove(chosen);
            }
        }
        else if (sa.hasParam("CopyForEachCanTarget")) {
            SpellAbility chosenSA = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa, "Select a spell to copy");
            chosenSA.setActivatingPlayer(controller);
            List<ITargetable> candidates = chosenSA.getTargetRestrictions().getAllCandidates(chosenSA, true);
            if (sa.hasParam("CanTargetPlayer")) {
                // Radiate
                // Remove targeted players because getAllCandidates include all the valid players
                for(Player p : chosenSA.getTargets().getTargetPlayers())
                    candidates.remove(p);
                
                mayChoseNewTargets = false;
                for (ITargetable o : candidates) {
                    SpellAbility copy = CardFactory.copySpellAbilityAndSrcCard(card, chosenSA.getSourceCard(), chosenSA, true);
                    copy.resetTargets();
                    copy.getTargets().add(o);
                    copies.add(copy);
                }
            } else {// Precursor Golem, Ink-Treader Nephilim
                final String type = sa.getParam("CopyForEachCanTarget");
                List<Card> valid = new ArrayList<Card>();
                for (final Object o : candidates) {
                    if (o instanceof Card) {
                        valid.add((Card) o);
                    }
                }
                valid = CardLists.getValidCards(valid, type, chosenSA.getActivatingPlayer(), chosenSA.getSourceCard());
                Card originalTarget = Iterables.getFirst(getTargetCards(chosenSA), null);
                valid.remove(originalTarget);
                mayChoseNewTargets = false;
                for (Card c : valid) {
                    SpellAbility copy = CardFactory.copySpellAbilityAndSrcCard(card, chosenSA.getSourceCard(), chosenSA, true);
                    copy.resetTargets();
                    copy.getTargets().add(c);
                    copies.add(copy);
                }
            }
        }
        else {
            SpellAbility chosenSA = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa, "Select a spell to copy");
            chosenSA.setActivatingPlayer(controller);
            for (int i = 0; i < amount; i++) {
                copies.add(CardFactory.copySpellAbilityAndSrcCard(card, chosenSA.getSourceCard(), chosenSA, true));
            }
        }
        
        for(SpellAbility copySA : copies) {
            controller.getController().playSpellAbilityForFree(copySA, mayChoseNewTargets);
        }
    } // end resolve

}
