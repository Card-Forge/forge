package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AssembleContraptionAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        //Pulls double duty as the OpenAttraction API. Same logic; always good to do as long as we have the appropriate cards.
        if(sa.getApi() == ApiType.OpenAttraction)
            return !ai.getCardsIn(ZoneType.AttractionDeck).isEmpty();
        return !ai.getCardsIn(ZoneType.ContraptionDeck).isEmpty();
    }
}
