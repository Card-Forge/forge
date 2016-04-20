package forge.game.ability.effects;

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.MagicStack;
import forge.util.Aggregates;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

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
        final Player activator = sa.getActivatingPlayer();

        final MagicStack stack = activator.getGame().getStack();
        for (final SpellAbility tgtSA : sas) {
            SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, there isn't really a target
                continue;
            }

            boolean changesOneTarget = sa.hasParam("ChangeSingleTarget"); // The only card known to replace targets with self is Spellskite
            // There is also Muck Drubb but it replaces ALL occurences of a single target with itself (unlike Spellskite that has to be activated for each).

            SpellAbilityStackInstance changingTgtSI = si;
            Player chooser = sa.getActivatingPlayer();

            // Redirect rules read 'you MAY choose new targets' ... okay!
            // TODO: Don't even ask to change targets, if the SA and subs don't actually have targets
            boolean isOptional = sa.hasParam("Optional");
            if (isOptional && !chooser.getController().confirmAction(sa, null, "Do you want to change targets of " + tgtSA.getHostCard() + "?")) {
                 continue;
            }
            if (changesOneTarget) {
                // 1. choose a target of target spell
                List<Pair<SpellAbilityStackInstance, GameObject>> allTargets = new ArrayList<>();
                while(changingTgtSI != null) {
                    SpellAbility changedSa = changingTgtSI.getSpellAbility(true); 
                    if (changedSa.usesTargeting()) {
                        for(GameObject it : changedSa.getTargets().getTargets())
                            allTargets.add(ImmutablePair.of(changingTgtSI, it));
                    }
                    changingTgtSI = changingTgtSI.getSubInstance();
                }
                if (allTargets.isEmpty()) {
                    // is it an error or not?
                    System.err.println("Player managed to target a spell without targets with Spellskite's ability.");
                    return;
                }

                Pair<SpellAbilityStackInstance, GameObject> chosenTarget = chooser.getController().chooseTarget(sa, allTargets);
                // 2. prepare new target choices
                SpellAbilityStackInstance replaceIn = chosenTarget.getKey();
                GameObject oldTarget = chosenTarget.getValue();
                TargetChoices oldTargetBlock = replaceIn.getTargetChoices();
                TargetChoices newTargetBlock = oldTargetBlock.clone();
                newTargetBlock.remove(oldTarget);
                replaceIn.updateTarget(newTargetBlock);
                // 3. test if updated choices would be correct.
                GameObject newTarget = Iterables.getFirst(getDefinedCardsOrTargeted(sa), null);
                if (replaceIn.getSpellAbility(true).canTarget(newTarget)) {
                    newTargetBlock.add(newTarget);
                    replaceIn.updateTarget(newTargetBlock);
                }
                else {
                    replaceIn.updateTarget(oldTargetBlock);
                }
            }
            else {
                while(changingTgtSI != null) {
                    SpellAbility changingTgtSA = changingTgtSI.getSpellAbility(true);
                    if (sa.hasParam("RandomTarget")){
                        changingTgtSA.resetTargets();
                        List<GameEntity> candidates = changingTgtSA.getTargetRestrictions().getAllCandidates(changingTgtSA, true);
                        GameEntity choice = Aggregates.random(candidates);
                        changingTgtSA.getTargets().add(choice);
                        changingTgtSI.updateTarget(changingTgtSA.getTargets());
                    }
                    else if (sa.hasParam("DefinedMagnet")){
                        GameObject newTarget = Iterables.getFirst(getDefinedCardsOrTargeted(sa, "DefinedMagnet"), null);
                        if (changingTgtSA.canTarget(newTarget)) {
                            changingTgtSA.resetTargets();
                            changingTgtSA.getTargets().add(newTarget);
                            changingTgtSI.updateTarget(changingTgtSA.getTargets());
                        }
                    }
                    else {
                        // Update targets, with a potential new target
                        TargetChoices newTarget = sa.getActivatingPlayer().getController().chooseNewTargetsFor(changingTgtSA);
                        if (null != newTarget) {
                            if (sa.hasParam("TargetRestriction")) {
                                if (newTarget.getFirstTargetedCard() != null && newTarget.getFirstTargetedCard().
                                        isValid(sa.getParam("TargetRestriction").split(","), activator, sa.getHostCard(), sa)) {
                                    changingTgtSI.updateTarget(newTarget);
                                } else if (newTarget.getFirstTargetedPlayer() != null && newTarget.getFirstTargetedPlayer().
                                        isValid(sa.getParam("TargetRestriction").split(","), activator, sa.getHostCard(), sa)) {
                                    changingTgtSI.updateTarget(newTarget);
                                }
                            } else {
                                changingTgtSI.updateTarget(newTarget);
                            }
                        }
                    }
                    changingTgtSI = changingTgtSI.getSubInstance();
                }
            }
            if (remember) {
                sa.getHostCard().addRemembered(tgtSA.getHostCard());
            }
        }
    }
}
