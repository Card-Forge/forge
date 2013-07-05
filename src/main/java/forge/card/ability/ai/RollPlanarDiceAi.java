package forge.card.ability.ai;


import forge.Card;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.ai.AiController;
import forge.game.ai.AiProps;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerControllerAi;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import java.util.List;

public class RollPlanarDiceAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        Card plane = sa.getSourceCard();

        boolean decideToRoll = false;
        boolean rollInMain1 = false;
        String modeName = "never";
        int maxActivations = aic.getIntProperty(AiProps.DEFAULT_MAX_PLANAR_DIE_ROLLS_PER_TURN);
        int chance = aic.getIntProperty(AiProps.DEFAULT_PLANAR_DIE_ROLL_CHANCE);
        int hesitationChance = aic.getIntProperty(AiProps.PLANAR_DIE_ROLL_HESITATION_CHANCE);
        int minTurnToRoll = aic.getIntProperty(AiProps.DEFAULT_MIN_TURN_TO_ROLL_PLANAR_DIE);
        
        if (plane.hasSVar("AIRollPlanarDieParams")) {
            String[] params = plane.getSVar("AIRollPlanarDieParams").toLowerCase().trim().split("\\|");
            for (String param : params) {
                String[] paramData = param.split("\\$");
                String paramName = paramData[0].trim();
                String paramValue = paramData[1].trim();

                switch (paramName) {
                    case "mode":
                        modeName = paramValue;
                        break;
                    case "chance":
                        chance = Integer.parseInt(paramValue);
                        break;
                    case "minturn":
                        minTurnToRoll = Integer.parseInt(paramValue);
                        break;
                    case "maxrollsperturn":
                        maxActivations = Integer.parseInt(paramValue);
                        break;
                    case "rollinmain1":
                        if (paramValue.equals("true")) {
                            rollInMain1 = true;
                        }
                        break;
                    case "lowpriority":
                        // this is handled in AiController.saComparator at the moment
                        break;
                    case "cardsinhandle": // num of cards in hand less than or equal to N
                        if (ai.getCardsIn(ZoneType.Hand).size() > Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "cardsinhandge": // num of cards in hand greater than or equal to N
                        if (ai.getCardsIn(ZoneType.Hand).size() < Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "cardsingraveyardle":
                        if (ai.getCardsIn(ZoneType.Graveyard).size() > Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "cardsingraveyardge":
                        if (ai.getCardsIn(ZoneType.Graveyard).size() < Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "hascreatureinplay": // TODO: All abilities below only test for "True". The "False" condition is not implemented/parsed.
                        if (!detectCreatureInZone(ai, ZoneType.Battlefield)) {
                            return false;
                        }
                        break;
                    case "opphascreatureinplay":
                        List<Player> otherPlayers = ai.getOpponents();
                        boolean oppHasCreature = false;
                        for (Player op : otherPlayers) {
                            oppHasCreature |= detectCreatureInZone(op, ZoneType.Battlefield);
                        }
                        if (!oppHasCreature) {
                            return false;
                        }
                        break;
                    case "hascolorcreatureinplay":
                        if (!detectColorCreatureInZone(ai, paramValue, ZoneType.Battlefield)) {
                            return false;
                        }
                        break;
                    case "hascolorinplay":
                        if (!detectColorInZone(ai, paramValue, ZoneType.Battlefield)) {
                            return false;
                        }
                        break;
                    case "hascoloringraveyard":
                        if (!detectColorInZone(ai, paramValue, ZoneType.Graveyard)) {
                            return false;
                        }
                        break;
                    default:
                        System.out.println(String.format("Unexpected AI hint parameter in card %s in RollPlanarDiceAi: %s.", plane.getName(), paramName));
                        break;
                }
            }
            
            switch (modeName) {
                case "always":
                    decideToRoll = true;
                    break;
                case "random":
                    if (MyRandom.getRandom().nextInt(100) < chance) {
                        decideToRoll = true;
                    }
                    break;
                case "never":
                    return false;
                default:
                    return false;
            }

            if (ai.getGame().getPhaseHandler().getTurn() < minTurnToRoll) {
                decideToRoll = false;
            } else if (!rollInMain1 && ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                decideToRoll = false;
            }

            if (ai.getGame().getPhaseHandler().getPlanarDiceRolledthisTurn() >= maxActivations) {
                decideToRoll = false;
            }
        
            // check if the AI hesitates
            if (MyRandom.getRandom().nextInt(100) < hesitationChance) {
                decideToRoll = false; // hesitate
            }
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

    private boolean detectColorInZone(Player p, String paramValue, ZoneType zone) {
        boolean hasColorInPlay = false;
        for (Card c : p.getCardsIn(zone)) {
            if (paramValue.contains("u") && c.isBlue()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("g") && c.isGreen()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("r") && c.isRed()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("w") && c.isWhite()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("b") && c.isBlack()) {
                hasColorInPlay = true;
                break;
            }
        }
        return hasColorInPlay;
    }

    private boolean detectCreatureInZone(Player p, ZoneType zone) {
        boolean hasCreatureInPlay = false;
        for (Card c : p.getCardsIn(zone)) {
            if (c.isCreature()) {
                hasCreatureInPlay = true;
                break;
            }
        }
        return hasCreatureInPlay;
    }

    private boolean detectColorCreatureInZone(Player p, String paramValue, ZoneType zone) {
        boolean hasColorInPlay = false;
        for (Card c : p.getCardsIn(zone)) {
            if (paramValue.contains("u") && c.isBlue() && c.isCreature()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("g") && c.isGreen() && c.isCreature()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("r") && c.isRed() && c.isCreature()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("w") && c.isWhite() && c.isCreature()) {
                hasColorInPlay = true;
                break;
            }
            if (paramValue.contains("b") && c.isBlack() && c.isCreature()) {
                hasColorInPlay = true;
                break;
            }
        }
        return hasColorInPlay;
    }
}
