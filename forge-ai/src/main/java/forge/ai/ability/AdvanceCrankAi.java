package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class AdvanceCrankAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        int nextSprocket = (ai.getCrankCounter() % 3) + 1;
        int crankCount = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isContraptionOnSprocket(nextSprocket));
        //Could evaluate whether we actually want to crank those, but this is probably fine for now.
        return crankCount >= 2;
    }
}
