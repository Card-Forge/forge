package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

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
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int amount = AbilityUtils.calculateAmount(host, sa.getParam("Num"), sa);

        GameEntityCounterTable table = new GameEntityCounterTable();
        for (final Player p : getTargetPlayers(sa)) {
            if ((!sa.usesTargeting()) || p.canBeTargetedBy(sa)) {
                if (amount >= 0) {
                    p.addPoisonCounters(amount, host, table);
                } else {
                    p.removePoisonCounters(-amount, host);
                }
            }
        }
        table.triggerCountersPutAll(game);
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

        String type = CounterEnumType.POISON.getName() + " counter";

        sb.append(" ").append(Lang.nounWithAmount(amount, type)).append(".");

        return sb.toString();
    }

}
