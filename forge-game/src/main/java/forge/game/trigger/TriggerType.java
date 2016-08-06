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
    AbilityCast(TriggerSpellAbilityCast.class),
    Always(TriggerAlways.class),
    Attached(TriggerAttached.class),
    AttackerBlocked(TriggerAttackerBlocked.class),
    AttackerBlockedByCreature(TriggerAttackerBlockedByCreature.class),
    AttackersDeclared(TriggerAttackersDeclared.class),
    AttackerUnblocked(TriggerAttackerUnblocked.class),
    Attacks(TriggerAttacks.class),
    BecomeMonstrous(TriggerBecomeMonstrous.class),
    BecomeRenowned(TriggerBecomeRenowned.class),
    BecomesTarget(TriggerBecomesTarget.class),
    BlockersDeclared(TriggerBlockersDeclared.class),
    Blocks(TriggerBlocks.class),
    Championed(TriggerChampioned.class),
    ChangesController(TriggerChangesController.class),
    ChangesZone(TriggerChangesZone.class),
    ChangesZoneAll(TriggerChangesZoneAll.class),
    Clashed(TriggerClashed.class),
    CombatDamageDoneOnce(TriggerCombatDamageDoneOnce.class),
    CounterAdded(TriggerCounterAdded.class),
    CounterAddedOnce(TriggerCounterAddedOnce.class),
    Countered(TriggerCountered.class),
    CounterRemoved(TriggerCounterRemoved.class),
    Cycled(TriggerCycled.class),
    DamageDone(TriggerDamageDone.class),
    DealtCombatDamageOnce(TriggerDealtCombatDamageOnce.class),
    Destroyed(TriggerDestroyed.class),
    Devoured(TriggerDevoured.class),
    Discarded(TriggerDiscarded.class),
    Drawn(TriggerDrawn.class),
    Evolved(TriggerEvolved.class),
    Exploited(TriggerExploited.class),
    Fight(TriggerFight.class),
    FlippedCoin(TriggerFlippedCoin.class),
    Investigated(TriggerInvestigated.class),
    LandPlayed(TriggerLandPlayed.class),
    LifeGained(TriggerLifeGained.class),
    LifeLost(TriggerLifeLost.class),
    LosesGame(TriggerLosesGame.class),
    NewGame(TriggerNewGame.class),
    PayCumulativeUpkeep(TriggerPayCumulativeUpkeep.class),
    PayEcho(TriggerPayEcho.class),
    Phase(TriggerPhase.class),
    PhaseIn(TriggerPhaseIn.class),
    PhaseOut(TriggerPhaseOut.class),
    PlanarDice(TriggerPlanarDice.class),
    PlaneswalkedFrom(TriggerPlaneswalkedFrom.class),
    PlaneswalkedTo(TriggerPlaneswalkedTo.class),
    Sacrificed(TriggerSacrificed.class),
    Scry(TriggerScry.class),
    SearchedLibrary(TriggerSearchedLibrary.class),
    SetInMotion(TriggerSetInMotion.class),
    Shuffled(TriggerShuffled.class),
    SpellAbilityCast(TriggerSpellAbilityCast.class),
    SpellCast(TriggerSpellAbilityCast.class),
    Tapped(TriggerTaps.class),
    Taps(TriggerTaps.class),
    TapsForMana(TriggerTapsForMana.class),
    Transformed(TriggerTransformed.class),
    TurnBegin(TriggerTurnBegin.class),
    TurnFaceUp(TriggerTurnFaceUp.class),
    Unequip(TriggerUnequip.class),
    Untaps(TriggerUntaps.class),
    Vote(TriggerVote.class);

    private final Class<? extends Trigger> classTrigger;
    private final Constructor<? extends Trigger> constructor;

    private TriggerType(Class<? extends Trigger> clasz) {
        classTrigger = clasz;
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
     * @param string
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
    
    public static TriggerType getTypeFor(Trigger t) {
        final Class<? extends Trigger> cls = t.getClass();
        for (final TriggerType v : TriggerType.values()) {
            if (v.classTrigger.equals(cls)) {
                return v;
            }
        }
        return null;
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
