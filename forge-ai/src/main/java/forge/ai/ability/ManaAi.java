package forge.ai.ability;

import forge.ai.*;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.game.CardTraitPredicates;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
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
import forge.util.IterableUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManaAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkAiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, java.lang.String)
     */
    @Override
    protected boolean checkAiLogic(Player ai, SpellAbility sa, String aiLogic) {
        if (aiLogic.startsWith("ManaRitual") || aiLogic.startsWith("BlackLotus")) {
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
        }
        if ("AtOppEOT".equals(logic)) {
            return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == ai
                    && (!ai.getManaPool().hasBurn() || !ai.canLoseLife() || ai.cantLoseForZeroOrLessLife());
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
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay); // handled elsewhere, does not meet the standard requirements
        }

        // TODO check if it would be worth it to keep mana open for opponents turn anyway
        if (ComputerUtil.activateForCost(sa, ai)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (sa.getPayCosts().hasNoManaCost() && sa.getPayCosts().isReusuableResource()
                && sa.getSubAbility() == null && (improvesPosition(ai, sa) || ComputerUtil.playImmediately(ai, sa))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
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
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.startsWith("ManaRitual")) {
            boolean result = doManaRitualLogic(aiPlayer, sa, true);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
    
    // Dark Ritual and other similar instants/sorceries that add mana to mana pool
    public static boolean doManaRitualLogic(Player ai, SpellAbility sa, boolean fromTrigger) {
        final Card host = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");
        final boolean manaBattery = logic.startsWith("ManaRitualBattery");

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
        int baseMana = manaSources.size();
        if (manaBattery) {
            manaSources.remove(host);
            baseMana = manaSources.size() + ai.getManaPool().totalMana();
        }
        int manaReceived = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa) : 1;
        manaReceived *= sa.getParam("Produced").split(" ").length;

        int selfCost = sa.getRootAbility().getPayCosts().getCostMana() != null ? sa.getRootAbility().getPayCosts().getCostMana().getMana().getCMC() : 0;
        baseMana -= selfCost;

        String produced = sa.getParam("Produced");
        byte producedColor = produced.equals("Any") ? MagicColor.ALL_COLORS : MagicColor.fromName(produced);

        int numCounters = 0;
        int manaSurplus = 0;
        if ("Count$xPaid".equals(host.getSVar("X")) && sa.getPayCosts().hasSpecificCostType(CostRemoveCounter.class)) {
            CounterType ctrType = sa.getPayCosts().getCostPartByType(CostRemoveCounter.class).counter;
            numCounters = host.getCounters(ctrType);
            manaReceived = numCounters;
            if (logic.startsWith("ManaRitualBattery.")) {
                manaSurplus = Integer.parseInt(logic.substring("ManaRitualBattery.".length()));
                // adds an extra mana even if no counters removed
                manaReceived += manaSurplus;
            }
        }

        int searchCMC = baseMana + manaReceived;

        if ("X".equals(sa.getParam("Produced"))) {
            String x = host.getSVar("X");
            if ("Count$CardsInYourHand".equals(x) && host.isInZone(ZoneType.Hand)) {
                searchCMC--; // the spell in hand will be used
            } else if (x.startsWith("Count$ValidGraveyard Card.named") && host.isInZone(ZoneType.Graveyard)) {
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
            testSaNoCost.setActivatingPlayer(ai);
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
                        CardPredicates.isColorless().or(CardPredicates.isColor(producedColor))));

        if (castableSpells.isEmpty()) {
            return false;
        }

        if (manaBattery) {
            // Don't remove more counters than would be needed to cast the more expensive thing we want to cast,
            // otherwise the AI grabs too many counters at once.
            int countersNeeded = Aggregates.max(castableSpells, Card::getCMC) - baseMana - manaSurplus;
            if (countersNeeded <= 0) {
                return false;
            }
            sa.setXManaCostPaid(Math.min(numCounters, countersNeeded));
        }

        // TODO: this will probably still waste the card from time to time. Somehow improve detection of castable material.
        return true;
    }

    // Optional untap choices happen before the main phase, so forecast a useful spell without
    // changing the battery's X payment. The activation path chooses X from the actual game state.
    public static boolean shouldUntapManaBattery(Player ai, SpellAbility manaAbility) {
        final Card host = manaAbility.getHostCard();
        final String logic = manaAbility.getParamOrDefault("AILogic", "");
        if (!logic.startsWith("ManaRitualBattery")
                || !"Count$xPaid".equals(host.getSVar("X"))
                || !manaAbility.getPayCosts().hasSpecificCostType(CostRemoveCounter.class)) {
            return false;
        }

        final CounterType counterType = manaAbility.getPayCosts()
                .getCostPartByType(CostRemoveCounter.class).counter;
        final int storedMana = host.getCounters(counterType);
        if (storedMana <= 0) {
            return false;
        }

        final int baseMana = ComputerUtilMana.getAvailableManaEstimate(ai, false,
                ability -> isPotentialMainPhaseManaSource(ai, ability.getHostCard())
                        && !isManaRitualBatteryAbility(ability));
        final CardCollection futureManaSources = ComputerUtilMana.getAvailableManaSources(ai, false);
        futureManaSources.removeIf(card -> !isPotentialMainPhaseManaSource(ai, card)
                || !hasNonBatteryManaAbility(card));

        final String produced = manaAbility.getParam("Produced");
        final byte producedColor = produced.equals("Any")
                ? MagicColor.ALL_COLORS : MagicColor.fromName(produced);
        byte availableManaColors = ColorSet.fromNames(
                ComputerUtilCost.getAvailableManaColors(ai, futureManaSources)).getColor();
        availableManaColors |= producedColor;

        final List<SpellAbility> handAbilities = new ArrayList<>();
        for (Card card : ai.getCardsIn(ZoneType.Hand)) {
            handAbilities.addAll(card.getSpellAbilities());
        }

        final String restrictValid = manaAbility.getParamOrDefault("RestrictValid", "Card");
        final int manaWithStorage = baseMana + storedMana;
        for (SpellAbility spell : ComputerUtilAbility.getOriginalAndAltCostAbilities(handAbilities, ai)) {
            final ManaCost cost = spell.getPayCosts().getTotalMana();
            if (!spell.isSpell() || spell.getHostCard().isInstant()
                    || (cost.getCMC() == 0 && cost.countX() == 0)
                    || (cost.getColorProfile() != 0 && !cost.canBePaidWithAvailable(availableManaColors))
                    || ComputerUtilAbility.getAbilitySourceName(spell)
                            .equals(ComputerUtilAbility.getAbilitySourceName(manaAbility))
                    || spell.hasParam("AINoRecursiveCheck")
                    || !CardPredicates.restriction(restrictValid.split(","), ai, host, manaAbility)
                            .test(spell.getHostCard())) {
                continue;
            }

            final int spellCost = cost.getCMC();
            if (spellCost > baseMana && spellCost <= manaWithStorage
                    && isUsefulNextMainPhaseSpell(ai, spell)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPotentialMainPhaseManaSource(Player ai, Card card) {
        return card.isInPlay() && !card.isPhasedOut()
                && (!card.isCreature() || !card.isSick())
                && (!card.isTapped() || card.canUntap(ai, true));
    }

    private static boolean hasNonBatteryManaAbility(Card card) {
        for (SpellAbility manaAbility : card.getManaAbilities()) {
            if (!isManaRitualBatteryAbility(manaAbility)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isManaRitualBatteryAbility(SpellAbility manaAbility) {
        return manaAbility.getParamOrDefault("AILogic", "").startsWith("ManaRitualBattery");
    }

    private static boolean isUsefulNextMainPhaseSpell(Player ai, SpellAbility spell) {
        final SpellAbility noCost = spell.copyWithNoManaCost();
        if (noCost == null) {
            return false;
        }
        noCost.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(noCost).doTriggerNoCostWithSubs(ai, noCost, false).willingToPlay();
    }

    private boolean improvesPosition(Player ai, SpellAbility sa) {
        boolean activateForTrigger = (!ai.getManaPool().hasBurn() || !ai.canLoseLife() || ai.cantLoseForZeroOrLessLife()) &&
                IterableUtil.any(IterableUtil.filter(sa.getHostCard().getTriggers(), CardTraitPredicates.hasParam("AILogic", "ActivateOnce")),
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
            test = new Mana((byte) ManaAtom.COLORLESS, source, null, ai);
            mp.addManaNoEvent(test);
        }
        boolean lose = mp.willManaBeLostAtEndOfPhase();
        if (test != null) {
            mp.removeManaNoEvent(test);
        }
        return !lose;
    }
}
