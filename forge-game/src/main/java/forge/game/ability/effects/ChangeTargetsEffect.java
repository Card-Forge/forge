package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameObjectPredicates;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.MagicStack;
import forge.util.Aggregates;
import forge.util.Localizer;

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
        final Player activator = sa.getActivatingPlayer();
        final Player chooser = sa.hasParam("Chooser") ? getDefinedPlayersOrTargeted(sa, "Chooser").get(0) : sa.getActivatingPlayer();

        final MagicStack stack = activator.getGame().getStack();
        for (final SpellAbility tgtSA : sas) {
            SpellAbilityStackInstance si = stack.getInstanceMatchingSpellAbilityID(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, there isn't really a target
                continue;
            }

            SpellAbilityStackInstance changingTgtSI = si;

            // Redirect rules read 'you MAY choose new targets' ... okay!
            // TODO: Don't even ask to change targets, if the SA and subs don't actually have targets
            boolean isOptional = sa.hasParam("Optional");
            if (isOptional && !chooser.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantChangeAbilityTargets", tgtSA.getHostCard().toString()), null)) {
                continue;
            }
            if (sa.hasParam("ChangeSingleTarget")) {
                // 1. choose a target of target spell
                List<Pair<SpellAbilityStackInstance, GameObject>> allTargets = new ArrayList<>();
                while (changingTgtSI != null) {
                    SpellAbility changedSa = changingTgtSI.getSpellAbility();
                    if (changedSa.usesTargeting()) {
                        for (GameObject it : changedSa.getTargets())
                            allTargets.add(ImmutablePair.of(changingTgtSI, it));
                    }
                    changingTgtSI = changingTgtSI.getSubInstance();
                }
                if (allTargets.isEmpty()) {
                    return;
                }

                Pair<SpellAbilityStackInstance, GameObject> chosenTarget = chooser.getController().chooseTarget(sa, allTargets);
                // 2. prepare new target choices
                SpellAbilityStackInstance replaceIn = chosenTarget.getKey();
                GameObject oldTarget = chosenTarget.getValue();
                TargetChoices oldTargetBlock = replaceIn.getTargetChoices();
                TargetChoices newTargetBlock = oldTargetBlock.clone();
                // gets the divided value from old target
                Integer div = oldTargetBlock.getDividedValue(oldTarget);
                // 3. test if updated choices would be correct.
                GameObject newTarget = Iterables.getFirst(getDefinedCardsOrTargeted(sa, "DefinedMagnet"), null);

                // CR 115.3. The same target can't be chosen multiple times for
                // any one instance of the word “target” on a spell or ability.
                if (!oldTargetBlock.contains(newTarget) && replaceIn.getSpellAbility().canTarget(newTarget)) {
                    newTargetBlock.remove(oldTarget);
                    newTargetBlock.add(newTarget);
                    if (div != null) {
                        newTargetBlock.addDividedAllocation(newTarget, div);
                    }
                    replaceIn.updateTarget(newTargetBlock, sa.getHostCard());
                }
            } else {
                while (changingTgtSI != null) {
                    SpellAbility changingTgtSA = changingTgtSI.getSpellAbility();
                    if (changingTgtSA.usesTargeting()) {
                        // random target and DefinedMagnet works on single targets
                        if (sa.hasParam("RandomTarget")) {
                            int div = changingTgtSA.getTotalDividedValue();
                            List<GameEntity> candidates = changingTgtSA.getTargetRestrictions().getAllCandidates(changingTgtSA, true);
                            if (sa.hasParam("RandomTargetRestriction")) {
                                candidates.removeIf(new java.util.function.Predicate<GameEntity>() {
                                    @Override
                                    public boolean test(GameEntity c) {
                                        return !c.isValid(sa.getParam("RandomTargetRestriction").split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa);
                                    }
                                });
                            }
                            // CR 115.7a If a target can't be changed to another legal target, the original target is unchanged
                            if (candidates.isEmpty()) {
                                return;
                            }
                            changingTgtSA.resetTargets();
                            GameEntity choice = Aggregates.random(candidates);
                            changingTgtSA.getTargets().add(choice);
                            if (changingTgtSA.isDividedAsYouChoose()) {
                                changingTgtSA.addDividedAllocation(choice, div);
                            }

                            changingTgtSI.updateTarget(changingTgtSA.getTargets(), sa.getHostCard());
                        }
                        else if (sa.hasParam("DefinedMagnet")) {
                            GameObject newTarget = Iterables.getFirst(getDefinedCardsOrTargeted(sa, "DefinedMagnet"), null);
                            if (newTarget != null && changingTgtSA.canTarget(newTarget)) {
                                int div = changingTgtSA.getTotalDividedValue();
                                changingTgtSA.resetTargets();
                                changingTgtSA.getTargets().add(newTarget);
                                changingTgtSI.updateTarget(changingTgtSA.getTargets(), sa.getHostCard());
                                if (changingTgtSA.isDividedAsYouChoose()) {
                                    changingTgtSA.addDividedAllocation(newTarget, div);
                                }
                            }
                        } else {
                            // Update targets, with a potential new target
                            Card source = sa.getHostCard();
                            if (changingTgtSA.getTargetCard() != null) {
                                // try to use old target so "Other" restriction of Meddle works
                                source = changingTgtSA.getTargetCard();
                            }
                            Predicate<GameObject> filter = sa.hasParam("TargetRestriction") ? GameObjectPredicates.restriction(sa.getParam("TargetRestriction").split(","), activator, source, sa) : null;
                            // TODO Creature.Other might not work yet as it should
                            TargetChoices newTarget = chooser.getController().chooseNewTargetsFor(changingTgtSA, filter, false);
                            changingTgtSI.updateTarget(newTarget, sa.getHostCard());
                        }
                    }
                    changingTgtSI = changingTgtSI.getSubInstance();
                }
            }
        }
    }
}
