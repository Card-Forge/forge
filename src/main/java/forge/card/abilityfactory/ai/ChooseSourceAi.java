package forge.card.abilityfactory.ai;

import java.util.List;
import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ChooseSourceAi extends SpellAiLogic {
 
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // TODO: AI Support! Currently this is copied from AF ChooseCard.
        //       When implementing AI, I believe AI also needs to be made aware of the damage sources chosen
        //       to be prevented (e.g. so the AI doesn't attack with a creature that will not deal any damage
        //       to the player because a CoP was pre-activated on it - unless, of course, there's another
        //       possible reason to attack with that creature).
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
                choices = CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.NonLegendary", host.getController(), host);
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
}
