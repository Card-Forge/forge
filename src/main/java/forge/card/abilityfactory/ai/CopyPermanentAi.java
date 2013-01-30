package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class CopyPermanentAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // Card source = sa.getSourceCard();
        // TODO - I'm sure someone can do this AI better

        if (sa.hasParam("AtEOT") && !Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.MAIN1)) {
            return false;
        } else {
            double chance = .4; // 40 percent chance with instant speed stuff
            if (AbilityFactory.isSorcerySpeed(sa)) {
                chance = .667; // 66.7% chance for sorcery speed (since it will
                               // never activate EOT)
            }
            final Random r = MyRandom.getRandom();
            if (r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1)) {
                return this.doTriggerAINoCost(aiPlayer, sa, false);
            } else {
                return false;
            }
        }
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getSourceCard();

        // ////
        // Targeting

        final Target abTgt = sa.getTarget();

        if (abTgt != null) {
            List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);
            list = CardLists.getTargetableCards(list, sa);
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    final Map<String, String> vars = c.getSVars();
                    return !vars.containsKey("RemAIDeck");
                }
            }); 
            abTgt.resetTargets();
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                Card choice;
                if (!CardLists.filter(list, Presets.CREATURES).isEmpty()) {
                    choice = CardFactoryUtil.getBestCreatureAI(list);
                } else {
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, true);
                }

                if (choice == null) { // can't find anything left
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        } else {
            // if no targeting, it should always be ok
        }

        return true;
    }

}
