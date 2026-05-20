package forge.ai.ability;

import forge.ai.*;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollection;

public class UnattachAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card host = sa.getHostCard();
        FCollection<GameEntity> targets = new FCollection<>();
        if (!sa.usesTargeting()) {
            targets = AbilityUtils.getDefinedEntities(host, sa.getParam("Defined"), sa);
        }

        if (!mandatory && !targets.isEmpty()) {
            Card attachment = (Card) targets.get(0);
            if (attachment.isEquipment() && ai.getYourTeam().contains(attachment.getController())) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            // currently no card exists to get rid of curse aura this way
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        return doTriggerNoCost(ai, sa, false);
    }

}
