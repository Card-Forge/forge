package forge.ai.ability;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.ai.*;
import forge.card.*;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCopyService;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;

public class ChooseCardNameAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
            }

            String logic = sa.getParam("AILogic");
            if (logic.equals("CursedScroll")) {
                if (SpecialCardAi.CursedScroll.consider(ai, sa)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            final TargetRestrictions tgt = sa.getTargetRestrictions();
            if (tgt != null) {
                sa.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    sa.getTargets().add(AiAttackController.choosePreferredDefenderPlayer(ai));
                } else {
                    sa.getTargets().add(ai);
                }
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        String aiLogic = sa.getParamOrDefault("AILogic", "");
        if ("PithingNeedle".equals(aiLogic)) {
            // Make sure theres something in play worth Needlings.
            // Planeswalker or equipment or something

            CardCollection oppPerms = CardLists.getValidCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), "Card.OppCtrl+hasNonManaActivatedAbility", ai, sa.getHostCard(), sa);
            if (oppPerms.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            }

            Card card = ComputerUtilCard.getBestPlaneswalkerAI(oppPerms);
            if (card != null) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // 5 percent chance to cast per opposing card with a non mana ability
            if (MyRandom.getRandom().nextFloat() <= .05 * oppPerms.size()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return ComputerUtilCard.getBestAI(options);
    }

    @Override
    public String chooseCardName(Player ai, SpellAbility sa, List<ICardFace> faces) {
        // this function is only for "Alhammarret, High Arbiter"

        if (faces.isEmpty()) {
            return "";
        } else if (faces.size() == 1) {
            return Iterables.getFirst(faces, null).getName();
        }

        List<Card> cards = Lists.newArrayList();
        final CardDb cardDb = StaticData.instance().getCommonCards();

        for (ICardFace face : faces) {
            final CardRules rules = cardDb.getRules(face.getName());
            boolean isOther = rules.getOtherPart() == face;
            final PaperCard paper = cardDb.getCard(rules.getName());
            final Card card = Card.fromPaperCard(paper, ai);

            if (rules.getSplitType() == CardSplitType.Split) {
                Card copy = CardCopyService.getLKICopy(card);
                // for calcing i need only one split side
                if (isOther) {
                    copy.getCurrentState().copyFrom(card.getState(CardStateName.RightSplit), true);
                } else {
                    copy.getCurrentState().copyFrom(card.getState(CardStateName.LeftSplit), true);
                }
                copy.updateStateForView();

                cards.add(copy);
            } else if (!isOther) {
                // other can't be cast that way, not need to prevent that
                cards.add(card);
            }
        }

        return ComputerUtilCard.getBestAI(cards).getName();
    }
}
