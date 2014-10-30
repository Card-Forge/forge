package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.List;
import java.util.Random;

public class TapAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(final Player ai, SpellAbility sa) {
        // If tapping all creatures do it either during declare attackers of AIs
        // turn
        // or during upkeep/begin combat?

        final Card source = sa.getHostCard();
        final Player opp = ai.getOpponent();
        final Game game = ai.getGame();

        if (game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_BEGIN)) {
            return false;
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        CardCollectionView validTappables = game.getCardsIn(ZoneType.Battlefield);

        if (sa.usesTargeting()) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            validTappables = opp.getCardsIn(ZoneType.Battlefield);
        }

        validTappables = CardLists.getValidCards(validTappables, valid, source.getController(), source);
        validTappables = CardLists.filter(validTappables, Presets.UNTAPPED);

        final Random r = MyRandom.getRandom();
        if (r.nextFloat() > Math.pow(.6667, sa.getActivationsThisTurn())) {
            return false;
        }

        if (validTappables.isEmpty()) {
            return false;
        }

        final List<Card> human = CardLists.filter(validTappables, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getController().equals(opp);
            }
        });
        final List<Card> compy = CardLists.filter(validTappables, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getController().equals(ai);
            }
        });
        if (human.size() <= compy.size()) {
            return false;
        }
        // in AI's turn, check if there are possible attackers, before tapping blockers
        if (game.getPhaseHandler().isPlayerTurn(ai) && !SpellAbilityAi.isSorcerySpeed(sa)) {
            validTappables = ai.getCardsIn(ZoneType.Battlefield);
            final boolean any = Iterables.any(validTappables, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CombatUtil.canAttack(c) && ComputerUtilCombat.canAttackNextTurn(c);
                }
            });
            if(!any) {
                return false;
            }
        }
        return true;
    }

    private CardCollectionView getTapAllTargets(final String valid, final Card source) {
        final Game game = source.getGame();
        CardCollectionView tmpList = game.getCardsIn(ZoneType.Battlefield);
        tmpList = CardLists.getValidCards(tmpList, valid, source.getController(), source);
        tmpList = CardLists.filter(tmpList, Presets.UNTAPPED);
        return tmpList;
    }

    @Override
    protected boolean doTriggerAINoCost(final Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        CardCollectionView validTappables = getTapAllTargets(valid, source);

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null) {
            sa.resetTargets();
            sa.getTargets().add(ai.getOpponent());
            validTappables = ai.getOpponent().getCardsIn(ZoneType.Battlefield);
        }

        if (mandatory) {
            return true;
        }

        final Random r = MyRandom.getRandom();
        boolean rr = false;
        if (r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn())) {
            rr = true;
        }

        if (validTappables.size() > 0) {
            final int human = Iterables.size(Iterables.filter(validTappables, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getController().isOpponentOf(ai);
                }
            }));
            final int compy = Iterables.size(Iterables.filter(validTappables, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.getController().isOpponentOf(ai);
                }
            }));
            if (human > compy) {
                return rr;
            }
        }
        return false;
    }
}
