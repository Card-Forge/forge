package forge.ai.ability;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import forge.ai.AiCardMemory;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.CardType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import java.util.Arrays;
import java.util.List;

public class ChooseTypeAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        if (!sa.hasParam("AILogic")) {
            return false;
        } else if ("MostProminentComputerControls".equals(sa.getParam("AILogic"))) {
            if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Mirror Entity Avatar")) {
                if (AiCardMemory.isRememberedCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ANIMATED_THIS_TURN)) {
                    return false;
                }
                if (!aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN1, aiPlayer)) {
                    return false;
                }

                List<String> valid = Lists.newArrayList(CardType.getAllCreatureTypes());
                String chosenType = ComputerUtilCard.getMostProminentType(aiPlayer.getCardsIn(ZoneType.Battlefield), valid);
                if (chosenType.isEmpty()) {
                    return false;
                }

                int maxX = ComputerUtilMana.determineMaxAffordableX(aiPlayer, sa);
                int avgPower = 0;

                if (maxX > 1) {
                    CardCollection cre = CardLists.filter(aiPlayer.getCardsIn(ZoneType.Battlefield), 
                            Predicates.and(CardPredicates.isType(chosenType), CardPredicates.Presets.UNTAPPED));
                    if (!cre.isEmpty()) {
                        for (Card c: cre) {
                            avgPower += c.getCurrentPower();
                        }
                        avgPower /= cre.size();
                        if (maxX > avgPower) {
                            sa.setSVar("PayX", String.valueOf(maxX));
                            AiCardMemory.rememberCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ANIMATED_THIS_TURN);
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        return doTriggerAINoCost(aiPlayer, sa, false);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            sa.resetTargets();
            sa.getTargets().add(ai);
        } else {
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa)) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

}
