package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

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
        
        public abstract void resolve(final SpellAbility sa);
        
        protected String getStackDescription(final SpellAbility sa) {
            // Unless overriden, let the spell description also be the stack description
            return sa.getDescription();
        }


        protected void resolveDrawback(final SpellAbility sa) {
            
            // if mana production has any type of SubAbility, undoable=false
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sa.setUndoable(false);
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
            if ( stackDesc != null ) {
                if ( !"None".equalsIgnoreCase(stackDesc) ) // by typing "none" they want to suppress output
                    sb.append( stackDesc.replace("CARDNAME", sa.getSourceCard().getName()));
            } else
                sb.append(this.getStackDescription(sa));
            
            
            // This inlcudes all subAbilities
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sb.append(abSub.getStackDescription());
            }
            return sb.toString();
        }

        protected List<Card> getTargetCards(SpellAbility sa) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargetCards() : AbilityFactory.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        protected List<Player> getTargetPlayers(SpellAbility sa) {
            return getTargetPlayers(sa, false, true);
        }

        protected List<Player> getTargetPlayersEmptyAsDefault(SpellAbility sa) {
            return getTargetPlayers(sa, true, true);
        }

        protected List<Player> getDefinedPlayersBeforeTargetOnes(SpellAbility sa) {
            return getTargetPlayers(sa, false, false);
        }
        
        // Each AF used its own preference in choosing target players: 
        // Some checked target first and params["Defined"] then - @see targetIsPreferred
        // Some wanted empty list when params["Defined"] was not set - @see wantEmptyAsDefault
        // Poor me had to gather it all in a single place
        private final static List<Player> emptyPlayerList = Collections.unmodifiableList(new ArrayList<Player>());
        private List<Player> getTargetPlayers(SpellAbility sa, final boolean wantEmptyAsDefault, final boolean targetIsPreferred) {
            final Target tgt = sa.getTarget();
            final String defined = sa.getParam("Defined");
            if ( tgt != null && ( targetIsPreferred || ( StringUtils.isEmpty(defined) && !wantEmptyAsDefault ) ) ) 
                return tgt.getTargetPlayers();
            if ( StringUtils.isEmpty(defined) && wantEmptyAsDefault ) 
                return emptyPlayerList;
            return AbilityFactory.getDefinedPlayers(sa.getSourceCard(), defined, sa);
        }
        
        protected List<SpellAbility> getTargetSpellAbilities(SpellAbility sa) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargetSAs() : AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }
        
        protected List<Object> getTargetObjects(SpellAbility sa) {
            final Target tgt = sa.getTarget();
            return tgt != null ? tgt.getTargets() : AbilityFactory.getDefinedObjects(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }


    }