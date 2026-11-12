package forge.game.ability.effects;

import com.google.common.collect.ImmutableMap;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.GameEventRandomLog;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RemoveFromMatchEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        CardCollection toRemove;
        boolean removeFromInventory = sa.hasParam("RemoveFromInventory");

        if (sa.hasParam("RemoveType")) {
            CardCollection cards = (CardCollection) host.getOwner().getGame().getCardsInGame();
            if (sa.hasParam("IncludeSideboard")) {
                CardCollection sideboard = (CardCollection) host.getGame().getCardsIn(ZoneType.Sideboard);
                cards.addAll(sideboard);
            }
            toRemove = (CardCollection) AbilityUtils.filterListByType(cards, sa.getParam("RemoveType"), sa);
        } else {
            toRemove = getTargetCards(sa);
        }
        // OnMatchLoss is used by Adventure challenges, not by any card
        if (removeFromInventory && sa.hasParam("OnMatchLoss")) {
            if (host.getGame().getMaingame() != null) {
                return;
            }
            final Map<Player, List<PaperCard>> pools = new LinkedHashMap<>();
            for (final Card c : toRemove) {
                if (c.isCollectible()) {
                    pools.computeIfAbsent(c.getOwner(), p -> new ArrayList<>()).add((PaperCard) c.getPaperCard());
                }
            }
            for (final Map.Entry<Player, List<PaperCard>> pool : pools.entrySet()) {
                final int amount = sa.hasParam("Amount")
                        ? AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa) : pool.getValue().size();
                host.getGame().getMatch().addMatchLossRemoval(pool.getKey().getRegisteredPlayer(),
                        ImmutableMap.copyOf(sa.getOriginalMapParams()), pool.getValue(), amount);
            }
            return;
        }
        String logMessage = sa.getParamOrDefault("LogMessage", "Removed from match");
        String remove = toRemove.toString().replace("[","").replace("]","");
        host.getController().getGame().fireEvent(new GameEventRandomLog(logMessage + ": " + remove));
        for (final Card tgtC : toRemove) {
            tgtC.getGame().getAction().ceaseToExist(tgtC, true);
            PaperCard rem = (PaperCard) tgtC.getPaperCard();
            host.getGame().getMatch().removeCard(rem);
            if (removeFromInventory) {
                host.getController().destroyPhysicalCard(tgtC);
            }
        }
    }
}