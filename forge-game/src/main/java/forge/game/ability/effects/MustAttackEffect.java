package forge.game.ability.effects;

import forge.game.GameEntity;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.List;

public class MustAttackEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();


        // end standard pre-

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String defender = null;
        if (sa.getParam("Defender").equals("Self")) {
            defender = host.toString();
        } else {
            defender = host.getController().toString();
        }

        for (final Player player : tgtPlayers) {
            sb.append("Creatures ").append(player).append(" controls attack ");
            sb.append(defender).append(" during his or her next turn.");
        }
        for (final Card c : getTargetCards(sa)) {
            sb.append(c).append(" must attack ");
            sb.append(defender).append(" during its controller's next turn if able.");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final String defender = sa.getParam("Defender");
        final GameEntity entity;
        if (defender.equals("Self")) {
            entity = sa.getHostCard();
        } else if (defender.equals("You")) {
            entity = sa.getActivatingPlayer();
        } else {
            throw new RuntimeException("Illegal defender " + defender + " for MustAttackEffect in card " + sa.getHostCard());
        }
        // System.out.println("Setting mustAttackEntity to: "+entity);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.setMustAttackEntity(entity);
            }
        }
        for (final Card c : getTargetCards(sa)) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                c.setMustAttackEntity(entity);
            }
        }

    } // mustAttackResolve()

}
