package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.*;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPart;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChangeZoneAi extends SpellAbilityAi {

    /**
     * <p>
     * changeZoneCanPlayAI.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String origin = null;
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (sa.hasParam("AILogic")) {
            if (sa.getParam("AILogic").equals("Always")) {
                return true;
            } else if (sa.getParam("AILogic").equals("BeforeCombat")) {
                if (aiPlayer.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_BEGIN)) {
                    return false;
                }
            }
        }

        if (sa.hasParam("Hidden") || ZoneType.isHidden(origin)) {
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
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        String origin = null;
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (sa.hasParam("Hidden") || ZoneType.isHidden(origin)) {
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
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        String origin = null;
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (sa.hasParam("Hidden") || ZoneType.isHidden(origin)) {
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
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean hiddenOriginCanPlayAI(final Player ai, final SpellAbility sa) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        ZoneType origin = null;
        final Player opponent = ai.getOpponent();
        boolean activateForCost = ComputerUtil.activateForCost(sa, ai);

        if (sa.hasParam("Origin")) {
            try {
                origin = ZoneType.smartValueOf(sa.getParam("Origin"));
            } catch (IllegalArgumentException ex) {
                // This happens when Origin is something like
                // "Graveyard,Library" (Doomsday)
                return false;
            }
        }
        final String destination = sa.getParam("Destination");

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)
                    && !(destination.equals("Battlefield") && !source.isLand())) {
                return false;
            }

            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                for (final CostPart part : abCost.getCostParts()) {
                    if (part instanceof CostDiscard) {
                        CostDiscard cd = (CostDiscard) part;
                        // this is mainly for typecycling
                        if (!cd.payCostFromSource() || !ComputerUtil.isWorseThanDraw(ai, source)) {
                            return false;
                        }
                    }
                }
            }

            //Ninjutsu
            if (sa.hasParam("Ninjutsu")) {
                if (source.isType("Legendary")
                        && !ai.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
                    final List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
                    if (Iterables.any(list, CardPredicates.nameEquals(source.getName()))) {
                        return false;
                    }
                }
                if (ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE)) {
                    return false;
                }
                List<Card> attackers = ai.getGame().getCombat().getUnblockedAttackers();
                boolean lowerCMC = false;
                for (Card attacker : attackers) {
                    if (attacker.getCMC() < source.getCMC()) {
                        lowerCMC = true;
                    }
                }
                if (!lowerCMC) {
                    return false;
                }
            }
        }

        // don't play if the conditions aren't met, unless it would trigger a beneficial sub-condition
        if (!activateForCost && !sa.getConditions().areMet(sa)) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!abSub.getConditions().areMet(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        Iterable<Player> pDefined = Lists.newArrayList(source.getController());
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            boolean isCurse = sa.isCurse();
            if (isCurse && sa.canTarget(opponent)) {
                sa.getTargets().add(opponent);
            } else if (!isCurse && sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            }
            pDefined = sa.getTargets().getTargetPlayers();
        } else {
            if (sa.hasParam("DefinedPlayer")) {
                pDefined = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("DefinedPlayer"), sa);
            } else {
                pDefined = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
            }
        }

        String type = sa.getParam("ChangeType");
        if (type != null) {
            if (type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                type = type.replace("X", Integer.toString(xPay));
            }
        }

        for (final Player p : pDefined) {
            List<Card> list = p.getCardsIn(origin);

            if (type != null && p == ai) {
                // AI only "knows" about his information
                list = CardLists.getValidCards(list, type, source.getController(), source);
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.isType("Legendary")) {
                            if (!ai.getCardsIn(ZoneType.Battlefield, c.getName()).isEmpty()) {
                                return false;
                            }
                        }
                        return true;
                    }
                });
            }
            if (origin != null && origin.isKnown()) {
                list = CardLists.getValidCards(list, type, source.getController(), source);
            }

            String num = sa.getParam("ChangeNum");
            if (num != null) {
                if (num.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
                    // Set PayX here to maximum value.
                    int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
                    xPay = Math.min(xPay, list.size());
                    source.setSVar("PayX", Integer.toString(xPay));
                }
            }

            if (!activateForCost && list.isEmpty()) {
                return false;
            }
        }

        // don't use fetching to top of library/graveyard before main2
        if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")) {
            if (!destination.equals("Battlefield") && !destination.equals("Hand")) {
                return false;
            }
            // Only tutor something in main1 if hand is almost empty
            if (ai.getCardsIn(ZoneType.Hand).size() > 1 && destination.equals("Hand")) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        return subAb == null || SpellApiToAi.Converter.get(subAb.getApi()).chkDrawbackWithSubs(ai, subAb);
    }

    /**
     * <p>
     * changeHiddenOriginPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean hiddenOriginPlayDrawbackAI(final Player aiPlayer, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player opp = aiPlayer.getOpponent();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            boolean isCurse = sa.isCurse();
            if (isCurse && sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else if (!isCurse && sa.canTarget(aiPlayer)) {
                sa.getTargets().add(aiPlayer);
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * changeHiddenTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean hiddenTriggerAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana

        final Card source = sa.getHostCard();


        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }

        // this works for hidden because the mana is paid first.
        final String type = sa.getParam("ChangeType");
        if (type != null && type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        Iterable<Player> pDefined;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            final Player opp = ai.getOpponent();
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
                return false;
            }

            if (mandatory) {
                return true;
            }
        } else {
            if (mandatory) {
                return true;
            }
            pDefined = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
        }

        for (final Player p : pDefined) {
            List<Card> list = p.getCardsIn(origin);

            // Computer should "know" his deck
            if (p == ai) {
                list = AbilityUtils.filterListByType(list, sa.getParam("ChangeType"), sa);
            }

            if (list.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // *********** Utility functions for Hidden ********************
    /**
     * <p>
     * basicManaFixing.
     * </p>
     * @param ai
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    private static Card basicManaFixing(final Player ai, final List<Card> list) { // Search for a Basic Land

        final List<Card> combined = new ArrayList<Card>(ai.getCardsIn(ZoneType.Battlefield));
        combined.addAll(ai.getCardsIn(ZoneType.Hand));

        final ArrayList<String> basics = new ArrayList<String>();

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

        for (int i = 0; i < basics.size(); i++) {
            final String b = basics.get(i);
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
        if (Iterables.any(result, Predicates.not(CardPredicates.Presets.BASIC_LANDS))) {
            result = CardLists.filter(result, Predicates.not(CardPredicates.Presets.BASIC_LANDS));
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
    private static Card chooseCreature(final Player ai, List<Card> list) {
        // Creating a new combat for testing purposes. 
        Combat combat = new Combat(ai.getOpponent());
        for (Card att : ai.getOpponent().getCreaturesInPlay()) {
            combat.addAttacker(att, ai);
        }
        AiBlockController block = new AiBlockController(ai);
        block.assignBlockers(combat);

        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
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
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean knownOriginCanPlayAI(final Player ai, final SpellAbility sa) {
        // Retrieve either this card, or target Cards in Graveyard
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        final ZoneType origin = ZoneType.smartValueOf(sa.getParam("Origin"));
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
        	return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            if (!isPreferredTarget(ai, sa, false)) {
                return false;
            }
        } else {
            // non-targeted retrieval
            final List<Card> retrieval = sa.knownDetermineDefined(sa.getParam("Defined"));

            if ((retrieval == null) || retrieval.isEmpty()) {
                return false;
            }

            // if (origin.equals("Graveyard")) {
            // return this card from graveyard: cards like Hammer of Bogardan
            // in general this is cool, but we should add some type of
            // restrictions

            // return this card from battlefield: cards like Blinking Spirit
            // in general this should only be used to protect from Imminent Harm
            // (dying or losing control of)
            if (origin.equals(ZoneType.Battlefield)) {
                if (ai.getGame().getStack().isEmpty()) {
                    return false;
                }

                final AbilitySub abSub = sa.getSubAbility();
                ApiType subApi = null;
                if (abSub != null) {
                    subApi = abSub.getApi();
                }

                // only use blink or bounce effects
                if (!(destination.equals(ZoneType.Exile) && (subApi == ApiType.DelayedTrigger || subApi == ApiType.ChangeZone))
                        && !destination.equals(ZoneType.Hand)) {
                    return false;
                }

                final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(ai, sa);
                boolean contains = false;
                for (final Card c : retrieval) {
                    if (objects.contains(c)) {
                        contains = true;
                    }
                }
                if (!contains) {
                    return false;
                }
            }
        }
        // don't return something to your hand if your hand is full of good stuff
        if (destination.equals(ZoneType.Hand) && origin.equals(ZoneType.Graveyard)) {
            final int handSize = ai.getCardsIn(ZoneType.Hand).size();
            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }
            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && handSize > 1) {
                return false;
            }
            if (ai.getGame().getPhaseHandler().isPlayerTurn(ai)
                    && handSize >= ai.getMaxHandSize()) {
                return false;
            }
        }
        
        //don't uearth after attacking is possible
        if (sa.hasParam("Unearth") && ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
        	return false;
        }

        if (destination.equals(ZoneType.Library) && origin.equals(ZoneType.Graveyard)) {
            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                return false;
            }
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !SpellApiToAi.Converter.get(subAb.getApi()).chkDrawbackWithSubs(ai, subAb)) {
        	return false;
        }

        return true;
    }

    /**
     * <p>
     * changeKnownOriginPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean knownOriginPlayDrawbackAI(final Player aiPlayer, final SpellAbility sa) {
        if (sa.getTargetRestrictions() == null) {
            return true;
        }

        return isPreferredTarget(aiPlayer, sa, false);
    }

    /**
     * <p>
     * changeKnownPreferredTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean isPreferredTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final TargetRestrictions tgt = sa.getTargetRestrictions();
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
        List<Card> list = CardLists.getValidCards(game.getCardsIn(origin), tgt.getValidTgts(), ai, source);
        list = CardLists.getTargetableCards(list, sa);
        if (sa.hasParam("AITgts")) {
            list = CardLists.getValidCards(list, sa.getParam("AITgts"), sa.getActivatingPlayer(), source);
        }
        if (source.isInZone(ZoneType.Hand)) {
            list = CardLists.filter(list, Predicates.not(CardPredicates.nameEquals(source.getName()))); // Don't get the same card back.
        }

        if (list.size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            return false;
        }

        // Narrow down the list:
        if (origin.equals(ZoneType.Battlefield)) {
            // filter out untargetables
            List<Card> aiPermanents = CardLists.filterControlledBy(list, ai);

            // Don't blink cards that will die.
            aiPermanents = ComputerUtil.getSafeTargets(ai, sa, aiPermanents);

            // Removal on blocker to save my creature
            if (tgt.getMinTargets(sa.getHostCard(), sa) <= 1) {
                if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    Combat currCombat = game.getCombat();
                    for (Card attacker : currCombat.getAttackers()) {
                        if (ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, currCombat) && !currCombat.getBlockers(attacker).isEmpty()) {
                            List<Card> blockers = currCombat.getBlockers(attacker);
                            ComputerUtilCard.sortByEvaluateCreature(blockers);
                            Combat combat = new Combat(ai);
                            combat.addAttacker(attacker, ai.getOpponent());
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
                    }
                }
            }
            
            // if it's blink or bounce, try to save my about to die stuff
            if ((destination.equals(ZoneType.Hand) || (destination.equals(ZoneType.Exile)
                    && (subApi == ApiType.DelayedTrigger || (subApi == ApiType.ChangeZone && subAffected.equals("Remembered")))))
                    && (tgt.getMinTargets(sa.getHostCard(), sa) <= 1)) {

                // check stack for something on the stack that will kill
                // anything i control
                if (!game.getStack().isEmpty()) {
                    final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(ai, sa);

                    final List<Card> threatenedTargets = new ArrayList<Card>();

                    for (final Card c : aiPermanents) {
                        if (objects.contains(c)) {
                            threatenedTargets.add(c);
                        }
                    }

                    if (!threatenedTargets.isEmpty()) {
                        // Choose "best" of the remaining to save
                        sa.getTargets().add(ComputerUtilCard.getBestAI(threatenedTargets));
                        return true;
                    }
                }
                // Save combatants
                else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    Combat combat = game.getCombat();
                    final List<Card> combatants = CardLists.filter(aiPermanents, CardPredicates.Presets.CREATURES);
                    ComputerUtilCard.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if (c.getShield().isEmpty() && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat) && c.getOwner() == ai && !c.isToken()) {
                            sa.getTargets().add(c);
                            return true;
                        }
                    }
                }
                
                // When bouncing opponents stuff, don't bounce cards with CMC 0
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        for (Card aura : c.getEnchantedBy()) {
                            if (aura.getController().isOpponentOf(ai)) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                        return c.isToken() || c.getCMC() > 0;
                    }
                });
                // TODO: Blink permanents with ETB triggers
                /*else if (!sa.isTrigger() && SpellAbilityAi.playReusable(ai, sa)) {
                    aiPermanents = CardLists.filter(aiPermanents, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (c.hasCounters()) {
                                return false; // don't blink something with
                            }
                            // counters TODO check good and
                            // bad counters
                            // checks only if there is a dangerous ETB effect
                            return !c.equals(sa.getHostCard()) && SpellPermanent.checkETBEffects(c, ai);
                        }
                    });
                    if (!aiPermanents.isEmpty()) {
                        // Choose "best" of the remaining to save
                        sa.getTargets().add(ComputerUtilCard.getBestAI(aiPermanents));
                        return true;
                    }
                }*/
            }

        } else if (origin.equals(ZoneType.Graveyard)) {
            if (destination.equals(ZoneType.Hand)) {
                // only retrieve cards from computer graveyard
                list = CardLists.filterControlledBy(list, ai);
            } else if (sa.hasParam("AttachedTo")) {
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        for (SpellAbility attach : c.getSpellAbilities()) {
                            if ("Pump".equals(attach.getParam("AILogic"))) {
                                return true; //only use good auras
                            }
                        }
                        return false;
                    }
                });
            }
        }

        // blink human targets only during combat
        if (origin.equals(ZoneType.Battlefield)
                && destination.equals(ZoneType.Exile)
                && (subApi == ApiType.DelayedTrigger || (subApi == ApiType.ChangeZone  && subAffected.equals("Remembered")))
                && !(ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS) || sa.isAbility())) {
            return false;
        }

        // Exile and bounce opponents stuff
        if (destination.equals(ZoneType.Exile) || origin.equals(ZoneType.Battlefield)) {

            // don't rush bouncing stuff when not going to attack
            if (!sa.isTrigger() && sa.getPayCosts() != null
                    && ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && ai.getGame().getPhaseHandler().isPlayerTurn(ai)
                    && ai.getCreaturesInPlay().isEmpty()) {
                return false;
            }
            list = CardLists.filterControlledBy(list, ai.getOpponents());
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card aura : c.getEnchantedBy()) {
                        if (c.getOwner().isOpponentOf(ai) && aura.getController().equals(ai)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        // Only care about combatants during combat
        if (ai.getGame().getPhaseHandler().inCombat()) {
            CardLists.getValidCards(list, "Card.attacking,Card.blocking", null, null);
        }

        if (list.isEmpty()) {
            return false;
        }

        if (!mandatory && list.size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            return false;
        }

        // target loop
        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                if (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield)) {
                    final Card mostExpensive = ComputerUtilCard.getMostExpensivePermanentAI(list, sa, false);
                    if (mostExpensive.isCreature()) {
                        // if a creature is most expensive take the best one
                        if (destination.equals(ZoneType.Exile)) {
                            // If Exiling things, don't give bonus to Tokens
                            choice = ComputerUtilCard.getBestCreatureAI(list);
                        } else {
                            choice = ComputerUtilCard.getBestCreatureToBounceAI(list);
                        }
                    } else {
                        choice = mostExpensive;
                    }
                    //option to hold removal instead only applies for single targeted removal
                    if (sa.isSpell() && tgt.getMaxTargets(source, sa) == 1) {
                        if (!ComputerUtilCard.useRemovalNow(sa, choice, 0, destination)) {
                            return false;
                        }
                    }
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.Presets.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByCmcDesc(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtilMana.hasEnoughManaSourcesToCast(potentialCard.getFirstSpellAbility(), ai)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
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
                if (sa.getTargets().getNumTargeted() == 0 || sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!sa.isTrigger() && !ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            list.remove(choice);
            sa.getTargets().add(choice);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownUnpreferredTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean isUnpreferredTarget(final Player ai, final SpellAbility sa,
            final boolean mandatory) {
        if (!mandatory) {
            return false;
        }

        final Card source = sa.getHostCard();
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        List<Card> list = CardLists.getValidCards(ai.getGame().getCardsIn(origin), tgt.getValidTgts(), ai, source);

        // Narrow down the list:
        if (origin.equals(ZoneType.Battlefield)) {
            // filter out untargetables
            list = CardLists.getTargetableCards(list, sa);

            // if Destination is hand, either bounce opponents dangerous stuff
            // or save my about to die stuff

            // if Destination is exile, filter out my cards
        } else if (origin.equals(ZoneType.Graveyard)) {
            // Retrieve from Graveyard to:

        }

        for (final Card c : sa.getTargets().getTargetCards()) {
            list.remove(c);
        }

        if (list.isEmpty()) {
            return false;
        }

        // target loop
        while (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                if (ComputerUtilCard.getMostExpensivePermanentAI(list, sa, false).isCreature()
                        && (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield))) {
                    // if a creature is most expensive take the best
                    choice = ComputerUtilCard.getBestCreatureToBounceAI(list);
                } else if (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield)) {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list, sa, false);
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.Presets.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByCmcDesc(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtilMana.hasEnoughManaSourcesToCast(potentialCard.getFirstSpellAbility(), ai)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
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
                if ((sa.getTargets().getNumTargeted() == 0) || (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa))) {
                    sa.resetTargets();
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
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
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean knownOriginTriggerAI(final Player ai, final SpellAbility sa,
            final boolean mandatory) {

        if (sa.getTargetRestrictions() == null) {
            // Just in case of Defined cases
            if (!mandatory && sa.hasParam("AttachedTo")) {
                final List<Card> list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("AttachedTo"), sa);
                if (!list.isEmpty()) {
                    final Card attachedTo = list.get(0);
                    // This code is for the Dragon auras
                    if (attachedTo.getController().isOpponentOf(ai)) {
                        return false;
                    }
                }
            }
        } else if (isPreferredTarget(ai, sa, mandatory)) {
            // do nothing
        } else if (!isUnpreferredTarget(ai, sa, mandatory)) {
            return false;
        }

        return true;
    }

    public static Card chooseCardToHiddenOriginChangeZone(ZoneType destination, List<ZoneType> origin, SpellAbility sa, List<Card> fetchList, Player player, final Player decider) {

        if( fetchList.isEmpty() )
            return null;
        
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
            fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (c.isType("Legendary")) {
                        if (!decider.getCardsIn(ZoneType.Battlefield, c.getName()).isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                }
            });
            if (player.isOpponentOf(decider) && sa.hasParam("GainControl") && activator.equals(decider)) {
                fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.hasSVar("RemAIDeck") || c.hasSVar("RemRandomDeck")) {
                            return false;
                        }
                        return true;
                    }
                });
            }
        }
        if (ZoneType.Exile.equals(destination) || origin.contains(ZoneType.Battlefield)
                || (ZoneType.Library.equals(destination) && origin.contains(ZoneType.Hand))) {
            // Exiling or bouncing stuff
            if (player.isOpponentOf(decider)) {
                c = ComputerUtilCard.getBestAI(fetchList);
            } else {
                c = ComputerUtilCard.getWorstAI(fetchList);
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
            List<Card> sameNamed = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(sa.getHostCard().getName())));
            if (origin.contains(ZoneType.Library) && !sameNamed.isEmpty()) {
                fetchList = sameNamed;
            }

            // Does AI need a land?
            List<Card> hand = decider.getCardsIn(ZoneType.Hand);
            if (CardLists.filter(hand, Presets.LANDS).isEmpty() && CardLists.filter(decider.getCardsIn(ZoneType.Battlefield), Presets.LANDS).size() < 4) {
                boolean canCastSomething = false;
                for (Card cardInHand : hand) {
                    canCastSomething |= ComputerUtilMana.hasEnoughManaSourcesToCast(cardInHand.getFirstSpellAbility(), decider);
                }
                if (!canCastSomething) {
                    System.out.println("Pulling a land as there are none in hand, less than 4 on the board, and nothing in hand is castable.");
                    c = basicManaFixing(decider, fetchList);
                }
            }
            if (c == null) {
                System.out.println("Don't need a land or none available; trying for a creature.");
                fetchList = CardLists.getNotType(fetchList, "Land");
                // Prefer to pull a creature, generally more useful for AI.
                c = chooseCreature(decider, CardLists.filter(fetchList, CardPredicates.Presets.CREATURES));
            }
            if (c == null) { // Could not find a creature.
                if (decider.getLife() <= 5) { // Desperate?
                    // Get something AI can cast soon.
                    System.out.println("5 Life or less, trying to find something castable.");
                    CardLists.sortByCmcDesc(fetchList);
                    for (Card potentialCard : fetchList) {
                       if (ComputerUtilMana.hasEnoughManaSourcesToCast(potentialCard.getFirstSpellAbility(), decider)) {
                           c = potentialCard;
                           break;
                       }
                    }
                } else {
                    // Get the best card in there.
                    System.out.println("No creature and lots of life, finding something good.");
                    c = ComputerUtilCard.getBestAI(fetchList);
                }
            }
        }
        if (c == null) {
            c = first;
        }
        return c;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // AI was never asked
        return true;
    }
    

    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Collection<Card> options, boolean isOptional, Player targetedPlayer) {
        // Called when looking for creature to attach aura or equipment
        return ComputerUtilCard.getBestAI(options);
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List)
     */
    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Collection<Player> options) {
        // Currently only used by Curse of Misfortunes, so this branch should never get hit
        // But just in case it does, just select the first option
        return Iterables.getFirst(options, null);
    }
    
}
