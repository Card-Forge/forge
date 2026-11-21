package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;

public class CloneAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        boolean useAbility = true;

        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"

        PhaseHandler phase = game.getPhaseHandler();

        if (sa.usesTargeting()) {
            sa.resetTargets();
            useAbility &= cloneTgtAI(sa, false);
        } else {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= !c.isCreature() && !c.isTapped() && !(c.getTurnInZone() == phase.getTurn());

                // for creatures that could be improved (like Figure of Destiny)
                if (c.isCreature() && (!sa.hasParam("Duration") || (!c.isTapped() && !c.isSick()))) {
                    int power = -5;
                    if (sa.hasParam("Power")) {
                        power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
                    }
                    int toughness = -5;
                    if (sa.hasParam("Toughness")) {
                        toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
                    }
                    if ((power + toughness) > (c.getCurrentPower() + c.getCurrentToughness())) {
                        bFlag = true;
                    }
                }
            }

            if (!bFlag) { // All of the defined stuff is cloned, not very useful
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            }
        }

        return useAbility ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance = true;

        if (sa.usesTargeting()) {
            chance = cloneTgtAI(sa, false);
        }

        return chance ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        Card host = sa.getHostCard();
        boolean chance = true;

        if (sa.usesTargeting()) {
            chance = cloneTgtAI(sa, mandatory);
        } else {
            if (sa.isReplacementAbility() && host.isCloned()) {
                // prevent StackOverflow from infinite loop copying another ETB RE
                return new AiAbilityDecision(0, AiPlayDecision.StopRunawayActivations);
            }
            if (sa.hasParam("Choices")) {
                // the cards to copy may be somewhere else than the battlefield (Superior Spider-Man: a graveyard)
                final ZoneType choiceZone = sa.hasParam("ChoiceZone")
                        ? ZoneType.smartValueOf(sa.getParam("ChoiceZone")) : ZoneType.Battlefield;
                CardCollectionView choices = CardLists.getValidCards(host.getGame().getCardsIn(choiceZone),
                        sa.getParam("Choices"), host.getController(), host, sa);

                chance = !choices.isEmpty();
                if (chance && copyIsNoCreature(sa) && !mandatory) {
                    boolean worth = false;
                    for (final Card c : choices) {
                        if (!ownLegend(aiPlayer, c) && nonCreatureCopyValue(aiPlayer, c) > 0) {
                            worth = true;
                            break;
                        }
                    }
                    chance = worth; // a copy that is no creature of a vanilla one is the rock that it already is
                }
            }
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something

        if (mandatory || chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /**
     * <p>
     * cloneTgtAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean cloneTgtAI(final SpellAbility sa, boolean mandatory) {
        CardCollection targets = CardUtil.getValidCardsToTarget(sa);
        if (targets.isEmpty()) {
            return false;
        }

        if (mandatory || "CloneBestCreature".equals(sa.getParam("AILogic"))) {
            CardCollection viable = ComputerUtilCard.filterOutFatalCopies(targets, sa.getActivatingPlayer(), false);
            if (!viable.isEmpty()) {
                targets = viable;
            } else if (!mandatory) {
                return false;
            }
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(targets));
            return true;
        }

        // Default:
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things that clone a target. Those can just use
        // AI:RemoveDeck:All until this can do a reasonably good job of picking
        // a good target
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if (sa.hasParam("AILogic") && (!sa.usesTargeting() || sa.isTargetNumberValid())) {
            // Had a special logic for it and managed to target, so confirm if viable
            if ("CloneBestCreature".equals(sa.getParam("AILogic"))) {
                return ComputerUtilCard.evaluateCreature(sa.getTargetCard()) > ComputerUtilCard.evaluateCreature(sa.getHostCard());
            } else if ("IfDefinedCreatureIsBetter".equals(sa.getParam("AILogic"))) {
                List<Card> defined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
                Card bestDefined = ComputerUtilCard.getBestCreatureAI(defined);
                return ComputerUtilCard.evaluateCreature(bestDefined) > ComputerUtilCard.evaluateCreature(sa.getHostCard());
            }
        }

        // Currently doesn't confirm anything that's not defined by AI logic
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#chooseSingleCard(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, java.lang.Iterable, boolean,
     * forge.game.player.Player)
     */
    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        final Card host = sa.getHostCard();
        final String name = host.getName();
        final Player ctrl = host.getController();

        final Card cloneTarget = getCloneTarget(sa);
        final boolean isOpp = cloneTarget.getController().isOpponentOf(sa.getActivatingPlayer());

        final boolean isVesuva = "Vesuva".equals(name) || "Sculpting Steel".equals(name);
        final boolean canCloneLegendary = "True".equalsIgnoreCase(sa.getParam("NonLegendary"));

        String filter = !isVesuva ? "Permanent.YouDontCtrl,Permanent.nonLegendary"
                : "Permanent.YouDontCtrl+!named" + name + ",Permanent.nonLegendary+!named" + name;

        // TODO: rewrite this block so that this is done somehow more elegantly
        if (canCloneLegendary) {
            filter = filter.replace(".nonLegendary+", ".").replace(".nonLegendary", "");
        }

        CardCollection newOptions = CardLists.getValidCards(options, filter, ctrl, host, sa);
        if (!newOptions.isEmpty()) {
            options = newOptions;
        }

        if (sa.hasParam("AiChoiceLogic")) {
            final String logic = sa.getParam("AiChoiceLogic");
            if ("BestOppCtrl".equals(logic)) {
                options = CardLists.filterControlledBy(options, ctrl.getOpponents());
            }
        }

        // prevent loop of choosing copy of same card
        if (isVesuva) {
            options = CardLists.filter(options, CardPredicates.sharesNameWith(host).negate());
        }

        if (copyIsNoCreature(sa)) {
            Card best = null;
            int bestValue = 0;
            for (final Card c : options) {
                if (ownLegend(ai, c)) {
                    continue;
                }
                final int value = nonCreatureCopyValue(ai, c);
                if (value > bestValue) {
                    bestValue = value;
                    best = c;
                }
            }
            if (best != null) {
                return best;
            }
            // nothing on the table is worth the copy: the vanilla ones are only a name, take the least harmful (not a legend of its own)
        }

        Card choice = isOpp ? ComputerUtilCard.getWorstAI(options) : ComputerUtilCard.getBestAI(options);

        // Superior Spider-Man: copy Doomsday Excruciator only when emptying the libraries wins (see ComboPlan.cloneChoice)
        if (!isOpp && ai.getController() instanceof PlayerControllerAi) {
            final Card steered = ((PlayerControllerAi) ai.getController()).getAi().deckPlan(ai).combos
                    .cloneChoice(ai, options);
            if (steered != null) {
                choice = steered;
            }
        }

        return choice;
    }

    /**
     * A copy that is not a creature (Machine God's Effigy: "except it's an artifact", "it's not a creature") has no body: the
     * 8/8 that makes the original the best creature on the table is a vanilla mana rock as a copy. Only what a permanent
     * does without fighting is worth copying: the enters-the-battlefield trigger (it fires for the copy), the abilities
     * that are not about combat, and the static ones that work on the rest of the table. 0 for the vanilla.
     */
    public static int nonCreatureCopyValue(final Player ai, final Card c) {
        int value = 0;
        for (final Trigger t : c.getTriggers()) {
            final TriggerType mode = t.getMode();
            if (mode == TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))
                    && t.getParamOrDefault("ValidCard", "").startsWith("Card.Self")) {
                value += 60;
            } else if (mode == TriggerType.Phase || mode == TriggerType.Drawn || mode == TriggerType.SpellCast
                    || mode == TriggerType.ChangesZone || mode == TriggerType.LifeGained) {
                value += 25;
            }
        }
        for (final StaticAbility st : c.getStaticAbilities()) {
            if (st.checkMode(StaticAbilityMode.Continuous) && !"Self".equals(st.getParam("Affected"))
                    && !"Card.Self".equals(st.getParam("Affected"))) {
                value += 40;
            } else if (!st.checkMode(StaticAbilityMode.Continuous)) {
                value += 20;
            }
        }
        for (final SpellAbility a : c.getSpellAbilities()) {
            if (a.isAbility() && !a.isManaAbility()) {
                value += 35;
            }
        }
        return value;
    }

    /** The copy is not a creature, so its value is what it does and not what it weighs. */
    private static boolean copyIsNoCreature(final SpellAbility sa) {
        return sa.hasParam("RemoveCardTypes") && "True".equalsIgnoreCase(sa.getParam("RemoveCardTypes"));
    }

    /**
     * The legend rule: the copy of a legendary permanent is legendary, and the AI keeps only one of two with the same name. That
     * is the AI's own legend, and also the opponent's one when the AI has a permanent of that name already.
     */
    private static boolean ownLegend(final Player ai, final Card c) {
        if (!c.getType().isLegendary()) {
            return false;
        }
        for (final Card mine : ai.getCardsIn(ZoneType.Battlefield)) {
            if (mine.getName().equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    protected Card getCloneTarget(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        Card tgtCard = host;
        if (sa.hasParam("CloneTarget")) {
            final List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
            if (!cloneTargets.isEmpty()) {
                tgtCard = cloneTargets.get(0);
            }
        } else if (sa.hasParam("Choices") && sa.usesTargeting()) {
            tgtCard = sa.getTargetCard();
        }

        return tgtCard;
    }

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player, forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler)
     */
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // don't use instant speed clone abilities outside computers
        // Combat_Begin step
        if (!ph.is(PhaseType.COMBAT_BEGIN)
                && ph.isPlayerTurn(ai) && !isSorcerySpeed(sa, ai)
                && !sa.hasParam("ActivationPhases") && sa.hasParam("Duration")) {
            return false;
        }

        // don't use instant speed clone abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if (!ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) || ph.isPlayerTurn(ai) || ph.getCombat().getAttackers().isEmpty()) {
            return false;
        }

        // don't activate during main2 unless this effect is permanent
        return !ph.is(PhaseType.MAIN2) || !sa.hasParam("Duration");
    }
}
