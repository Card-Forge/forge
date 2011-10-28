package forge.card.abilityFactory;

import java.util.HashMap;

import forge.AllZone;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
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
public class AbilityFactory_DelayedTrigger {
    /** Constant <code>tempCreator</code> */
    private static AbilityFactory tempCreator = new AbilityFactory();

    /**
     * <p>
     * getAbility.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getAbility(final AbilityFactory af) {
        final SpellAbility ability = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7502962478028160305L;

            @Override
            public boolean canPlayAI() {
                return delTrigCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                doResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return delTrigStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return doTriggerAI(af, this, mandatory);
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
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getSpell(final AbilityFactory af) {
        final SpellAbility spell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -6981410664429186904L;

            @Override
            public boolean canPlayAI() {
                return delTrigCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                doResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return delTrigStackDescription(af, this);
            }
        };
        return spell;
    }

    /**
     * <p>
     * getDrawback.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.Ability_Sub} object.
     */
    public static Ability_Sub getDrawback(final AbilityFactory AF) {
        final Ability_Sub drawback = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
            private static final long serialVersionUID = 6192972525033429820L;

            @Override
            public boolean chkAIDrawback() {
                return doChkAI_Drawback(AF, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return doTriggerAI(AF, this, mandatory);
            }

            @Override
            public void resolve() {
                doResolve(AF, this);
            }
        };

        return drawback;
    }

    /**
     * <p>
     * doChkAI_Drawback.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean doChkAI_Drawback(final AbilityFactory AF, final SpellAbility SA) {
        HashMap<String, String> params = AF.getMapParams();
        String svarName = params.get("Execute");
        SpellAbility trigsa = tempCreator.getAbility(AF.getHostCard().getSVar(svarName), AF.getHostCard());

        if (trigsa instanceof Ability_Sub) {
            return ((Ability_Sub) trigsa).chkAIDrawback();
        } else {
            return trigsa.canPlayAI();
        }
    }

    /**
     * <p>
     * doTriggerAI.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean doTriggerAI(final AbilityFactory AF, final SpellAbility SA, final boolean mandatory) {
        HashMap<String, String> params = AF.getMapParams();
        String svarName = params.get("Execute");
        SpellAbility trigsa = tempCreator.getAbility(AF.getHostCard().getSVar(svarName), AF.getHostCard());

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
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean delTrigCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        String svarName = params.get("Execute");
        SpellAbility trigsa = tempCreator.getAbility(af.getHostCard().getSVar(svarName), af.getHostCard());

        return trigsa.canPlayAI();
    }

    /**
     * <p>
     * delTrigStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static String delTrigStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> mapParams = af.getMapParams();

        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        if (mapParams.containsKey("SpellDescription")) {
            sb.append(mapParams.get("SpellDescription"));
        } else if (mapParams.containsKey("TriggerDescription")) {
            sb.append(mapParams.get("TriggerDescription"));
        }

        Ability_Sub abSub = sa.getSubAbility();
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
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void doResolve(final AbilityFactory AF, final SpellAbility SA) {
        HashMap<String, String> mapParams = AF.getMapParams();

        if (mapParams.containsKey("Cost")) {
            mapParams.remove("Cost");
        }

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        Trigger delTrig = TriggerHandler.parseTrigger(mapParams, AF.getHostCard(), true);

        AllZone.getTriggerHandler().registerDelayedTrigger(delTrig);
    }
}
