package forge.card.ability.ai;


import forge.Card;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.ai.AiController;
import forge.game.ai.AiProps;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerControllerAi;
import forge.util.MyRandom;

public class RollPlanarDiceAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        Card plane = sa.getSourceCard();

        boolean decideToRoll = false;
        int maxActivations = aic.getIntProperty(AiProps.DEFAULT_MAX_PLANAR_DIE_ROLLS_PER_TURN);
        int chance = aic.getIntProperty(AiProps.DEFAULT_PLANAR_DIE_ROLL_CHANCE);
        int hesitationChance = aic.getIntProperty(AiProps.PLANAR_DIE_ROLL_HESITATION_CHANCE);
        
        if (!plane.hasSVar("AIRollPlanarDieInMain1") && ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        } else if (plane.hasSVar("AIRollPlanarDieInMain1") && plane.getSVar("AIRollPlanarDieInMain1").toLowerCase().equals("false")) {
            return false;
        }

        if (plane.hasSVar("AIRollPlanarDie")) {
            switch (plane.getSVar("AIRollPlanarDie")) {
                case "Always":
                    decideToRoll = true;
                    break;
                case "Random":
                    if (plane.hasSVar("AIRollPlanarDieChance")) {
                        chance = Integer.parseInt(plane.getSVar("AIRollPlanarDieChance"));
                    }
                    if (MyRandom.getRandom().nextInt(100) < chance) {
                        decideToRoll = true;
                    }
                    break;
                case "Never":
                    break;
                default:
                    break;
            }
        }

        if (plane.hasSVar("AIRollPlanarDieMaxPerTurn")) {
            maxActivations = Integer.parseInt(plane.getSVar("AIRollPlanarDieMaxPerTurn"));
        }
        if (ai.getGame().getPhaseHandler().getPlanarDiceRolledthisTurn() >= maxActivations) {
            decideToRoll = false;
        }
        
        // check if the AI hesitates
        if (MyRandom.getRandom().nextInt(100) < hesitationChance) {
            decideToRoll = false; // hesitate
        }

        return decideToRoll ? true : false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // for potential implementation of drawback checks?
        return canPlayAI(aiPlayer, sa);
    }
}

