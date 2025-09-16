package forge.ai.ability;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.ai.*;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.*;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ChangeZoneAi extends SpellAbilityAi {
    /*
     * This class looks horribly convoluted with hidden/known + CanPlay/Drawback/Trigger
     * and static functions like chooseCardToHiddenOriginChangeZone(). It might be a good
     * idea to re-factor ChangeZoneAi into more specific effects since it is really doing
     * too much: blink/bounce/exile/tutor/Raise Dead/Surgical Extraction/......
     */

    // multipleCardsToChoose is used by Intuition and can be adapted to be used by other
    // cards where multiple cards are fetched at once and they need to be coordinated
    private static CardCollection multipleCardsToChoose = new CardCollection();

    protected boolean willPayCosts(Player ai, SpellAbility sa, Cost cost, Card source) {
        if (sa.isHidden()) {
            if (!ComputerUtilCost.checkSacrificeCost(ai, cost, source, sa)
                    && !"Battlefield".equals(sa.getParam("Destination")) && !source.isLand()) {
                return false;
            }

            if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 4, sa)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, cost, source, sa)) {
                for (final CostPart part : cost.getCostParts()) {
                    if (part instanceof CostDiscard) {
                        CostDiscard cd = (CostDiscard) part;
                        // this is mainly for typecycling
                        if (!cd.payCostFromSource() || !ComputerUtil.isWorseThanDraw(ai, source)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        if (sa.isCraft()) {
            CardCollection payingCards = new CardCollection();
            int needed = 0;
            for (final CostPart part : cost.getCostParts()) {
                if (part instanceof CostExile) {
                    if (part.payCostFromSource()) {
                        continue;
                    }
                    int amt = part.getAbilityAmount(sa);
                    needed += amt;
                    CardCollection toAdd = ComputerUtil.chooseExileFrom(ai, (CostExile) part, source, amt, sa, true);
                    if (toAdd != null) {
                        payingCards.addAll(toAdd);
                    }
                }
            }
            if (payingCards.size() < needed) {
                return false;
            }
        }

        return super.willPayCosts(ai, sa, cost, source);
    }

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if (sa.getHostCard() != null && sa.getHostCard().hasSVar("AIPreferenceOverride")) {
            // currently used by SacAndUpgrade logic, might need simplification
            sa.getHostCard().removeSVar("AIPreferenceOverride");
        }

        if (aiLogic.equals("BeforeCombat")) {
            if (ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_BEGIN)) {
                return false;
            }
        } else if (aiLogic.equals("SurpriseBlock")) {
            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else if (aiLogic.equals("PriorityOptionalCost")) {
            boolean highPriority = false;
            // if we have more than one of these in hand, might not be worth waiting for optional cost payment on the additional copy
            highPriority |= CardLists.count(ai.getCardsIn(ZoneType.Hand), CardPredicates.nameEquals(sa.getHostCard().getName())) > 1;
            // if we are in danger in combat, no need to wait to pay the optional cost
            highPriority |= ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && ai.getGame().getCombat() != null && ComputerUtilCombat.lifeInDanger(ai, ai.getGame().getCombat());

            if (!highPriority) {
                if (Iterables.isEmpty(sa.getOptionalCosts())) {
                    return false;
                }
            }
        } else if (aiLogic.equals("NoSameCreatureType")) {
            final List<ZoneType> origin = Lists.newArrayList();
            if (sa.hasParam("Origin")) {
                origin.addAll(ZoneType.listValueOf(sa.getParam("Origin")));
            } else if (sa.hasParam("TgtZone")) {
                origin.addAll(ZoneType.listValueOf(sa.getParam("TgtZone")));
            }
            CardCollection list = CardLists.getValidCards(ai.getGame().getCardsIn(origin),
                    sa.getTargetRestrictions().getValidTgts(), ai, sa.getHostCard(), sa);

            final List<String> creatureTypes = Lists.newArrayList();
            for (Card c : list) {
                creatureTypes.addAll(c.getType().getCreatureTypes());
            }

            for (String type : creatureTypes) {
                int freq = Collections.frequency(creatureTypes, type);
                if (freq > 1) {
                    return false;
                }
            }

            return true;
        } else if (aiLogic.equals("Pongify")) {
            return SpecialAiLogic.doPongifyLogic(ai, sa);
        }

        return super.checkAiLogic(ai, sa, aiLogic);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        multipleCardsToChoose.clear();
        String aiLogic = sa.getParam("AILogic");
        if (aiLogic != null) {
            if (aiLogic.equals("Always")) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else if (aiLogic.startsWith("SacAndUpgrade")) { // Birthing Pod, Natural Order, etc.
                return doSacAndUpgradeLogic(aiPlayer, sa);
            } else if (aiLogic.startsWith("SacAndRetFromGrave")) { // Recurring Nightmare, etc.
                return doSacAndReturnFromGraveLogic(aiPlayer, sa);
            } else if (aiLogic.equals("Necropotence")) {
                return SpecialCardAi.Necropotence.consider(aiPlayer, sa);
            } else if (aiLogic.equals("ReanimateAll")) {
                return SpecialCardAi.LivingDeath.consider(aiPlayer, sa);
            } else if (aiLogic.equals("TheScarabGod")) {
                return SpecialCardAi.TheScarabGod.consider(aiPlayer, sa);
            } else if (aiLogic.equals("SorinVengefulBloodlord")) {
                return SpecialCardAi.SorinVengefulBloodlord.consider(aiPlayer, sa);
            } else if (aiLogic.equals("Intuition")) {
                // This logic only fills the multiple cards array, the decision to play is made
                // separately in hiddenOriginCanPlayAI later.
                multipleCardsToChoose = SpecialCardAi.Intuition.considerMultiple(aiPlayer, sa);
            } else if (aiLogic.equals("MazesEnd")) {
                return SpecialCardAi.MazesEnd.consider(aiPlayer, sa);
            } else if (aiLogic.equals("Pongify")) {
                if (sa.isTargetNumberValid()) {
                    // Pre-targeted in checkAiLogic
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if (aiLogic.equals("ReturnCastable")) {
                if (!sa.getHostCard().getExiledCards().isEmpty()
                        && ComputerUtilMana.canPayManaCost(sa.getHostCard().getExiledCards().getFirst().getFirstSpellAbility(), aiPlayer, 0, false)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        if (sa.isHidden()) {
            return hiddenOriginCanPlayAI(aiPlayer, sa);
        }
        return knownOriginCanPlayAI(aiPlayer, sa);
    }

    /**
     * <p>
     * changeZonePlayDrawbackAI.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     *
     * @return a boolean.
     */
    @Override
    public AiAbilityDecision chkDrawback(SpellAbility sa, Player aiPlayer) {
        if (sa.isHidden()) {
            return hiddenOriginPlayDrawbackAI(aiPlayer, sa);
        }
        return knownOriginPlayDrawbackAI(aiPlayer, sa);
    }

    /**
     * <p>
     * changeZoneTriggerAINoCost.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     *
     * @return a boolean.
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (sa.isReplacementAbility() && "Command".equals(sa.getParam("Destination")) && "ReplacedCard".equals(sa.getParam("Defined"))) {
            // Process the commander replacement effect ("return to Command zone instead")
            return doReturnCommanderLogic(sa, aiPlayer);
        }

        if ("Always".equals(aiLogic)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if ("IfNotBuffed".equals(aiLogic)) {
            if (ComputerUtilCard.isUselessCreature(aiPlayer, sa.getHostCard())) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            int delta = 0;
            for (Card enc : sa.getHostCard().getEnchantedBy()) {
                if (enc.getController().isOpponentOf(aiPlayer)) {
                    delta--;
                } else {
                    delta++;
                }
            }
            if (delta <= 0) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if ("SaviorOfOllenbock".equals(aiLogic)) {
            if (SpecialCardAi.SaviorOfOllenbock.consider(aiPlayer, sa)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (sa.isHidden()) {
            return hiddenTriggerAI(aiPlayer, sa, mandatory);
        }
        return knownOriginTriggerAI(aiPlayer, sa, mandatory);
    }

    // *************************************************************************************
    // ************ Hidden Origin (Library/Hand/Sideboard/Non-targetd other)
    // ***************
    // ******* Hidden origin cards are chosen on the resolution of the spell
    // ***************
    // ******* It is possible for these to have Destination of Battlefield
    // *****************
    // ****** Example: Cavern Harpy where you don't choose the card until
    // resolution *******
    // *************************************************************************************

    /**
     * <p>
     * changeHiddenOriginCanPlayAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static AiAbilityDecision hiddenOriginCanPlayAI(final Player ai, final SpellAbility sa) {
        // Fetching should occur fairly often as it helps cast more spells, and have access to more mana
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        List<ZoneType> origin = null;
        final Player opponent = AiAttackController.choosePreferredDefenderPlayer(ai);
        boolean activateForCost = ComputerUtil.activateForCost(sa, ai);

        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }
        final String destination = sa.getParam("Destination");

        if (sa.isNinjutsu()) {
            if (!source.ignoreLegendRule() && ai.isCardInPlay(source.getName())) {
                return new AiAbilityDecision(0, AiPlayDecision.WouldDestroyLegend);
            }
            if (ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE)) {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
            }

            if (ai.getGame().getCombat() == null) {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
            }
            List<Card> attackers = ai.getGame().getCombat().getUnblockedAttackers();
            boolean lowerCMC = false;
            for (Card attacker : attackers) {
                if (attacker.getCMC() < source.getCMC()) {
                    lowerCMC = true;
                    break;
                }
            }
            if (!lowerCMC) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        Iterable<Player> pDefined = Lists.newArrayList(source.getController());
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null && tgt.canTgtPlayer()) {
            sa.resetTargets();
            boolean isCurse = sa.isCurse();
            if (isCurse && sa.canTarget(opponent)) {
                sa.getTargets().add(opponent);
            } else if (!isCurse && sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            }
            if (!sa.isTargetNumberValid()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
            pDefined = sa.getTargets().getTargetPlayers();
        } else {
            if (sa.hasParam("DefinedPlayer")) {
                pDefined = AbilityUtils.getDefinedPlayers(source, sa.getParam("DefinedPlayer"), sa);
            } else {
                pDefined = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);
            }
        }

        String type = sa.getParam("ChangeType");
        if (type != null && type.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            sa.setXManaCostPaid(xPay);
            type = type.replace("X", Integer.toString(xPay));
        }

        for (final Player p : pDefined) {
            CardCollectionView list = p.getCardsIn(origin);

            // remove cards that won't be seen if library can't be searched
            if (!ai.canSearchLibraryWith(sa, p)) {
                list = CardLists.filter(list, CardPredicates.inZone(ZoneType.Library).negate());
            }

            if (type != null && p == ai) {
                // AI only "knows" about his information
                list = CardLists.getValidCards(list, type, source.getController(), source, sa);
                list = CardLists.filter(list, c -> {
                    if (c.getType().isLegendary()) {
                        return !ai.isCardInPlay(c.getName());
                    }
                    return true;
                });
            }
            // TODO: prevent ai searching its own library when Ob Nixilis, Unshackled is in play
            if (origin != null && origin.size() == 1 && origin.get(0).isKnown()) {
                // FIXME: make this properly interact with several origin zones
                list = CardLists.getValidCards(list, type, source.getController(), source, sa);
            }

            if (!activateForCost && list.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            if ("Atarka's Command".equals(sourceName)
                    && (list.size() < 2 || ai.getLandsPlayedThisTurn() < 1)) {
                // be strict on playing lands off charms
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            String num = sa.getParamOrDefault("ChangeNum", "1");
            if (num.contains("X")) {
                if (sa.getSVar("X").equals("Count$xPaid")) {
                    // Set PayX here to maximum value.
                    int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                    if (xPay == 0) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
                    }
                    xPay = Math.min(xPay, list.size());
                    sa.setXManaCostPaid(xPay);
                } else {
                    // Figure out the X amount, bail if it's zero (nothing will change zone).
                    int xValue = AbilityUtils.calculateAmount(source, "X", sa);
                    if (xValue == 0) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
                    }
                }
            }

            if (sourceName.equals("Temur Sabertooth")) {
                // activated bounce + pump
                boolean pumpDecision = ComputerUtilCard.shouldPumpCard(ai, sa.getSubAbility(), source, 0, 0, Arrays.asList("Indestructible"));
                AiAbilityDecision saveDecision = ComputerUtilCard.canPumpAgainstRemoval(ai, sa.getSubAbility());
                if (pumpDecision || saveDecision.willingToPlay()) {
                    for (Card c : list) {
                        if (ComputerUtilCard.evaluateCreature(c) < ComputerUtilCard.evaluateCreature(source)) {
                            return new AiAbilityDecision(100, AiPlayDecision.ResponseToStackResolve);
                        }
                    }
                }
                if (canBouncePermanent(ai, sa, list) != null) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // don't use fetching to top of library/graveyard before main2
        if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")) {
            if (!destination.equals("Battlefield") && !destination.equals("Hand")) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            // Only tutor something in main1 if hand is almost empty
            if (ai.getCardsIn(ZoneType.Hand).size() > 1 && destination.equals("Hand")
                    && !aiLogic.equals("AnyMainPhase")) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (ComputerUtil.waitForBlocking(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * <p>
     * changeHiddenOriginPlayDrawbackAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static AiAbilityDecision hiddenOriginPlayDrawbackAI(final Player aiPlayer, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(aiPlayer);
        if (tgt != null && tgt.canTgtPlayer()) {
            boolean isCurse = sa.isCurse();
            if (isCurse && sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else if (!isCurse && sa.canTarget(aiPlayer)) {
                sa.getTargets().add(aiPlayer);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * <p>
     * changeHiddenTriggerAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static AiAbilityDecision hiddenTriggerAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana

        List<ZoneType> origin = new ArrayList<>();
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }

        // this works for hidden because the mana is paid first.
        final String type = sa.getParam("ChangeType");
        if (!mandatory && sa.getPayCosts().hasXInAnyCostPart() && type != null && type.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            sa.setXManaCostPaid(xPay);
        }

        Iterable<Player> pDefined;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            if (sa.isCurse()) {
                if (sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                } else if (mandatory && sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                }
            } else {
                if (sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                } else if (mandatory && sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                }
            }

            pDefined = sa.getTargets().getTargetPlayers();

            if (Iterables.isEmpty(pDefined)) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }

            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        } else {
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            pDefined = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
        }

        for (final Player p : pDefined) {
            CardCollectionView list = p.getCardsIn(origin);

            // Computer should "know" his deck
            if (p == ai) {
                list = AbilityUtils.filterListByType(list, sa.getParam("ChangeType"), sa);
            }

            if (list.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // *********** Utility functions for Hidden ********************
    /**
     * <p>
     * basicManaFixing.
     * </p>
     * @param ai
     *
     * @param list
     *            a List<Card> object.
     * @return a {@link forge.game.card.Card} object.
     */
    private static Card basicManaFixing(final Player ai, final List<Card> list) { // Search for a Basic Land
        final CardCollectionView combined = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));
        final List<String> basics = new ArrayList<>();

        // what types can I go get?
        for (final String name : MagicColor.Constant.BASIC_LANDS) {
            if (!CardLists.getType(list, name).isEmpty()) {
                basics.add(name);
            }
        }

        // Which basic land is least available from hand and play, that I still
        // have in my deck
        int minSize = Integer.MAX_VALUE;
        String minType = null;

        for (String b : basics) {
            final int num = CardLists.getType(combined, b).size();
            if (num < minSize) {
                minType = b;
                minSize = num;
            }
        }

        List<Card> result = list;
        if (minType != null) {
            result = CardLists.getType(list, minType);
        }

        // pick dual lands if available
        if (result.stream().anyMatch(CardPredicates.NONBASIC_LANDS)) {
            result = CardLists.filter(result, CardPredicates.NONBASIC_LANDS);
        }

        return result.get(0);
    }

    /**
     * <p>
     * areAllBasics.
     * </p>
     *
     * @param types
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static boolean areAllBasics(final String types) {
        for (String ct : types.split(",")) {
            if (!MagicColor.Constant.BASIC_LANDS.contains(ct)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Some logic for picking a creature card from a list.
     * @param list
     * @return Card
     */
    private static Card chooseCreature(final Player ai, CardCollection list) {
        if (ComputerUtil.aiLifeInDanger(ai, false, 0)) {
            // need something AI can cast now
            ComputerUtilCard.sortByEvaluateCreature(list);
            for (Card c : list) {
                if (ComputerUtilMana.hasEnoughManaSourcesToCast(c.getFirstSpellAbility(), ai))
                   return c;
            }
            return null;
        }

        // not urgent, get the largest creature possible
        return ComputerUtilCard.getBestCreatureAI(list);
    }

    // *************************************************************************************
    // **************** Known Origin (Battlefield/Graveyard/Exile) *************************
    // ******* Known origin cards are chosen during casting of the spell (target) **********
    // *************************************************************************************

    /**
     * <p>
     * changeKnownOriginCanPlayAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static AiAbilityDecision knownOriginCanPlayAI(final Player ai, final SpellAbility sa) {
        // Retrieve either this card, or target Cards in Graveyard

        final List<ZoneType> origin = Lists.newArrayList();
        if (sa.hasParam("Origin")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("Origin")));
        } else if (sa.hasParam("TgtZone")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("TgtZone")));
        }

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));

        if (sa.usesTargeting()) {
            if (!isPreferredTarget(ai, sa, false, false)) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        } else {
            // non-targeted retrieval
            final List<Card> retrieval = sa.knownDetermineDefined(sa.getParam("Defined"));

            if (retrieval == null || retrieval.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            // return this card from graveyard: cards like Hammer of Bogardan
            // in general this is cool, but we should add some type of restrictions

            // return this card from battlefield: cards like Blinking Spirit
            // in general this should only be used to protect from Imminent Harm
            // (dying or losing control of)
            if (origin.contains(ZoneType.Battlefield)) {
                if (ai.getGame().getStack().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                final AbilitySub abSub = sa.getSubAbility();
                ApiType subApi = null;
                if (abSub != null) {
                    subApi = abSub.getApi();
                }

                // only use blink or bounce effects
                if (!(destination.equals(ZoneType.Exile)
                        && (subApi == ApiType.DelayedTrigger || subApi == ApiType.ChangeZone || "DelayedBlink".equals(sa.getParam("AILogic"))))
                        && !destination.equals(ZoneType.Hand)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(ai, sa);
                boolean contains = false;
                for (final Card c : retrieval) {
                    if (objects.contains(c)) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            if (destination == ZoneType.Battlefield) {
                if (ComputerUtil.isETBprevented(retrieval.get(0))) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                // predict whether something may put a ETBing creature below zero toughness
                // (e.g. Reassembing Skeleton + Elesh Norn, Grand Cenobite)
                for (final Card c : retrieval) {
                    if (c.isCreature()) {
                        final Card copy = CardCopyService.getLKICopy(c);
                        ComputerUtilCard.applyStaticContPT(c.getGame(), copy, null);
                        if (copy.getNetToughness() <= 0) {
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        }
                    }
                }
                // predict Legendary cards already present
                boolean nothingWillReturn = true;
                for (final Card c : retrieval) {
                    final boolean isCraftSa = sa.isCraft() && sa.getHostCard().equals(c);
                    if (isCraftSa || (!(!c.ignoreLegendRule() && ai.isCardInPlay(c.getName())))) {
                        nothingWillReturn = false;
                        break;
                    }
                }
                if (nothingWillReturn) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
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
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (aiLogic.equals("SurvivalOfTheFittest")) {
            return ph.getNextTurn().equals(ai) && ph.is(PhaseType.END_OF_TURN);
        } else if (aiLogic.equals("Main1") && ph.is(PhaseType.MAIN1, ai)) {
            return true;
        }

        if (sa.isHidden()) {
            return true;
        }

        final List<ZoneType> origin = Lists.newArrayList();
        if (sa.hasParam("Origin")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("Origin")));
        } else if (sa.hasParam("TgtZone")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("TgtZone")));
        }

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));

        // don't return something to your hand if your hand is full of good stuff
        if (destination.equals(ZoneType.Hand) && origin.contains(ZoneType.Graveyard)) {
            final int handSize = ai.getCardsIn(ZoneType.Hand).size();
            if (ph.getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }
            if (ph.getPhase().isBefore(PhaseType.MAIN2) && handSize > 1) {
                return false;
            }
            if (ph.isPlayerTurn(ai) && handSize >= ai.getMaxHandSize()) {
                return false;
            }
        }

        //don't unearth after attacking is possible
        if (sa.isKeyword(Keyword.UNEARTH) && ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }

        if (destination.equals(ZoneType.Library) && origin.contains(ZoneType.Graveyard)) {
            if (ph.getPhase().isBefore(PhaseType.MAIN2)) {
                return false;
            }
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }
        }

        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    /**
     * <p>
     * changeKnownOriginPlayDrawbackAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static AiAbilityDecision knownOriginPlayDrawbackAI(final Player aiPlayer, final SpellAbility sa) {
        if ("MimicVat".equals(sa.getParam("AILogic"))) {
            if (SpecialCardAi.MimicVat.considerExile(aiPlayer, sa)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (!isPreferredTarget(aiPlayer, sa, false, true)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else {
            // if we are here, we have a target
            // so we can play the ability
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
    }

    /**
     * <p>
     * changeKnownPreferredTarget.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean isPreferredTarget(final Player ai, final SpellAbility sa, final boolean mandatory, boolean immediately) {
        final Card source = sa.getHostCard();
        final List<ZoneType> origin = Lists.newArrayList();
        if (sa.hasParam("Origin")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("Origin")));
        } else if (sa.hasParam("TgtZone")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("TgtZone")));
        }

        if (origin.contains(ZoneType.Stack) && doExileSpellLogic(ai, sa, mandatory)) {
            return true;
        }

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final Game game = ai.getGame();

        final AbilitySub abSub = sa.getSubAbility();
        ApiType subApi = null;
        String subAffected = "";
        if (abSub != null) {
            subApi = abSub.getApi();
            if (abSub.hasParam("Defined")) {
                subAffected = abSub.getParam("Defined");
            }
        }

        sa.resetTargets();
        // X controls the minimum targets
        if ("X".equals(sa.getTargetRestrictions().getMinTargets()) && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());

            // TODO need to set XManaCostPaid for targets, maybe doesn't need PayX anymore?
            sa.setXManaCostPaid(xPay);
        }
        CardCollection list = CardLists.getTargetableCards(game.getCardsIn(origin), sa);

        list = ComputerUtil.filterAITgts(sa, ai, list, true);
        if (sa.hasParam("AITgtsOnlyBetterThanSelf")) {
            list = CardLists.filter(list, card -> ComputerUtilCard.evaluateCreature(card) > ComputerUtilCard.evaluateCreature(source) + 30);
        }

        if (source.isInZone(ZoneType.Hand)) {
            list = CardLists.filter(list, CardPredicates.nameNotEquals(source.getName())); // Don't get the same card back.
        }
        if (sa.isSpell()) {
            list.remove(source); // spells can't target their own source, because it's actually in the stack zone
        }

        // list = CardLists.canSubsequentlyTarget(list, sa);

        if (sa.hasParam("AttachedTo")) {
            list = CardLists.filter(list, c -> {
                for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
                    if (card.isValid(sa.getParam("AttachedTo"), ai, c, sa)) {
                        return true;
                    }
                }
                return false;
            });
        }
        if (sa.hasParam("AttachAfter")) {
            list = CardLists.filter(list, c -> {
                for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
                    if (card.isValid(sa.getParam("AttachAfter"), ai, c, sa)) {
                        return true;
                    }
                }
                return false;
            });
        }

        if (list.size() < sa.getMinTargets()) {
            return false;
        }

        immediately = immediately || ComputerUtil.playImmediately(ai, sa);

        if (list.isEmpty() && immediately && sa.getMaxTargets() == 0) {
            return true;
        }

        // Narrow down the list:
        if (origin.contains(ZoneType.Battlefield)) {
            if ("Polymorph".equals(sa.getParam("AILogic"))) {
                list = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);
                if (list.isEmpty()) {
                    return false;
                }
                Card worst = ComputerUtilCard.getWorstAI(list);
                if (worst.isCreature() && ComputerUtilCard.evaluateCreature(worst) >= 200) {
                    return false;
                }
                if (!worst.isCreature() && worst.getCMC() > 1) {
                    return false;
                }
                sa.getTargets().add(worst);
                return true;
            }

            // Combat bouncing
            if (sa.getMinTargets() <= 1) {
                if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    Combat currCombat = game.getCombat();
                    CardCollection attackers = currCombat.getAttackers();
                    ComputerUtilCard.sortByEvaluateCreature(attackers);
                    for (Card attacker : attackers) {
                        CardCollection blockers = currCombat.getBlockers(attacker);
                        // Save my attacker by bouncing a blocker
                        if (attacker.getController().equals(ai) && attacker.getShieldCount() == 0
                                && ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, currCombat)
                                && !currCombat.getBlockers(attacker).isEmpty()) {
                            ComputerUtilCard.sortByEvaluateCreature(blockers);
                            Combat combat = new Combat(ai);
                            combat.addAttacker(attacker, ai.getWeakestOpponent());
                            for (Card blocker : blockers) {
                                combat.addBlocker(attacker, blocker);
                            }
                            for (Card blocker : blockers) {
                                combat.removeFromCombat(blocker);
                                if (!ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, combat) && sa.canTarget(blocker)) {
                                    sa.getTargets().add(blocker);
                                    return true;
                                } else {
                                    combat.addBlocker(attacker, blocker);
                                }
                            }
                        }
                        // Save my blocker by bouncing the attacker (cannot handle blocking multiple attackers)
                        if (attacker.getController().isOpponentOf(ai) && !blockers.isEmpty()) {
                            for (Card blocker : blockers) {
                                if (ComputerUtilCombat.blockerWouldBeDestroyed(ai, blocker, currCombat) && sa.canTarget(attacker)) {
                                    sa.getTargets().add(attacker);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            // if it's blink or bounce, try to save my about to die stuff
            final boolean blink = (destination.equals(ZoneType.Exile) && (subApi == ApiType.DelayedTrigger
                    || "DelayedBlink".equals(sa.getParam("AILogic")) || (subApi == ApiType.ChangeZone && subAffected.equals("Remembered"))));
            if ((destination.equals(ZoneType.Hand) || blink) && (sa.getMinTargets() <= 1)) {
                // save my about to die stuff
                Card tobounce = canBouncePermanent(ai, sa, list);
                if (tobounce != null) {
                    if ("BounceOnce".equals(sa.getParam("AILogic")) && isBouncedThisTurn(ai, tobounce)) {
                        return false;
                    }

                    sa.getTargets().add(tobounce);

                    boolean saheeliFelidarCombo = ComputerUtilAbility.getAbilitySourceName(sa).equals("Felidar Guardian")
                            && tobounce.getName().equals("Saheeli Rai")
                            && CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Felidar Guardian")).size() <
                            CardLists.filter(ai.getOpponents().getCardsIn(ZoneType.Battlefield), CardPredicates.CREATURES).size() + ai.getOpponentsGreatestLifeTotal() + 10;

                    // remember that the card was bounced already unless it's a special combo case
                    if (!saheeliFelidarCombo) {
                        rememberBouncedThisTurn(ai, tobounce);
                    }

                    return true;
                }
                // blink logic: get my own permanents back or blink permanents with ETB effects
                if (blink) {
                    CardCollection blinkTargets = CardLists.filter(list, c -> !c.isToken() && c.getOwner().equals(ai) && (c.getController().isOpponentOf(ai) || c.hasETBTrigger(false)));
                    if (!blinkTargets.isEmpty()) {
                        CardCollection opponentBlinkTargets = CardLists.filterControlledBy(blinkTargets, ai.getOpponents());
                        // prefer post-combat unless targeting opponent's stuff or part of another ability
                        if (immediately || sa.getParent() != null || sa.isTrigger() || !opponentBlinkTargets.isEmpty() || !game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                            while (!blinkTargets.isEmpty() && sa.canAddMoreTarget()) {
                                Card choice = null;
                                // first prefer targeting opponents stuff
                                if (!opponentBlinkTargets.isEmpty()) {
                                    choice = ComputerUtilCard.getBestAI(opponentBlinkTargets);
                                    opponentBlinkTargets.remove(choice);
                                }
                                else {
                                    choice = ComputerUtilCard.getBestAI(blinkTargets);
                                }
                                sa.getTargets().add(choice);
                                blinkTargets.remove(choice);
                            }
                            return true;
                        }
                    }
                }
                // bounce opponent's stuff
                list = CardLists.filterControlledBy(list, ai.getOpponents());
                if (!CardLists.getNotType(list, "Land").isEmpty()) {
                    // When bouncing opponents stuff other than lands, don't bounce cards with CMC 0
                    list = CardLists.filter(list, c -> {
                        for (Card aura : c.getEnchantedBy()) {
                            return aura.getController().isOpponentOf(ai);
                        }
                        if (blink) {
                            return c.isToken();
                        }
                        return c.isToken() || c.getCMC() > 0;
                    });
                }
            }

        } else if (origin.contains(ZoneType.Graveyard)) {
            if (destination.equals(ZoneType.Exile) || destination.equals(ZoneType.Library)) {
                // Don't use these abilities before main 2 if possible
                if (!immediately && game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                        && !sa.hasParam("ActivationPhases") && !ComputerUtil.castSpellInMain1(ai, sa)) {
                    return false;
                }
                if (!immediately && (!game.getPhaseHandler().getNextTurn().equals(ai)
                            || game.getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN))
                        && !sa.hasParam("PlayerTurn") && !isSorcerySpeed(sa, ai)
                        && !ComputerUtil.activateForCost(sa, ai)) {
                    return false;
                }
            } else if (destination.equals(ZoneType.Hand)) {
                // only retrieve cards from computer graveyard
                list = CardLists.filterControlledBy(list, ai);
            } else if (sa.hasParam("AttachedTo")) {
                list = CardLists.filter(list, c -> {
                    for (SpellAbility attach : c.getSpellAbilities()) {
                        if ("Pump".equals(attach.getParam("AILogic"))) {
                            return true; //only use good auras
                        }
                    }
                    return false;
                });
            }
        }

        // blink human targets only during combat
        if (origin.contains(ZoneType.Battlefield)
                && destination.equals(ZoneType.Exile)
                && (subApi == ApiType.DelayedTrigger || (subApi == ApiType.ChangeZone  && subAffected.equals("Remembered")))
                && !(game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS) || sa.isAbility())) {
            return false;
        }

        // Exile and bounce opponents stuff
        if (destination.equals(ZoneType.Exile) || origin.contains(ZoneType.Battlefield)) {
            // don't rush bouncing stuff when not going to attack
            if (!immediately && game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && game.getPhaseHandler().isPlayerTurn(ai)
                    && ai.getCreaturesInPlay().isEmpty()) {
                return false;
            }

            if (!sa.hasParam("AITgtOwnCards")) {
                list = CardLists.filterControlledBy(list, ai.getOpponents());
                list = CardLists.filter(list, c -> {
                    for (Card aura : c.getEnchantedBy()) {
                        if (c.getOwner().isOpponentOf(ai) && aura.getController().equals(ai)) {
                            return false;
                        }
                    }
                    return true;
                });
            }

            // See if maybe there's a special priority applicable for this, in case the opponent
            // has dangerous unblockables in play
            if (CardLists.getNotType(list, "Creature").isEmpty()) {
                list = ComputerUtilCard.prioritizeCreaturesWorthRemovingNow(ai, list, false);
            }
        }

        // Only care about combatants during combat
        if (game.getPhaseHandler().inCombat() && origin.contains(ZoneType.Battlefield)) {
            CardCollection newList = CardLists.getValidCards(list, "Card.attacking,Card.blocking", null, null, null);
            if (!newList.isEmpty() || !sa.isTrigger()) {
                list = newList;
            }
        }

        boolean doWithoutTarget = sa.isPwAbility() && sa.usesTargeting()
                && sa.getMinTargets() == 0
                && sa.getPayCosts().hasSpecificCostType(CostPutCounter.class);

        if (list.isEmpty() && !doWithoutTarget) {
            return false;
        }

        // Check if the opponent can save a creature from bounce/blink/whatever by paying
        // the Unless cost (for example, Erratic Portal)
        list.removeAll(getSafeTargetsIfUnlessCostPaid(ai, sa, list));

        if (!mandatory && list.size() < sa.getTargetRestrictions().getMinTargets(sa.getHostCard(), sa)) {
            return false;
        }

        // target loop
        while (sa.canAddMoreTarget()) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                if (destination.equals(ZoneType.Battlefield) || origin.contains(ZoneType.Battlefield)) {
                    // filter by MustTarget requirement
                    CardCollection originalList = new CardCollection(list);
                    boolean mustTargetFiltered = StaticAbilityMustTarget.filterMustTargetCards(ai, list, sa);

                    final Card mostExpensive = ComputerUtilCard.getMostExpensivePermanentAI(list);
                    if (mostExpensive.isCreature()) {
                        // if a creature is most expensive take the best one
                        if (destination.equals(ZoneType.Exile)) {
                            // If Exiling things, don't give bonus to Tokens
                            choice = ComputerUtilCard.getBestCreatureAI(list);
                        } else if (origin.contains(ZoneType.Graveyard)) {
                            choice = mostExpensive;
                            // Karmic Guide can chain another creature
                            for (Card c : list) {
                                if ("Karmic Guide".equals(c.getName())) {
                                    choice = c;
                                    break;
                                }
                            }
                        } else {
                            choice = ComputerUtilCard.getBestCreatureToBounceAI(list);
                        }
                    } else {
                        choice = mostExpensive;
                    }

                    //option to hold removal instead only applies for single targeted removal
                    if (!immediately && sa.getMaxTargets() == 1) {
                        if (!ComputerUtilCard.useRemovalNow(sa, choice, 0, destination)) {
                            return false;
                        }
                    }

                    // Restore original list for next loop if filtered by MustTarget requirement
                    if (mustTargetFiltered) {
                        list = originalList;
                    }
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            CardLists.sortByCmcDesc(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtilMana.hasEnoughManaSourcesToCast(potentialCard.getFirstSpellAbility(), ai)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            choice = ComputerUtilCard.getBestAI(nonLands);
                        }
                    }
                    if (choice == null) {
                        // No creatures or spells?
                        CardLists.shuffle(list);
                        choice = list.get(0);
                    }
                } else {
                    choice = ComputerUtilCard.getBestAI(list);
                }
            }
            if (choice == null) { // can't find anything left
                if (sa.getTargets().isEmpty() || !sa.isTargetNumberValid()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    if (!doWithoutTarget) {
                        return false;
                    } else {
                        break;
                    }
                } else {
                    if (!sa.isTrigger() && !ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        boolean aiTgtsOK = false;
                        if (sa.hasParam("AIMinTgts")) {
                            int minTgts = Integer.parseInt(sa.getParam("AIMinTgts"));
                            if (sa.getTargets().size() >= minTgts) {
                                aiTgtsOK = true;
                            }
                        }
                        if (!aiTgtsOK) {
                            return false;
                        }
                    }
                    break;
                }
            }

            // if max CMC exceeded, do not choose this card (but keep looking for other options)
            if (sa.hasParam("MaxTotalTargetCMC")) {
                if (choice.getCMC() > sa.getTargetRestrictions().getMaxTotalCMC(choice, sa) - sa.getTargets().getTotalTargetedCMC()) {
                    list.remove(choice);
                    continue;
                }
            }

            // if max power exceeded, do not choose this card (but keep looking for other options)
            if (sa.hasParam("MaxTotalTargetPower")) {
                if (choice.getNetPower() > sa.getTargetRestrictions().getMaxTotalPower(choice, sa) -sa.getTargets().getTotalTargetedPower()) {
                    list.remove(choice);
                    continue;
                }
            }

            // honor the Same Creature Type restriction
            if (sa.getTargetRestrictions().isWithSameCreatureType()) {
                Card firstTarget = sa.getTargetCard();
                if (firstTarget != null && !choice.sharesCreatureTypeWith(firstTarget)) {
                    list.remove(choice);
                    continue;
                }
            }

            list.remove(choice);
            if (sa.canTarget(choice)) {
                sa.getTargets().add(choice);
            }
        }

        // Honor the Single Zone restriction. For now, simply remove targets that do not belong to the same zone as the first targeted card.
        // TODO: ideally the AI should consider at this point which targets exactly to pick (e.g. one card in the first player's graveyard
        // vs. two cards in the second player's graveyard, which cards are more relevant to be targeted, etc.). Consider improving.
        if (sa.getTargetRestrictions().isSingleZone()) {
            Card firstTgt = sa.getTargetCard();
            CardCollection toRemove = new CardCollection();
            if (firstTgt != null) {
                for (Card t : sa.getTargets().getTargetCards()) {
                    if (!t.getController().equals(firstTgt.getController())) {
                        toRemove.add(t);
                    }
                }
                sa.getTargets().removeAll(toRemove);
            }
        }

        return true;
    }

    /**
     * Checks if a permanent threatened by a stack ability or in combat can
     * be saved by bouncing.
     * @param ai controlling player
     * @param sa ChangeZone ability
     * @param list possible targets
     * @return target to bounce, null if no good targets
     */
    private static Card canBouncePermanent(final Player ai, SpellAbility sa, CardCollectionView list) {
        Game game = ai.getGame();
        // filter out untargetables
        CardCollectionView aiPermanents = CardLists.filterControlledBy(list, ai);
        CardCollection aiPlaneswalkers = CardLists.filter(aiPermanents, CardPredicates.PLANESWALKERS);

        // Felidar Guardian + Saheeli Rai combo support
        if (sa.getHostCard().getName().equals("Felidar Guardian")) {
            CardCollectionView saheeli = ai.getCardsIn(ZoneType.Battlefield, "Saheeli Rai");
            if (!saheeli.isEmpty()) {
                return saheeli.get(0);
            }
        }

        // Don't blink cards that will die.
        aiPermanents = ComputerUtil.getSafeTargets(ai, sa, aiPermanents);
        if (!game.getStack().isEmpty()) {
            final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(ai, sa);
            final List<Card> threatenedTargets = Lists.newArrayList(aiPermanents);
            threatenedTargets.retainAll(objects);

            if (!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
                return ComputerUtilCard.getBestAI(threatenedTargets);
            }
        }
        // Save combatants
        else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            Combat combat = game.getCombat();
            final CardCollection combatants = CardLists.filter(aiPermanents,
                    CardPredicates.CREATURES);
            ComputerUtilCard.sortByEvaluateCreature(combatants);

            for (final Card c : combatants) {
                if (c.getShieldCount() == 0
                        && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c,
                                combat) && c.getOwner() == ai && !c.isToken()) {
                    return c;
                }
            }
        }
        // Reload planeswalkers
        else if (!aiPlaneswalkers.isEmpty() && (sa.getHostCard().isSorcery() || !game.getPhaseHandler().isPlayerTurn(ai))) {
            int maxLoyaltyToConsider = 2;
            int loyaltyDiff = 2;
            int chance = 30;
            if (ai.getController().isAI()) {
                AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
                maxLoyaltyToConsider = aic.getIntProperty(AiProps.BLINK_RELOAD_PLANESWALKER_MAX_LOYALTY);
                loyaltyDiff = aic.getIntProperty(AiProps.BLINK_RELOAD_PLANESWALKER_LOYALTY_DIFF);
                chance = aic.getIntProperty(AiProps.BLINK_RELOAD_PLANESWALKER_CHANCE);
            }
            if (MyRandom.percentTrue(chance)) {
                aiPlaneswalkers.sort(CardPredicates.compareByCounterType(CounterEnumType.LOYALTY));
                for (Card pw : aiPlaneswalkers) {
                    int curLoyalty = pw.getCounters(CounterEnumType.LOYALTY);
                    int freshLoyalty = Integer.parseInt(pw.getCurrentState().getBaseLoyalty());
                    if (freshLoyalty - curLoyalty >= loyaltyDiff && curLoyalty <= maxLoyaltyToConsider) {
                        return pw;
                    }
                }
            }
        }
        // Reload Undying and Persist, get rid of -1/-1 counters, get rid of enemy auras if able
        Card bestChoice = null;
        int bestEval = 0;
        for (Card c : aiPermanents) {
            if (c.isCreature()) {
                boolean hasValuableAttachments = false;
                boolean hasOppAttachments = false;
                int numNegativeCounters = 0;
                int numTotalCounters = 0;
                for (Card attached : c.getAttachedCards()) {
                    if (attached.isAura()) {
                        if (attached.getController() == c.getController()) {
                            hasValuableAttachments = true;
                        } else if (attached.getController().isOpponentOf(c.getController())) {
                            hasOppAttachments = true;
                        }
                    }
                }
                Map<CounterType, Integer> counters = c.getCounters();
                for (CounterType ct : counters.keySet()) {
                    int amount = counters.get(ct);
                    if (ComputerUtil.isNegativeCounter(ct, c)) {
                        numNegativeCounters += amount;
                    }
                    numTotalCounters += amount;
                }
                if (hasValuableAttachments || (ComputerUtilCard.isUselessCreature(ai, c) && !hasOppAttachments)) {
                    continue;
                }

                Card considered = null;
                if ((c.hasKeyword(Keyword.PERSIST) || c.hasKeyword(Keyword.UNDYING))
                        && !ComputerUtilCard.hasActiveUndyingOrPersist(c)) {
                    considered = c;
                } else if (hasOppAttachments || (numTotalCounters > 0 && numNegativeCounters > numTotalCounters / 2)) {
                    considered = c;
                }

                if (considered != null) {
                    int eval = ComputerUtilCard.evaluateCreature(c);
                    if (eval > bestEval) {
                        bestEval = eval;
                        bestChoice = considered;
                    }
                }
            }
        }

        return bestChoice;
    }

    private static boolean isUnpreferredTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (!mandatory) {
            if (!"Always".equals(sa.getParam("AILogic"))) {
                return false;
            }
        }

        final Card source = sa.getHostCard();
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        List<Card> list = CardUtil.getValidCardsToTarget(sa);

        if (list.isEmpty()) {
            return false;
        }

        // target loop
        while (!sa.isMinTargetChosen()) {
            // AI Targeting
            Card choice = null;

            // Filter out cards TargetsForEachPlayer
            list = CardLists.canSubsequentlyTarget(list, sa);

            if (!list.isEmpty()) {
                Card mostExpensivePermanent = ComputerUtilCard.getMostExpensivePermanentAI(list);
                if (mostExpensivePermanent.isCreature()
                        && (destination.equals(ZoneType.Battlefield) || tgt.getZone().contains(ZoneType.Battlefield))) {
                    // if a creature is most expensive take the best
                    choice = ComputerUtilCard.getBestCreatureToBounceAI(list);
                } else if (destination.equals(ZoneType.Battlefield) || tgt.getZone().contains(ZoneType.Battlefield)) {
                    choice = mostExpensivePermanent;
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            CardLists.sortByCmcDesc(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtilMana.hasEnoughManaSourcesToCast(potentialCard.getFirstSpellAbility(), ai)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            choice = ComputerUtilCard.getBestAI(nonLands);
                        }
                    }
                    if (choice == null) {
                        // No creatures or spells?
                        CardLists.shuffle(list);
                        choice = list.get(0);
                    }
                } else {
                    choice = ComputerUtilCard.getBestAI(list);
                }
            }
            if (choice == null) { // can't find anything left
                if (sa.getTargets().isEmpty() || sa.getTargets().size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
                    sa.resetTargets();
                    return false;
                } 
                if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                    return false;
                }
                break;
            }

            list.remove(choice);
            sa.getTargets().add(choice);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownOriginTriggerAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static AiAbilityDecision knownOriginTriggerAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final String logic = sa.getParamOrDefault("AILogic", "");

        if ("DeathgorgeScavenger".equals(logic)) {
            if (SpecialCardAi.DeathgorgeScavenger.consider(ai, sa)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if ("ExtraplanarLens".equals(logic)) {
            if (SpecialCardAi.ExtraplanarLens.consider(ai, sa)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if ("ExileCombatThreat".equals(logic)) {
            return doExileCombatThreatLogic(ai, sa);
        }

        if (!sa.usesTargeting()) {
            // Just in case of Defined cases
            if (!mandatory && sa.hasParam("AttachedTo")) {
                final List<Card> list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("AttachedTo"), sa);
                if (!list.isEmpty()) {
                    final Card attachedTo = list.get(0);
                    // This code is for the Dragon auras
                    if (!attachedTo.getController().isOpponentOf(ai)) {
                        // If the AI is not the controller of the attachedTo card, then it is not a valid target.
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    } else {
                        // If the AI is the controller of the attachedTo card, then it is a valid target.
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
            }
        } else if (isPreferredTarget(ai, sa, mandatory, true)) {
            // do nothing
        } else {
            if (isUnpreferredTarget(ai, sa, mandatory)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                // If the AI is not the controller of the attachedTo card, then it is not a valid target.
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static Card chooseCardToHiddenOriginChangeZone(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, Player player, final Player decider) {
        if (fetchList.isEmpty()) {
            return null;
        }
        if (sa.hasParam("AILogic")) {
            String logic = sa.getParamOrDefault("AILogic", "");
            if ("NeverBounceItself".equals(logic)) {
                Card source = sa.getHostCard();
                if (fetchList.contains(source) && (fetchList.size() > 1 || !sa.getRootAbility().isMandatory())) {
                    // For cards that should never be bounced back to hand with their own [e.g. triggered] abilities, such as guild lands.
                    fetchList.remove(source);
                }
            } else if ("WorstCard".equals(logic)) {
                return ComputerUtilCard.getWorstAI(fetchList);
            } else if ("BestCard".equals(logic)) {
                return ComputerUtilCard.getBestAI(fetchList); // generally also means the most expensive one or close to it
            } else if ("Mairsil".equals(logic)) {
                return SpecialCardAi.MairsilThePretender.considerCardFromList(fetchList);
            } else if ("SurvivalOfTheFittest".equals(logic)) {
                return SpecialCardAi.SurvivalOfTheFittest.considerCardToGet(decider, sa);
            } else if ("MazesEnd".equals(logic)) {
                return SpecialCardAi.MazesEnd.considerCardToGet(decider, sa);
            } else if ("Intuition".equals(logic)) {
                if (!multipleCardsToChoose.isEmpty()) {
                    Card choice = multipleCardsToChoose.get(0);
                    multipleCardsToChoose.remove(0);
                    return choice;
                }
            } else if (logic.startsWith("ExilePreference")) {
                return doExilePreferenceLogic(decider, sa, fetchList);
            } else if (logic.equals("BounceOwnTrigger")) {
                return doBounceOwnTriggerLogic(decider, sa, fetchList);
            }
        }
        if (fetchList.isEmpty()) {
            return null;
        }
        String type = sa.getParam("ChangeType");
        if (type == null) {
            type = "Card";
        }

        Card c = null;
        final Player activator = sa.getActivatingPlayer();

        CardLists.shuffle(fetchList);
        // Save a card as a default, in case we can't find anything suitable.
        Card first = fetchList.get(0);
        if (ZoneType.Battlefield.equals(destination)) {
            fetchList = CardLists.filter(fetchList, c1 -> {
                if (c1.getType().isLegendary()) {
                    return !decider.isCardInPlay(c1.getName());
                }
                return true;
            });
            if (player.isOpponentOf(decider) && sa.hasParam("GainControl") && activator.equals(decider)) {
                fetchList = CardLists.filter(fetchList, c12 -> !ComputerUtilCard.isCardRemAIDeck(c12) && !ComputerUtilCard.isCardRemRandomDeck(c12));
            }
        }
        if (ZoneType.Exile.equals(destination) || origin.contains(ZoneType.Battlefield)
                || (ZoneType.Library.equals(destination) && origin.contains(ZoneType.Hand))) {
            // Exiling or bouncing stuff
            if (player.isOpponentOf(decider)) {
                c = ComputerUtilCard.getBestAI(fetchList);
            } else {
                if (!sa.hasParam("Mandatory") && origin.contains(ZoneType.Battlefield) && sa.hasParam("ChangeNum")) {
                    // exclude tokens, they won't come back, and enchanted stuff, since auras will go away
                    fetchList = prefilterOwnListForBounceAnyNum(fetchList, decider);
                    if (fetchList.isEmpty()) {
                        return null;
                    }
                }

                c = ComputerUtilCard.getWorstAI(fetchList);
                if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Temur Sabertooth")) {
                    Card tobounce = canBouncePermanent(player, sa, fetchList);
                    if (tobounce != null) {
                        c = tobounce;
                        rememberBouncedThisTurn(player, c);
                    }
                }
            }
        } else if (origin.contains(ZoneType.Library) && (type.contains("Basic") || areAllBasics(type))) {
            c = basicManaFixing(decider, fetchList);
        } else if (ZoneType.Hand.equals(destination) && CardLists.getNotType(fetchList, "Creature").isEmpty()) {
            c = chooseCreature(decider, fetchList);
        } else if (ZoneType.Battlefield.equals(destination) || ZoneType.Graveyard.equals(destination)) {
            if (!activator.equals(decider) && sa.hasParam("GainControl")) {
                c = ComputerUtilCard.getWorstAI(fetchList);
            } else {
                c = ComputerUtilCard.getBestAI(fetchList);
            }
        } else {
            // Don't fetch another tutor with the same name
            CardCollection sameNamed = CardLists.filter(fetchList, CardPredicates.nameNotEquals(ComputerUtilAbility.getAbilitySourceName(sa)));
            if (origin.contains(ZoneType.Library) && !sameNamed.isEmpty()) {
                fetchList = sameNamed;
            }

            // Does AI need a land?
            CardCollectionView hand = decider.getCardsIn(ZoneType.Hand);
            if (!hand.anyMatch(CardPredicates.LANDS) && CardLists.count(decider.getCardsIn(ZoneType.Battlefield), CardPredicates.LANDS) < 4) {
                boolean canCastSomething = false;
                for (Card cardInHand : hand) {
                    canCastSomething = canCastSomething || ComputerUtilMana.hasEnoughManaSourcesToCast(cardInHand.getFirstSpellAbility(), decider);
                }
                if (!canCastSomething) {
                    c = basicManaFixing(decider, fetchList);
                }
            }
            if (c == null) {
                if (fetchList.allMatch(CardPredicates.LANDS)) {
                    // we're only choosing from lands, so get the best land
                    c = ComputerUtilCard.getBestLandAI(fetchList);
                } else {
                    fetchList = CardLists.getNotType(fetchList, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    c = chooseCreature(decider, CardLists.filter(fetchList, CardPredicates.CREATURES));
                }
            }
            if (c == null) { // Could not find a creature.
                if (decider.getLife() <= 5) { // Desperate?
                    // Get something AI can cast soon.
                    CardLists.sortByCmcDesc(fetchList);
                    for (Card potentialCard : fetchList) {
                       if (ComputerUtilMana.hasEnoughManaSourcesToCast(potentialCard.getFirstSpellAbility(), decider)) {
                           c = potentialCard;
                           break;
                       }
                    }
                } else {
                    // Get the best card in there.
                    c = ComputerUtilCard.getBestAI(fetchList);
                }
            }
        }
        if (c == null) {
            c = first;
        }
        return c;
    }

    private static CardCollection prefilterOwnListForBounceAnyNum(CardCollection fetchList, Player decider) {
        fetchList = CardLists.filter(fetchList, card -> {
            if (card.isToken()) {
                return false;
            }
            if (card.isCreature() && ComputerUtilCard.isUselessCreature(decider, card)) {
                return true;
            }
            if (card.isEquipped()) {
                return false;
            }
            if (card.isEnchanted()) {
                for (Card enc : card.getEnchantedBy()) {
                    if (enc.getOwner().isOpponentOf(decider)) {
                        return true;
                    }
                }
                return false;
            }
            if (card.hasCounters()) {
                if (card.isPlaneswalker()) {
                    int maxLoyaltyToConsider = 2;
                    int loyaltyDiff = 2;
                    int chance = 30;
                    if (decider.getController().isAI()) {
                        AiController aic = ((PlayerControllerAi) decider.getController()).getAi();
                        maxLoyaltyToConsider = aic.getIntProperty(AiProps.BLINK_RELOAD_PLANESWALKER_MAX_LOYALTY);
                        loyaltyDiff = aic.getIntProperty(AiProps.BLINK_RELOAD_PLANESWALKER_LOYALTY_DIFF);
                        chance = aic.getIntProperty(AiProps.BLINK_RELOAD_PLANESWALKER_CHANCE);
                    }
                    if (MyRandom.percentTrue(chance)) {
                        int curLoyalty = card.getCounters(CounterEnumType.LOYALTY);
                        int freshLoyalty = Integer.parseInt(card.getCurrentState().getBaseLoyalty());
                        if (freshLoyalty - curLoyalty >= loyaltyDiff && curLoyalty <= maxLoyaltyToConsider) {
                            return true;
                        }
                    }
                } else if (card.isCreature() && card.getCounters(CounterEnumType.M1M1) > 0) {
                    return true;
                }
                return false; // TODO: improve for other counters
            } else if (card.isAura()) {
                return false;
            }
            return true;
        });

        return fetchList;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        // AI was never asked
        return true;
    }

    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // Called when looking for creature to attach aura or equipment
        return AttachAi.attachGeneralAI(ai, sa, (List<Card>)options, !isOptional, (Card) params.get("Attach"), sa.getParam("AILogic"));
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List)
     */
    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        // Called when attaching Aura to player or adding creature to combat
        if (params != null && params.containsKey("Attacker")) {
            return (Player) ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), options);
        }
        return AttachAi.attachToPlayerAIPreferences(ai, sa, true, (List<Player>)options);
    }

    @Override
    protected GameEntity chooseSingleAttackableEntity(Player ai, SpellAbility sa, Iterable<GameEntity> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), options);
        }
        // should not be reached
        return super.chooseSingleAttackableEntity(ai, sa, options, params);
    }

    private AiAbilityDecision doSacAndReturnFromGraveLogic(final Player ai, final SpellAbility sa) {
        Card source = sa.getHostCard();
        String definedSac = StringUtils.split(source.getSVar("AIPreference"), "$")[1];

        CardCollection listToSac = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), definedSac, ai, source, sa);
        listToSac.sort(Collections.reverseOrder(CardLists.CmcComparatorInv));

        CardCollection listToRet = CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), CardPredicates.CREATURES);
        listToRet.sort(CardLists.CmcComparatorInv);

        if (!listToSac.isEmpty() && !listToRet.isEmpty()) {
            Card worstSac = listToSac.getFirst();
            Card bestRet = listToRet.getFirst();

            if (bestRet.getCMC() > worstSac.getCMC()
                    && ComputerUtilCard.evaluateCreature(bestRet) > ComputerUtilCard.evaluateCreature(worstSac)) {
                sa.resetTargets();
                sa.getTargets().add(bestRet);
                source.setSVar("AIPreferenceOverride", "Creature.cmcEQ" + worstSac.getCMC());
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }

    private AiAbilityDecision doSacAndUpgradeLogic(final Player ai, final SpellAbility sa) {
        Card source = sa.getHostCard();
        PhaseHandler ph = ai.getGame().getPhaseHandler();
        String logic = sa.getParam("AILogic");
        boolean sacWorst = logic.contains("SacWorst");

        if (!ph.is(PhaseType.MAIN2)) {
            // Should be given a chance to cast other spells as well as to use a previously upgraded creature
            return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
        }

        String definedSac = StringUtils.split(source.getSVar("AIPreference"), "$")[1];
        String definedGoal = sa.getParam("ChangeType");
        boolean anyCMC = !definedGoal.contains(".cmc");

        CardCollection listToSac = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), definedSac, ai, source, sa);
        listToSac.sort(!sacWorst ? CardLists.CmcComparatorInv : Collections.reverseOrder(CardLists.CmcComparatorInv));

        for (Card sacCandidate : listToSac) {
            int sacCMC = sacCandidate.getCMC();

            int goalCMC = source.hasSVar("X") ? AbilityUtils.calculateAmount(source, source.getSVar("X").replace("Sacrificed$CardManaCost", "Number$" + sacCMC), sa) : sacCMC + 1;
            String curGoal = definedGoal;

            if (!anyCMC) {
                // TODO: improve the detection of X in the "cmc**X" part to avoid clashing with other letters in the definition
                curGoal = definedGoal.replace("X", String.format("%d", goalCMC));
            }

            CardCollection listGoal = CardLists.getValidCards(ai.getCardsIn(ZoneType.Library), curGoal, ai, source, sa);

            if (!anyCMC) {
                listGoal = CardLists.getValidCards(listGoal, curGoal, source.getController(), source, sa);
            } else {
                listGoal = CardLists.getValidCards(listGoal, curGoal + (curGoal.contains(".") ? "+" : ".") + "cmcGE" + goalCMC, source.getController(), source, sa);
            }

            listGoal = CardLists.filter(listGoal, c -> {
                if (c.getType().isLegendary()) {
                    return !ai.isCardInPlay(c.getName());
                }
                return true;
            });

            if (!listGoal.isEmpty()) {
                // make sure we're upgrading sacCMC->goalCMC
                source.setSVar("AIPreferenceOverride", "Creature.cmcEQ" + sacCMC);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    public AiAbilityDecision doReturnCommanderLogic(SpellAbility sa, Player aiPlayer) {
        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams = (Map<AbilityKey, Object>)sa.getReplacingObject(AbilityKey.OriginalParams);
        SpellAbility causeSa = (SpellAbility)originalParams.get(AbilityKey.Cause);
        SpellAbility causeSub = null;
        ZoneType destination = (ZoneType)originalParams.get(AbilityKey.Destination);

        if (Objects.equals(ZoneType.Hand, destination)) {
            // If the commander is being moved to your hand, don't replace since its easier to cast it again
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // Squee, the Immortal: easier to recast it (the call below has to be "contains" since SA is an intrinsic effect)
        if (sa.getHostCard().getName().contains("Squee, the Immortal") &&
                (destination == ZoneType.Graveyard || destination == ZoneType.Exile)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (causeSa != null && (causeSub = causeSa.getSubAbility()) != null) {
            ApiType subApi = causeSub.getApi();

            if (subApi == ApiType.ChangeZone && "Exile".equals(causeSub.getParam("Origin"))
                    && "Battlefield".equals(causeSub.getParam("Destination"))) {
                // A blink effect implemented using ChangeZone API
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else // This is an intrinsic effect that blinks the card (e.g. Obzedat, Ghost Council), no need to
                // return the commander to the Command zone.
                if (subApi == ApiType.DelayedTrigger) {
                    SpellAbility exec = causeSub.getAdditionalAbility("Execute");
                if (exec != null && exec.getApi() == ApiType.ChangeZone) {
                    // A blink effect implemented using a delayed trigger
                    if (!"Exile".equals(exec.getParam("Origin")) || !"Battlefield".equals(exec.getParam("Destination"))) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    } else {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
            } else {
                    if (causeSa.getHostCard() == null || !causeSa.getHostCard().equals(sa.getReplacingObject(AbilityKey.Card))
                            || !causeSa.getActivatingPlayer().equals(aiPlayer)) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    } else {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
        }

        // Normally we want the commander back in Command zone to recast it later
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static AiAbilityDecision doExileCombatThreatLogic(final Player aiPlayer, final SpellAbility sa) {
        final Combat combat = aiPlayer.getGame().getCombat();

        if (combat == null) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }

        Card choice = null;
        int highestEval = -1;
        if (combat.getAttackingPlayer().isOpponentOf(aiPlayer)) {
            for (Card attacker : combat.getAttackers()) {
                if (sa.canTarget(attacker)) {
                    int eval = ComputerUtilCard.evaluateCreature(attacker);
                    if (combat.isUnblocked(attacker)) {
                        eval += 100; // TODO: make this smarter
                    }
                    if (eval > highestEval) {
                        highestEval = eval;
                        choice = attacker;
                    }
                }
            }
        } else {
            // either the current AI player or one of its teammates is attacking, the opponent(s) are blocking
            for (Card blocker : combat.getAllBlockers()) {
                if (sa.canTarget(blocker)) {
                    if (blocker.getController().isOpponentOf(aiPlayer)) { // TODO: unnecessary sanity check?
                        int eval = ComputerUtilCard.evaluateCreature(blocker);
                        if (eval > highestEval) {
                            highestEval = eval;
                            choice = blocker;
                        }
                    }
                }
            }
        }

        if (choice != null) {
            sa.getTargets().add(choice);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }

    public static Card doExilePreferenceLogic(final Player aiPlayer, final SpellAbility sa, CardCollection fetchList) {
        // Filter by preference. If nothing is preferred, choose the best/worst/random target for the opponent
        // or for the AI depending on the settings. This logic must choose at least something if at all possible,
        // since it's called from chooseSingleCard.
        if (fetchList.isEmpty()) {
            return null; // there was nothing to choose at all
        }

        final Card host = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");
        final String valid = logic.split(":")[1];
        final boolean isCurse = logic.contains("Curse");
        final boolean isOwnOnly = logic.contains("OwnOnly");
        final boolean isWorstChoice = logic.contains("Worst");
        final boolean isRandomChoice = logic.contains("Random");

        if (logic.endsWith("HighestCMC")) {
            return ComputerUtilCard.getMostExpensivePermanentAI(fetchList);
        } else if (logic.contains("MostProminent")) {
            CardCollection scanList = new CardCollection();
            if (logic.endsWith("OwnType")) {
                scanList.addAll(aiPlayer.getCardsIn(ZoneType.Library));
                scanList.addAll(aiPlayer.getCardsIn(ZoneType.Hand));
            } else if (logic.endsWith("OppType")) {
                // this assumes that the deck list is known to the AI before the match starts,
                // so it's possible to figure out what remains in library/hand if you know what's
                // in graveyard, exile, etc.
                scanList.addAll(aiPlayer.getOpponents().getCardsIn(ZoneType.Library));
                scanList.addAll(aiPlayer.getOpponents().getCardsIn(ZoneType.Hand));
            }

            if (logic.contains("NonLand")) {
                scanList = CardLists.filter(scanList, CardPredicates.NON_LANDS);
            }

            if (logic.contains("NonExiled")) {
                CardCollection exiledBy = new CardCollection();
                for (Card exiled : aiPlayer.getGame().getCardsIn(ZoneType.Exile)) {
                    if (exiled.getExiledWith() != null && exiled.getExiledWith().getName().equals(host.getName())) {
                        exiledBy.add(exiled);
                    }
                }
                scanList = CardLists.filter(scanList, card -> {
                    if (exiledBy.isEmpty()) {
                        return true;
                    }
                    for (Card c : exiledBy) {
                        return !c.getType().sharesCardTypeWith(card.getType());
                    }
                    return true;
                });
            }

            CardCollectionView graveyardList = aiPlayer.getGame().getCardsIn(ZoneType.Graveyard);
            Set<CardType.CoreType> presentTypes = new HashSet<>();
            for (Card inGrave : graveyardList) {
                for(CardType.CoreType type : inGrave.getType().getCoreTypes()) {
                    presentTypes.add(type);
                }
            }

            final Map<CardType.CoreType, Integer> typesInDeck = Maps.newHashMap();
            for (final Card c : scanList) {
                for (CardType.CoreType ct : c.getType().getCoreTypes()) {
                    if (presentTypes.contains(ct)) {
                        Integer count = typesInDeck.get(ct);
                        if (count == null) {
                            count = 0;
                        }
                        typesInDeck.put(ct, count + 1);
                    }
                }
            }
            int max = 0;
            CardType.CoreType maxType = CardType.CoreType.Land;

            for (final Map.Entry<CardType.CoreType, Integer> entry : typesInDeck.entrySet()) {
                final CardType.CoreType type = entry.getKey();

                if (max < entry.getValue()) {
                    max = entry.getValue();
                    maxType = type;
                }
            }

            final CardType.CoreType determinedMaxType = maxType;
            CardCollection preferredList = CardLists.filter(fetchList, card -> card.getType().hasType(determinedMaxType));
            CardCollection preferredOppList = CardLists.filter(preferredList, CardPredicates.isControlledByAnyOf(aiPlayer.getOpponents()));

            if (!preferredOppList.isEmpty()) {
                return Aggregates.random(preferredOppList);
            } else if (!preferredList.isEmpty()) {
                return Aggregates.random(preferredList);
            }

            return Aggregates.random(fetchList);
        }

        CardCollection preferredList = CardLists.filter(fetchList, card -> {
            boolean playerPref = true;
            if (isCurse) {
                playerPref = card.getController().isOpponentOf(aiPlayer);
            } else if (isOwnOnly) {
                playerPref = card.getController().equals(aiPlayer) || !card.getController().isOpponentOf(aiPlayer);
            }

            if (!playerPref) {
                return false;
            }

            return card.isValid(valid, aiPlayer, host, sa); // for things like ExilePreference:Land.Basic
        });

        if (!preferredList.isEmpty()) {
            if (isRandomChoice) {
                return Aggregates.random(preferredList);
            }
            return isWorstChoice ? ComputerUtilCard.getWorstAI(preferredList) : ComputerUtilCard.getBestAI(preferredList);
        } else {
            if (isRandomChoice) {
                return Aggregates.random(preferredList);
            }
            return isWorstChoice ? ComputerUtilCard.getWorstAI(fetchList) : ComputerUtilCard.getBestAI(fetchList);
        }
    }

    private static boolean doExileSpellLogic(final Player ai, final SpellAbility sa, final boolean mandatory) {
        List<ApiType> dangerousApi = null;
        CardCollection spells = new CardCollection(ai.getGame().getStackZone().getCards());
        Collections.reverse(spells);
        if (!mandatory && !spells.isEmpty()) {
            spells = spells.subList(0, 1);
            spells = ComputerUtil.filterAITgts(sa, ai, spells, true);
            dangerousApi = Arrays.asList(ApiType.DealDamage, ApiType.DamageAll, ApiType.Destroy, ApiType.DestroyAll, ApiType.Sacrifice, ApiType.SacrificeAll);
        }

        for (Card c : spells) {
            SpellAbility topSA = ai.getGame().getStack().getSpellMatchingHost(c);
            if (topSA != null && (dangerousApi == null ||
                    (dangerousApi.contains(topSA.getApi()) && topSA.getActivatingPlayer().isOpponentOf(ai)))
                    && sa.canTarget(topSA)) {
                sa.resetTargets();
                sa.getTargets().add(topSA);
                return sa.isTargetNumberValid();
            }
        }
        return false;
    }

    private static CardCollection getSafeTargetsIfUnlessCostPaid(Player ai, SpellAbility sa, Iterable<Card> potentialTgts) {
        // Determines if the controller of each potential target can negate the ChangeZone effect
        // by paying the Unless cost. Returns the list of targets that can be saved that way.
        final Card source = sa.getHostCard();
        final CardCollection canBeSaved = new CardCollection();

        for (Card potentialTgt : potentialTgts) {
            String unlessCost = sa.hasParam("UnlessCost") ? sa.getParam("UnlessCost").trim() : null;

            if (unlessCost != null && !unlessCost.endsWith(">")) {
                Player opp = potentialTgt.getController();
                int usableManaSources = ComputerUtilMana.getAvailableManaEstimate(opp);

                int toPay = 0;
                boolean setPayX = false;
                if (unlessCost.equals("X") && sa.getSVar(unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtilCost.getMaxXValue(sa, ai, true);
                } else {
                    toPay = AbilityUtils.calculateAmount(source, unlessCost, sa);
                }

                if (toPay == 0 || toPay <= usableManaSources) {
                    canBeSaved.add(potentialTgt);
                }

                if (setPayX) {
                    sa.setXManaCostPaid(toPay);
                }
            }
        }

        return canBeSaved;
    }

    private static void rememberBouncedThisTurn(Player ai, Card c) {
        AiCardMemory.rememberCard(ai, c, AiCardMemory.MemorySet.BOUNCED_THIS_TURN);
    }

    private static boolean isBouncedThisTurn(Player ai, Card c) {
        return AiCardMemory.isRememberedCard(ai, c, AiCardMemory.MemorySet.BOUNCED_THIS_TURN);
    }

    private static Card doBounceOwnTriggerLogic(Player ai, SpellAbility sa, CardCollection choices) {
        CardCollection unprefChoices = CardLists.filter(choices, c -> !c.isToken() && c.getOwner().equals(ai));
        // TODO check for threatened cards
        CardCollection prefChoices = CardLists.filter(unprefChoices, c -> c.hasETBTrigger(false));
        if (!prefChoices.isEmpty()) {
            return ComputerUtilCard.getBestAI(prefChoices);
        }
        if (!unprefChoices.isEmpty() && sa.getSubAbility() != null) {
            // some extra benefit like First Responder
            return ComputerUtilCard.getWorstAI(unprefChoices);
        }
        return null;
    }

    @Override
    public boolean willPayUnlessCost(SpellAbility sa, Player payer, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card host = sa.getHostCard();

        int lifeLoss = 0;
        if (cost.hasSpecificCostType(CostDamage.class)) {
            if (!payer.canLoseLife()) {
                return true;
            }
            CostDamage damageCost = cost.getCostPartByType(CostDamage.class);
            lifeLoss = ComputerUtilCombat.predictDamageTo(payer, damageCost.getAbilityAmount(sa), host, false);
            if (lifeLoss == 0) {
                return true;
            }
        } else if (cost.hasSpecificCostType(CostPayLife.class)) {
            CostPayLife lifeCost = cost.getCostPartByType(CostPayLife.class);
            lifeLoss = lifeCost.getAbilityAmount(sa);
        }

        for (Card c : AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa)) {
            if (c.isToken()) {
                return false;
            }
            if (!c.isCreature() || c.getBasePower() < lifeLoss || payer.getLife() < lifeLoss * 2) { // costs use either pay 3 life or deal 3 damage
                return false;
            }
        }

        return super.willPayUnlessCost(sa, payer, cost, alreadyPaid, payers);
    }
}
