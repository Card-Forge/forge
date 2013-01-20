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
    Transformed(TriggerTransformed.class),
    Tapped(TriggerTaps.class),
    Untaps(TriggerUntaps.class),
    Taps(TriggerTaps.class),

    ChangesZone(TriggerChangesZone.class),

    Clashed(TriggerClashed.class),
    TapsForMana(TriggerTapsForMana.class),
    CounterAdded(TriggerCounterAdded.class),
    CounterRemoved(TriggerCounterRemoved.class),
    Unequip(TriggerUnequip.class),
    DamageDone(TriggerDamageDone.class),
    Championed(TriggerChampioned.class),
    TurnFaceUp(TriggerTurnFaceUp.class),
    Attacks(TriggerAttacks.class),
    AttackerBlocked(TriggerAttackerBlocked.class),
    Blocks(TriggerBlocks.class),
    AttackerUnblocked(TriggerAttackerUnblocked.class),
    ChangesController(TriggerChangesController.class),
    Always(TriggerAlways.class),
    Sacrificed(TriggerSacrificed.class),
    SpellAbilityCast(TriggerSpellAbilityCast.class),
    SpellCast(TriggerSpellAbilityCast.class),
    AbilityCast(TriggerSpellAbilityCast.class),
    Cycled(TriggerCycled.class),
    BecomesTarget(TriggerBecomesTarget.class),
    Phase(TriggerPhase.class),
    AttackersDeclared(TriggerAttackersDeclared.class),
    LifeGained(TriggerLifeGained.class),
    LifeLost(TriggerLifeLost.class),
    Drawn(TriggerDrawn.class),
    Discarded(TriggerDiscarded.class),
    Shuffled(TriggerShuffled.class),
    LandPlayed(TriggerLandPlayed.class),
    LosesGame(TriggerLosesGame.class),
    PlanarDice(TriggerPlanarDice.class),
    PlaneswalkedTo(TriggerPlaneswalkedTo.class),
    PlaneswalkedFrom(TriggerPlaneswalkedFrom.class),
    SetInMotion(TriggerSetInMotion.class);

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
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("No constructor found that would take Map as 1st parameter in class " + classTrigger.getName());
    }
}
