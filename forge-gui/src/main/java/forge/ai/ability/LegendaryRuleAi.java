package forge.ai.ability;

import java.util.Collection;

import com.google.common.collect.Iterables;

import forge.game.ability.SpellAbilityAi;
import forge.game.card.Card;
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
    public Card chooseSingleCard(Player ai, SpellAbility sa, Collection<Card> options, boolean isOptional) {
        // Choose a single legendary/planeswalker card to keep
        Card firstOption = Iterables.getFirst(options, null);
        boolean choosingFromPlanewalkers = firstOption.isPlaneswalker();
        
        if ( choosingFromPlanewalkers ) {
            // AI decision making - should AI compare counters?
        } else {
            // AI decision making - should AI compare damage and debuffs?
        }
        
        return firstOption;
    }

}
