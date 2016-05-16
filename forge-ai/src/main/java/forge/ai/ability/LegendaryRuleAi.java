package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class LegendaryRuleAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return false; // should not get here
    }
    

    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        // Choose a single legendary/planeswalker card to keep
        Card firstOption = Iterables.getFirst(options, null);
        boolean choosingFromPlanewalkers = firstOption.isPlaneswalker();
        
        if ( choosingFromPlanewalkers ) {
            // AI decision making - should AI compare counters?
        } else {
            // AI decision making - should AI compare damage and debuffs?
        }

        // TODO: Can this be made more generic somehow?
        if (firstOption.getName().equals("Dark Depths")) {
            Card best = firstOption;
            for (Card c : options) {
                if (c.getCounters(CounterType.ICE) < best.getCounters(CounterType.ICE)) {
                    best = c;
                }
            }
            return best;
        } else if (firstOption.getCounters(CounterType.KI) > 0) {
        	// Extra Rule for KI counter
        	Card best = firstOption;
            for (Card c : options) {
                if (c.getCounters(CounterType.KI) > best.getCounters(CounterType.KI)) {
                    best = c;
                }
            }
            return best;
        }

        return firstOption;
    }

}
