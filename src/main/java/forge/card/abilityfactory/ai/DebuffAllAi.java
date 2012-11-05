package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DebuffAllAi extends SpellAiLogic {
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        String valid = "";
        final Random r = MyRandom.getRandom();
        // final Card source = sa.getSourceCard();
        final Card hostCard = sa.getAbilityFactory().getHostCard();
        final Player opp = ai.getOpponent();
        
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn()); // to
        // prevent
        // runaway
        // activations

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        List<Card> comp = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, hostCard.getController(), hostCard);
        List<Card> human = CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid, hostCard.getController(), hostCard);

        // TODO - add blocking situations here also

        // only count creatures that can attack
        human = CardLists.filter(human, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CombatUtil.canAttack(c, opp);
            }
        });

        // don't use DebuffAll after Combat_Begin until AI is improved
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_BEGIN)) {
            return false;
        }

        if (comp.size() > human.size()) {
            return false;
        }

        return (r.nextFloat() < .6667) && chance;
    } // debuffAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return true;
    }

    /**
     * <p>
     * debuffAllChkDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

}