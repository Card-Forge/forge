package forge.game.replacement;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import forge.game.card.Card;

/**
 * TODO: Write javadoc for this type.
 *
 */
public enum ReplacementType {
    AddCounter(ReplaceAddCounter.class),
    AssignDealDamage(ReplaceAssignDealDamage.class),
    Attached(ReplaceAttached.class),
    BeginPhase(ReplaceBeginPhase.class),
    BeginTurn(ReplaceBeginTurn.class),
    Counter(ReplaceCounter.class),
    CopySpell(ReplaceCopySpell.class),
    CreateToken(ReplaceToken.class),
    DamageDone(ReplaceDamage.class),
    DealtDamage(ReplaceDealtDamage.class),
    DeclareBlocker(ReplaceDeclareBlocker.class),
    Destroy(ReplaceDestroy.class),
    Discard(ReplaceDiscard.class),
    Draw(ReplaceDraw.class),
    DrawCards(ReplaceDrawCards.class),
    GainLife(ReplaceGainLife.class),
    GameLoss(ReplaceGameLoss.class),
    Learn(ReplaceLearn.class),
    LifeReduced(ReplaceLifeReduced.class),
    LoseMana(ReplaceLoseMana.class),
    Mill(ReplaceMill.class),
    Moved(ReplaceMoved.class),
    PayLife(ReplacePayLife.class),
    PlanarDiceResult(ReplacePlanarDiceResult.class),
    ProduceMana(ReplaceProduceMana.class),
    Proliferate(ReplaceProliferate.class),
    RemoveCounter(ReplaceRemoveCounter.class),
    RollPlanarDice(ReplaceRollPlanarDice.class),
    Scry(ReplaceScry.class),
    SetInMotion(ReplaceSetInMotion.class),
    Surveil(ReplaceSurveil.class),
    Tap(ReplaceTap.class),
    Transform(ReplaceTransform.class),
    TurnFaceUp(ReplaceTurnFaceUp.class),
    Untap(ReplaceUntap.class);

    Class<? extends ReplacementEffect> clasz;
    ReplacementType(Class<? extends ReplacementEffect> cls) {
        clasz = cls;
    }

    public static ReplacementType smartValueOf(String value) {
        final String valToCompate = value.trim();
        for (final ReplacementType v : ReplacementType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new RuntimeException("Element " + value + " not found in ReplacementType enum");
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
        throw new RuntimeException("No constructor found that would take Map as 1st parameter in class " + clasz.getName());
    }
}
