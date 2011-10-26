package forge.card.abilityFactory;

import java.util.HashMap;

import forge.card.spellability.Ability_Sub;
import forge.card.spellability.SpellAbility;

// Cleanup is not the same as other AFs, it is only used as a Drawback, and only used to Cleanup particular card states
// That need to be reset. I'm creating this to clear Remembered Cards at the
// end of an Effect so they don't get shown on a card
// After the effect finishes resolving.
/**
 * <p>
 * AbilityFactory_Cleanup class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory_Cleanup {

    private AbilityFactory_Cleanup() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getDrawback.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.Ability_Sub} object.
     */
    public static Ability_Sub getDrawback(final AbilityFactory af) {
        final Ability_Sub drawback = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6192972525033429820L;

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public void resolve() {
                doResolve(af, this);
            }
        };

        return drawback;
    }

    /**
     * <p>
     * doResolve.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void doResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        if (params.containsKey("ClearRemembered")) {
            sa.getSourceCard().clearRemembered();
        }
        if (params.containsKey("ClearImprinted")) {
            sa.getSourceCard().clearImprinted();
        }
    }

} // end class AbilityFactory_Cleanup
