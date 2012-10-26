package forge.card.abilityfactory;

import java.util.Map;

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
        public abstract String getStackDescription(final Map<String, String> params, final SpellAbility sa);
    }