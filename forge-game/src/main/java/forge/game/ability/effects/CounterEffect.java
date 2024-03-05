package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellPermanent;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CounterEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Counter");

        boolean isAbility = false;
        for (final SpellAbility tgtSA : getTargetSpells(sa)) {
            sb.append(" ");
            sb.append(tgtSA.getHostCard());
            isAbility = tgtSA.isAbility();
            if (isAbility) {
                sb.append("'s ability");
            }
        }

        if (isAbility && sa.hasParam("DestroyPermanent")) {
            sb.append(" and destroy it");
        }

        if (sa.hasParam("UnlessCost")) {
            sb.append(" unless its controller pays {" + sa.getParam("UnlessCost") + "}");
        }

        sb.append(".");
        return sb.toString();
    } // end counterStackDescription

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getActivatingPlayer().getGame();
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        final CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(params, sa);

        for (final SpellAbility tgtSA : getTargetSpells(sa)) {
            final Card tgtSACard = tgtSA.getHostCard();
            // should remember even that spell cannot be countered
            // currently all effects using this are targeted in case the spell gets countered before
            // so don't need to worry about LKI (else X amounts would be missing)
            if (sa.hasParam("RememberCounteredCMC")) {
                sa.getHostCard().addRemembered(Integer.valueOf(tgtSACard.getCMC()));
            }
            if (sa.hasParam("RememberForCounter")) {
                sa.getHostCard().addRemembered(tgtSACard);
            }

            if (tgtSA.isSpell() && !tgtSA.isCounterableBy(sa)) {
                continue;
            }

            final SpellAbilityStackInstance si = game.getStack().getInstanceMatchingSpellAbilityID(tgtSA);
            if (si == null) {
                continue;
            }

            if (sa.hasParam("CounterNoManaSpell") && tgtSA.isSpell() && tgtSA.getTotalManaSpent() != 0) {
                continue;
            }

            if (sa.hasParam("ConditionWouldDestroy") && !checkForConditionWouldDestroy(sa, tgtSA)) {
                continue;
            }

            if (sa.hasParam("RememberSplicedOntoCounteredSpell")) {
                if (tgtSA.getSplicedCards() != null) {
                    sa.getHostCard().addRemembered(tgtSA.getSplicedCards());
                }
            }

            if (!removeFromStack(tgtSA, sa, si, params)) {
                continue;
            }

            // Destroy Permanent may be able to be turned into a SubAbility
            if (tgtSA.isAbility() && sa.hasParam("DestroyPermanent")) {
                game.getAction().destroy(tgtSACard, sa, true, params);
            }

            if (sa.hasParam("RememberCountered")) {
                sa.getHostCard().addRemembered(tgtSACard);
            }
        }
        zoneMovements.triggerChangesZoneAll(game, sa);
    }

    public static boolean checkForConditionWouldDestroy(SpellAbility sa, SpellAbility tgtSA) {
        List<SpellAbility> testChain = Lists.newArrayList();

        // TODO: add anything that may be important for the test chain here
        SpellAbility currentTgtSA = tgtSA;
        while (currentTgtSA != null) {
            testChain.add(currentTgtSA);
            currentTgtSA = currentTgtSA.getSubAbility();
        }

        for (SpellAbility viableTgtSA : testChain) {
            if (checkSingleSAForConditionWouldDestroy(sa, viableTgtSA)) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkSingleSAForConditionWouldDestroy(SpellAbility sa, SpellAbility tgtSA) {
        Game game = sa.getHostCard().getGame();

        if (tgtSA.getApi() != ApiType.Destroy && tgtSA.getApi() != ApiType.DestroyAll) {
            return false;
        }

        String wouldDestroy = sa.getParam("ConditionWouldDestroy");
        CardCollectionView cardsOTB = game.getCardsIn(ZoneType.Battlefield);
        // Potential candidates that our condition (ConditionWouldDestroy) is checking for
        CardCollection conditionCandidates = CardLists.getValidCards(cardsOTB, wouldDestroy, sa.getActivatingPlayer(), sa.getHostCard(), sa);

        // Determine which cards will be affected by the target SA
        CardCollection affected = new CardCollection();
        if (tgtSA.hasParam("ValidTgts") || tgtSA.hasParam("Defined")) {
            affected.addAll(getDefinedCardsOrTargeted(tgtSA));
        } else if (tgtSA.hasParam("ValidCards")) {
            affected.addAll(CardLists.getValidCards(cardsOTB, tgtSA.getParam("ValidCards"), tgtSA.getActivatingPlayer(), tgtSA.getHostCard(), tgtSA));
        }

        // Determine which of the condition-specific candidates are potentially affected with the target SA
        CardCollection validAffected = new CardCollection();
        for (Card cand : conditionCandidates) {
            if (affected.contains(cand)) {
                validAffected.add(cand);
            }
        }

        // Special case: Wild Swing random destruction - only counter if all targets are valid and each can be destroyed (100% chance
        // to destroy one of the owned lands)
        // TODO: this is hacky... make the detection of this ability more robust and generic?
        boolean isRandomDestruction = false;
        if (validAffected.isEmpty() && tgtSA.getRootAbility().getApi() == ApiType.Pump
            && tgtSA.getRootAbility().hasParam("TargetMax")
            && tgtSA.getRootAbility().getSubAbility() != null
            && tgtSA.getRootAbility().getSubAbility().getApi() == ApiType.ChooseCard
            && tgtSA.getRootAbility().getSubAbility().hasParam("AtRandom")
            && "ChosenCard".equals(tgtSA.getParam("Defined"))) {
            isRandomDestruction = true;
            boolean allValid = true;
            affected.addAll(getDefinedCardsOrTargeted(tgtSA.getRootAbility()));
            for (Card cand : conditionCandidates) {
                if (affected.contains(cand)) {
                    validAffected.add(cand);
                }
            }
            CardCollectionView rootTgts = tgtSA.getRootAbility().getTargets().getTargetCards();
            for (Card rootTgt : rootTgts) {
                if (!validAffected.contains(rootTgt)) {
                    allValid = false;
                    break;
                }
            }
            if (!allValid) {
                return false;
            }
        }

        if (validAffected.isEmpty()) {
            return false;
        } else if (tgtSA.hasParam("Sacrifice")) {
            return false; // Sacrifice doesn't count as Destroy
        }

        // Dry run Destroy on each validAffected to see if it can be destroyed at this moment
        boolean willDestroyCondition = false;
        final boolean noRegen = tgtSA.hasParam("NoRegen");
        Map<AbilityKey, Object> testParams = AbilityKey.newMap();
        testParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());

        boolean willDestroyAll = true;
        for (Card aff : validAffected) {
            if (tgtSA.usesTargeting() && !aff.canBeTargetedBy(tgtSA)) {
                willDestroyAll = false;
                continue; // Should account for Protection/Hexproof/etc.
            }

            Card toBeDestroyed = CardFactory.copyCard(aff, true);

            game.getTriggerHandler().setSuppressAllTriggers(true);
            boolean destroyed = game.getAction().destroy(toBeDestroyed, tgtSA, !noRegen, testParams);
            game.getTriggerHandler().setSuppressAllTriggers(false);

            if (destroyed) {
                willDestroyCondition = true; // this should pick up replacement effects replacing Destroy
                if (!isRandomDestruction) {
                    break;
                }
            } else {
                willDestroyAll = false;
            }
        }

        return isRandomDestruction ? willDestroyAll : willDestroyCondition;
    }

    /**
     * <p>
     * removeFromStack.
     * </p>
     *
     * @param tgtSA
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param srcSA
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param si
     *            a {@link forge.game.spellability.SpellAbilityStackInstance}
     *            object.
     */
    private static boolean removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si, Map<AbilityKey, Object> params) {
        final Game game = tgtSA.getActivatingPlayer().getGame();
        Card movedCard = null;
        final Card c = tgtSA.getHostCard();

        // Run any applicable replacement effects.
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(tgtSA.getHostCard());
        repParams.put(AbilityKey.SpellAbility, tgtSA);
        repParams.put(AbilityKey.Cause, srcSA);
        if (game.getReplacementHandler().run(ReplacementType.Counter, repParams) != ReplacementResult.NotReplaced) {
            return false;
        }
        game.getStack().remove(si);

        // if the target card on stack was a spell with Bestow, then unbestow it
        c.unanimateBestow();

        params.put(AbilityKey.StackSa, tgtSA);
        params.put(AbilityKey.StackSi, si);

        String destination =  srcSA.hasParam("Destination") ? srcSA.getParam("Destination") : tgtSA.isAftermath() ? "Exile" : "Graveyard";
        if (srcSA.hasParam("DestinationChoice")) { //Hinder
            List<String> pos = Arrays.asList(srcSA.getParam("DestinationChoice").split(","));
            destination = srcSA.getActivatingPlayer().getController().chooseSomeType(Localizer.getInstance().getMessage("lblRemoveDestination"), tgtSA, pos, null);
        }
        if (tgtSA.isAbility()) {
            // For Ability-targeted counterspells - do not move it anywhere,
            // even if Destination$ is specified.
        } else if (destination.equals("Graveyard")) {
            movedCard = game.getAction().moveToGraveyard(c, srcSA, params);
        } else if (destination.equals("Exile")) {
            if (!c.canExiledBy(srcSA, true)) {
                return false;
            }
            movedCard = game.getAction().exile(c, srcSA, params);
        } else if (destination.equals("Hand")) {
            movedCard = game.getAction().moveToHand(c, srcSA, params);
        } else if (destination.equals("Battlefield")) {
            // card is no longer cast
            c.setCastSA(null);
            c.setCastFrom(null);
            if (tgtSA instanceof SpellPermanent) {
                c.setController(srcSA.getActivatingPlayer(), 0);
                movedCard = game.getAction().moveToPlay(c, srcSA.getActivatingPlayer(), srcSA, params);
            } else {
                movedCard = game.getAction().moveToPlay(c, srcSA.getActivatingPlayer(), srcSA, params);
                movedCard.setController(srcSA.getActivatingPlayer(), 0);
            }
        } else if (destination.equals("TopOfLibrary")) {
            movedCard = game.getAction().moveToLibrary(c, srcSA, params);
        } else if (destination.equals("BottomOfLibrary")) {
            movedCard = game.getAction().moveToBottomOfLibrary(c, srcSA, params);
        } else if (destination.equals("ShuffleIntoLibrary")) {
            movedCard = game.getAction().moveToBottomOfLibrary(c, srcSA, params);
            c.getController().shuffle(srcSA);
        } else {
            throw new IllegalArgumentException("AbilityFactory_CounterMagic: Invalid Destination argument for card "
                    + srcSA.getHostCard().getName());
        }
        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.Player, tgtSA.getActivatingPlayer());
        runParams.put(AbilityKey.Cause, srcSA);
        runParams.put(AbilityKey.CounteredSA, tgtSA);
        game.getTriggerHandler().runTrigger(TriggerType.Countered, runParams, false);

        if (!tgtSA.isAbility()) {
            game.getGameLog().add(GameLogEntryType.ZONE_CHANGE, "Send countered spell to " + destination);
        }

        return true;
    }

}
