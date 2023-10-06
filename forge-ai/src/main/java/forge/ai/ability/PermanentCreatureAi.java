package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentCreatureAi extends PermanentAi {

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        final Game game = ai.getGame();

        if ("Never".equals(aiLogic)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card card = sa.getHostCard();
        final Game game = ai.getGame();

        // FRF Dash Keyword
        if (sa.isDash()) {
            //only checks that the dashed creature will attack
            if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                if (game.getReplacementHandler().wouldPhaseBeSkipped(ai, "BeginCombat"))
                    return false;
                if (ComputerUtilCost.canPayCost(sa.getHostCard().getSpellPermanent(), ai, false)) {
                    //do not dash if creature can be played normally
                    return false;
                }
                Card dashed = CardUtil.getLKICopy(sa.getHostCard());
                dashed.setSickness(false);
                return ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, dashed);
            } else {
                return false;
            }
        }

        // Blitz Keyword: avoid casting in Main2
        if (sa.isBlitz() && ph.getPhase().isAfter(PhaseType.MAIN1)) {
            return false;
        }

        // Prevent the computer from summoning Ball Lightning type creatures
        // after attacking
        if (card.hasSVar("EndOfTurnLeavePlay")
                && (!ph.isPlayerTurn(ai) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                || game.getReplacementHandler().wouldPhaseBeSkipped(ai, "BeginCombat"))) {
            // AiPlayDecision.AnotherTime
            return false;
        }

        // Flash logic
        boolean advancedFlash = false;
        if (ai.getController().isAI()) {
            advancedFlash = ((PlayerControllerAi)ai.getController()).getAi().getBooleanProperty(AiProps.FLASH_ENABLE_ADVANCED_LOGIC);
        }
        if (card.hasKeyword(Keyword.FLASH) || (!ai.canCastSorcery() && sa.canCastTiming(ai))) {
            if (advancedFlash) {
                return doAdvancedFlashLogic(card, ai, sa);
            } else {
                // save cards with flash for surprise blocking
                if ((ai.isUnlimitedHandSize() || ai.getCardsIn(ZoneType.Hand).size() <= ai.getMaxHandSize()
                        || ph.getPhase().isBefore(PhaseType.END_OF_TURN))
                        && ai.getManaPool().totalMana() <= 0
                        && (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS))
                        && (!card.hasETBTrigger(true) && !card.hasSVar("AmbushAI"))
                        && game.getStack().isEmpty()
                        && !ComputerUtil.castPermanentInMain1(ai, sa)) {
                    // AiPlayDecision.AnotherTime;
                    return false;
                }
            }
        }

        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    private boolean doAdvancedFlashLogic(Card card, final Player ai, SpellAbility sa) {
        Game game = ai.getGame();
        PhaseHandler ph = game.getPhaseHandler();
        Combat combat = game.getCombat();
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();

        boolean isOppTurn = ph.getPlayerTurn().isOpponentOf(ai);
        boolean isOwnEOT = ph.is(PhaseType.END_OF_TURN, ai);
        boolean isEOTBeforeMyTurn = ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
        boolean isMyDeclareBlockers = ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS, ai) && ph.inCombat();
        boolean isOppDeclareAttackers = ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) && isOppTurn && ph.inCombat();
        boolean isMyMain1OrLater = ph.is(PhaseType.MAIN1, ai) || (ph.getPhase().isAfter(PhaseType.MAIN1) && ph.getPlayerTurn().equals(ai));
        boolean canRespondToStack = false;
        if (!game.getStack().isEmpty()) {
            SpellAbility peekSa = game.getStack().peekAbility();
            Player activator = peekSa.getActivatingPlayer();
            if (activator != null && activator.isOpponentOf(ai) && peekSa.getApi() != ApiType.DestroyAll
                    && peekSa.getApi() != ApiType.DamageAll) {
                canRespondToStack = true;
            }
        }

        boolean hasETBTrigger = card.hasETBTrigger(true);
        boolean hasAmbushAI = card.hasSVar("AmbushAI");
        boolean defOnlyAmbushAI = hasAmbushAI && "BlockOnly".equals(card.getSVar("AmbushAI"));
        boolean loseFloatMana = ai.getManaPool().totalMana() > 0 && !ManaEffectAi.canRampPool(ai, card);
        boolean willDiscardNow = isOwnEOT && !ai.isUnlimitedHandSize() && ai.getCardsIn(ZoneType.Hand).size() > ai.getMaxHandSize();
        boolean willDieNow = combat != null && ComputerUtilCombat.lifeInSeriousDanger(ai, combat);
        boolean wantToCastInMain1 = ph.is(PhaseType.MAIN1, ai) && ComputerUtil.castPermanentInMain1(ai, sa);
        boolean isCommander = card.isCommander();

        // figure out if the card might be a valuable blocker
        boolean valuableBlocker = false;
        if (combat != null && combat.getDefendingPlayers().contains(ai)) {
            // Currently we use a rather simplistic assumption that if we're behind on creature count on board,
            // a flashed in creature might prove to be good as an additional defender
            int numUntappedPotentialBlockers = CardLists.filter(ai.getCreaturesInPlay(), new Predicate<Card>() {
                @Override
                public boolean apply(final Card card) {
                    return card.isUntapped() && !ComputerUtilCard.isUselessCreature(ai, card);
                }
            }).size();

            if (combat.getAttackersOf(ai).size() > numUntappedPotentialBlockers) {
                valuableBlocker = true;
            }
        }

        int chanceToObeyAmbushAI = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_OBEY_AMBUSHAI);
        int chanceToAddBlocker = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_CAST_AS_VALUABLE_BLOCKER);
        int chanceToCastForETB = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_CAST_DUE_TO_ETB_EFFECTS);
        int chanceToRespondToStack = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_RESPOND_TO_STACK_WITH_ETB);
        int chanceToProcETBBeforeMain1 = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_CAST_FOR_ETB_BEFORE_MAIN1);
        boolean canCastAtOppTurn = true;
        for (Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            for (StaticAbility s : c.getStaticAbilities()) {
                if ("CantBeCast".equals(s.getParam("Mode")) && "True".equals(s.getParam("NonCasterTurn"))) {
                    canCastAtOppTurn = false;
                }
            }
        }

        if (loseFloatMana || willDiscardNow || willDieNow) {
            // Will lose mana in pool or about to discard a card in cleanup or about to die in combat, so use this opportunity
            return true;
        } else if (isCommander && isMyMain1OrLater) {
            // Don't hold out specifically if this card is a commander, since otherwise it leads to stupid AI choices
            return true;
        } else if (wantToCastInMain1) {
            // Would rather cast it in Main 1 or as soon as possible anyway, so go for it
            return isMyMain1OrLater;
        } else if (hasAmbushAI && MyRandom.percentTrue(chanceToObeyAmbushAI)) {
            // Is an ambusher, so try to hold for declare blockers in combat where the AI defends, if possible
            return defOnlyAmbushAI && canCastAtOppTurn ? isOppDeclareAttackers : (isOppDeclareAttackers || isMyDeclareBlockers);
        } else if (valuableBlocker && isOppDeclareAttackers && MyRandom.percentTrue(chanceToAddBlocker)) {
            // Might serve as a valuable blocker in a combat where we are behind on untapped blockers
            return true;
        } else if (hasETBTrigger && MyRandom.percentTrue(chanceToCastForETB)) {
            // Instant speed is good when a card has an ETB trigger, but prolly don't cast in own turn before Main 1 not
            // to mana lock the AI or lose the chance to consider other options. Try to utilize it as a response to stack
            // if possible.
            return isMyMain1OrLater || isOppTurn || MyRandom.percentTrue(chanceToProcETBBeforeMain1);
        } else if (hasETBTrigger && canRespondToStack && MyRandom.percentTrue(chanceToRespondToStack)) {
            // Try to do something meaningful in response to an opposing effect on stack. Note that this is currently
            // too random to likely be meaningful, serious improvement might be needed.
            return canCastAtOppTurn || ph.getPlayerTurn().equals(ai);
        } else {
            // Doesn't have a ETB trigger and doesn't seem to be good as an ambusher, try to surprise the opp before my turn
            // TODO: maybe implement a way to reserve mana for this
            return canCastAtOppTurn ? isEOTBeforeMyTurn : isOwnEOT;
        }
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        if (!super.checkApiLogic(ai, sa)) {
            return false;
        }

        final Card card = sa.getHostCard();
        final ManaCost mana = card.getManaCost();
        final Game game = ai.getGame();

        /*
         * Checks if the creature will have non-positive toughness after
         * applying static effects. Exceptions: 1. has "etbCounter" keyword (eg.
         * Endless One) 2. paid non-zero for X cost 3. has ETB trigger 4. has
         * ETB replacement 5. has NoZeroToughnessAI svar (eg. Veteran Warleader)
         * 
         * 1. and 2. should probably be merged and applied on the card after
         * checking for effects like Doubling Season for getNetToughness to see
         * the true value. 3. currently allows the AI to suicide creatures as
         * long as it has an ETB. Maybe it should check if said ETB is actually
         * worth it. Not sure what 4. is for. 5. needs to be updated to ensure
         * that the net toughness is still positive after static effects.
         */
        // AiPlayDecision.WouldBecomeZeroToughnessCreature
        if (card.hasStartOfKeyword("etbCounter") || mana.countX() != 0
                || card.hasETBTrigger(false) || card.hasETBReplacement() || card.hasSVar("NoZeroToughnessAI")) {
                return true;
        }

        final Card copy = CardUtil.getLKICopy(card);
        ComputerUtilCard.applyStaticContPT(game, copy, null);
        if (copy.getNetToughness() > 0) {
            return true;
        }

        return false;
    }

}
