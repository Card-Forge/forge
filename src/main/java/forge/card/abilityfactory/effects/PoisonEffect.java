package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */

    public class PoisonEffect extends SpellEffect {

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public void resolve(Map<String, String> params, SpellAbility sa) {
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Num"), sa);
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            for (final Player p : tgtPlayers) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    p.addPoisonCounters(amount, sa.getSourceCard());
                }
            }
        }

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
            final StringBuilder sb = new StringBuilder();
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Num"), sa);
        
            if (!(sa instanceof AbilitySub)) {
                sb.append(sa.getSourceCard()).append(" - ");
            } else {
                sb.append(" ");
            }
        
            final String conditionDesc = params.get("ConditionDescription");
            if (conditionDesc != null) {
                sb.append(conditionDesc).append(" ");
            }
        
            ArrayList<Player> tgtPlayers;
        
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtPlayers = tgt.getTargetPlayers();
            } else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }
        
            if (tgtPlayers.size() > 0) {
                final Iterator<Player> it = tgtPlayers.iterator();
                while (it.hasNext()) {
                    final Player p = it.next();
                    sb.append(p);
                    if (it.hasNext()) {
                        sb.append(", ");
                    } else {
                        sb.append(" ");
                    }
                }
            }
        
            sb.append("get");
            if (tgtPlayers.size() < 2) {
                sb.append("s");
            }
            sb.append(" ").append(amount).append(" poison counter");
            if (amount != 1) {
                sb.append("s.");
            } else {
                sb.append(".");
            }
        
            return sb.toString();
        } 
    
    }

