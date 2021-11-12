package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class UntapAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
        	if (ApiType.AddPhase == abSub.getApi() 
        			&& source.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_END)) {
        		return false;
        	}
            CardCollectionView list = CardLists.filter(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.TAPPED);
            final String valid = sa.getParamOrDefault("ValidCards", "");
            list = CardLists.getValidCards(list, valid.split(","), source.getController(), source, sa);
            // don't untap if only opponent benefits
            PlayerCollection goodControllers = aiPlayer.getAllies();
            goodControllers.add(aiPlayer);
            list = CardLists.filter(list, CardPredicates.isControlledByAnyOf(goodControllers));
            return !list.isEmpty();
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        Card source = sa.getHostCard();

        if (sa.hasParam("ValidCards")) {
            String valid = sa.getParam("ValidCards");
            CardCollectionView list = CardLists.filter(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.TAPPED);
            list = CardLists.getValidCards(list, valid.split(","), source.getController(), source, sa);
            return mandatory || !list.isEmpty();
        }

        return mandatory;
    }
}
