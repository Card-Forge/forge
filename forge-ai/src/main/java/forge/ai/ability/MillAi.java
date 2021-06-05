package forge.ai.ability;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class MillAi extends SpellAbilityAi {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        PhaseHandler ph = ai.getGame().getPhaseHandler();

        if (aiLogic.equals("Main1")) {
            return !ph.getPhase().isBefore(PhaseType.MAIN2) || sa.hasParam("ActivationPhases")
                    || ComputerUtil.castSpellInMain1(ai, sa);
        } else if (aiLogic.equals("EndOfOppTurn")) {
            return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
        } else if (aiLogic.equals("LilianaMill")) {
            // Only mill if a "Raise Dead" target is available, in case of control decks with few creatures
            return CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES).size() >= 1;
        } else if (aiLogic.equals("Rebirth")) {
            return ai.getLife() <= 8;
        }
        return true;
    }
    
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        if ("ExileAndPlayUntilEOT".equals(sa.getParam("AILogic"))) {
            return ph.is(PhaseType.MAIN1) && ph.isPlayerTurn(ai); // try to maximize the chance of being able to play the card this turn
        } else if ("ExileAndPlayOrDealDamage".equals(sa.getParam("AILogic"))) {
            return (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)) && ph.isPlayerTurn(ai); // Chandra, Torch of Defiance and similar
        }
        if (!sa.hasParam("Planeswalker")) { // Planeswalker abilities are only activated at sorcery speed
            if ("You".equals(sa.getParam("Defined")) && !(!SpellAbilityAi.isSorcerySpeed(sa) && ph.is(PhaseType.END_OF_TURN)
                    && ph.getNextTurn().equals(ai))) {
                return false; // only self-mill at opponent EOT
            }
        }
        if (sa.getHostCard().isCreature() && sa.getPayCosts().hasTapCost()) {
            // creatures with a tap cost to mill (e.g. Doorkeeper) should be activated at the opponent's end step
            // because they are also potentially useful for combat
            return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
        }
        return true;
    }
    
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        /*
         * TODO:
         * - logic in targetAI looks dodgy
         * - decide whether to self-mill (eg. delirium, dredge, bad top card)
         * - interrupt opponent's top card (eg. Brainstorm, good top card)
         * - check for Laboratory Maniac effect (needs to check for actual
         * effect due to possibility of "lose abilities" effect)
         */
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;   // prevents mill 0 infinite loop?
        }
        
        if (("You".equals(sa.getParam("Defined")) || "Player".equals(sa.getParam("Defined")))
                && ai.getCardsIn(ZoneType.Library).size() < 10) {
            return false;   // prevent self and each player mill when library is small
        }
        
        if (sa.getTargetRestrictions() != null && !targetAI(ai, sa, false)) {
            return false;
        }

        if ((sa.getParam("NumCards").equals("X") || sa.getParam("NumCards").equals("Z"))
                && sa.getSVar("X").startsWith("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard = getNumToDiscard(ai, sa);
            sa.setXManaCostPaid(cardsToDiscard);
            return cardsToDiscard > 0;
        }
        return true;
    }

    private boolean targetAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        if (tgt != null) {
            sa.resetTargets();
            final Map<Player, Integer> list = Maps.newHashMap();
            for (final Player o : ai.getOpponents()) {
                if (!sa.canTarget(o)) {
                    continue;
                }

                // need to set as target for some calcuate
                sa.getTargets().add(o);
                final int numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
                sa.getTargets().remove(o);

                // if it would mill none, try other one
                if (numCards <= 0) {
                    if ((sa.getParam("NumCards").equals("X") || sa.getParam("NumCards").equals("Z")))
                    {
                        if (source.getSVar("X").startsWith("Count$xPaid")) {
                            // Spell is PayX based
                        } else if (source.getSVar("X").startsWith("Remembered$ChromaSource")) {
                            // Cards like Sanity Grinding
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }

                final CardCollectionView pLibrary = o.getCardsIn(ZoneType.Library);
                if (pLibrary.isEmpty()) {
                    continue;
                }

                // if that player can be miled, select this one.
                if (numCards >= pLibrary.size()) {
                    sa.getTargets().add(o);
                    return true;
                }

                list.put(o, numCards);
            }

            // can't target opponent?
            if (list.isEmpty()) {
                if (mandatory && sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                    return true;
                }
                // TODO Obscure case when you know what your top card is so you might?
                // want to mill yourself here
                return false;
            }

            // select Player which would cause the most damage
            // JAVA 1.8 use Map.Entry.comparingByValue()
            Map.Entry<Player, Integer> max = Collections.max(list.entrySet(), new Comparator<Map.Entry<Player,Integer>>(){
                @Override
                public int compare(Map.Entry<Player, Integer> o1, Map.Entry<Player, Integer> o2) {
                    return o1.getValue() - o2.getValue();
                }
            });

            sa.getTargets().add(max.getKey());
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return targetAI(aiPlayer, sa, true);
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (!targetAI(aiPlayer, sa, mandatory)) {
            return false;
        }

        if (sa.getParam("NumCards").equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            sa.setXManaCostPaid(getNumToDiscard(aiPlayer, sa));
        }

        return true;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        if ("TimmerianFiends".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.TimmerianFiends.consider(player, sa);
        }

        return true;
    }

    /*
     * return num of cards to discard
     */
    private int getNumToDiscard(final Player ai, final SpellAbility sa) {
        // need list of affected players
        List<Player> list = Lists.newArrayList();
        if (sa.usesTargeting()) {
            list.addAll(Lists.newArrayList(sa.getTargets().getTargetPlayers()));
        } else {
            list.addAll(AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa));
        }

        // get targeted or defined Player with largest library 
        final Player m = Collections.max(list, PlayerPredicates.compareByZoneSize(ZoneType.Library));

        int cardsToDiscard =  m.getCardsIn(ZoneType.Library).size();

        // if ai is in affected list too, try to not mill himself
        if (list.contains(ai)) {
            cardsToDiscard = Math.min(ai.getCardsIn(ZoneType.Library).size() - 5, cardsToDiscard);
        }

        return Math.min(ComputerUtilCost.getMaxXValue(sa, ai), cardsToDiscard);
    }
}
