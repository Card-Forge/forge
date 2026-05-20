package forge.ai.ability;


import forge.ai.*;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class FogAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final Card hostCard = sa.getHostCard();
        final Combat combat = game.getCombat();

        // Don't cast it, if the effect is already in place
        if (game.getReplacementHandler().isPreventCombatDamageThisTurn()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // TODO Test if we can even Fog successfully
        if (handleMemoryCheck(ai, sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // Only cast when Stack is empty, so Human uses spells/abilities first
        if (!game.getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // TODO Only cast outside of combat if I won't be able to cast inside of combat
        if (combat == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // AI should only activate this during Opponents Declare Blockers phase
        if (!game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai) ||
            !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // TODO Be careful of effects that don't let you cast spells during combat
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        int remainingLife = ComputerUtilCombat.lifeThatWouldRemain(ai, combat);
        int dmg = ai.getLife() - remainingLife;

        // Count the number of Fog spells in hand
        int fogs = countAvailableFogs(ai);
        if (fogs > 2 && dmg > 2) {
            // Playing a fog deck. If you got them play them.
            return new AiAbilityDecision(100, AiPlayDecision.Tempo);
        }
        if (dmg > 2 &&
                hostCard.hasKeyword(Keyword.BUYBACK) &&
                CardLists.count(ai.getCardsIn(ZoneType.Battlefield), Card::isLand) > 3) {
            // Constant mists sacrifices a land to buyback. But if AI is running it, they are probably ok sacrificing some lands
            return new AiAbilityDecision(100, AiPlayDecision.Tempo);
        }

        if ("SeriousDamage".equals(sa.getParam("AILogic"))) {
            if (dmg > ai.getLife() / 4) {
                return new AiAbilityDecision(100, AiPlayDecision.Tempo);
            } else if (dmg >= 5) {
                return new AiAbilityDecision(100, AiPlayDecision.Tempo);
            } else if (ai.getLife() < ai.getStartingLife() / 3) {
                return new AiAbilityDecision(100, AiPlayDecision.Tempo);
            }
        }
        // TODO Compare to poison counters?

        // Cast it if life is in danger
        if (ComputerUtilCombat.lifeInDanger(ai, game.getCombat())) {
            return new AiAbilityDecision(100, AiPlayDecision.Tempo);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    private boolean handleMemoryCheck(Player ai, SpellAbility sa) {
        Card hostCard = sa.getHostCard();
        Game game = ai.getGame();

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
        return false;
    }

    private int countAvailableFogs(Player ai) {
        int fogs = 0;
        for (Card c : ai.getCardsActivatableInExternalZones(false)) {
            for (SpellAbility ability : c.getSpellAbilities()) {
                if (ApiType.Fog.equals(ability.getApi())) {
                    fogs++;
                    break;
                }
            }
        }

        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            for (SpellAbility ability : c.getSpellAbilities()) {
                if (ApiType.Fog.equals(ability.getApi())) {
                    fogs++;
                    break;
                }
            }
        }
        return fogs;
    }


    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance;
        final Game game = ai.getGame();

        // should really check if other player is attacking this player
        if (ai.isOpponentOf(game.getPhaseHandler().getPlayerTurn())) {
            chance = game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        if (chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Game game = aiPlayer.getGame();
        boolean chance;
        if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer().getWeakestOpponent())) {
            chance = game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        if (mandatory || chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
