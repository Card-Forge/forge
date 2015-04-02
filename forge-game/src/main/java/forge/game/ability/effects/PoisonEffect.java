package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

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
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Num"), sa);

            final TargetRestrictions tgt = sa.getTargetRestrictions();
            for (final Player p : getTargetPlayers(sa)) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    if (amount >= 0) {
                        p.addPoisonCounters(amount, sa.getHostCard());
                    } else {
                        p.removePoisonCounters(-amount, sa.getHostCard());
                    }
                }
            }
        }

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        protected String getStackDescription(SpellAbility sa) {
            final StringBuilder sb = new StringBuilder();
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Num"), sa);

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

