package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AdvanceCrankAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        int nextSprocket = (ai.getCrankCounter() % 3) + 1;
        int crankCount = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isContraptionOnSprocket(nextSprocket));
        if (crankCount < 2) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return super.canPlay(ai, sa);
    }

}
