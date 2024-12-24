package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AdvanceCrankAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        int nextSprocket = (ai.getCrankCounter() % 3) + 1;
        int crankCount = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isContraptionOnSprocket(nextSprocket));
        //Could evaluate whether we actually want to crank those, but this is probably fine for now.
        if(crankCount < 2)
            return false;
        return super.canPlayAI(ai, sa);
    }

    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph, String logic) {
        if(logic.equals("AtOppEOT"))
            return ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN);

        return super.checkPhaseRestrictions(ai, sa, ph, logic);
    }
}
