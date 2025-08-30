package forge.game.trigger;

import forge.game.card.Card;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;


/**
 * TODO: Write javadoc for this type.
 *
 */
public enum TriggerType {
    Abandoned(TriggerAbandoned.class),
    AbilityCast(TriggerSpellAbilityCastOrCopy.class),
    AbilityResolves(TriggerAbilityResolves.class),
    AbilityTriggered(TriggerAbilityTriggered.class),
    Adapt(TriggerAdapt.class),
    Airbend(TriggerElementalbend.class),
    Always(TriggerAlways.class),
    Attached(TriggerAttached.class),
    AttackerBlocked(TriggerAttackerBlocked.class),
    AttackerBlockedOnce(TriggerAttackerBlockedOnce.class),
    AttackerBlockedByCreature(TriggerAttackerBlockedByCreature.class),
    AttackersDeclared(TriggerAttackersDeclared.class),
    AttackersDeclaredOneTarget(TriggerAttackersDeclared.class),
    AttackerUnblocked(TriggerAttackerUnblocked.class),
    AttackerUnblockedOnce(TriggerAttackerUnblockedOnce.class),
    Attacks(TriggerAttacks.class),
    BecomeMonarch(TriggerBecomeMonarch.class),
    BecomeMonstrous(TriggerBecomeMonstrous.class),
    BecomeRenowned(TriggerBecomeRenowned.class),
    BecomesCrewed(TriggerBecomesCrewed.class),
    BecomesPlotted(TriggerBecomesPlotted.class),
    BecomesSaddled(TriggerBecomesSaddled.class),
    BecomesTarget(TriggerBecomesTarget.class),
    BecomesTargetOnce(TriggerBecomesTargetOnce.class),
    BlockersDeclared(TriggerBlockersDeclared.class),
    Blocks(TriggerBlocks.class),
    CaseSolved(TriggerCaseSolved.class),
    Championed(TriggerChampioned.class),
    ChangesController(TriggerChangesController.class),
    ChangesZone(TriggerChangesZone.class),
    ChangesZoneAll(TriggerChangesZoneAll.class),
    ChaosEnsues(TriggerChaosEnsues.class),
    ClaimPrize(TriggerClaimPrize.class),
    Clashed(TriggerClashed.class),
    ClassLevelGained(TriggerClassLevelGained.class),
    CommitCrime(TriggerCommitCrime.class),
    ConjureAll(TriggerConjureAll.class),
    CollectEvidence(TriggerCollectEvidence.class),
    CounterAdded(TriggerCounterAdded.class),
    CounterAddedOnce(TriggerCounterAddedOnce.class),
    CounterPlayerAddedAll(TriggerCounterPlayerAddedAll.class),
    CounterTypeAddedAll(TriggerCounterTypeAddedAll.class),
    CounterAddedAll(TriggerCounterAddedAll.class),
    Countered(TriggerCountered.class),
    CounterRemoved(TriggerCounterRemoved.class),
    CounterRemovedOnce(TriggerCounterRemovedOnce.class),
    CrankContraption(TriggerCrankContraption.class),
    Crewed(TriggerCrewedSaddled.class),
    Cycled(TriggerCycled.class),
    DamageAll(TriggerDamageAll.class),
    DamageDealtOnce(TriggerDamageDealtOnce.class),
    DamageDone(TriggerDamageDone.class),
    DamageDoneOnce(TriggerDamageDoneOnce.class),
    DamageDoneOnceByController(TriggerDamageDoneOnceByController.class),
    DamagePreventedOnce(TriggerDamagePreventedOnce.class),
    DayTimeChanges (TriggerDayTimeChanges.class),
    Destroyed(TriggerDestroyed.class),
    Devoured(TriggerDevoured.class),
    Discarded(TriggerDiscarded.class),
    DiscardedAll(TriggerDiscardedAll.class),
    Discover(TriggerDiscover.class),
    Drawn(TriggerDrawn.class),
    DungeonCompleted(TriggerCompletedDungeon.class),
    Earthbend(TriggerElementalbend.class),
    Evolved(TriggerEvolved.class),
    ExcessDamage(TriggerExcessDamage.class),
    ExcessDamageAll(TriggerExcessDamageAll.class),
    ElementalBend(TriggerElementalbend.class),
    Enlisted(TriggerEnlisted.class),
    Exerted(TriggerExerted.class),
    Exiled(TriggerExiled.class),
    Exploited(TriggerExploited.class),
    Explores(TriggerExplores.class),
    Fight(TriggerFight.class),
    FightOnce(TriggerFightOnce.class),
    Firebend(TriggerElementalbend.class),
    FlippedCoin(TriggerFlippedCoin.class),
    Forage(TriggerForage.class),
    Foretell(TriggerForetell.class),
    FullyUnlock(TriggerFullyUnlock.class),
    GiveGift(TriggerGiveGift.class),
    Immediate(TriggerImmediate.class),
    Investigated(TriggerInvestigated.class),
    LandPlayed(TriggerLandPlayed.class),
    LifeChanged(TriggerLifeChanged.class),
    LifeGained(TriggerLifeGained.class),
    LifeLost(TriggerLifeLost.class),
    LifeLostAll(TriggerLifeLostAll.class),
    LosesGame(TriggerLosesGame.class),
    ManaAdded(TriggerManaAdded.class),
    ManaExpend(TriggerManaExpend.class),
    ManifestDread(TriggerManifestDread.class),
    Mentored(TriggerMentored.class),
    Milled(TriggerMilled.class),
    MilledOnce(TriggerMilledOnce.class),
    MilledAll(TriggerMilledAll.class),
    Mutates(TriggerMutates.class),
    NewGame(TriggerNewGame.class),
    PayCumulativeUpkeep(TriggerPayCumulativeUpkeep.class),
    PayEcho(TriggerPayEcho.class),
    PayLife(TriggerPayLife.class),
    Phase(TriggerPhase.class),
    PhaseIn(TriggerPhaseIn.class),
    PhaseOut(TriggerPhaseOut.class),
    PhaseOutAll(TriggerPhaseOutAll.class),
    PlanarDice(TriggerPlanarDice.class),
    PlaneswalkedFrom(TriggerPlaneswalkedFrom.class),
    PlaneswalkedTo(TriggerPlaneswalkedTo.class),
    Proliferate(TriggerProliferate.class),
    RingTemptsYou(TriggerRingTemptsYou.class),
    RolledDie(TriggerRolledDie.class),
    RolledDieOnce(TriggerRolledDieOnce.class),
    RoomEntered(TriggerEnteredRoom.class),
    Saddled(TriggerCrewedSaddled.class),
    Sacrificed(TriggerSacrificed.class),
    SacrificedOnce(TriggerSacrificedOnce.class),
    Scry(TriggerScry.class),
    SearchedLibrary(TriggerSearchedLibrary.class),
    SeekAll(TriggerSeekAll.class),
    SetInMotion(TriggerSetInMotion.class),
    Shuffled(TriggerShuffled.class),
    Specializes(TriggerSpecializes.class),
    SpellAbilityCast(TriggerSpellAbilityCastOrCopy.class),
    SpellAbilityCopy(TriggerSpellAbilityCastOrCopy.class),
    SpellCast(TriggerSpellAbilityCastOrCopy.class),
    SpellCastOrCopy(TriggerSpellAbilityCastOrCopy.class),
    SpellCopy(TriggerSpellAbilityCastOrCopy.class),
    Stationed(TriggerCrewedSaddled.class),
    Surveil(TriggerSurveil.class),
    TakesInitiative(TriggerTakesInitiative.class),
    TapAll(TriggerTapAll.class),
    Taps(TriggerTaps.class),
    TapsForMana(TriggerTapsForMana.class),
    TokenCreated(TriggerTokenCreated.class),
    TokenCreatedOnce(TriggerTokenCreatedOnce.class),
    Trains(TriggerTrains.class),
    Transformed(TriggerTransformed.class),
    TurnBegin(TriggerTurnBegin.class),
    TurnFaceUp(TriggerTurnFaceUp.class),
    Unattach(TriggerUnattach.class),
    UnlockDoor(TriggerUnlockDoor.class),
    UntapAll(TriggerUntapAll.class),
    Untaps(TriggerUntaps.class),
    VisitAttraction(TriggerVisitAttraction.class),
    Vote(TriggerVote.class),
    Waterbend(TriggerElementalbend.class)          
    ;

    private final Constructor<? extends Trigger> constructor;

    TriggerType(Class<? extends Trigger> clasz) {
        constructor = findConstructor(clasz);
    }

    private static Constructor<? extends Trigger> findConstructor(Class<? extends Trigger> clasz) {
        @SuppressWarnings("unchecked")
        Constructor<? extends Trigger>[] cc = (Constructor<? extends Trigger>[]) clasz.getDeclaredConstructors();
        for (Constructor<? extends Trigger> c : cc) {
            Class<?>[] pp = c.getParameterTypes();
            if (pp[0].isAssignableFrom(Map.class)) {
                return c;
            }
        }
        throw new RuntimeException("No constructor found that would take Map as 1st parameter in class " + clasz.getName());
    }

    /**
     * TODO: Write javadoc for this method.
     * @param value
     * @return
     */
    public static TriggerType smartValueOf(String value) {
        final String valToCompate = value.trim();
        for (final TriggerType v : TriggerType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in TriggerType enum");
    }

    /**
     * TODO: Write javadoc for this method.
     * @param mapParams
     * @param host
     * @param intrinsic
     * @return
     */
    public Trigger createTrigger(Map<String, String> mapParams, Card host, boolean intrinsic) {
        try {
            Trigger res = constructor.newInstance(mapParams, host, intrinsic);
            res.setMode(this);
            return res;
        } catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
