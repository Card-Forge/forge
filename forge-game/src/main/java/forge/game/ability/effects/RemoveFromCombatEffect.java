package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class RemoveFromCombatEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        sb.append("Remove ");
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(" from combat.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final boolean rem = sa.hasParam("RememberRemovedFromCombat");

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        for (final Card c : getTargetCards(sa)) {
        	final Combat combat = game.getPhaseHandler().getCombat();
            if (combat != null && (tgt == null || c.canBeTargetedBy(sa))) {
            	combat.removeFromCombat(c);
                if (rem) {
                    sa.getHostCard().addRemembered(c);
                }
            }
        }
    }
}
