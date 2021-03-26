package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class BalanceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        Card source = sa.getHostCard();
        Game game = activator.getGame();
        String valid = sa.hasParam("Valid") ? sa.getParam("Valid") : "Card";
        ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
        
        int min = Integer.MAX_VALUE;
        
        final FCollectionView<Player> players = game.getPlayersInTurnOrder();
        final List<CardCollection> validCards = new ArrayList<>(players.size());
        
        for(int i = 0; i < players.size(); i++) {
            // Find the minimum of each Valid per player
            validCards.add(CardLists.getValidCards(players.get(i).getCardsIn(zone), valid, activator, source, sa));
            min = Math.min(min, validCards.get(i).size());
        }
        
        CardZoneTable table = new CardZoneTable();
        for(int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int numToBalance = validCards.get(i).size() - min;
            if (numToBalance == 0) {
                continue;
            }
            if (zone.equals(ZoneType.Hand)) {
                boolean firstDiscard = p.getNumDiscardedThisTurn() == 0;
                final CardCollection discardedByPlayer = new CardCollection();
                for (Card card : p.getController().chooseCardsToDiscardFrom(p, sa, validCards.get(i), numToBalance, numToBalance)) {
                    if ( null == card ) continue;
                    if (p.discard(card, sa, table) != null) {
                        discardedByPlayer.add(card);
                    }
                }

                if (!discardedByPlayer.isEmpty()) {
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    runParams.put(AbilityKey.Player, p);
                    runParams.put(AbilityKey.Cards, discardedByPlayer);
                    runParams.put(AbilityKey.Cause, sa);
                    runParams.put(AbilityKey.FirstTime, firstDiscard);
                    game.getTriggerHandler().runTrigger(TriggerType.DiscardedAll, runParams, false);
                }
            } else { // Battlefield
                for(Card card : p.getController().choosePermanentsToSacrifice(sa, numToBalance, numToBalance,  validCards.get(i), valid)) {
                    if ( null == card ) continue; 
                    game.getAction().sacrifice(card, sa, table);
                }
            }
        }
        table.triggerChangesZoneAll(game);
    }
}
