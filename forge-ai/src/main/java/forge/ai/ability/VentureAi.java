package forge.ai.ability;

import com.google.common.collect.Lists;
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
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // If this has a mana cost, do it at opponent's EOT if able to prevent spending mana early; if sorcery, do it in Main2
        PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
        if (sa.getPayCosts().hasManaCost() || sa.getPayCosts().hasTapCost()) {
            if (isSorcerySpeed(sa, aiPlayer)) {
                return ph.is(PhaseType.MAIN2, aiPlayer);
            } else {
                return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == aiPlayer;
            }
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(aiPlayer, sa);
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
            if (player.getController().isAI()) { // FIXME: is this needed? Can simulation ever run this for a non-AI player?
                room.setActivatingPlayer(player, true);
                if (((PlayerControllerAi)player.getController()).getAi().canPlaySa(room) == AiPlayDecision.WillPlay) {
                    viableRooms.add(room);
                }
            }
        }

        if (!viableRooms.isEmpty()) {
            // choose a room at random from the ones that are deemed playable
            return Aggregates.random(viableRooms);
        }

        return Aggregates.random(spells); // If we're here, we should choose at least something, so choose a random thing then
    }

}
