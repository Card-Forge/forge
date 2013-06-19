package forge.card.ability;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;


import forge.card.cardfactory.CardFactoryUtil;
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

    public abstract class SpellAbilityEffect extends SaTargetRountines {

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
                AbilityUtils.resolve(abSub);
            }
        }

        /**
         * Returns this effect description with needed prelude and epilogue.
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
            if (stackDesc != null) {
                if ("SpellDescription".equalsIgnoreCase(stackDesc)) { // by typing "none" they want to suppress output
                    sb.append(params.get("SpellDescription").replace("CARDNAME", sa.getSourceCard().getName()));
                } else if (!"None".equalsIgnoreCase(stackDesc)) { // by typing "none" they want to suppress output
                    makeSpellDescription(sa, sb, stackDesc);
                }
            } else {
                final String conditionDesc = sa.getParam("ConditionDescription");
                final String baseDesc = this.getStackDescription(sa);
                if (conditionDesc != null) {
                    sb.append(conditionDesc).append(" ");
                } 
                sb.append(baseDesc);
            }

            // This includes all subAbilities
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sb.append(abSub.getStackDescription());
            }
            
            if (sa.hasParam("Announce")) {
                String svar = sa.getParam("Announce");
                int amount = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSVar(svar));
                sb.append(String.format(" (%s=%d)", svar, amount));
            }

            return sb.toString();
        }

        /**
         * TODO: Write javadoc for this method.
         * @param sa
         * @param sb
         * @param stackDesc
         */
        private void makeSpellDescription(final SpellAbility sa, StringBuilder sb, String stackDesc) {
            StringTokenizer st = new StringTokenizer(stackDesc, "{}", true);
            boolean isPlainText = true;
            while( st.hasMoreTokens() )
            {
                String t = st.nextToken();
                if ( "{".equals(t) ) { isPlainText = false; continue; }
                if ( "}".equals(t) ) { isPlainText = true; continue; }
                if ( isPlainText ) 
                    sb.append(t.replace("CARDNAME", sa.getSourceCard().getName()));
                else {
                    List<?> objs = null;
                    if ( t.startsWith("p:") )
                        objs = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), t.substring(2), sa);
                    else if ( t.startsWith("s:"))
                        objs = AbilityUtils.getDefinedSpellAbilities(sa.getSourceCard(), t.substring(2), sa);
                    else if ( t.startsWith("c:"))
                        objs = AbilityUtils.getDefinedCards(sa.getSourceCard(), t.substring(2), sa);
                    else 
                        objs = AbilityUtils.getDefinedObjects(sa.getSourceCard(), t, sa);
                            
                    sb.append(StringUtils.join(objs, ", "));
                }
            }
        }
    }
