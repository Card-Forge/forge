package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Constant;
import forge.GameEntity;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.ApiType;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.abilityfactory.effects.AttachEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostDiscard;
import forge.card.cost.CostPart;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.ComputerUtilBlock;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.MyRandom;

public class ChangeZoneAi extends SpellAiLogic { 

    /**
     * <p>
     * changeZoneCanPlayAI.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (ZoneType.isHidden(origin, sa.hasParam("Hidden"))) {
            return hiddenOriginCanPlayAI(aiPlayer, sa);
        } else if (ZoneType.isKnown(origin)) {
            return knownOriginCanPlayAI(aiPlayer, sa);
        }

        return false;
    }

    /**
     * <p>
     * changeZonePlayDrawbackAI.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (ZoneType.isHidden(origin, sa.hasParam("Hidden"))) {
            return hiddenOriginPlayDrawbackAI(aiPlayer, sa);
        } else if (ZoneType.isKnown(origin)) {
            return knownOriginPlayDrawbackAI(aiPlayer, sa);
        }

        return false;
    }



    /**
     * <p>
     * changeZoneTriggerAINoCost.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (ZoneType.isHidden(origin, sa.hasParam("Hidden"))) {
            return hiddenTriggerAI(aiPlayer, sa, mandatory);
        } else if (ZoneType.isKnown(origin)) {
            return knownOriginTriggerAI(aiPlayer, sa, mandatory);
        }

        return false;
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean hiddenOriginCanPlayAI(final Player ai, final SpellAbility sa) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        ZoneType origin = null;
        final Player opponent = ai.getOpponent();
        
        if (sa.hasParam("Origin")) {
            origin = ZoneType.smartValueOf(sa.getParam("Origin"));
        }
        final String destination = sa.getParam("Destination");

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)
                    && !(destination.equals("Battlefield") && !source.isLand())) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                for (final CostPart part : abCost.getCostParts()) {
                    if (part instanceof CostDiscard) {
                        CostDiscard cd = (CostDiscard) part;
                        // this is mainly for typecycling
                        if (!cd.getThis() || !ComputerUtil.isWorseThanDraw(ai, source)) {
                            return false;
                        }
                    }
                }
            }
            
            //Ninjutsu
            if (sa.hasParam("Ninjutsu")) {
                if (source.isType("Legendary") && !Singletons.getModel().getGame().isCardInPlay("Mirror Gallery")) {
                    final List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
                    if (Iterables.any(list, CardPredicates.nameEquals(source.getName()))) {
                        return false;
                    }
                }
                if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE)) {
                    return false;
                }
                List<Card> attackers = new ArrayList<Card>();
                attackers.addAll(Singletons.getModel().getGame().getCombat().getUnblockedAttackers());
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
        if (!AbilityFactory.checkConditional(sa)) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!AbilityFactory.checkConditional(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        final Random r = MyRandom.getRandom();
        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        ArrayList<Player> pDefined = new ArrayList<Player>();
        pDefined.add(source.getController());
        final Target tgt = sa.getTarget();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            boolean isCurse = sa.isCurse();
            if (isCurse && sa.canTarget(opponent)) {
                tgt.addTarget(opponent);
            } else if (!isCurse && sa.canTarget(ai)) {
                tgt.addTarget(ai);
            }
            pDefined = tgt.getTargetPlayers();
        } else {
            if (sa.hasParam("DefinedPlayer")) {
                pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("DefinedPlayer"), sa);
            } else {
                pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
            }
        }

        String type = sa.getParam("ChangeType");
        if (type != null) {
            if (type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                type = type.replace("X", Integer.toString(xPay));
            }
        }

        for (final Player p : pDefined) {
            List<Card> list = p.getCardsIn(origin);

            if ((type != null) && p.isComputer()) {
                // AI only "knows" about his information
                list = CardLists.getValidCards(list, type, source.getController(), source);
            }

            if (list.isEmpty()) {
                return false;
            }
        }

        // don't use fetching to top of library/graveyard before main2
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")) {
            if (!destination.equals("Battlefield") && !destination.equals("Hand")) {
                return false;
            }
            // Only tutor something in main1 if hand is almost empty
            if (ai.getCardsIn(ZoneType.Hand).size() > 1 && destination.equals("Hand")) {
                return false;
            }
        }

        chance &= (r.nextFloat() < .8);

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * changeHiddenOriginPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean hiddenOriginPlayDrawbackAI(final Player aiPlayer, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:
        final Target tgt = sa.getTarget();
        final Player opp = aiPlayer.getOpponent();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            boolean isCurse = sa.isCurse();
            if (isCurse && sa.canTarget(opp)) {
                tgt.addTarget(opp);
            } else if (!isCurse && sa.canTarget(aiPlayer)) {
                tgt.addTarget(aiPlayer);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean hiddenTriggerAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        // Fetching should occur fairly often as it helps cast more spells, and
        // have access to more mana

        final Card source = sa.getSourceCard();


        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }

        // this works for hidden because the mana is paid first.
        final String type = sa.getParam("ChangeType");
        if ((type != null) && type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        ArrayList<Player> pDefined;
        final Target tgt = sa.getTarget();
        if ((tgt != null) && tgt.canTgtPlayer()) {
            final Player opp = ai.getOpponent();
            if (sa.isCurse()) {
                if (sa.canTarget(opp)) {
                    tgt.addTarget(opp);
                } else if (mandatory && sa.canTarget(ai)) {
                    tgt.addTarget(ai);
                }
            } else {
                if (sa.canTarget(ai)) {
                    tgt.addTarget(ai);
                } else if (mandatory && sa.canTarget(opp)) {
                    tgt.addTarget(opp);
                }
            }

            pDefined = tgt.getTargetPlayers();

            if (pDefined.isEmpty()) {
                return false;
            }

            if (mandatory) {
                return pDefined.size() > 0;
            }
        } else {
            if (mandatory) {
                return true;
            }
            pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        for (final Player p : pDefined) {
            List<Card> list = p.getCardsIn(origin);

            // Computer should "know" his deck
            if (p.isComputer()) {
                list = AbilityFactory.filterListByType(list, sa.getParam("ChangeType"), sa);
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
     * @return a {@link forge.Card} object.
     */
    private static Card basicManaFixing(final Player ai, final List<Card> list) { // Search for a
                                                               // Basic Land

        final List<Card> combined = new ArrayList<Card>(ai.getCardsIn(ZoneType.Battlefield));
        combined.addAll(ai.getCardsIn(ZoneType.Hand));

        final ArrayList<String> basics = new ArrayList<String>();

        // what types can I go get?
        for (final String name : Constant.Color.BASIC_LANDS) {
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
        for(String ct : types.split(",")) {
            if (!Constant.Color.BASIC_LANDS.contains(ct))
                return false;
        }
        return true;
    }

    /**
     * Some logic for picking a creature card from a list.
     * @param list
     * @return Card
     */
    private static Card chooseCreature(final Player ai, List<Card> list) {
        Card card = null;
        Combat combat = new Combat();
        combat.initiatePossibleDefenders(ai);
        List<Card> attackers = ai.getOpponent().getCreaturesInPlay();
        for (Card att : attackers) {
            combat.addAttacker(att);
        }
        combat = ComputerUtilBlock.getBlockers(ai, combat, ai.getCreaturesInPlay());

        if (CombatUtil.lifeInDanger(ai, combat)) {
            // need something AI can cast now
            CardLists.sortByEvaluateCreature(list);
            for (Card c : list) {
               if (ComputerUtil.payManaCost(c.getFirstSpellAbility(), ai, true, 0, false)) {
                   card = c;
                   break;
               }
            }
        } else {
            // not urgent, get the largest creature possible
            card = CardFactoryUtil.getBestCreatureAI(list);
        }
        return card;
    }

    // *************************************************************************************
    // **************** Known Origin (Battlefield/Graveyard/Exile)
    // *************************
    // ******* Known origin cards are chosen during casting of the spell
    // (target) **********
    // *************************************************************************************

    /**
     * <p>
     * changeKnownOriginCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean knownOriginCanPlayAI(final Player ai, final SpellAbility sa) {
        // Retrieve either this card, or target Cards in Graveyard
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        final ZoneType origin = ZoneType.smartValueOf(sa.getParam("Origin"));
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));

        final Random r = MyRandom.getRandom();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());

        final Target tgt = sa.getTarget();
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
                if (Singletons.getModel().getGame().getStack().size() == 0) {
                    return false;
                }

                final AbilitySub abSub = sa.getSubAbility();
                ApiType subAPI = null;
                if (abSub != null) {
                    subAPI = abSub.getApi();
                }

                // only use blink or bounce effects
                if (!(destination.equals(ZoneType.Exile) && (subAPI == ApiType.DelayedTrigger || subAPI == ApiType.ChangeZone))
                        && !destination.equals(ZoneType.Hand)) {
                    return false;
                }

                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(ai, sa);
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
            if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }
            if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && handSize > 1) {
                return false;
            }
            if (Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)
                    && handSize >= ai.getMaxHandSize()) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return (chance);
    }

    /**
     * <p>
     * changeKnownOriginPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean knownOriginPlayDrawbackAI(final Player aiPlayer, final SpellAbility sa) {
        if (sa.getTarget() == null) {
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean isPreferredTarget(final Player ai, final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final Target tgt = sa.getTarget();

        final AbilitySub abSub = sa.getSubAbility();
        ApiType subAPI = null;
        String subAffected = "";
        if (abSub != null) {
            subAPI = abSub.getApi();
            if (abSub.hasParam("Defined")) {
                subAffected = abSub.getParam("Defined");
            }
        }

        tgt.resetTargets();
        List<Card> list = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(origin), tgt.getValidTgts(), ai, source);
        if (sa.hasParam("AITgts")) {
            list = CardLists.getValidCards(list, sa.getParam("AITgts"), sa.getActivatingPlayer(), source);
        }
        if (source.isInZone(ZoneType.Hand)) {
            list = CardLists.filter(list, Predicates.not(CardPredicates.nameEquals(source.getName()))); // Don't get the same card back.
        }

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            return false;
        }

        // Narrow down the list:
        if (origin.equals(ZoneType.Battlefield)) {
            // filter out untargetables
            list = CardLists.getTargetableCards(list, sa);
            List<Card> aiPermanents = CardLists.filterControlledBy(list, ai);

            // Don't blink cards that will die.
            aiPermanents = CardLists.filter(aiPermanents, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.getSVar("Targeting").equals("Dies");
                }
            });

            // if it's blink or bounce, try to save my about to die stuff
            if ((destination.equals(ZoneType.Hand) || (destination.equals(ZoneType.Exile) 
                    && (subAPI == ApiType.DelayedTrigger || (subAPI == ApiType.ChangeZone && subAffected.equals("Remembered")))))
                    && (tgt.getMinTargets(sa.getSourceCard(), sa) <= 1)) {

                // check stack for something on the stack that will kill
                // anything i control
                if (Singletons.getModel().getGame().getStack().size() > 0) {
                    final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(ai, sa);

                    final List<Card> threatenedTargets = new ArrayList<Card>();

                    for (final Card c : aiPermanents) {
                        if (objects.contains(c)) {
                            threatenedTargets.add(c);
                        }
                    }

                    if (!threatenedTargets.isEmpty()) {
                        // Choose "best" of the remaining to save
                        tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                        return true;
                    }
                }
                // Save combatants
                else if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    final List<Card> combatants = CardLists.filter(aiPermanents, CardPredicates.Presets.CREATURES);
                    CardLists.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c) && !c.getOwner().isHuman() && !c.isToken()) {
                            tgt.addTarget(c);
                            return true;
                        }
                    }
                }
                // Blink permanents with ETB triggers
                else if (sa.isAbility() && (sa.getPayCosts() != null) && AbilityFactory.playReusable(ai, sa)) {
                    aiPermanents = CardLists.filter(aiPermanents, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (c.getNumberOfCounters() > 0) {
                                return false; // don't blink something with
                            }
                            // counters TODO check good and
                            // bad counters
                            // checks only if there is a dangerous ETB effect
                            return SpellPermanent.checkETBEffects(c, ai);
                        }
                    });
                    if (!aiPermanents.isEmpty()) {
                        // Choose "best" of the remaining to save
                        tgt.addTarget(CardFactoryUtil.getBestAI(aiPermanents));
                        return true;
                    }
                }
            }

        } else if (origin.equals(ZoneType.Graveyard)) {
            if (destination.equals(ZoneType.Hand)) {
                // only retrieve cards from computer graveyard
                list = CardLists.filterControlledBy(list, ai);
                System.out.println("changeZone:" + list);
            }

        }

        // blink human targets only during combat
        if (origin.equals(ZoneType.Battlefield)
                && destination.equals(ZoneType.Exile)
                && (subAPI == ApiType.DelayedTrigger || (subAPI == ApiType.ChangeZone  && subAffected.equals("Remembered")))
                && !(Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) || sa
                        .isAbility())) {
            return false;
        }

        // Exile and bounce opponents stuff
        if (destination.equals(ZoneType.Exile) || origin.equals(ZoneType.Battlefield)) {

            // don't rush bouncing stuff when not going to attack
            if (!sa.isTrigger() && sa.getPayCosts() != null
                    && Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)
                    && ai.getCreaturesInPlay().isEmpty()) {
                return false;
            }
            list = CardLists.filterControlledBy(list, ai.getOpponent());
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card aura : c.getEnchantedBy()) {
                        if (c.getOwner().isHuman() && aura.getController().equals(ai)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        // Only care about combatants during combat
        if (Singletons.getModel().getGame().getPhaseHandler().inCombat()) {
            CardLists.getValidCards(list, "Card.attacking,Card.blocking", null, null);
        }

        if (list.isEmpty()) {
            return false;
        }

        if (!mandatory && (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
            return false;
        }

        // target loop
        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                final Card mostExpensive = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, false);
                if (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield)) {
                    if (mostExpensive.isCreature()) {
                        // if a creature is most expensive take the best one
                        if (destination.equals(ZoneType.Exile)) {
                            // If Exiling things, don't give bonus to Tokens
                            choice = CardFactoryUtil.getBestCreatureAI(list);
                        } else {
                            choice = CardFactoryUtil.getBestCreatureToBounceAI(list);
                        }
                    } else {
                        choice = mostExpensive;
                    }
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.Presets.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByMostExpensive(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtil.payManaCost(potentialCard.getFirstSpellAbility(), ai, true, 0, false)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
                            choice = CardFactoryUtil.getBestAI(nonLands);
                        }
                    }
                    if (choice == null) {
                        // No creatures or spells?
                        CardLists.shuffle(list);
                        choice = list.get(0);
                    }
                } else {
                    choice = CardFactoryUtil.getBestAI(list);
                }
            }
            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() == 0) || (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownUnpreferredTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean isUnpreferredTarget(final Player ai, final SpellAbility sa,
            final boolean mandatory) {
        if (!mandatory) {
            return false;
        }

        final Card source = sa.getSourceCard();
        final ZoneType origin = ZoneType.smartValueOf(sa.getParam("Origin"));
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final Target tgt = sa.getTarget();

        List<Card> list = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(origin), tgt.getValidTgts(), ai, source);

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

        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        if (list.isEmpty()) {
            return false;
        }

        // target loop
        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                if (CardFactoryUtil.getMostExpensivePermanentAI(list, sa, false).isCreature()
                        && (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield))) {
                    // if a creature is most expensive take the best
                    choice = CardFactoryUtil.getBestCreatureToBounceAI(list);
                } else if (destination.equals(ZoneType.Battlefield) || origin.equals(ZoneType.Battlefield)) {
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, false);
                } else if (destination.equals(ZoneType.Hand) || destination.equals(ZoneType.Library)) {
                    List<Card> nonLands = CardLists.getNotType(list, "Land");
                    // Prefer to pull a creature, generally more useful for AI.
                    choice = chooseCreature(ai, CardLists.filter(nonLands, CardPredicates.Presets.CREATURES));
                    if (choice == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByMostExpensive(nonLands);
                            for (Card potentialCard : nonLands) {
                               if (ComputerUtil.payManaCost(potentialCard.getFirstSpellAbility(), ai, true, 0, false)) {
                                   choice = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
                            choice = CardFactoryUtil.getBestAI(nonLands);
                        }
                    }
                    if (choice == null) {
                        // No creatures or spells?
                        CardLists.shuffle(list);
                        choice = list.get(0);
                    }
                } else {
                    choice = CardFactoryUtil.getBestAI(list);
                }
            }
            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() == 0) || (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                    tgt.resetTargets();
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * changeKnownOriginTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean knownOriginTriggerAI(final Player ai, final SpellAbility sa,
            final boolean mandatory) {

        if (sa.getTarget() == null) {
            // Just in case of Defined cases
            if (!mandatory && sa.hasParam("AttachedTo")) {
                final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                        sa.getParam("AttachedTo"), sa);
                if (!list.isEmpty()) {
                    final Card attachedTo = list.get(0);
                    // This code is for the Dragon auras
                    if (attachedTo.getController().isHuman()) {
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

    /**
     * <p>
     * changeHiddenOriginResolveAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public static void hiddenOriginResolveAI(final Player ai, final SpellAbility sa, Player player) {
        final Target tgt = sa.getTarget();
        final Card card = sa.getSourceCard();
        final boolean defined = sa.hasParam("Defined");
    
        if (tgt != null) {
            if (!tgt.getTargetPlayers().isEmpty()) {
                player = tgt.getTargetPlayers().get(0);
                if (!player.canBeTargetedBy(sa)) {
                    return;
                }
            }
        }
    
        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }
    
        String type = sa.getParam("ChangeType");
        if (type == null) {
            type = "Card";
        }
    
        int changeNum = sa.hasParam("ChangeNum") ? AbilityFactory.calculateAmount(card, sa.getParam("ChangeNum"),
                sa) : 1;
    
        List<Card> fetchList;
        if (defined) {
            fetchList = new ArrayList<Card>(AbilityFactory.getDefinedCards(card, sa.getParam("Defined"), sa));
            if (!sa.hasParam("ChangeNum")) {
                changeNum = fetchList.size();
            }
        } else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                && !sa.hasParam("DefinedPlayer")) {
            fetchList = Singletons.getModel().getGame().getCardsIn(origin);
            fetchList = AbilityFactory.filterListByType(fetchList, type, sa);
        } else {
            fetchList = player.getCardsIn(origin);
            fetchList = AbilityFactory.filterListByType(fetchList, type, sa);
        }
    
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final List<Card> fetched = new ArrayList<Card>();
        final String remember = sa.getParam("RememberChanged");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");
    
        if (sa.hasParam("Unimprint")) {
            card.clearImprinted();
        }
    
        for (int i = 0; i < changeNum; i++) {
            if (sa.hasParam("DifferentNames")) {
                for (Card c : fetched) {
                    fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(c.getName())));
                }
            }
            if ((fetchList.size() == 0) || (destination == null)) {
                break;
            }
    
            // Improve the AI for fetching.
            Card c = null;
            if (sa.hasParam("AtRandom")) {
                c = CardUtil.getRandom(fetchList);
            } else if (defined) {
                c = fetchList.get(0);
            } else {
                CardLists.shuffle(fetchList);
                // Save a card as a default, in case we can't find anything suitable.
                Card first = fetchList.get(0);
                if (ZoneType.Battlefield.equals(destination)) {
                    fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
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
                    if (player.isHuman() && sa.hasParam("GainControl")) {
                        fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
                            @Override
                            public boolean apply(final Card c) {
                                if (!c.getSVar("RemAIDeck").equals("") || !c.getSVar("RemRandomDeck").equals("")) {
                                    return false;
                                }
                                return true;
                            }
                        });
                    }
                }
                if (ZoneType.Exile.equals(destination) || origin.contains(ZoneType.Battlefield)) {
                    // Exiling or bouncing stuff
                    if (player.isHuman()) {
                        c = CardFactoryUtil.getBestAI(fetchList);
                    } else {
                        c = CardFactoryUtil.getWorstAI(fetchList);
                    }
                } else if (origin.contains(ZoneType.Library)
                        && (type.contains("Basic") || areAllBasics(type))) {
                    c = basicManaFixing(ai, fetchList);
                } else if (ZoneType.Hand.equals(destination) && CardLists.getNotType(fetchList, "Creature").size() == 0) {
                    c = chooseCreature(ai, fetchList);
                } else if (ZoneType.Battlefield.equals(destination) || ZoneType.Graveyard.equals(destination)) {
                    c = CardFactoryUtil.getBestAI(fetchList);
                } else {
                    // Don't fetch another tutor with the same name
                    List<Card> sameNamed = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(card.getName())));
                    if (origin.contains(ZoneType.Library) && !sameNamed.isEmpty()) {
                        fetchList = sameNamed;
                    }
    
                    // Does AI need a land?
                    List<Card> hand = ai.getCardsIn(ZoneType.Hand);
                    if (CardLists.filter(hand, Presets.LANDS).size() == 0 && CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Presets.LANDS).size() < 4) {
                        boolean canCastSomething = false;
                        for (Card cardInHand : hand) {
                            canCastSomething |= ComputerUtil.payManaCost(cardInHand.getFirstSpellAbility(), ai, true, 0, false);
                        }
                        if (!canCastSomething) {
                            System.out.println("Pulling a land as there are none in hand, less than 4 on the board, and nothing in hand is castable.");
                            c = basicManaFixing(ai, fetchList);
                        }
                    }
                    if (c == null) {
                        System.out.println("Don't need a land or none available; trying for a creature.");
                        fetchList = CardLists.getNotType(fetchList, "Land");
                        // Prefer to pull a creature, generally more useful for AI.
                        c = chooseCreature(ai, CardLists.filter(fetchList, CardPredicates.Presets.CREATURES));
                    }
                    if (c == null) { // Could not find a creature.
                        if (ai.getLife() <= 5) { // Desperate?
                            // Get something AI can cast soon.
                            System.out.println("5 Life or less, trying to find something castable.");
                            CardLists.sortByMostExpensive(fetchList);
                            for (Card potentialCard : fetchList) {
                               if (ComputerUtil.payManaCost(potentialCard.getFirstSpellAbility(), ai, true, 0, false)) {
                                   c = potentialCard;
                                   break;
                               }
                            }
                        } else {
                            // Get the best card in there.
                            System.out.println("No creature and lots of life, finding something good.");
                            c = CardFactoryUtil.getBestAI(fetchList);
                        }
                    }
                }
                if (c == null) {
                    c = first;
                }
            }
    
            fetched.add(c);
            fetchList.remove(c);
        }
    
        if (origin.contains(ZoneType.Library) && !defined && !"False".equals(sa.getParam("Shuffle"))) {
            player.shuffle();
        }
    
        for (final Card c : fetched) {
            Card movedCard = null;
            if (ZoneType.Library.equals(destination)) {
                final int libraryPos = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;
                movedCard = Singletons.getModel().getGame().getAction().moveToLibrary(c, libraryPos);
            } else if (ZoneType.Battlefield.equals(destination)) {
                if (sa.hasParam("Tapped")) {
                    c.setTapped(true);
                }
                if (sa.hasParam("GainControl")) {
                    c.addController(sa.getSourceCard());
                }
    
                if (sa.hasParam("AttachedTo")) {
                    final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                            sa.getParam("AttachedTo"), sa);
                    if (!list.isEmpty()) {
                        final Card attachedTo = list.get(0);
                        if (c.isEnchanting()) {
                            // If this Card is already Enchanting something
                            // Need to unenchant it, then clear out the commands
                            final GameEntity oldEnchanted = c.getEnchanting();
                            c.removeEnchanting(oldEnchanted);
                            c.clearEnchantCommand();
                            c.clearUnEnchantCommand();
                        }
                        c.enchantEntity(attachedTo);
                    }
                }
    
                if (sa.hasParam("Attacking")) {
                    Singletons.getModel().getGame().getCombat().addAttacker(c);
                }
                // Auras without Candidates stay in their current location
                if (c.isAura()) {
                    final SpellAbility saAura = AttachEffect.getAttachSpellAbility(c);
                    if (!saAura.getTarget().hasCandidates(saAura, false)) {
                        continue;
                    }
                }
    
                movedCard = Singletons.getModel().getGame().getAction().moveTo(c.getController().getZone(destination), c);
                if (sa.hasParam("Tapped")) {
                    movedCard.setTapped(true);
                }
            } else if (destination.equals(ZoneType.Exile)) {
                movedCard = Singletons.getModel().getGame().getAction().exile(c);
                if (sa.hasParam("ExileFaceDown")) {
                    movedCard.setState(CardCharacteristicName.FaceDown);
                }
            } else {
                movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c);
            }
    
            if (remember != null) {
                card.addRemembered(movedCard);
            }
            if (forget != null) {
                sa.getSourceCard().getRemembered().remove(movedCard);
            }
            // for imprinted since this doesn't use Target
            if (imprint != null) {
                card.addImprinted(movedCard);
            }
        }
        
        if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined)
                || (sa.hasParam("Shuffle") && "True".equals(sa.getParam("Shuffle")))) {
            player.shuffle();
        }
    
        if (!ZoneType.Battlefield.equals(destination) && !"Card".equals(type) && !defined) {
            final String picked = sa.getSourceCard().getName() + " - Computer picked:";
            if (fetched.size() > 0) {
                GuiChoose.one(picked, fetched);
            } else {
                GuiChoose.one(picked, new String[] { "<Nothing>" });
            }
        }
    } // end changeHiddenOriginResolveAI

}