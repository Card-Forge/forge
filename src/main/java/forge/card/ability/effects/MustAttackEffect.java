package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.GameEntity;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class MustAttackEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();


        // end standard pre-

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String defender = null;
        if (sa.getParam("Defender").equals("Self")) {
            defender = host.toString();
        } else {
            // TODO - if more needs arise in the future
        }

        for (final Player player : tgtPlayers) {
            sb.append("Creatures ").append(player).append(" controls attack ");
            sb.append(defender).append(" during his or her next turn.");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final Target tgt = sa.getTarget();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                GameEntity entity;
                if (sa.getParam("Defender").equals("Self")) {
                    entity = sa.getSourceCard();
                } else {
                    entity = p.getOpponent();
                }
                // System.out.println("Setting mustAttackEntity to: "+entity);
                p.setMustAttackEntity(entity);
            }
        }

    } // mustAttackResolve()

}
