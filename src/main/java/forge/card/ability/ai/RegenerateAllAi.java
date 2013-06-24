package forge.card.ability.ai;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.ITargetable;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class RegenerateAllAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        boolean chance = false;
        final Cost abCost = sa.getPayCosts();
        final Game game = ai.getGame();
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, hostCard)) {
                return false;
            }

            if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, abCost, hostCard)) {
                return false;
            }

            if (!ComputerUtilCost.checkLifeCost(ai, abCost, hostCard, 4, null)) {
                return false;
            }
        }

        // filter AIs battlefield by what I can target
        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);
        list = CardLists.filter(list, CardPredicates.isController(ai));

        if (list.size() == 0) {
            return false;
        }

        int numSaved = 0;
        if (!game.getStack().isEmpty()) {
            final List<ITargetable> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);

            for (final Card c : list) {
                if (objects.contains(c) && c.getShield() == 0) {
                    numSaved++;
                }
            }
        } else {
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                final List<Card> combatants = CardLists.filter(list, CardPredicates.Presets.CREATURES);
                final Combat combat = game.getCombat();
                for (final Card c : combatants) {
                    if (c.getShield() == 0 && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
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
