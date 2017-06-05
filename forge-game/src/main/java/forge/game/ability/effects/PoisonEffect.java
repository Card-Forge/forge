package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */

    public class PoisonEffect extends SpellAbilityEffect {

        /* (non-Javadoc)
         * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
         */
        @Override
        public void resolve(SpellAbility sa) {
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Num"), sa);

            for (final Player p : getTargetPlayers(sa)) {
                if ((!sa.usesTargeting()) || p.canBeTargetedBy(sa)) {
                    if (amount >= 0) {
                        p.addPoisonCounters(amount, sa.getHostCard());
                    } else {
                        p.removePoisonCounters(-amount, sa.getHostCard());
                    }
                }
            }
        }

        /* (non-Javadoc)
         * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
         */
        @Override
        protected String getStackDescription(SpellAbility sa) {
            final StringBuilder sb = new StringBuilder();
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Num"), sa);

            final List<Player> tgtPlayers = getTargetPlayers(sa);

            sb.append(Lang.joinHomogenous(tgtPlayers));
            sb.append(" ");

            sb.append("get");
            if (tgtPlayers.size() < 2) {
                sb.append("s");
            }

            String type = CounterType.POISON.getName() + " counter";

            sb.append(" ").append(Lang.nounWithAmount(amount, type)).append(".");

            return sb.toString();
        }

    }

