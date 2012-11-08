package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.util.MyRandom;

public class FightAi extends SpellAiLogic { 
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        Target tgt = sa.getTarget();
        tgt.resetTargets();

        List<Card> aiCreatures = ai.getCreaturesInPlay();
        aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
        aiCreatures = CardLists.filter(aiCreatures, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getSVar("Targeting").equals("Dies");
            }
        });

        List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);

        final Random r = MyRandom.getRandom();
        if (r.nextFloat() > Math.pow(.6667, sa.getActivationsThisTurn())) {
            return false;
        }

        if (sa.hasParam("TargetsFromDifferentZone")) {
            if (humCreatures.size() > 0 && aiCreatures.size() > 0) {
                for (Card humanCreature : humCreatures) {
                    for (Card aiCreature : aiCreatures) {
                        if (humanCreature.getKillDamage() <= aiCreature.getNetAttack()
                                && humanCreature.getNetAttack() < aiCreature.getKillDamage()) {
                            // todo: check min/max targets; see if we picked the best matchup
                            tgt.addTarget(humanCreature);
                            tgt.addTarget(aiCreature);
                            return true;
                        } else if (humanCreature.getSVar("Targeting").equals("Dies")) {
                            tgt.addTarget(humanCreature);
                            tgt.addTarget(aiCreature);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        for (Card creature1 : humCreatures) {
            for (Card creature2 : humCreatures) {
                if (creature1.equals(creature2)) {
                    continue;
                }
                if (sa.hasParam("TargetsWithoutSameCreatureType") 
                        && creature1.sharesCreatureTypeWith(creature2)) {
                    continue;
                }
                if (creature1.getKillDamage() <= creature2.getNetAttack()
                        && creature1.getNetAttack() >= creature2.getKillDamage()) {
                    // todo: check min/max targets; see if we picked the best matchup
                    tgt.addTarget(creature1);
                    tgt.addTarget(creature2);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // check AI life before playing this drawback?
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return false;
    }

}