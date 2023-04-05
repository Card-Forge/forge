package forge.ai.ability;

import com.google.common.base.Predicates;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PhasesAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // This still needs to be fleshed out
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        List<Card> tgtCards;
        if (tgt == null) {
            tgtCards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            if (tgtCards.contains(source)) {
                // Protect it from something
                final boolean isThreatened = ComputerUtil.predictThreatenedObjects(aiPlayer, null, true).contains(source);
                return isThreatened;
            } else {
                // Card def = tgtCards.get(0);
                // Phase this out if it might attack me, or before it can be
                // declared as a blocker
            }

            return false;
        } else {
            if (!phasesPrefTargeting(tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt == null) {
            return mandatory;
        }

        if (phasesPrefTargeting(tgt, sa, mandatory)) {
            return true;
        } else if (mandatory) {
            // not enough preferred targets, but mandatory so keep going:
            return sa.isTargetNumberValid() || phasesUnpreferredTargeting(aiPlayer.getGame(), sa, mandatory);
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        boolean randomReturn = true;

        if (tgt == null) {

        } else {
            if (!phasesPrefTargeting(tgt, sa, false)) {
                return false;
            }
        }
        return randomReturn;
    }

    private boolean phasesPrefTargeting(final TargetRestrictions tgt, final SpellAbility sa,
            final boolean mandatory) {
        // Card source = sa.getHostCard();

        // List<Card> phaseList =
        // AllZoneUtil.getCardsIn(Zone.Battlefield).getTargetableCards(source)
        // .getValidCards(tgt.getValidTgts(), source.getController(), source);

        // List<Card> aiPhaseList =
        // phaseList.getController(AllZone.getComputerPlayer());

        // If Something in the Phase List might die from a bad combat, or a
        // spell on the stack save it

        // List<Card> humanPhaseList =
        // phaseList.getController(AllZone.getHumanPlayer());

        // If something in the Human List is causing issues, phase it out

        return false;
    }

    private boolean phasesUnpreferredTargeting(final Game game, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();

        CardCollectionView list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

        // in general, if it's our own creature, choose the weakest one, if it's the opponent's creature,
        // choose the strongest one
        if (!list.isEmpty()) {
            CardCollectionView oppList = CardLists.filter(list, Predicates.not(CardPredicates.isController(source.getController())));
            sa.resetTargets();
            sa.getTargets().add(!oppList.isEmpty() ? ComputerUtilCard.getBestAI(oppList) : ComputerUtilCard.getWorstAI(list));
            return true;
        }

        return false;
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, Collection<T> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // TODO: improve the selection logic, e.g. for cards like Change of Plans. Currently will
        //  confirm everything unless AILogic is "DontPhaseOut", in which case it'll confirm nothing.
        if ("DontPhaseOut".equals(sa.getParam("AILogic"))) {
            return null;
        }

        return super.chooseSingleEntity(ai, sa, options, isOptional, targetedPlayer, params);
    }
}
