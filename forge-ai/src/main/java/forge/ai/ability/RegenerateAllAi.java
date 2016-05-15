package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class RegenerateAllAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        boolean chance = false;
        final Game game = ai.getGame();

        // filter AIs battlefield by what I can target
        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard, sa);
        list = CardLists.filter(list, CardPredicates.isController(ai));

        if (list.size() == 0) {
            return false;
        }

        int numSaved = 0;
        if (!game.getStack().isEmpty()) {
            final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa, true);

            for (final Card c : list) {
                if (objects.contains(c) && c.getShieldCount() == 0) {
                    numSaved++;
                }
            }
        } else {
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                final List<Card> combatants = CardLists.filter(list, CardPredicates.Presets.CREATURES);
                final Combat combat = game.getCombat();
                for (final Card c : combatants) {
                    if (c.getShieldCount() == 0 && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        numSaved++;
                    }
                }
            }
        }

        if (numSaved > 1) {
            chance = true;
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance = true;

        return chance;
    }

}
