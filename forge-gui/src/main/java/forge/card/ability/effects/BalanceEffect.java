package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

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
        Card source = sa.getSourceCard();
        Game game = activator.getGame();
        String valid = sa.hasParam("Valid") ? sa.getParam("Valid") : "Card";
        ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
        
        int min = Integer.MAX_VALUE;
        
        final List<Player> players = game.getPlayers();
        final List<List<Card>> validCards = new ArrayList<List<Card>>(players.size());
        
        for(int i = 0; i < players.size(); i++) {
            // Find the minimum of each Valid per player
            validCards.add(CardLists.getValidCards(players.get(i).getCardsIn(zone), valid, activator, source));
            min = Math.min(min, validCards.get(i).size());
        }
        
        for(int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int numToBalance = validCards.get(i).size() - min;
            if (numToBalance == 0) {
                continue;
            }
            if (zone.equals(ZoneType.Hand)) {
                for (Card card : p.getController().chooseCardsToDiscardFrom(p, sa, validCards.get(i), numToBalance, numToBalance)) {
                    if ( null == card ) continue;
                    p.discard(card, sa);
                }
            } else { // Battlefield
                for(Card card : p.getController().choosePermanentsToSacrifice(sa, numToBalance, numToBalance,  validCards.get(i), valid)) {
                    if ( null == card ) continue; 
                    game.getAction().sacrifice(card, sa);
                }
            }
        }
    }
}
