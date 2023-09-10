package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostTap;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;

public class UntapAi extends SpellAbilityAi {
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        final Card source = sa.getHostCard();
        if ("EOT".equals(aiLogic) && (source.getGame().getPhaseHandler().getNextTurn() != ai
                || !source.getGame().getPhaseHandler().getPhase().equals(PhaseType.END_OF_TURN))) {
            return false;
        } else if ("PoolExtraMana".equals(aiLogic)) {
            return doPoolExtraManaLogic(ai, sa);
        } else if ("PreventCombatDamage".equals(aiLogic)) {
            return doPreventCombatDamageLogic(ai, sa);
            // In the future if you want to give Pseudo vigilance to a creature you attacked with
            // activate during your own during the end of combat step
        }

        return !("Never".equals(aiLogic));
    }

    @Override
    protected boolean willPayCosts(final Player ai, final SpellAbility sa, final Cost cost, final Card source) {
        if (!ComputerUtilCost.checkAddM1M1CounterCost(cost, source)) {
            return false;
        }

        return ComputerUtilCost.checkDiscardCost(ai, cost, source, sa);
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (!sa.usesTargeting()) {
            final List<Card> pDefined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            return pDefined.isEmpty() || (pDefined.get(0).isTapped() && pDefined.get(0).getController() == ai);
        } else {
            // If we already selected a target just use that

            return untapPrefTargeting(ai, sa, false);
        }
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (!sa.usesTargeting()) {
            if (mandatory) {
                return true;
            } else if ("Never".equals(sa.getParam("AILogic"))) {
                return false;
            }

            final List<Card> pDefined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            return pDefined.isEmpty() || (pDefined.get(0).isTapped() && pDefined.get(0).getController() == ai);
        } else {
            if (untapPrefTargeting(ai, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return untapUnpreferredTargeting(sa, mandatory);
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        boolean randomReturn = true;

        if (!sa.usesTargeting()) {
            // who cares if its already untapped, it's only a subability?
        } else {
            if (!untapPrefTargeting(ai, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapPrefTargeting.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapPrefTargeting(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();

        if (alreadyAssignedTarget(sa)) {
            if (sa.getTargets().size() > 0) {
                // If we selected something lets assume its valid
                return true;
            }
        }
        sa.resetTargets();

        final PlayerCollection targetController = new PlayerCollection();
        if (sa.isCurse()) {
            targetController.addAll(ai.getOpponents());
        } else {
            targetController.add(ai);
        }

        CardCollection list = CardLists.getTargetableCards(targetController.getCardsIn(ZoneType.Battlefield), sa);

        if (!sa.isCurse()) {
            list = ComputerUtil.getSafeTargets(ai, sa, list);
        }

        if (list.isEmpty()) {
            return false;
        }

        // For some abilities, it may be worth to target even an untapped card if we're targeting mostly for the subability
        boolean targetUntapped = false;
        if (sa.getSubAbility() != null) {
            SpellAbility subSa = sa.getSubAbility();
            if (subSa.getApi() == ApiType.RemoveFromCombat && "RemoveBestAttacker".equals(subSa.getParam("AILogic"))) {
                targetUntapped = true;
            }
        }

        CardCollection untapList = targetUntapped ? list : CardLists.filter(list, Presets.TAPPED);
        // filter out enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = {"Creature", "Land", "Artifact"};
        untapList = CardLists.getValidCards(untapList, tappablePermanents, source.getController(), source, sa);

        // Try to avoid potential infinite recursion,
        // e.g. Kiora's Follower untapping another Kiora's Follower and repeating infinitely
        if (sa.getPayCosts().hasOnlySpecificCostType(CostTap.class)) {
            CardCollection toRemove = new CardCollection();
            for (Card c : untapList) {
                for (SpellAbility ab : c.getAllSpellAbilities()) {
                    if (ab.getApi() == ApiType.Untap
                            && ab.getPayCosts().hasOnlySpecificCostType(CostTap.class)
                            && ab.canTarget(source)) {
                        toRemove.add(c);
                        break;
                    }
                }
            }
            untapList.removeAll(toRemove);
        }

        //try to exclude things that will already be untapped due to something on stack or because something is
        //already targeted in a parent or sub SA
        if (!sa.isTrigger() || mandatory) { // but if just confirming trigger no need to look for other targets and might still help anyway
            CardCollection toExclude = ComputerUtilAbility.getCardsTargetedWithApi(ai, untapList, sa, ApiType.Untap);
            untapList.removeAll(toExclude);
        }

        while (sa.canAddMoreTarget()) {
            Card choice = null;

            if (untapList.isEmpty()) {
                // Animate untapped lands (Koth of the Hammer)
                if (sa.getSubAbility() != null && sa.getSubAbility().getApi() == ApiType.Animate && !list.isEmpty()
                        && ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    choice = ComputerUtilCard.getWorstPermanentAI(list, false, false, false, false);
                } else if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    // check if the cost is acceptable anyway (e.g. Planeswalker +Loyalty)
                    if (ComputerUtil.activateForCost(sa, ai)) {
                        return true;
                    }
                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            } else {
                choice = detectPriorityUntapTargets(untapList);

                if (choice == null) {
                    if (CardLists.getNotType(untapList, "Creature").isEmpty()) {
                        choice = ComputerUtilCard.getBestCreatureAI(untapList); // if only creatures take the best
                    } else if (!sa.getPayCosts().hasManaCost() || sa.isTrigger()
                            || "Always".equals(sa.getParam("AILogic"))) {
                        choice = ComputerUtilCard.getMostExpensivePermanentAI(untapList);
                    }
                }
            }

            if (choice == null) { // can't find anything left
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            untapList.remove(choice);
            list.remove(choice);
            // TODO ComputerUtilCard.willUntap(ai, choice)
            sa.getTargets().add(choice);
        }
        return true;
    }

    /**
     * <p>
     * untapUnpreferredTargeting.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean untapUnpreferredTargeting(final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        CardCollection list = CardLists.getTargetableCards(source.getGame().getCardsIn(ZoneType.Battlefield), sa);

        // filter by enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        CardCollection tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source, sa);

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = CardLists.filter(list, Presets.UNTAPPED);

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        return untapTargetList(source, tgt, sa, mandatory, tapList);
    }

    private boolean untapTargetList(final Card source, final TargetRestrictions tgt, final SpellAbility sa, final boolean mandatory, 
            final CardCollection tapList) {
        tapList.removeAll(sa.getTargets().getTargetCards());

        if (tapList.isEmpty()) {
            return false;
        }

        while (sa.canAddMoreTarget()) {
            Card choice = null;

            if (tapList.isEmpty()) {
                if (sa.getTargets().size() < tgt.getMinTargets(source, sa) || sa.getTargets().size() == 0) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            choice = ComputerUtilCard.getBestAI(tapList);

            if (choice == null) { // can't find anything left
                if (sa.getTargets().size() < tgt.getMinTargets(source, sa) || sa.getTargets().size() == 0) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            sa.getTargets().add(choice);
        }

        return true;
    }

    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> list, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        CardCollection pref = CardLists.filterControlledBy(list, ai.getYourTeam());
        if (Iterables.isEmpty(pref)) {
            if (isOptional) {
                return null;
            }
        } else {
            list = pref;
        }
        return ComputerUtilCard.getBestAI(list);
    }

    private static Card detectPriorityUntapTargets(final List<Card> untapList) {
        // See if there are cards that are *especially* worth untapping, like Time Vault
        for (Card c : untapList) {
            if ("True".equals(c.getSVar("UntapMe"))) {
                return c;
            }
        }

        // See if there's anything to untap that is tapped and that doesn't untap during the next untap step by itself
        CardCollection noAutoUntap = CardLists.filter(untapList, Predicates.not(Untap.CANUNTAP));
        if (!noAutoUntap.isEmpty()) {
            return ComputerUtilCard.getBestAI(noAutoUntap);
        }

        return null;
    }

    private boolean doPreventCombatDamageLogic(final Player ai, final SpellAbility sa) {
        // Only Maze of Ith and Maze of Shadows uses this. Feel free to use it aggressively.
        Game game = ai.getGame();
        Card source = sa.getHostCard();
        sa.resetTargets();

        if (!game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
            return false;
        }

        // If damage can't be prevented. Just return false.

         Combat activeCombat = game.getCombat();
        if (activeCombat == null) {
            return false;
        }

        CardCollection list = CardLists.getTargetableCards(activeCombat.getAttackers(), sa);

        if (list.isEmpty()) {
            return false;
        }

         if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // Blockers already set. Are there any dangerous unblocked creatures? Sort by creature that will deal the most damage?
            Card card = ComputerUtilCombat.mostDangerousAttacker(list, ai, activeCombat, true);

            if (card == null) { return false; }

             sa.getTargets().add(card);
             return true;
        }

        return false;
    }

    private static boolean alreadyAssignedTarget(final SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            String aiLogic = sa.getParam("AILogic");
            return "PreventCombatDamage".equals(aiLogic);
        }
        return false;
    }

    private boolean doPoolExtraManaLogic(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final PhaseHandler ph = source.getGame().getPhaseHandler();
        final Game game = ai.getGame();

        if (source.isTapped()) {
            return true;
        }

        // Check if something is playable if we untap for an additional mana with this, then proceed
        CardCollection inHand = CardLists.filter(ai.getCardsIn(ZoneType.Hand), Predicates.not(CardPredicates.Presets.LANDS));
        // The AI is not very good at timing non-permanent spells this way, so filter them out
        // (it may actually be possible to enable this for sorceries, but that'll need some canPlay shenanigans)
        CardCollection playable = CardLists.filter(inHand, Presets.PERMANENTS);

        CardCollection untappingCards = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(Card card) {
                boolean hasUntapLandLogic = false;
                for (SpellAbility sa : card.getSpellAbilities()) {
                    if ("PoolExtraMana".equals(sa.getParam("AILogic"))) {
                        hasUntapLandLogic = true;
                        break;
                    }
                }
                return hasUntapLandLogic && card.isUntapped();
            }
        });

        // TODO: currently limited to Main 2, somehow improve to let the AI use this SA at other time?
        if (ph.is(PhaseType.MAIN2, ai)) {
            for (Card c : playable) {
                for (SpellAbility ab : c.getBasicSpells()) {
                    if (!ComputerUtilMana.hasEnoughManaSourcesToCast(ab, ai)) {
                        // TODO: Currently limited to predicting something that can be paid with any color,
                        // can ideally be improved to work by color.
                        ManaCostBeingPaid reduced = new ManaCostBeingPaid(ab.getPayCosts().getCostMana().getManaCostFor(ab));
                        reduced.decreaseShard(ManaCostShard.GENERIC, untappingCards.size());
                        if (ComputerUtilMana.canPayManaCost(reduced, ab, ai, false)) {
                            CardCollection manaLandsTapped = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                                    Presets.LANDS_PRODUCING_MANA, Presets.TAPPED);
                            manaLandsTapped = CardLists.getValidCards(manaLandsTapped, sa.getParam("ValidTgts"), ai, source, null);

                            if (!manaLandsTapped.isEmpty()) {
                                // already have a tapped land, so agree to proceed with untapping it
                                return true;
                            }

                            // pool one additional mana by tapping a land to try to ramp to something
                            CardCollection manaLands = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                                    Presets.LANDS_PRODUCING_MANA, Presets.UNTAPPED);
                            manaLands = CardLists.getValidCards(manaLands, sa.getParam("ValidTgts"), ai, source, null);

                            if (manaLands.isEmpty()) {
                                // nothing to untap
                                return false;
                            }

                            Card landToPool = manaLands.getFirst();
                            SpellAbility manaAb = landToPool.getManaAbilities().getFirst();

                            ComputerUtil.playNoStack(ai, manaAb, game, false);

                            return true;
                        }
                    }
                }
            }
        }

        // no harm in doing this past declare blockers during the opponent's turn and right before our turn,
        // maybe we'll serendipitously untap into something like a removal spell or burn spell that'll help
        return ph.getNextTurn() == ai
                && (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS));

        // haven't found any immediate playable options
    }

}
