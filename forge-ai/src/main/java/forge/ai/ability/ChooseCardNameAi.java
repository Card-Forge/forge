package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.ai.AiAttackController;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.CardStateName;
import forge.card.ICardFace;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.item.PaperCard;

public class ChooseCardNameAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }

            String logic = sa.getParam("AILogic");
            if (logic.equals("CursedScroll")) {
                return SpecialCardAi.CursedScroll.consider(ai, sa);
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
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return mandatory;
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
                Card copy = CardUtil.getLKICopy(card);
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
