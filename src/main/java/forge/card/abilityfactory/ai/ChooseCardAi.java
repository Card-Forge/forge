package forge.card.abilityfactory.ai;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.zone.ZoneType;

public class ChooseCardAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Card host = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else {
                return false;
            }
        }
        if (sa.hasParam("AILogic")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            List<Card> choices = Singletons.getModel().getGame().getCardsIn(choiceZone);
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
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        return canPlayAI(ai, sa);
    }
}
