package forge.ai;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.ICardFace;
import forge.card.mana.ManaCost;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.card.CardState;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;

/**
 * Base class for API-specific AI logic
 * <p>
 * The three main methods are canPlayAI(), chkAIDrawback and doTriggerAINoCost.
 */
public abstract class SpellAbilityAi {

    public Predicate<Card> CREATURE_OR_TAP_ABILITY = c -> {
        if (c.isCreature()) {
            return true;
        }

        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility() && sa.getPayCosts().hasTapCost()) {
                return true;
            }
        }
        return false;
    };

    public final AiAbilityDecision canPlayWithSubs(final Player aiPlayer, final SpellAbility sa) {
        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        if (!decision.willingToPlay() && !"PlayForSub".equals(sa.getParam("AILogic"))) {
            return decision;
        }
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb == null) {
            return decision;
        }

        return chkDrawbackWithSubs(aiPlayer, subAb);
    }

    /**
     * Handles the AI decision to play a "main" SpellAbility
     */
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        // TODO this is redundant when reached from canPlayAndPayForFace
        if (sa.getRestrictions() != null && !sa.getRestrictions().canPlay(sa.getHostCard(), sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
        }

        return canPlayWithoutRestrict(ai, sa);
    }

    protected AiAbilityDecision canPlayWithoutRestrict(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();

        if (sa.hasParam("AILogic")) {
            final String logic = sa.getParam("AILogic");
            final boolean alwaysOnDiscard = "AlwaysOnDiscard".equals(logic) && ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN, ai)
                    && !ai.isUnlimitedHandSize() && ai.getCardsIn(ZoneType.Hand).size() > ai.getMaxHandSize();
            if (!checkAiLogic(ai, sa, logic)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            if (!alwaysOnDiscard && !checkPhaseRestrictions(ai, sa, ai.getGame().getPhaseHandler(), logic)) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingPhaseRestrictions);
            }
        } else if (!checkPhaseRestrictions(ai, sa, ai.getGame().getPhaseHandler())) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingPhaseRestrictions);
        } else if (ComputerUtil.preventRunAwayActivations(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.StopRunawayActivations);
        }

        AiAbilityDecision decision = checkApiLogic(ai, sa);
        if (!decision.willingToPlay()) {
            return decision;
        }

        // needs to be after API logic because needs to check possible X Cost
        final Cost cost = sa.getPayCosts();
        if (cost != null && !willPayCosts(ai, sa, cost, source)) {
            return new AiAbilityDecision(0, AiPlayDecision.CostNotAcceptable);
        }

        // for cards like Figure of Destiny
        // (it's unlikely many valid effect would work like this -
        // and while in theory AI could turn some conditions true in response that's far too advanced as default)
        if (!checkConditions(ai, sa)) {
            SpellAbility sub = sa.getSubAbility();
            if (sub == null || !checkConditions(ai, sub)) {
                return new AiAbilityDecision(0, AiPlayDecision.NeedsToPlayCriteriaNotMet);
            }
        }
        return decision;
    }

    protected boolean checkConditions(final Player ai, final SpellAbility sa) {
        // copy it to disable some checks that the AI need to check extra
        SpellAbilityCondition con = (SpellAbilityCondition) sa.getConditions().copy();

        // if manaspent, check if AI can pay the colored mana as cost
        if (!con.getManaSpent().isEmpty()) {
            // need to use ManaCostBeingPaid check, can't use Cost#canPay
            ManaCostBeingPaid paid = new ManaCostBeingPaid(new ManaCost(con.getManaSpent()));
            if (ComputerUtilMana.canPayManaCost(paid, sa, ai, sa.isTrigger())) {
                con.setManaSpent("");
            }
        }

        return con.areMet(sa);
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        return true;
    }

    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph,
            final String logic) {
         if (logic.equals("AtOppEOT")) {
            return ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN);
         }
        return checkPhaseRestrictions(ai, sa, ph);
    }

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("Never".equals(aiLogic)) {
            return false;
        }
        if (!"Once".equals(aiLogic)) {
            return !sa.getHostCard().getAbilityActivatedThisTurn().getActivators(sa).contains(ai);
        }
        return true;
    }

    /**
     * The rest of the logic not covered by the canPlayAI template is defined here
     */
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa.getActivationsThisTurn() == 0 || MyRandom.getRandom().nextFloat() < .8f) {
            // 80% chance to play the ability
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    public final boolean doTrigger(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        // this evaluation order is currently intentional as it does more stuff that helps avoiding some crashes
        if (!ComputerUtilCost.canPayCost(sa, aiPlayer, true) && !mandatory) {
            return false;
        }

        // a mandatory SpellAbility with targeting but without candidates,
        // does not need to go any deeper
        if (sa.usesTargeting() && mandatory && sa.getTargetRestrictions().getNumCandidates(sa, true) == 0) {
            return sa.isTargetNumberValid();
        }

        return doTriggerNoCostWithSubs(aiPlayer, sa, mandatory).willingToPlay();
    }

    public final AiAbilityDecision doTriggerNoCostWithSubs(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        AiAbilityDecision decision = doTriggerNoCost(aiPlayer, sa, mandatory);
        if (!decision.willingToPlay() && !"Always".equals(sa.getParam("AILogic"))) {
            return decision;
        }
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb == null) {
            if (decision.willingToPlay()) {
                return decision;
            }

            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        decision = chkDrawbackWithSubs(aiPlayer, subAb);
        if (decision.willingToPlay()) {
            return decision;
        }

        if (mandatory) {
            return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
     }

    /**
     * Handles the AI decision to play a triggered SpellAbility
     */
    protected AiAbilityDecision doTriggerNoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        AiAbilityDecision decision = canPlayWithoutRestrict(aiPlayer, sa);
        if (decision.willingToPlay() && (!mandatory || sa.isTargetNumberValid())) {
            // This is a weird check. Why do we care if its not mandatory if we WANT to do it?
            return decision;
        }

        // not mandatory, short way out
        if (!mandatory) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // invalid target might prevent it
        if (sa.usesTargeting()) {
            // make list of players it does try to target
            List<Player> players = Lists.newArrayList();
            players.addAll(aiPlayer.getOpponents());
            players.addAll(aiPlayer.getAllies());
            players.add(aiPlayer);

            // try to target opponent, then ally, then itself
            for (final Player p : players) {
                if (sa.canTarget(p)) {
                    sa.resetTargets();
                    sa.getTargets().add(p);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param aiPlayer
     * @param ab
     * @return
     */
    public AiAbilityDecision chkDrawbackWithSubs(Player aiPlayer, AbilitySub ab) {
        final AbilitySub subAb = ab.getSubAbility();
        AiAbilityDecision decision = SpellApiToAi.Converter.get(ab).chkDrawback(aiPlayer, ab);
        if (!decision.willingToPlay()) {
            return decision;
        }

        if (subAb == null) {
            return decision;
        }

        return chkDrawbackWithSubs(aiPlayer, subAb);
    }

    /**
     * Handles the AI decision to play a sub-SpellAbility
     */
    public AiAbilityDecision chkDrawback(final Player aiPlayer, final SpellAbility sa) {
        // sub-SpellAbility might use targets too
        if (sa.usesTargeting()) {
            // no Candidates, no adding to Stack
            if (!sa.getTargetRestrictions().hasCandidates(sa)) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
            // but if it does, it should override this function
            System.err.println("Warning: default (ie. inherited from base class) implementation of chkAIDrawback is used by " + sa.getHostCard().getName() + " for " + this.getClass().getName() + ". Consider declaring an overloaded method");
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of confirmAction is used by " + sa.getHostCard().getName() + " for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, Collection<T> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        boolean hasPlayer = false;
        boolean hasCard = false;
        boolean hasAttackableCard = false;

        for (T ent : options) {
            if (ent instanceof Player) {
                hasPlayer = true;
            } else if (ent instanceof Card card) {
                hasCard = true;
                if (card.isPlaneswalker() || card.isBattle()) {
                    hasAttackableCard = true;
                }
            }
        }

        if (hasPlayer && hasAttackableCard) {
            return (T) chooseSingleAttackableEntity(ai, sa, (Collection<GameEntity>) options, params);
        } else if (hasCard) {
            return (T) chooseSingleCard(ai, sa, (Collection<Card>) options, isOptional, targetedPlayer, params);
        } else if (hasPlayer) {
            return (T) chooseSinglePlayer(ai, sa, (Collection<Player>) options, params);
        }

        return null;
    }

    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells, Map<String, Object> params) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSingleSpellAbility is used by " + sa.getHostCard().getName() + " for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return spells.get(0);
    }

    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSingleCard is used by " + sa.getHostCard().getName() + " for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return Iterables.getFirst(options, null);
    }

    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSinglePlayer is used by " + sa.getHostCard().getName() + " for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return Iterables.getFirst(options, null);
    }

    protected GameEntity chooseSingleAttackableEntity(Player ai, SpellAbility sa, Iterable<GameEntity> options, Map<String, Object> params) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSinglePlayerOrPlaneswalker is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return Iterables.getFirst(options, null);
    }

    public String chooseCardName(Player ai, SpellAbility sa, List<ICardFace> faces) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseCardName is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");

        final ICardFace face = Iterables.getFirst(faces, null);
        return face == null ? "" : face.getName();
    }

    public ICardFace chooseCardFace(Player ai, SpellAbility sa, List<ICardFace> faces) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseCardFace is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");

        return Iterables.getFirst(faces, null);
    }

    public CardState chooseCardState(Player ai, SpellAbility sa, List<CardState> faces, Map<String, Object> params) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseCardState is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");

        return Iterables.getFirst(faces, null);
    }

    public int chooseNumber(Player player, SpellAbility sa, int min, int max, Map<String, Object> params) {
        return max;
    }

    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, Map<String, Object> params) {
        return Iterables.getFirst(options, null);
    }

    public boolean chooseBinary(BinaryChoiceType kindOfChoice, SpellAbility sa, Map<String, Object> params) {
        return MyRandom.getRandom().nextBoolean();
    }

    /**
     * Checks if the AI is willing to pay for additional costs
     * <p>
     * Evaluated costs are: life, discard, sacrifice and counter-removal
     */
    protected boolean willPayCosts(final Player payer, final SpellAbility sa, final Cost cost, final Card source) {
        if (!ComputerUtilCost.checkLifeCost(payer, cost, source, 4, sa)) {
            return false;
        }
        if (!ComputerUtilCost.checkDiscardCost(payer, cost, source, sa)) {
            return false;
        }
        if (!ComputerUtilCost.checkSacrificeCost(payer, cost, source, sa)) {
            return false;
        }
        if (!ComputerUtilCost.checkRemoveCounterCost(cost, source, sa)) {
            return false;
        }
        return true;
    }

    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card source = sa.getHostCard();
        final String aiLogic = sa.getParam("UnlessAI");
        boolean payNever = "Never".equals(aiLogic);
        boolean isMine = sa.getActivatingPlayer().equals(payer);

        if (payNever) { return false; }

        // AI will only pay when it's not already paid and only opponents abilities
        if (alreadyPaid || (payers.size() > 1 && isMine)) {
            return false;
        }

        return ComputerUtilCost.checkLifeCost(payer, cost, source, 4, sa)
                && ComputerUtilCost.checkDamageCost(payer, cost, source, 4, sa)
                && (isMine || ComputerUtilCost.checkSacrificeCost(payer, cost, source, sa))
                && (isMine || ComputerUtilCost.checkDiscardCost(payer, cost, source, sa));
    }

    public List<OptionalCostValue> chooseOptionalCosts(Player payer, SpellAbility chosen, List<OptionalCostValue> optionalCostValues) {
        List<OptionalCostValue> chosenOptCosts = Lists.newArrayList();
        Cost costSoFar = chosen.getPayCosts().copy();

        for (OptionalCostValue opt : optionalCostValues) {
            // Choose the optional cost if it can be paid (to be improved later, check for playability and other conditions perhaps)
            Cost fullCost = opt.getCost().copy().add(costSoFar);
            SpellAbility fullCostSa = chosen.copyWithDefinedCost(fullCost);

            if (opt.getType() == OptionalCost.Kicker1 || opt.getType() == OptionalCost.Kicker2) {
                SpellAbility kickedSaCopy = fullCostSa.copy();
                kickedSaCopy.addOptionalCost(opt.getType());
                Card copy = CardCopyService.getLKICopy(chosen.getHostCard());
                copy.setCastSA(kickedSaCopy);
                if (ComputerUtilCard.checkNeedsToPlayReqs(copy, kickedSaCopy) != AiPlayDecision.WillPlay) {
                    // don't choose kickers we don't want to play
                    continue;
                }
            }

            if (ComputerUtilCost.canPayCost(fullCostSa, payer, false)) {
                chosenOptCosts.add(opt);
                costSoFar.add(opt.getCost());
            }
        }

        return chosenOptCosts;
    }

    /**
     * <p>
     * isSorcerySpeed.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean isSorcerySpeed(SpellAbility sa, Player ai) {
        sa = sa.getRootAbility();
        if (sa.isLandAbility()) {
            return true;
        }
        if (sa.isSpell() || sa.isPwAbility()) {
            return !sa.withFlash(sa.getHostCard(), ai);
        }
        return sa.isActivatedAbility() && sa.getRestrictions().isSorcerySpeed();
    }

    /**
     * <p>
     * playReusable.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    protected static boolean playReusable(final Player ai, final SpellAbility sa) {
        PhaseHandler phase = ai.getGame().getPhaseHandler();

        // TODO probably also consider if winter orb or similar are out

        if (sa instanceof AbilitySub) {
            return true; // This is only true for Drawbacks and triggers
        }

        if (!sa.getPayCosts().isReusuableResource()) {
            return false;
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return true;
        }

        if (sa.isPwAbility() && phase.is(PhaseType.MAIN2)) {
            return true;
        }
        if (sa.isSpell() && !sa.isBuyback()) {
            return false;
        }

        return phase.is(PhaseType.END_OF_TURN) && phase.getNextTurn().equals(ai);
    }
}
