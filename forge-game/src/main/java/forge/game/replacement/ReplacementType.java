package forge.game.replacement;

import forge.game.card.Card;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum ReplacementType {
	AddCounter(ReplaceAddCounter.class),
    Counter(ReplaceCounter.class),
    DamageDone(ReplaceDamage.class),
    Destroy(ReplaceDestroy.class),
    Discard(ReplaceDiscard.class),
    Draw(ReplaceDraw.class),
    GainLife(ReplaceGainLife.class),
    GameLoss(ReplaceGameLoss.class),
    Moved(ReplaceMoved.class),
    ProduceMana(ReplaceProduceMana.class),
    SetInMotion(ReplaceSetInMotion.class),
    TurnFaceUp(ReplaceTurnFaceUp.class),
    Untap(ReplaceUntap.class);
    
    Class<? extends ReplacementEffect> clasz;
    private ReplacementType(Class<? extends ReplacementEffect> cls) {
        clasz = cls;
    }
    
    public static ReplacementType getTypeFor(ReplacementEffect e) {
        final Class<? extends ReplacementEffect> cls = e.getClass();
        for (final ReplacementType v : ReplacementType.values()) {
            if (v.clasz.equals(cls)) {
                return v;
            }
        }
        return null;
    }

    public static ReplacementType smartValueOf(String value) {
        final String valToCompate = value.trim();
        for (final ReplacementType v : ReplacementType.values()) {
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
    public ReplacementEffect createReplacement(Map<String, String> mapParams, Card host, boolean intrinsic) {
        @SuppressWarnings("unchecked")
        Constructor<? extends ReplacementEffect>[] cc = (Constructor<? extends ReplacementEffect>[]) clasz.getDeclaredConstructors();
        for (Constructor<? extends ReplacementEffect> c : cc) {
            Class<?>[] pp = c.getParameterTypes();
            if (pp[0].isAssignableFrom(Map.class)) {
                try {
                    ReplacementEffect res = c.newInstance(mapParams, host, intrinsic);
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
        throw new RuntimeException("No constructor found that would take Map as 1st parameter in class " + clasz.getName());
    }
}
