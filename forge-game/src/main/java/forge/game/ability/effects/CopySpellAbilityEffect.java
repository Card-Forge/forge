package forge.game.ability.effects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.CardTranslation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CopySpellAbilityEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<SpellAbility> tgtSpells = getTargetSpells(sa);

        sb.append("Copy ");
        // TODO Someone fix this Description when Copying Charms
        final Iterator<SpellAbility> it = tgtSpells.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getHostCard());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
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
        final Card card = sa.getHostCard();
        Player controller = sa.getActivatingPlayer();

        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa);
        }

        if (sa.hasParam("Controller")) {
            controller = AbilityUtils.getDefinedPlayers(card, sa.getParam("Controller"), sa).get(0);
        }

        boolean isOptional = sa.hasParam("Optional");
        if (isOptional && !controller.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoyouWantCopyTheSpell", CardTranslation.getTranslatedName(card.getName())))) {
            return;
        }

        final List<SpellAbility> tgtSpells = getTargetSpells(sa);


        if (tgtSpells.size() == 0 || amount == 0) {
            return;
        }

        boolean mayChooseNewTargets = true;
        List<SpellAbility> copies = new ArrayList<>();
        
        if (sa.hasParam("CopyMultipleSpells")) {
            final int spellCount = Integer.parseInt(sa.getParam("CopyMultipleSpells"));

            for (int multi = 0; multi < spellCount && !tgtSpells.isEmpty(); multi++) {
                String prompt = Localizer.getInstance().getMessage("lblSelectMultiSpellCopyToStack", Lang.getOrdinal(multi + 1));
                SpellAbility chosen = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa, prompt,
                        ImmutableMap.of());
                SpellAbility copiedSpell = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosen);
                copiedSpell.getHostCard().setController(card.getController(), card.getGame().getNextTimestamp());
                copiedSpell.setActivatingPlayer(controller);
                copies.add(copiedSpell);
                tgtSpells.remove(chosen);
            }
        }
        else if (sa.hasParam("CopyForEachCanTarget")) {
            SpellAbility chosenSA = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa,
                    Localizer.getInstance().getMessage("lblSelectASpellCopy"), ImmutableMap.of());
            chosenSA.setActivatingPlayer(controller);
            // Find subability or rootability that has targets
            SpellAbility targetedSA = chosenSA;
            while (targetedSA != null) {
                if (targetedSA.usesTargeting() && targetedSA.getTargets().getNumTargeted() != 0) {
                    break;
                }
                targetedSA = targetedSA.getSubAbility();
            }
            if (targetedSA == null) {
            	return;
            }
            final List<GameEntity> candidates = targetedSA.getTargetRestrictions().getAllCandidates(targetedSA, true);
            if (sa.hasParam("CanTargetPlayer")) {
                // Radiate
                // Remove targeted players because getAllCandidates include all the valid players
                for(Player p : targetedSA.getTargets().getTargetPlayers())
                    candidates.remove(p);
                
                mayChooseNewTargets = false;
                for (GameEntity o : candidates) {
                    SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA);
                    resetFirstTargetOnCopy(copy, o, targetedSA);
                    copies.add(copy);
                }
            } else {// Precursor Golem, Ink-Treader Nephilim
                final String type = sa.getParam("CopyForEachCanTarget");
                CardCollection valid = new CardCollection();
                List<Player> players = Lists.newArrayList();
                Player originalTargetPlayer = Iterables.getFirst(getTargetPlayers(chosenSA), null);
                for (final GameEntity o : candidates) {
                    if (o instanceof Card) {
                        valid.add((Card) o);
                    } else if (o instanceof Player) {
                        final Player p = (Player) o;
                        if (p.equals(originalTargetPlayer))
                            continue;
                        if (p.isValid(type.split(","), chosenSA.getActivatingPlayer(), chosenSA.getHostCard(), sa)) {
                            players.add(p);
                        }
                    }
                }
                valid = CardLists.getValidCards(valid, type.split(","), chosenSA.getActivatingPlayer(), chosenSA.getHostCard(), sa);
                Card originalTarget = Iterables.getFirst(getTargetCards(chosenSA), null);
                valid.remove(originalTarget);
                mayChooseNewTargets = false;
                if (sa.hasParam("ChooseOnlyOne")) {
                    Card choice = controller.getController().chooseSingleEntityForEffect(valid, sa, Localizer.getInstance().getMessage("lblChooseOne"), null);
                    if (choice != null) {
                        valid = new CardCollection(choice);
                    }
                }

                for (final Card c : valid) {
                    SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA);
                    resetFirstTargetOnCopy(copy, c, targetedSA);
                    copies.add(copy);
                }
                for (final Player p : players) {
                    SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA);
                    resetFirstTargetOnCopy(copy, p, targetedSA);
                    copies.add(copy);
                }
            }
        }
        else {
            SpellAbility chosenSA = controller.getController().chooseSingleSpellForEffect(tgtSpells, sa,
                    Localizer.getInstance().getMessage("lblSelectASpellCopy"), ImmutableMap.of());
            chosenSA.setActivatingPlayer(controller);
            for (int i = 0; i < amount; i++) {
                SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(sa, chosenSA);

                // extra case for Epic to remove the keyword and the last part of the SpellAbility
                if (sa.hasParam("Epic")) {
                    copy.getHostCard().removeIntrinsicKeyword("Epic");
                    SpellAbility sub = copy;
                    while (sub.getSubAbility() != null && !sub.hasParam("Epic")) {
                        sub = sub.getSubAbility();
                    }
                    if (sub != null) {
                        sub.getParent().setSubAbility(sub.getSubAbility());
                    }
                }

                copies.add(copy);
            }
        }
        
        for(SpellAbility copySA : copies) {
            if (mayChooseNewTargets && copySA.usesTargeting()) {
                // TODO: ideally this should be implemented by way of allowing the player to cancel targeting
                // but in that case preserving whatever target was specified for the original spell (since
                // "changing targets" is the optional part).
                copySA.getTargetRestrictions().setMandatory(true);
            }
            controller.getController().playSpellAbilityForFree(copySA, mayChooseNewTargets);
        }
    } // end resolve

    private void resetFirstTargetOnCopy(SpellAbility copy, GameEntity obj, SpellAbility targetedSA) {
        copy.resetFirstTarget(obj, targetedSA);
        AbilitySub subAb = copy.getSubAbility();
        while (subAb != null) {
            subAb.resetFirstTarget(obj, targetedSA);
            subAb = subAb.getSubAbility();
        }
    }

}
