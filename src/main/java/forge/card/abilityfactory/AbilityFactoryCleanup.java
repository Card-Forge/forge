package forge.card.abilityfactory;

import java.util.HashMap;

import forge.card.spellability.AbilitySub;
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
public final class AbilityFactoryCleanup {

    private AbilityFactoryCleanup() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getDrawback.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    public static AbilitySub getDrawback(final AbilityFactory af) {
        final AbilitySub drawback = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6192972525033429820L;

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public void resolve() {
                AbilityFactoryCleanup.doResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void doResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        if (params.containsKey("ClearRemembered")) {
            sa.getSourceCard().clearRemembered();
        }
        if (params.containsKey("ClearImprinted")) {
            sa.getSourceCard().clearImprinted();
        }
        if (params.containsKey("ClearChosenX")) {
            sa.getSourceCard().setSVar("ChosenX", "");
        }
    }

} // end class AbilityFactory_Cleanup
