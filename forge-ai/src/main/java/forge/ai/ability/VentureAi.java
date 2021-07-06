package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.AiPlayDecision;
import forge.ai.AiProps;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.card.ICardFace;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

import java.util.List;
import java.util.Map;

public class VentureAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // TODO: is it ever a bad idea to venture into a dungeon?
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(aiPlayer, sa);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }

    // AI that handles choosing the next room in a dungeon
    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells, Map<String, Object> params) {
        List<SpellAbility> viableRooms = Lists.newArrayList();

        for (SpellAbility room : spells) {
            if (player.getController().isAI()) { // FIXME: is this needed? Can simulation ever run this for a non-AI player?
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

    // AI that chooses which dungeon to venture into
    @Override
    public String chooseCardName(Player ai, SpellAbility sa, List<ICardFace> faces) {
        // TODO: improve the conditions that define which dungeon is a viable option to choose
        List<String> dungeonNames = Lists.newArrayList();
        for (ICardFace face : faces) {
            dungeonNames.add(face.getName());
        }

        // Don't choose Tomb of Annihilation when life in danger unless we can win right away or can't lose for 0 life
        if (ai.getController().isAI()) { // FIXME: is this needed? Can simulation ever run this for a non-AI player?
            int lifeInDanger = (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.AI_IN_DANGER_THRESHOLD));
            if ((ai.getLife() <= lifeInDanger && !ai.cantLoseForZeroOrLessLife())
                    && !(ai.getLife() > 1 && ai.getWeakestOpponent().getLife() == 1)) {
                dungeonNames.remove("Tomb of Annihilation");
            }
        }

        return Aggregates.random(dungeonNames);
    }
}
