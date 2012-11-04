package forge.card.abilityfactory;

import java.util.List;
import java.util.Map;

import forge.Card;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

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
        @SuppressWarnings("unused")
        protected String getStackDescription(final Map<String, String> params, final SpellAbility sa) {
            // Unless overriden, let the spell description also be the stack description
            return sa.getDescription();
        }


        protected void resolveDrawback(final SpellAbility sa) {
            
            // if mana production has any type of SubAbility, undoable=false
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                if ( sa.getManaPart() != null ) 
                    sa.getManaPart().setUndoable(false);
                AbilityFactory.resolve(abSub, false);
            }
        }
        
        /**
         * Returns this effect description with needed prelude and epilogue
         * @param params
         * @param commonSpell
         * @return
         */
        public final String getStackDescriptionWithSubs(final Map<String, String> params, final SpellAbility sa) {
            StringBuilder sb = new StringBuilder();
            
            // prelude for when this is root ability
            if (!(sa instanceof AbilitySub)) {
                sb.append(sa.getSourceCard()).append(" -");
            }
            sb.append(" ");

            // Own description
            String stackDesc = params.get("StackDescription");
            String spellDesc = params.get("SpellDescription");
            if ( stackDesc != null ) {
                if ( !"None".equalsIgnoreCase(stackDesc) ) // by typing "none" they want to suppress output
                    sb.append( stackDesc.replace("CARDNAME", sa.getSourceCard().getName()));
            } else if ( spellDesc != null ) {
                sb.append( spellDesc.replace("CARDNAME", sa.getSourceCard().getName()) );
            } else
                sb.append(this.getStackDescription(params, sa));
            
            
            // This inlcudes all subAbilities
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sb.append(abSub.getStackDescription());
            }
            return sb.toString();
        }

        protected List<Card> getTargetCards(SpellAbility sa, final Map<String, String> params) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargetCards() : AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        protected List<Player> getTargetPlayers(SpellAbility sa, final Map<String, String> params) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargetPlayers() : AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
        
        protected List<SpellAbility> getTargetSpellAbilities(SpellAbility sa, final Map<String, String> params) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargetSAs() : AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
        }
        
        protected List<Object> getTargetObjects(SpellAbility sa, final Map<String, String> params) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargets() : AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        }


    }