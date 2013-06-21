package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.ITargetable;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.TargetChoices;
import forge.game.player.Player;
import forge.game.zone.MagicStack;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChangeTargetsEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<SpellAbility> sas = getTargetSpells(sa);
        final boolean remember = sa.hasParam("RememberTargetedCard");
        
        final MagicStack stack = sa.getActivatingPlayer().getGame().getStack();
        for (final SpellAbility tgtSA : sas) {
            SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, there isn't really a target
                continue;
            }

            boolean preserveNumber = sa.hasParam("PreserveNumber"); // Redirect is not supposed to change number of targets 
            boolean changesOneTarget = sa.hasParam("ChangeSingleTarget"); // The only card known to replace targets with self is Spellskite

            SpellAbilityStackInstance changingTgtSI = si;
            Player chooser = sa.getActivatingPlayer();

            if( changesOneTarget ) {
                // 1. choose a target of target spell
                List<Pair<SpellAbilityStackInstance, ITargetable>> allTargets = new ArrayList<>();
                while(changingTgtSI != null) {
                    SpellAbility changedSa = changingTgtSI.getSpellAbility(); 
                    if(changedSa.usesTargeting()) {
                        for(ITargetable it : changedSa.getTargets().getTargets())
                            allTargets.add(ImmutablePair.of(changingTgtSI, it));
                    }
                    changingTgtSI = changingTgtSI.getSubInstace();
                }
                if( allTargets.isEmpty() ) {
                    System.err.println("Player managed to target a spell without targets with Spellskite's ability.");
                    return;
                }
                
                Pair<SpellAbilityStackInstance, ITargetable> chosenTarget = chooser.getController().chooseTarget(sa, allTargets);
                // 2. prepare new target choices
                SpellAbilityStackInstance replaceIn = chosenTarget.getKey();
                ITargetable oldTarget = chosenTarget.getValue();
                TargetChoices oldTargetBlock = replaceIn.getTargetChoices();
                TargetChoices newTargetBlock = oldTargetBlock.clone();
                newTargetBlock.remove(oldTarget);
                replaceIn.updateTarget(newTargetBlock);
                // 3. test if updated choices would be correct.
                ITargetable newTarget = Iterables.getFirst(getDefinedCardsOrTargeted(sa), null);
                if(replaceIn.getSpellAbility().canTarget(newTarget)) {
                    newTargetBlock.add(newTarget);
                    replaceIn.updateTarget(newTargetBlock);
                } else
                    replaceIn.updateTarget(oldTargetBlock);
            } else 
                while(changingTgtSI != null) {
                    // Update targets, with a potential new target
                    SpellAbility changingTgtSA = changingTgtSI.getSpellAbility();
                    TargetChoices newTarget = sa.getActivatingPlayer().getController().chooseNewTargetsFor(changingTgtSA);
                    if ( null != newTarget)
                        changingTgtSI.updateTarget(newTarget);
                    changingTgtSI = changingTgtSI.getSubInstace();
                }

            if (remember) {
                sa.getSourceCard().addRemembered(tgtSA.getSourceCard());
            }
        }
    }
}
