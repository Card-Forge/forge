package forge.card.ability.ai;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.ai.ComputerUtilCard;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class CopyPermanentAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // Card source = sa.getSourceCard();
        // TODO - I'm sure someone can do this AI better

        if (sa.hasParam("AtEOT") && !aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN1)) {
            return false;
        } else {
            double chance = .4; // 40 percent chance with instant speed stuff
            if (SpellAbilityAi.isSorcerySpeed(sa)) {
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
    protected boolean doTriggerAINoCost(final Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getSourceCard();

        // ////
        // Targeting

        final TargetRestrictions abTgt = sa.getTargetRestrictions();

        if (abTgt != null) {
            List<Card> list = aiPlayer.getGame().getCardsIn(ZoneType.Battlefield);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);
            list = CardLists.getTargetableCards(list, sa);
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    final Map<String, String> vars = c.getSVars();
                    return !vars.containsKey("RemAIDeck");
                }
            }); 
            sa.resetTargets();
            // target loop
            while (sa.getTargets().getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.isEmpty()) {
                    if ((sa.getTargets().getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (sa.getTargets().getNumTargeted() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return !c.isType("Legendary") || c.getController().isOpponentOf(aiPlayer);
                    }
                });
                Card choice;
                if (!CardLists.filter(list, Presets.CREATURES).isEmpty()) {
                    choice = ComputerUtilCard.getBestCreatureAI(list);
                } else {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list, sa, true);
                }

                if (choice == null) { // can't find anything left
                    if ((sa.getTargets().getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (sa.getTargets().getNumTargeted() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                sa.getTargets().add(choice);
            }
        } else {
            // if no targeting, it should always be ok
        }

        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        //TODO: add logic here
        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, List<Card> options, boolean isOptional) {
        // Select a card to attach to
        return ComputerUtilCard.getBestAI(options);
    }

}
