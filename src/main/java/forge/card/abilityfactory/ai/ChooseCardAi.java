package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ChooseCardAi extends SpellAiLogic {
 
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
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
        if (params.containsKey("AILogic")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (params.containsKey("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(params.get("ChoiceZone"));
            }
            List<Card> choices = Singletons.getModel().getGame().getCardsIn(choiceZone);
            if (params.containsKey("Choices")) {
                choices = CardLists.getValidCards(choices, params.get("Choices"), host.getController(), host);
            }
            if (params.containsKey("TargetControls")) {
                choices = CardLists.filterControlledBy(choices, ai.getOpponent());
            }
            if (params.get("AILogic").equals("AtLeast1")) {
                if (choices.isEmpty()) {
                    return false;
                }
            } else if (params.get("AILogic").equals("AtLeast2") || params.get("AILogic").equals("BestBlocker")) {
                if (choices.size() < 2) {
                    return false;
                }
            } else if (params.get("AILogic").equals("Clone")) {
                choices = CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.NonLegendary", host.getController(), host);
                if (choices.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }
    
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player ai) {
        return canPlayAI(ai, params, sa);
    }
}