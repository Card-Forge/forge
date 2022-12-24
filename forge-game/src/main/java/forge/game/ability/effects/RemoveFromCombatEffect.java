package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

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
        final Combat combat = game.getPhaseHandler().getCombat();

        for (final Card c : getTargetCards(sa)) {
            if (combat == null || !c.isInPlay()) {
                continue;
            }

            // Unblock creatures that were blocked only by this card (e.g. Ydwen Efreet)
            if (sa.hasParam("UnblockCreaturesBlockedOnlyBy")) {
                CardCollection attackers = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("UnblockCreaturesBlockedOnlyBy"), sa);
                if (!attackers.isEmpty()) {
                    CardCollection blockedByCard = combat.getAttackersBlockedBy(attackers.getFirst());
                    for (Card atk : blockedByCard) {
                        boolean blockedOnlyByCard = true;
                        for (Card blocker : combat.getBlockers(atk)) {
                            if (!blocker.equals(attackers.getFirst())) {
                                blockedOnlyByCard = false;
                                break;
                            }
                        }
                        if (blockedOnlyByCard) {
                            combat.setBlocked(atk, false);
                        }
                    }
                }
            }

            game.getCombat().saveLKI(c);
            combat.removeFromCombat(c);

            if (rem) {
                sa.getHostCard().addRemembered(c);
            }
        }
    }
}
