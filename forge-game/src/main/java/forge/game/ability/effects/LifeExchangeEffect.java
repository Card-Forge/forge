package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

import java.util.List;
import java.util.Map;

public class LifeExchangeEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Player activatingPlayer = sa.getActivatingPlayer();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() == 1) {
            sb.append(activatingPlayer).append(" exchanges life totals with ");
            sb.append(tgtPlayers.get(0));
        } else if (tgtPlayers.size() > 1) {
            sb.append(tgtPlayers.get(0)).append(" exchanges life totals with ");
            sb.append(tgtPlayers.get(1));
        }
        sb.append(".");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        Player p1;
        Player p2;

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() == 1) {
            p1 = sa.getActivatingPlayer();
            p2 = tgtPlayers.get(0);
        } else {
            p1 = tgtPlayers.get(0);
            p2 = tgtPlayers.get(1);
        }

        final int life1 = p1.getLife();
        final int life2 = p2.getLife();

        if (sa.hasParam("RememberDifference")) {
            final int diff = life1 - life2;
            source.addRemembered(diff);
        }

        final Map<Player, Integer> lossMap = Maps.newHashMap();
        if ((life1 > life2) && p1.canLoseLife() && p2.canGainLife()) {
            final int diff = life1 - life2;
            final int lost = p1.loseLife(diff, false, false);
            p2.gainLife(diff, source, sa);
            if (lost > 0) {
                lossMap.put(p1, lost);
            }
        } else if ((life2 > life1) && p2.canLoseLife() && p1.canGainLife()) {
            final int diff = life2 - life1;
            final int lost = p2.loseLife(diff, false, false);
            p1.gainLife(diff, source, sa);
            if (lost > 0) {
                lossMap.put(p2, lost);
            }
        } else {
            // they are equal or can't be exchanged, so nothing to do
        }
        if (!lossMap.isEmpty()) { // Run triggers if any player actually lost life
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPIMap(lossMap);
            source.getGame().getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runParams, false);
        }
    }
}
