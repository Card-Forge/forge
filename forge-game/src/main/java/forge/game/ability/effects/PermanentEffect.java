package forge.game.ability.effects;

import com.google.common.collect.Lists;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class PermanentEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.
     * SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Player p = sa.getActivatingPlayer();
        sa.getHostCard().setController(p, 0);
        final Card host = sa.getHostCard();

        final Card c = p.getGame().getAction().moveToPlay(host, p, sa);
        sa.setHostCard(c);

        // some extra for Dashing
        if (sa.isDash()) {
            c.setSVar("EndOfTurnLeavePlay", "Dash");
            registerDelayedTrigger(sa, "Hand", Lists.newArrayList(c));
        }
    }
}
