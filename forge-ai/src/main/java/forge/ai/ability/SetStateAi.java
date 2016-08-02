package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SetStateAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(final Player aiPlayer, final SpellAbility sa) {
        final Card source = sa.getHostCard();

        if (!source.hasAlternateState()) {
            System.err.println("Warning: SetState without ALTERNATE on " + source.getName() + ".");
            return false;
        }

        // Prevent transform into legendary creature if copy already exists
        // Check first if Legend Rule does still apply
        if (!aiPlayer.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {

            // check if the other side is legendary and if such Card already is in Play
            final CardState other = source.getAlternateState();

            if (other != null && other.getType().isLegendary() && aiPlayer.isCardInPlay(other.getName())) {
                if (!other.getType().isCreature()) {
                    return false;
                }

                final Card othercard = aiPlayer.getCardsIn(ZoneType.Battlefield, other.getName()).getFirst();

                // for legendary KI counter creatures
                if (othercard.getCounters(CounterType.KI) >= source.getCounters(CounterType.KI)) {
                    // if the other legendary is useless try to replace it
                    if (!isUselessCreature(aiPlayer, othercard)) {
                        return false;
                    }
                }
            }
        }
        
        if (sa.getTargetRestrictions() != null) {
            return false;
        }

        if("Transform".equals(sa.getParam("Mode"))) {
            return !source.hasKeyword("CARDNAME can't transform");
        } else if ("Flip".equals(sa.getParam("Mode"))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // Gross generalization, but this always considers alternate
        // states more powerful
        return !sa.getHostCard().isInAlternateState();
    }

    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        final Card source = sa.getHostCard();

        if (!source.hasAlternateState()) {
            System.err.println("Warning: SetState without ALTERNATE on " + source.getName() + ".");
            return false;
        }

        if("Transform".equals(sa.getParam("Mode"))) {
            // need a copy for evaluation
            Card transformed = CardUtil.getLKICopy(source);
            transformed.getCurrentState().copyFrom(source, source.getAlternateState());
            transformed.updateStateForView();

            int valueSource = ComputerUtilCard.evaluateCreature(source);
            int valueTransformed = ComputerUtilCard.evaluateCreature(transformed);

            // it would not survive being transformed
            if (transformed.getNetToughness() < 1) {
                return false;
            }

            // check which state would be better for attacking
            if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                boolean transformAttack = false;

                // if an opponent can't block it, no need to transform (back)
                for (Player opp : ai.getOpponents()) {
                    
                    boolean attackSource = !ComputerUtilCard.canBeBlockedProfitably(opp, source);
                    boolean attackTransformed = !ComputerUtilCard.canBeBlockedProfitably(opp, transformed);

                    // both forms can attack, try to use the one with better value 
                    if (attackSource && attackTransformed) {
                        return valueSource <= valueTransformed;
                    } else if (attackTransformed) { // only transformed cam attack
                        transformAttack = true;
                    }
                }

                // can only attack in transformed form
                if (transformAttack) {
                    return true;
                }
            } else if (ph.isPlayerTurn(ai) && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                if (ph.inCombat() && ph.getCombat().isUnblocked(source)) {
                    // if source is unblocked, check for the power
                    return source.getNetPower() <= transformed.getNetPower();
                }
            }
            // no clear way, alternate state is better,
            // but for more cleaner way use Evaluate for check
            return valueSource <= valueTransformed;
        }
        return true;
    }
}
