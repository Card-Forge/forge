package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.card.CardStateName;

import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SetStateAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(final Player aiPlayer, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String mode = sa.getParam("Mode");

        // turning face is most likely okay
        // TODO only do this at beneficial moment (e.g. surprise during combat or morph trigger), might want to reserve mana to protect them from easy removal
        if ("TurnFaceUp".equals(mode) || "TurnFaceDown".equals(mode)) {
            return true;
        }

        // Prevent transform into legendary creature if copy already exists
        if (!isSafeToTransformIntoLegendary(aiPlayer, source)) {
            return false;
        }

        if (sa.getSVar("X").equals("Count$xPaid")) {
            final int xPay = ComputerUtilCost.getMaxXValue(sa, aiPlayer, sa.isTrigger());
            sa.setXManaCostPaid(xPay);
        }

        if ("Transform".equals(mode) || "Flip".equals(mode)) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean checkAiLogic(final Player aiPlayer, final SpellAbility sa, final String aiLogic) {
        return super.checkAiLogic(aiPlayer, sa, aiLogic);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // Gross generalization, but this always considers alternate states more powerful
        return !sa.getHostCard().isInAlternateState();
    }

    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        final String mode = sa.getParam("Mode");
        final Card source = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");

        if ("Transform".equals(mode)) {
            if (!sa.usesTargeting()) {
                // no Transform with Defined which is not Self
                if (!source.canTransform(sa)) {
                    return false;
                }
                return shouldTransformCard(source, ai, ph) || "Always".equals(logic);
            } else {
                sa.resetTargets();

                // select only the ones that can transform
                CardCollection list = CardLists.filter(CardUtil.getValidCardsToTarget(sa), CardPredicates.Presets.CREATURES, c -> c.canTransform(sa));

                if (list.isEmpty()) {
                    return false;
                }

                for (final Card c : list) {
                    if (shouldTransformCard(c, ai, ph) || "Always".equals(logic)) {
                        sa.getTargets().add(c);
                        if (sa.isMaxTargetChosen()) {
                            break;
                        }
                    }
                }

                return sa.isMinTargetChosen();
            }
        } else if ("TurnFaceUp".equals(mode) || "TurnFaceDown".equals(mode)) {
            if (sa.usesTargeting()) {
                sa.resetTargets();

                List<Card> list = CardUtil.getValidCardsToTarget(sa);

                if (list.isEmpty()) {
                    return false;
                }

                for (final Card c : list) {
                    if (shouldTurnFace(c, ai, ph, mode) || "Always".equals(logic)) {
                        sa.getTargets().add(c);
                        if (!sa.canAddMoreTarget()) {
                            break;
                        }
                    }
                }

                return sa.isTargetNumberValid();
            } else {
                CardCollection list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
                if (list.isEmpty()) {
                    return false;
                }
                return shouldTurnFace(list.get(0), ai, ph, mode) || "Always".equals(logic);
            }
        }
        return true;
    }

    private boolean shouldTransformCard(Card card, Player ai, PhaseHandler ph) {
        if (!card.hasAlternateState()) {
            System.err.println("Warning: SetState without ALTERNATE on " + card.getName() + ".");
            return false;
        }

        // need a copy for evaluation
        Card transformed = CardCopyService.getLKICopy(card);
        transformed.getCurrentState().copyFrom(card.getAlternateState(), true);
        transformed.updateStateForView();

        // TODO: compareCards assumes that a creature will transform into a creature. Need to improve this
        // for other things potentially transforming.
        return compareCards(card, transformed, ai, ph);
    }

    private boolean shouldTurnFace(Card card, Player ai, PhaseHandler ph, String mode) {
        if (card.isFaceDown()) {
            if ("TurnFaceDown".equals(mode)) {
                return false;
            }
            // hidden agenda
            if (card.getState(CardStateName.Original).hasIntrinsicKeyword("Hidden agenda")
                    && card.isInZone(ZoneType.Command)) {
                String chosenName = card.getNamedCard();
                for (Card cast : ai.getGame().getStack().getSpellsCastThisTurn()) {
                    if (cast.getController() == ai && cast.getName().equals(chosenName)) {
                        return true;
                    }
                }
                return false;
            }

            // non-permanent facedown can't be turned face up
            if (!card.getRules().getType().isPermanent() || !card.canBeTurnedFaceUp()) {
                return false;
            }            
        } else {
            if ("TurnFaceUp".equals(mode)) {
                return false;
            }
            // doublefaced or meld cards can't be turned face down
            if (card.isTransformable() || card.isMeldable()) {
                return false;
            }
        }

        // need a copy for evaluation
        Card transformed = CardCopyService.getLKICopy(card);
        if (!card.isFaceDown()) {
            transformed.turnFaceDown(true);
        } else {
            transformed.forceTurnFaceUp();
        }
        transformed.updateStateForView();
        return compareCards(card, transformed, ai, ph);
    }
    
    private boolean compareCards(Card original, Card copy, Player ai, PhaseHandler ph) {
        int valueCard = ComputerUtilCard.evaluateCreature(original);
        int valueTransformed = ComputerUtilCard.evaluateCreature(copy);

        // card controlled by opponent, try to kill it or weak it
        if (original.getController().isOpponentOf(ai)) {
            return copy.getNetToughness() < 1 || valueCard > valueTransformed;
        }

        // it would not survive being transformed
        if (copy.getNetToughness() < 1) {
            return false;
        }

        // check which state would be better for attacking
        if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            boolean transformAttack = false;

            // if an opponent can't block it, no need to transform (back)
            for (Player opp : ai.getOpponents()) {
                boolean attackCard = !ComputerUtilCard.canBeBlockedProfitably(opp, original, true);
                boolean attackTransformed = !ComputerUtilCard.canBeBlockedProfitably(opp, copy, true);

                // both forms can attack, try to use the one with better value 
                if (attackCard && attackTransformed) {
                    return valueCard <= valueTransformed;
                } else if (attackTransformed) { // only transformed can attack
                    transformAttack = true;
                }
            }

            // can only attack in transformed form
            if (transformAttack) {
                return true;
            }
        } else if (ph.isPlayerTurn(ai) && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            if (ph.inCombat() && ph.getCombat().isUnblocked(original)) {
                // if source is unblocked, check for the power
                return original.getNetPower() <= copy.getNetPower();
            }
        }
        // no clear way, alternate state is better,
        // but for more cleaner way use Evaluate for check
        return valueCard <= valueTransformed;
    }

    private boolean isSafeToTransformIntoLegendary(Player aiPlayer, Card source) {
        // Prevent transform into legendary creature if copy already exists
        // Check first if Legend Rule does still apply
        if (!source.ignoreLegendRule()) {
            if (!source.hasAlternateState()) {
                System.err.println("Warning: SetState without ALTERNATE on " + source.getName() + ".");
                return false;
            }

            // check if the other side is legendary and if such Card already is in Play
            final CardState other = source.getAlternateState();

            if (other != null && other.getType().isLegendary() && aiPlayer.isCardInPlay(other.getName())) {
                if (!other.getType().isCreature()) {
                    return false;
                }

                final Card othercard = aiPlayer.getCardsIn(ZoneType.Battlefield, other.getName()).getFirst();

                // for legendary KI counter creatures
                if (othercard.getCounters(CounterEnumType.KI) >= source.getCounters(CounterEnumType.KI)) {
                    // if the other legendary is useless try to replace it
                    return ComputerUtilCard.isUselessCreature(aiPlayer, othercard);
                }
            }
        }

        return true;
    }

    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        // TODO: improve the AI for when it may want to transform something that's optional to transform
        return isSafeToTransformIntoLegendary(player, sa.getHostCard());
    }
}
