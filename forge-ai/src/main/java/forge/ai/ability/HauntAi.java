package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

public class HauntAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return false; // should not get here
    }
    

    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> creats, boolean isOptional, Player targetedPlayer) {
        final List<Card> oppCreats = CardLists.filterControlledBy(creats, ai.getOpponents());
        return ComputerUtilCard.getWorstCreatureAI(oppCreats.isEmpty() ? creats : oppCreats);
    }

}