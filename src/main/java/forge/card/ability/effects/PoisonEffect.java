package forge.card.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */

    public class PoisonEffect extends SpellAbilityEffect {

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public void resolve(SpellAbility sa) {
            final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Num"), sa);

            final Target tgt = sa.getTarget();
            for (final Player p : getTargetPlayers(sa)) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    p.addPoisonCounters(amount, sa.getSourceCard());
                }
            }
        }

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        protected String getStackDescription(SpellAbility sa) {
            final StringBuilder sb = new StringBuilder();
            final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Num"), sa);

            final List<Player> tgtPlayers = getTargetPlayers(sa);

            sb.append(StringUtils.join(tgtPlayers, ", "));
            sb.append(" ");

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

