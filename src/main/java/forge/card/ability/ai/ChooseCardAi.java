package forge.card.ability.ai;

import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ChooseCardAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(final Player ai, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Game game = ai.getGame();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (sa.canTarget(ai.getOpponent())) {
                sa.getTargets().add(ai.getOpponent());
            } else {
                return false;
            }
        }
        if (sa.hasParam("AILogic")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            List<Card> choices = ai.getGame().getCardsIn(choiceZone);
            if (sa.hasParam("Choices")) {
                choices = CardLists.getValidCards(choices, sa.getParam("Choices"), host.getController(), host);
            }
            if (sa.hasParam("TargetControls")) {
                choices = CardLists.filterControlledBy(choices, ai.getOpponent());
            }
            if (sa.getParam("AILogic").equals("AtLeast1")) {
                if (choices.isEmpty()) {
                    return false;
                }
            } else if (sa.getParam("AILogic").equals("AtLeast2") || sa.getParam("AILogic").equals("BestBlocker")) {
                if (choices.size() < 2) {
                    return false;
                }
            } else if (sa.getParam("AILogic").equals("Clone")) {
                choices = CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host);
                if (choices.isEmpty()) {
                    return false;
                }
            } else if (sa.getParam("AILogic").equals("Never")) {
                return false;
            } else if (sa.getParam("AILogic").equals("NeedsPrevention")) {
                if (!game.getPhaseHandler().getPhase() .equals(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    return false;
                }
                choices = CardLists.filter(choices, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (!c.isAttacking(ai) || !game.getCombat().isUnblocked(c)) {
                            return false;
                        }
                        if (host.getName().equals("Forcefield")) {
                            return ComputerUtilCombat.damageIfUnblocked(c, ai, game.getCombat()) > 1;
                        }
                        return ComputerUtilCombat.damageIfUnblocked(c, ai, game.getCombat()) > 0;
                    }
                });
                if (choices.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, SpellAbility sa, List<Card> options, boolean isOptional) {
        final Card host = sa.getSourceCard();
        final String logic = sa.getParam("AILogic");
        Card choice = null;
        if (logic == null) {
            // Base Logic is choose "best"
            choice = ComputerUtilCard.getBestAI(options);
        } else if ("WorstCard".equals(logic)) {
            choice = ComputerUtilCard.getWorstAI(options);
        } else if (logic.equals("BestBlocker")) {
            if (!CardLists.filter(options, Presets.UNTAPPED).isEmpty()) {
                options = CardLists.filter(options, Presets.UNTAPPED);
            }
            choice = ComputerUtilCard.getBestCreatureAI(options);
        } else if (logic.equals("Clone")) {
            if (!CardLists.getValidCards(options, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host).isEmpty()) {
                options = CardLists.getValidCards(options, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host);
            }
            choice = ComputerUtilCard.getBestAI(options);
        } else if (logic.equals("Untap")) {
            if (!CardLists.getValidCards(options, "Permanent.YouCtrl,Permanent.tapped", host.getController(), host).isEmpty()) {
                options = CardLists.getValidCards(options, "Permanent.YouCtrl,Permanent.tapped", host.getController(), host);
            }
            choice = ComputerUtilCard.getBestAI(options);
        } else if (logic.equals("NeedsPrevention")) {
            List<Card> better =  CardLists.filter(options, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    final Game game = ai.getGame();
                    if (!c.isAttacking(ai) || !game.getCombat().isUnblocked(c)) {
                        return false;
                    }
                    if (host.getName().equals("Forcefield")) {
                        return ComputerUtilCombat.damageIfUnblocked(c, ai, game.getCombat()) > 1;
                    }
                    return ComputerUtilCombat.damageIfUnblocked(c, ai, game.getCombat()) > 0;
                }
            });
            if (!better.isEmpty()) {
                choice = ComputerUtilCard.getBestAI(better);
            } else {
                choice = ComputerUtilCard.getBestAI(options);
            }
        } else {
            choice = ComputerUtilCard.getBestAI(options);
        }
        return choice;
    }
}
