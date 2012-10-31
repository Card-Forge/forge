package forge.card.abilityfactory;

import java.util.Map;

import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * AbilityFactory_AlterLife class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryAlterLife.java 17656 2012-10-22 19:32:56Z Max mtg $
 */

    public abstract class SpellEffect {
        public abstract void resolve(final Map<String, String> params, final SpellAbility sa);
        protected abstract String getStackDescription(final Map<String, String> params, final SpellAbility sa);


        /**
         * TODO: Write javadoc for this method.
         * @param params
         * @param commonSpell
         * @return
         */
        public final String getStackDescriptionWithSubs(final Map<String, String> params, final SpellAbility sa) {
            final String s1 = this.getStackDescription(params, sa);
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                return s1 + abSub.getStackDescription();
            }
            return s1;
        }
    }