package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
        CardCollection exiled = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Exile), "Permanent.nonAura+IsRemembered", ai, sa.getHostCard(), sa);
        if (ComputerUtilCard.evaluatePermanentList(exiled) > 20) {
            return true;
        }

        return false;
    }

}
