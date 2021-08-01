package forge.ai.ability;

import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
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
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // Choose a single legendary/planeswalker card to keep
        CardCollection legends = new CardCollection(options);
        CardCollection badOptions = ComputerUtil.choosePermanentsToSacrifice(ai, legends, legends.size() -1, sa, false, false);
        legends.removeAll(badOptions);
        Card firstOption = Iterables.getFirst(legends, null);
        boolean choosingFromPlanewalkers = firstOption.isPlaneswalker();
        
        if (choosingFromPlanewalkers) {
            // AI decision making - should AI compare counters?
        } else {
            // AI decision making - should AI compare damage and debuffs?
        }

        // TODO: Can this be made more generic somehow?
        if (firstOption.getName().equals("Dark Depths")) {
            Card best = firstOption;
            for (Card c : options) {
                if (c.getCounters(CounterEnumType.ICE) < best.getCounters(CounterEnumType.ICE)) {
                    best = c;
                }
            }
            return best;
        } else if (firstOption.getCounters(CounterEnumType.KI) > 0) {
        	// Extra Rule for KI counter
        	Card best = firstOption;
            for (Card c : options) {
                if (c.getCounters(CounterEnumType.KI) > best.getCounters(CounterEnumType.KI)) {
                    best = c;
                }
            }
            return best;
        }

        return firstOption;
    }

}
