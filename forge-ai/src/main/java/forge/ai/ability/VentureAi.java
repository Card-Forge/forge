package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

import java.util.List;
import java.util.Map;

public class VentureAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // If this has a mana cost, do it at opponent's EOT if able to prevent spending mana early; if sorcery, do it in Main2
        PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
        if (sa.getPayCosts().hasManaCost() || sa.getPayCosts().hasTapCost()) {
            if (isSorcerySpeed(sa, aiPlayer)) {
                if (ph.is(PhaseType.MAIN2, aiPlayer)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else {
                if (ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == aiPlayer) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        return decision;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    // AI that handles choosing the next room in a dungeon
    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells, Map<String, Object> params) {
        List<SpellAbility> viableRooms = Lists.newArrayList();

        for (SpellAbility room : spells) {
            if (player.getController().isAI()) {
                room.setActivatingPlayer(player);
                AiPlayDecision playDecision = ((PlayerControllerAi)player.getController()).getAi().canPlaySa(room);
                if (playDecision == AiPlayDecision.WillPlay) {
                    viableRooms.add(room);
                }
            }
        }

        if (!viableRooms.isEmpty()) {
            return Aggregates.random(viableRooms);
        }

        return Aggregates.random(spells);
    }

}
