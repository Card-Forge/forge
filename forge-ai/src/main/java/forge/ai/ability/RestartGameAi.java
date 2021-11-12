package forge.ai.ability;

import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class RestartGameAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        if (ComputerUtil.aiLifeInDanger(ai, true, 0)) {
            return true;
        }

        // check if enough good permanents will be available to be returned, so AI can "autowin"
        CardCollection exiled = new CardCollection(Iterables.filter(sa.getHostCard().getRemembered(), Card.class));
        exiled = CardLists.filter(exiled, Presets.PERMANENTS);
        if (ComputerUtilCard.evaluatePermanentList(exiled) > 20) {
            return true;
        }

        return false;
    }

}
