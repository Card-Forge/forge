package forge.ai.ability;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.game.CardTraitPredicates;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.cost.CostPart;
import forge.game.cost.CostRemoveCounter;
import forge.game.keyword.Keyword;
import forge.game.mana.Mana;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

public class ManaEffectAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkAiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, java.lang.String)
     */
    @Override
    protected boolean checkAiLogic(Player ai, SpellAbility sa, String aiLogic) {
        if (aiLogic.startsWith("ManaRitual")) {
            return doManaRitualLogic(ai, sa, false);
        } else if ("Always".equals(aiLogic)) {
            return true;
        }
        return super.checkAiLogic(ai, sa, aiLogic);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler)
     */
    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        if (improvesPosition(ai, sa)) {
            return true;
        }
        if (!ph.is(PhaseType.MAIN2)) {
            return false;
        }
        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler,
     * java.lang.String)
     */
    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph, String logic) {
        if (logic.startsWith("ManaRitual")) {
             return ph.is(PhaseType.MAIN2, ai) || ph.is(PhaseType.MAIN1, ai);
        } else if ("AtOppEOT".equals(logic)) {
            return (!ai.getManaPool().hasBurn() || !ai.canLoseLife() || ai.cantLoseForZeroOrLessLife()) && ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == ai;
        }
        return super.checkPhaseRestrictions(ai, sa, ph, logic);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            return true; // handled elsewhere, does not meet the standard requirements
        }

        // TODO check if it would be worth it to keep mana open for opponents turn anyway
        if (ComputerUtil.activateForCost(sa, ai)) {
            return true;
        }

        return sa.getPayCosts().hasNoManaCost() && sa.getPayCosts().isReusuableResource()
                && sa.getSubAbility() == null && (improvesPosition(ai, sa) || ComputerUtil.playImmediately(ai, sa));
    }

    /**
     * @param aiPlayer
     *            the AI player.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.startsWith("ManaRitual")) {
            return doManaRitualLogic(aiPlayer, sa, true);
        }

        return true;
    }
    
    // Dark Ritual and other similar instants/sorceries that add mana to mana pool
    public static boolean doManaRitualLogic(Player ai, SpellAbility sa, boolean fromTrigger) {
        final Card host = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (sa.usesTargeting()) { // Rousing Refrain
            PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            if (targetableOpps.isEmpty()) {
                return false;
            }
            Player mostCards = targetableOpps.max(PlayerPredicates.compareByZoneSize(ZoneType.Hand));
            sa.resetTargets();
            sa.getTargets().add(mostCards);
            if (fromTrigger) {
                return true;
            }
        }
        
        CardCollection manaSources = ComputerUtilMana.getAvailableManaSources(ai, true);
        int numManaSrcs = manaSources.size();
        int manaReceived = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa) : 1;
        manaReceived *= sa.getParam("Produced").split(" ").length;

        int selfCost = sa.getRootAbility().getPayCosts().getCostMana() != null ? sa.getRootAbility().getPayCosts().getCostMana().getMana().getCMC() : 0;

        String produced = sa.getParam("Produced");
        byte producedColor = produced.equals("Any") ? MagicColor.ALL_COLORS : MagicColor.fromName(produced);

        int numCounters = 0;
        int manaSurplus = 0;
        if ("Count$xPaid".equals(host.getSVar("X")) && sa.getPayCosts().hasSpecificCostType(CostRemoveCounter.class)) {
            CounterType ctrType = CounterType.get(CounterEnumType.KI); // Petalmane Baku
            for (CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostRemoveCounter) {
                    ctrType = ((CostRemoveCounter)part).counter;
                    break;
                }
            }
            numCounters = host.getCounters(ctrType);
            manaReceived = numCounters;
            if (logic.startsWith("ManaRitualBattery.")) {
                manaSurplus = Integer.valueOf(logic.substring(18)); // adds an extra mana even if no counters removed
                manaReceived += manaSurplus;
            }
        }

        int searchCMC = numManaSrcs - selfCost + manaReceived;

        if ("X".equals(sa.getParam("Produced"))) {
            String x = host.getSVar("X");
            if ("Count$CardsInYourHand".equals(x) && host.isInZone(ZoneType.Hand)) {
                searchCMC--; // the spell in hand will be used
            } else if (x.startsWith("Count$NamedInAllYards") && host.isInZone(ZoneType.Graveyard)) {
                searchCMC--; // the spell in graveyard will be used
            }
        }

        if (searchCMC <= 0) {
            return false;
        }

        String restrictValid = sa.getParamOrDefault("RestrictValid", "Card");

        CardCollection cardList = new CardCollection();
        // TODO check other zones
        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(ai.getCardsIn(ZoneType.Hand), ai);
        for (final SpellAbility testSa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, ai)) {
            ManaCost cost = testSa.getPayCosts().getTotalMana();
            boolean canPayWithAvailableColors = cost.canBePaidWithAvailable(ColorSet.fromNames(
                    ComputerUtilCost.getAvailableManaColors(ai, (List<Card>)null)).getColor());
            
            if (cost.getCMC() == 0 && cost.countX() == 0) {
                // no mana cost, no need to activate this SA then (additional mana not needed)
                continue;
            } else if (cost.getColorProfile() != 0 && !canPayWithAvailableColors) {
                // don't have one of each shard represented, may not be able to pay the cost
                continue;
            }

            if (ComputerUtilAbility.getAbilitySourceName(testSa).equals(ComputerUtilAbility.getAbilitySourceName(sa))
                    || testSa.hasParam("AINoRecursiveCheck")) {
                // prevent infinitely recursing mana ritual and other abilities with reentry
                continue;
            }

            SpellAbility testSaNoCost = testSa.copyWithNoManaCost();
            if (testSaNoCost == null) {
                continue;
            }
            testSaNoCost.setActivatingPlayer(ai, true);
            if (((PlayerControllerAi)ai.getController()).getAi().canPlaySa(testSaNoCost) == AiPlayDecision.WillPlay) {
                if (testSa.getHostCard().isPermanent() && !testSa.getHostCard().hasKeyword(Keyword.HASTE)
                    && !ai.getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
                    // AI will waste a ritual in Main 1 unless the casted permanent is a haste creature
                    continue;
                }
                if (testSa.getHostCard().isInstant()) {
                    // AI is bad at choosing which instants are worth a Ritual
                    continue;
                }

                // the AI is willing to play the spell
                if (!cardList.contains(testSa.getHostCard())) {
                    cardList.add(testSa.getHostCard());
                }
            }
        }

        CardCollection castableSpells = CardLists.filter(cardList,
                Arrays.asList(
                        CardPredicates.restriction(restrictValid.split(","), ai, host, sa),
                        CardPredicates.lessCMC(searchCMC),
                        Predicates.or(CardPredicates.isColorless(), CardPredicates.isColor(producedColor))));

        if (logic.startsWith("ManaRitualBattery")) {
            // Don't remove more counters than would be needed to cast the more expensive thing we want to cast,
            // otherwise the AI grabs too many counters at once.
            int maxCtrs = Aggregates.max(castableSpells, CardPredicates.Accessors.fnGetCmc) - manaSurplus;
            sa.setXManaCostPaid(Math.min(numCounters, maxCtrs));
        }

        // TODO: this will probably still waste the card from time to time. Somehow improve detection of castable material.
        return castableSpells.size() > 0;
    }

    private boolean improvesPosition(Player ai, SpellAbility sa) {
        boolean activateForTrigger = (!ai.getManaPool().hasBurn() || !ai.canLoseLife() || ai.cantLoseForZeroOrLessLife()) &&
                Iterables.any(Iterables.filter(sa.getHostCard().getTriggers(), CardTraitPredicates.hasParam("AILogic", "ActivateOnce")),
                t -> sa.getHostCard().getAbilityActivatedThisTurn(t.getOverridingAbility()) == 0);

        PhaseHandler ph = ai.getGame().getPhaseHandler();
        // TODO if threatened use right away
        return ph.is(PhaseType.END_OF_TURN) && (ph.getNextTurn() == ai || ComputerUtilCard.willUntap(ai, sa.getHostCard()))
                && (activateForTrigger || canRampPool(ai, sa.getHostCard()));   
    }

    public static boolean canRampPool(Player ai, Card source) {
        ManaPool mp = ai.getManaPool();
        Mana test = null;
        if (mp.isEmpty()) {
            // TODO use color from ability
            test = new Mana((byte) ManaAtom.COLORLESS, source, null);
            mp.addMana(test, false);
        }
        boolean lose = mp.willManaBeLostAtEndOfPhase();
        if (test != null) {
            mp.removeMana(test, false);
        }
        return !lose;
    }
}
