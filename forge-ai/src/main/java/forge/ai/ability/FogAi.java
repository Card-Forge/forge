package forge.ai.ability;


import java.util.List;

import forge.ai.AiCardMemory;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCombat;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

public class FogAi extends SpellAbilityAi {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final Card hostCard = sa.getHostCard();

        // Don't cast it, if the effect is already in place
        if (game.getReplacementHandler().isPreventCombatDamageThisTurn()) {
            return false;
        }

        // if card would be destroyed, react and use immediately if it's not own turn
        if ((AiCardMemory.isRememberedCard(ai, hostCard, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT))
                && (!game.getStack().isEmpty())
                && (!game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer()))) {
            final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(ai, null);
            if (objects.contains(hostCard)) {
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK);
                return true;
            }
        }

        // Reserve mana to cast this card if it will be likely needed
        if (((game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer()))
                || (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)))
                && (AiCardMemory.isMemorySetEmpty(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT))
                && (ComputerUtil.aiLifeInDanger(ai, false, 0))) {
            boolean reserved = ((PlayerControllerAi) ai.getController()).getAi().reserveManaSources(sa, PhaseType.COMBAT_DECLARE_BLOCKERS, true);
            if (reserved) {
                AiCardMemory.rememberCard(ai, hostCard, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT);
            }
        }

        // AI should only activate this during Human's Declare Blockers phase
        if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
            return false;
        }
        if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return false;
        }

        // Only cast when Stack is empty, so Human uses spells/abilities first
        if (!game.getStack().isEmpty()) {
            return false;
        }

        if ("SeriousDamage".equals(sa.getParam("AILogic")) && game.getCombat() != null) {
            int dmg = 0;
            for (Card atk : game.getCombat().getAttackersOf(ai)) {
                if (game.getCombat().isUnblocked(atk)) {
                    dmg += atk.getNetCombatDamage();
                } else if (atk.hasKeyword(Keyword.TRAMPLE)) {
                    dmg += atk.getNetCombatDamage() - Aggregates.sum(game.getCombat().getBlockers(atk), CardPredicates.Accessors.fnGetNetToughness);
                }
            }

            if (dmg > ai.getLife() / 4) {
                return true;
            } else if (dmg >= 5) {
                return true;
            } else if (ai.getLife() < ai.getStartingLife() / 3) {
                return true;
            }
        }

        // Cast it if life is in danger
        return ComputerUtilCombat.lifeInDanger(ai, game.getCombat());
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI should only activate this during Human's turn
        boolean chance;
        final Game game = ai.getGame();

        // should really check if other player is attacking this player
        if (ai.isOpponentOf(game.getPhaseHandler().getPlayerTurn())) {
            chance = game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Game game = aiPlayer.getGame();
        boolean chance;
        if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer().getWeakestOpponent())) {
            chance = game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        return chance || mandatory;
    }
}
