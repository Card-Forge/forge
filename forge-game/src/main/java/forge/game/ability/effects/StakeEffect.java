package forge.game.ability.effects;

import com.google.common.collect.ImmutableMap;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.List;

public class StakeEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final List<PaperCard> cards = new ArrayList<>();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            if (activator.equals(c.getOwner()) && c.isCollectible()) {
                cards.add((PaperCard) c.getPaperCard());
            }
        }
        final int amount = sa.hasParam("Amount")
                ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa)
                : cards.size();
        // keyed by the script line, so a line that runs again in a later game replaces its stake
        activator.getGame().getMatch().addStake(activator.getRegisteredPlayer(),
                ImmutableMap.copyOf(sa.getOriginalMapParams()), cards, amount);
    }
}
