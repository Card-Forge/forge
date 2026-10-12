package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.game.ability.AbilityKey;
import forge.game.trigger.TriggerType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import forge.game.GameObject;
import forge.game.GameObjectPredicates;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Localizer;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class ChangeTargetsEffect extends SpellAbilityEffect {

    @Override
    public void buildSpellAbility(SpellAbility sa) {
        super.buildSpellAbility(sa);
        if (sa.usesTargeting()) {
            sa.getTargetRestrictions().setZone(ZoneType.Stack);
        }
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<SpellAbility> sas = getTargetSpells(sa);
        final boolean random = sa.hasParam("RandomTarget");
        final Player activator = sa.getActivatingPlayer();
        final Player chooser = sa.hasParam("Chooser") ? getDefinedPlayersOrTargeted(sa, "Chooser").get(0) : activator;

        final MagicStack stack = activator.getGame().getStack();

        for (final SpellAbility tgtSA : sas) {
            SpellAbilityStackInstance si = stack.getInstanceMatchingSpellAbilityID(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, there isn't really a target
                continue;
            }

            SpellAbilityStackInstance changingTgtSI = si;
            Set<GameObject> distinctObjects = Sets.newHashSet();

            // Redirect rules read 'you MAY choose new targets' ... okay!
            // TODO: Don't even ask to change targets, if the SA and subs don't actually have targets
            if (sa.hasParam("Optional") && !chooser.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantChangeAbilityTargets", tgtSA.getHostCard().toString()), null)) {
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
                TargetChoices newTargetBlock = replaceIn.getTargetChoices();
                TargetChoices oldTargetBlock = newTargetBlock.clone();
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
                    replaceIn.updateTarget(oldTargetBlock, distinctObjects);
                }
            } else if (random) {
                // CR 115.7a: changing "the target(s)" is all-or-nothing, so picks are staged first
                List<Pair<SpellAbilityStackInstance, GameObject>> picks = new ArrayList<>();
                boolean feasible = true;
                while (changingTgtSI != null) {
                    SpellAbility changingTgtSA = changingTgtSI.getSpellAbility();
                    if (changingTgtSA.usesTargeting()) {
                        List<GameObject> candidates = Lists.newArrayList(changingTgtSA.getTargetRestrictions().getAllCandidates(changingTgtSA));
                        if (changingTgtSA.getTargetRestrictions().getZone().contains(ZoneType.Stack)) {
                            for (final SpellAbilityStackInstance ab : activator.getGame().getStack()) {
                                SpellAbility abilityOnStack = ab.getSpellAbility();
                                if (changingTgtSA.canTargetSpellAbility(abilityOnStack)) {
                                    candidates.add(abilityOnStack);
                                }
                            }
                        }
                        if (sa.hasParam("RandomTargetRestriction")) {
                            candidates.removeIf(c -> !c.isValid(sa.getParam("RandomTargetRestriction").split(","), activator, sa.getHostCard(), sa));
                        }
                        if (candidates.isEmpty()) {
                            feasible = false;
                            break;
                        }
                        picks.add(ImmutablePair.of(changingTgtSI, Aggregates.random(candidates)));
                    }
                    changingTgtSI = changingTgtSI.getSubInstance();
                }
                if (feasible) {
                    for (Pair<SpellAbilityStackInstance, GameObject> pick : picks) {
                        SpellAbilityStackInstance pickSI = pick.getKey();
                        SpellAbility pickSA = pickSI.getSpellAbility();
                        GameObject choice = pick.getValue();
                        TargetChoices oldTarget = pickSA.getTargets();
                        pickSA.resetTargets();
                        pickSA.getTargets().add(choice);
                        if (pickSA.isDividedAsYouChoose()) {
                            pickSA.addDividedAllocation(choice, pickSA.getTotalDividedValue());
                        }
                        pickSI.updateTarget(oldTarget, distinctObjects);
                    }
                }
            } else {
                while (changingTgtSI != null) {
                    SpellAbility changingTgtSA = changingTgtSI.getSpellAbility();
                    if (changingTgtSA.usesTargeting()) {
                        if (sa.hasParam("DefinedMagnet")) {
                            GameObject newTarget = Iterables.getFirst(getDefinedCardsOrTargeted(sa, "DefinedMagnet"), null);
                            if (newTarget != null && changingTgtSA.canTarget(newTarget)) {
                                int div = changingTgtSA.getTotalDividedValue();
                                TargetChoices oldTarget = changingTgtSA.getTargets();
                                changingTgtSA.resetTargets();
                                changingTgtSA.getTargets().add(newTarget);
                                if (changingTgtSA.isDividedAsYouChoose()) {
                                    changingTgtSA.addDividedAllocation(newTarget, div);
                                }
                                changingTgtSI.updateTarget(oldTarget, distinctObjects);
                            }
                        } else {
                            // Update targets, with a potential new target
                            Card source = sa.getHostCard();
                            if (changingTgtSA.getTargetCard() != null) {
                                // try to use old target so "Other" restriction of Meddle works
                                source = changingTgtSA.getTargetCard();
                            }
                            Predicate<GameObject> filter = sa.hasParam("TargetRestriction") ? GameObjectPredicates.restriction(sa.getParam("TargetRestriction").split(","), activator, source, sa) : null;
                            TargetChoices oldTarget = changingTgtSA.getTargets();
                            chooser.getController().chooseNewTargetsFor(changingTgtSA, filter, false);
                            changingTgtSI.updateTarget(oldTarget, distinctObjects);
                        }
                    }
                    changingTgtSI = changingTgtSI.getSubInstance();
                }
            }

            for (GameObject tgt : distinctObjects) {
                Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.SourceSA, tgtSA);
                runParams.put(AbilityKey.Target, tgt);
                if (tgt instanceof Card c) {
                    if (!c.hasBecomeTargetThisTurn()) {
                        runParams.put(AbilityKey.FirstTime, null);
                    }
                    if (c.isValiant(tgtSA.getActivatingPlayer())) {
                        runParams.put(AbilityKey.Valiant, null);
                    }
                    c.addTargetFromThisTurn(tgtSA.getActivatingPlayer());
                }
                activator.getGame().getTriggerHandler().runTrigger(TriggerType.BecomesTarget, runParams, false);
            }
            // Only run BecomesTargetOnce when at least one target is changed
            if (!distinctObjects.isEmpty()) {
                Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.SourceSA, tgtSA);
                runParams.put(AbilityKey.Targets, distinctObjects);
                // cause is the permanent/card responsible for this retargeting (e.g. the Chef's
                // Kiss-style redirect source), not necessarily whoever ends up choosing/rolling
                // the new target - that's fine for "did You cause this" checks (ValidCause$
                // Card.YouCtrl) as long as Chooser$ (a different player making the actual choice)
                // is never combined with random retargeting; it isn't today.
                runParams.put(AbilityKey.Cause, sa.getHostCard());
                if (random) {
                    runParams.put(AbilityKey.Random, true);
                }
                activator.getGame().getTriggerHandler().runTrigger(TriggerType.BecomesTargetOnce, runParams, false);
            }
        }
    }
}
