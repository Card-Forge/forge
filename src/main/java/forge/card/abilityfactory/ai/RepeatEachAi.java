package forge.card.abilityfactory.ai;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class RepeatEachAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");
        
        if ("CloneTokens".equals(logic)) {
            List<Card> allTokens = aiPlayer.getCreaturesInPlay();
            allTokens = CardLists.filter(allTokens, Presets.TOKEN);

            if (allTokens.size() < 2) {
                return false;
            }
        }
        
        // TODO Add some normal AI variability here
        
        return true;
    }

}
