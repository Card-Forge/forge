package forge.card.abilityfactory;

import java.util.HashMap;

import forge.AllZone;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

/**
 * Created by IntelliJ IDEA. User: Administrator Date: 5/18/11 Time: 8:53 PM To
 * change this template use File | Settings | File Templates.
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryDelayedTrigger {
    /** Constant <code>tempCreator</code>. */
    private static AbilityFactory tempCreator = new AbilityFactory();

    /**
     * <p>
     * getAbility.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getAbility(final AbilityFactory af) {
        final SpellAbility ability = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7502962478028160305L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDelayedTrigger.delTrigCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDelayedTrigger.doResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDelayedTrigger.delTrigStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDelayedTrigger.doTriggerAI(af, this, mandatory);
            }
        };
        return ability;
    }

    /**
     * <p>
     * getSpell.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getSpell(final AbilityFactory af) {
        final SpellAbility spell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -6981410664429186904L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDelayedTrigger.delTrigCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDelayedTrigger.doResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryDelayedTrigger.delTrigStackDescription(af, this);
            }
        };
        return spell;
    }

    /**
     * <p>
     * getDrawback.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    public static AbilitySub getDrawback(final AbilityFactory abilityFactory) {
        final AbilitySub drawback = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 6192972525033429820L;

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryDelayedTrigger.doChkDrawbackAI(abilityFactory, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDelayedTrigger.doTriggerAI(abilityFactory, this, mandatory);
            }

            @Override
            public void resolve() {
                AbilityFactoryDelayedTrigger.doResolve(abilityFactory, this);
            }
        };

        return drawback;
    }

    /**
     * <p>
     * doChkAI_Drawback.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean doChkDrawbackAI(final AbilityFactory abilityFactory, final SpellAbility spellAbility) {
        final HashMap<String, String> params = abilityFactory.getMapParams();
        final String svarName = params.get("Execute");
        final SpellAbility trigsa = AbilityFactoryDelayedTrigger.tempCreator.getAbility(
                abilityFactory.getHostCard().getSVar(svarName), abilityFactory.getHostCard());

        if (trigsa instanceof AbilitySub) {
            return ((AbilitySub) trigsa).chkAIDrawback();
        } else {
            return trigsa.canPlayAI();
        }
    }

    /**
     * <p>
     * doTriggerAI.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean doTriggerAI(final AbilityFactory abilityFactory, final SpellAbility spellAbility, final boolean mandatory) {
        final HashMap<String, String> params = abilityFactory.getMapParams();
        final String svarName = params.get("Execute");
        final SpellAbility trigsa = AbilityFactoryDelayedTrigger.tempCreator.getAbility(
                abilityFactory.getHostCard().getSVar(svarName), abilityFactory.getHostCard());

        if (!params.containsKey("OptionalDecider")) {
            return trigsa.doTrigger(true);
        } else {
            return trigsa.doTrigger(!params.get("OptionalDecider").equals("You"));
        }
    }

    /**
     * <p>
     * delTrigCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean delTrigCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final String svarName = params.get("Execute");
        final SpellAbility trigsa = AbilityFactoryDelayedTrigger.tempCreator.getAbility(
                af.getHostCard().getSVar(svarName), af.getHostCard());

        return trigsa.canPlayAI();
    }

    /**
     * <p>
     * delTrigStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static String delTrigStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> mapParams = af.getMapParams();

        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        if (mapParams.containsKey("SpellDescription")) {
            sb.append(mapParams.get("SpellDescription"));
        } else if (mapParams.containsKey("TriggerDescription")) {
            sb.append(mapParams.get("TriggerDescription"));
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();

    }

    /**
     * <p>
     * doResolve.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void doResolve(final AbilityFactory abilityFactory, final SpellAbility spellAbility) {
        final HashMap<String, String> mapParams = abilityFactory.getMapParams();

        if (mapParams.containsKey("Cost")) {
            mapParams.remove("Cost");
        }

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        final Trigger delTrig = TriggerHandler.parseTrigger(mapParams, abilityFactory.getHostCard(), true);

        AllZone.getTriggerHandler().registerDelayedTrigger(delTrig);
    }
}
