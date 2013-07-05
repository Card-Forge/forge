package forge.card.trigger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import forge.Card;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum TriggerType {
    AbilityCast(TriggerSpellAbilityCast.class),
    Always(TriggerAlways.class),
    Attached(TriggerAttached.class),
    AttackerBlocked(TriggerAttackerBlocked.class),
    AttackersDeclared(TriggerAttackersDeclared.class),
    AttackerUnblocked(TriggerAttackerUnblocked.class),
    Attacks(TriggerAttacks.class),
    BecomesTarget(TriggerBecomesTarget.class),
    Blocks(TriggerBlocks.class),
    Championed(TriggerChampioned.class),
    ChangesController(TriggerChangesController.class),
    ChangesZone(TriggerChangesZone.class),
    Clashed(TriggerClashed.class),
    CombatDamageDoneOnce(TriggerCombatDamageDoneOnce.class),
    CounterAdded(TriggerCounterAdded.class),
    Countered(TriggerCountered.class),
    CounterRemoved(TriggerCounterRemoved.class),
    Cycled(TriggerCycled.class),
    DamageDone(TriggerDamageDone.class),
    Destroyed(TriggerDestroyed.class),
    Devoured(TriggerDevoured.class),
    Discarded(TriggerDiscarded.class),
    Drawn(TriggerDrawn.class),
    Evolved(TriggerEvolved.class),
    FlippedCoin(TriggerFlippedCoin.class),
    LandPlayed(TriggerLandPlayed.class),
    LifeGained(TriggerLifeGained.class),
    LifeLost(TriggerLifeLost.class),
    LosesGame(TriggerLosesGame.class),
    NewGame(TriggerNewGame.class),
    PayCumulativeUpkeep(TriggerPayCumulativeUpkeep.class),
    Phase(TriggerPhase.class),
    PlanarDice(TriggerPlanarDice.class),
    PlaneswalkedFrom(TriggerPlaneswalkedFrom.class),
    PlaneswalkedTo(TriggerPlaneswalkedTo.class),
    Sacrificed(TriggerSacrificed.class),
    SetInMotion(TriggerSetInMotion.class),
    Shuffled(TriggerShuffled.class),
    SpellAbilityCast(TriggerSpellAbilityCast.class),
    SpellCast(TriggerSpellAbilityCast.class),
    Tapped(TriggerTaps.class),
    Taps(TriggerTaps.class),
    TapsForMana(TriggerTapsForMana.class),
    Transformed(TriggerTransformed.class),
    TurnFaceUp(TriggerTurnFaceUp.class),
    Unequip(TriggerUnequip.class),
    Untaps(TriggerUntaps.class);

    private final Class<? extends Trigger> classTrigger;
    private TriggerType(Class<? extends Trigger> clasz) {
        classTrigger = clasz;
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
        @SuppressWarnings("unchecked")
        Constructor<? extends Trigger>[] cc = (Constructor<? extends Trigger>[]) classTrigger.getDeclaredConstructors();
        for (Constructor<? extends Trigger> c : cc) {
            Class<?>[] pp = c.getParameterTypes();
            if (pp[0].isAssignableFrom(Map.class)) {
                try {
                    Trigger res = c.newInstance(mapParams, host, intrinsic);
                    res.setMode(this);
                    return res;
                } catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("No constructor found that would take Map as 1st parameter in class " + classTrigger.getName());
    }
}
