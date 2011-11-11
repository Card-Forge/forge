package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Counters;
import forge.MyRandom;
import forge.Player;
import forge.PlayerZone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * <p>
 * AbilityFactory_Counters class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryCounters {
    // An AbilityFactory subclass for Putting or Removing Counters on Cards.

    // *******************************************
    // ********** PutCounters *****************
    // *******************************************

    /**
     * <p>
     * createAbilityPutCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPutCounters(final AbilityFactory af) {

        final SpellAbility abPutCounter = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -1259638699008542484L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.putStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.putCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.putResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.putDoTriggerAI(af, this, mandatory);
            }

        };
        return abPutCounter;
    }

    /**
     * <p>
     * createSpellPutCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPutCounters(final AbilityFactory af) {
        final SpellAbility spPutCounter = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -323471693082498224L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.putStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryCounters.putCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.putResolve(af, this);
            }

        };
        return spPutCounter;
    }

    /**
     * <p>
     * createDrawbackPutCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPutCounters(final AbilityFactory af) {
        final SpellAbility dbPutCounter = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -323471693082498224L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.putStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.putResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounters.putPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.putDoTriggerAI(af, this, mandatory);
            }

        };
        return dbPutCounter;
    }

    /**
     * <p>
     * putStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String putStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final Card card = af.getHostCard();
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);

        sb.append("Put ");
        if (params.containsKey("UpTo")) {
            sb.append("up to ");
        }
        sb.append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" on ");

        ArrayList<Card> tgtCards;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * putCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean putCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on
        // what the expected targets could be
        final HashMap<String, String> params = af.getMapParams();
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        CardList list;
        Card choice = null;
        final String type = params.get("CounterType");
        final String amountStr = params.get("CounterNum");

        final Player player = af.isCurse() ? AllZone.getHumanPlayer() : AllZone.getComputerPlayer();

        list = player.getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.canTarget(sa)
                        && !c.hasKeyword("CARDNAME can't have counters placed on it.")
                        && !(c.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && type.equals("M1M1"));
            }
        });

        if (abTgt != null) {
            list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);

            if (list.size() < abTgt.getMinTargets(source, sa)) {
                return false;
            }
        } else { // "put counter on this"
            final PlayerZone pZone = AllZone.getZoneOf(source);
            // Don't activate Curse abilities on my cards and non-curse abilites
            // on my opponents
            if (!pZone.getPlayer().equals(player)) {
                return false;
            }
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkCreatureSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // TODO handle proper calculation of X values based on Cost
        int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(amount));
        }

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // Targeting
        if (abTgt != null) {
            abTgt.resetTargets();
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                if (af.isCurse()) {
                    choice = AbilityFactoryCounters.chooseCursedTarget(list, type, amount);
                } else {
                    choice = AbilityFactoryCounters.chooseBoonTarget(list, type);
                }

                if (choice == null) { // can't find anything left
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        } else {
            // Placeholder: No targeting necessary
            final int currCounters = sa.getSourceCard().getCounters(Counters.valueOf(type));
            // each non +1/+1 counter on the card is a 10% chance of not
            // activating this ability.

            if (!(type.equals("P1P1") || type.equals("ICE")) && (r.nextFloat() < (.1 * currCounters))) {
                return false;
            }
        }

        // Don't use non P1P1/M1M1 counters before main 2 if possible
        if (AllZone.getPhase().isBefore(Constant.Phase.MAIN2) && !params.containsKey("ActivationPhases")
                && !(type.equals("P1P1") || type.equals("M1M1"))) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        if (AbilityFactory.playReusable(sa)) {
            return chance;
        }

        return ((r.nextFloat() < .6667) && chance);
    } // putCanPlayAI

    /**
     * <p>
     * putPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean putPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        boolean chance = true;
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        CardList list;
        Card choice = null;
        final String type = params.get("CounterType");
        final String amountStr = params.get("CounterNum");
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);

        final Player player = af.isCurse() ? AllZone.getHumanPlayer() : AllZone.getComputerPlayer();

        list = player.getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.canTarget(sa);
            }
        });

        if (abTgt != null) {
            list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);

            if (list.size() == 0) {
                return false;
            }

            abTgt.resetTargets();
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        break;
                    }
                }

                if (af.isCurse()) {
                    choice = AbilityFactoryCounters.chooseCursedTarget(list, type, amount);
                } else {

                }

                if (choice == null) { // can't find anything left
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    } // putPlayDrawbackAI

    /**
     * <p>
     * putDoTriggerAI.
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
    private static boolean putDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // if there is a cost, it's gotta be optional
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        final HashMap<String, String> params = af.getMapParams();
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        // boolean chance = true;
        boolean preferred = true;
        CardList list;
        final Player player = af.isCurse() ? AllZone.getHumanPlayer() : AllZone.getComputerPlayer();
        final String type = params.get("CounterType");
        final String amountStr = params.get("CounterNum");
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);

        if (abTgt == null) {
            // No target. So must be defined
            list = new CardList(AbilityFactory.getDefinedCards(source, params.get("Defined"), sa).toArray());

            if (!mandatory) {
                // TODO - If Trigger isn't mandatory, when wouldn't we want to
                // put a counter?
                // things like Powder Keg, which are way too complex for the AI
            }
        } else {
            list = player.getCardsIn(Zone.Battlefield);
            list = list.getTargetableCards(sa);
            if (abTgt != null) {
                list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
            }
            if (list.isEmpty() && mandatory) {
                // If there isn't any prefered cards to target, gotta choose
                // non-preferred ones
                list = player.getOpponent().getCardsIn(Zone.Battlefield);
                list = list.getTargetableCards(sa);
                if (abTgt != null) {
                    list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
                }
                preferred = false;
            }
            // Not mandatory, or the the list was regenerated and is still
            // empty,
            // so return false since there are no targets
            if (list.isEmpty()) {
                return false;
            }

            Card choice = null;

            // Choose targets here:
            if (af.isCurse()) {
                if (preferred) {
                    choice = AbilityFactoryCounters.chooseCursedTarget(list, type, amount);
                }

                else {
                    if (type.equals("M1M1")) {
                        choice = CardFactoryUtil.getWorstCreatureAI(list);
                    } else {
                        choice = CardFactoryUtil.getRandomCard(list);
                    }
                }
            } else {
                if (preferred) {
                    choice = AbilityFactoryCounters.chooseBoonTarget(list, type);
                }

                else {
                    if (type.equals("P1P1")) {
                        choice = CardFactoryUtil.getWorstCreatureAI(list);
                    } else {
                        choice = CardFactoryUtil.getRandomCard(list);
                    }
                }
            }

            // TODO - I think choice can be null here. Is that ok for
            // addTarget()?
            abTgt.addTarget(choice);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            // chance &= subAb.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * chooseCursedTarget.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param amount
     *            a int.
     * @return a {@link forge.Card} object.
     */
    private static Card chooseCursedTarget(final CardList list, final String type, final int amount) {
        Card choice;
        if (type.equals("M1M1")) {
            // try to kill the best killable creature, or reduce the best one
            final CardList killable = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getNetDefense() <= amount;
                }
            });
            if (killable.size() > 0) {
                choice = CardFactoryUtil.getBestCreatureAI(killable);
            } else {
                choice = CardFactoryUtil.getBestCreatureAI(list);
            }
        } else {
            // improve random choice here
            choice = CardFactoryUtil.getRandomCard(list);
        }
        return choice;
    }

    /**
     * <p>
     * chooseBoonTarget.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    private static Card chooseBoonTarget(final CardList list, final String type) {
        Card choice;
        if (type.equals("P1P1")) {
            choice = CardFactoryUtil.getBestCreatureAI(list);
        } else if (type.equals("DIVINITY")) {
            final CardList boon = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.getCounters(Counters.DIVINITY) == 0;
                }
            });
            choice = CardFactoryUtil.getMostExpensivePermanentAI(boon, null, false);
        } else {
            // The AI really should put counters on cards that can use it.
            // Charge counters on things with Charge abilities, etc. Expand
            // these above
            choice = CardFactoryUtil.getRandomCard(list);
        }
        return choice;
    }

    /**
     * <p>
     * putResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void putResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Card card = af.getHostCard();
        final String type = params.get("CounterType");
        int counterAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);
        final int max = params.containsKey("MaxFromEffect") ? Integer.parseInt(params.get("MaxFromEffect")) : -1;

        if (params.containsKey("UpTo")) {
            final Integer[] integers = new Integer[counterAmount + 1];
            for (int j = 0; j <= counterAmount; j++) {
                integers[j] = Integer.valueOf(j);
            }
            final Integer i = GuiUtils.getChoiceOptional("How many counters?", integers);
            if (null == i) {
                return;
            } else {
                counterAmount = i.intValue();
            }
        }

        ArrayList<Card> tgtCards;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (final Card tgtCard : tgtCards) {
            if ((tgt == null) || tgtCard.canTarget(sa)) {
                if (max != -1) {
                    counterAmount = max - tgtCard.getCounters(Counters.valueOf(type));
                }
                final PlayerZone zone = AllZone.getZoneOf(tgtCard);
                if (zone == null) {
                    // Do nothing, token disappeared
                } else if (zone.is(Constant.Zone.Battlefield)) {
                    tgtCard.addCounter(Counters.valueOf(type), counterAmount);
                } else {
                    // adding counters to something like re-suspend cards
                    tgtCard.addCounterFromNonEffect(Counters.valueOf(type), counterAmount);
                }
            }
        }
    }

    // *******************************************
    // ********** RemoveCounters *****************
    // *******************************************

    /**
     * <p>
     * createAbilityRemoveCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityRemoveCounters(final AbilityFactory af) {
        final SpellAbility abRemCounter = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 8581011868395954121L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.removeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.removeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.removeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.removeDoTriggerAI(af, this, mandatory);
            }

        };
        return abRemCounter;
    }

    /**
     * <p>
     * createSpellRemoveCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellRemoveCounters(final AbilityFactory af) {
        final SpellAbility spRemoveCounter = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5065591869141835456L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.removeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryCounters.removeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.removeResolve(af, this);
            }

        };
        return spRemoveCounter;
    }

    /**
     * <p>
     * createDrawbackRemoveCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRemoveCounters(final AbilityFactory af) {
        final SpellAbility spRemoveCounter = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -5065591869141835456L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.removeStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.removeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounters.removePlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.removeDoTriggerAI(af, this, mandatory);
            }

        };
        return spRemoveCounter;
    }

    /**
     * <p>
     * removeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String removeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(card).append(" - ");
        } else {
            sb.append(" ");
        }

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);

        sb.append("Remove ");
        if (params.containsKey("UpTo")) {
            sb.append("up to ");
        }
        sb.append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from");

        ArrayList<Card> tgtCards;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }
        for (final Card c : tgtCards) {
            sb.append(" ").append(c);
        }

        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * removeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean removeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        // Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        // CardList list;
        // Card choice = null;
        final HashMap<String, String> params = af.getMapParams();

        final String type = params.get("CounterType");
        // String amountStr = params.get("CounterNum");

        // TODO - currently, not targeted, only for Self

        // Player player = af.isCurse() ? AllZone.getHumanPlayer() :
        // AllZone.getComputerPlayer();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // TODO handle proper calculation of X values based on Cost
        // final int amount = calculateAmount(af.getHostCard(), amountStr, sa);

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // currently, not targeted

        // Placeholder: No targeting necessary
        final int currCounters = sa.getSourceCard().getCounters(Counters.valueOf(type));
        // each counter on the card is a 10% chance of not activating this
        // ability.
        if (r.nextFloat() < (.1 * currCounters)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    /**
     * <p>
     * removePlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean removePlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        // Target abTgt = sa.getTarget();
        // final Card source = sa.getSourceCard();
        // CardList list;
        // Card choice = null;
        // HashMap<String,String> params = af.getMapParams();

        // String type = params.get("CounterType");
        // String amountStr = params.get("CounterNum");

        // TODO - currently, not targeted, only for Self

        // Player player = af.isCurse() ? AllZone.getHumanPlayer() :
        // AllZone.getComputerPlayer();

        // TODO handle proper calculation of X values based on Cost
        // final int amount = calculateAmount(af.getHostCard(), amountStr, sa);

        // prevent run-away activations - first time will always return true
        boolean chance = true;

        // currently, not targeted

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * removeDoTriggerAI.
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
    private static boolean removeDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the
        // expected targets could be
        boolean chance = true;

        // TODO - currently, not targeted, only for Self

        // Note: Not many cards even use Trigger and Remove Counters. And even
        // fewer are not mandatory
        // Since the targeting portion of this would be what

        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * removeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void removeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final Card card = af.getHostCard();
        final String type = params.get("CounterType");
        int counterAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);

        ArrayList<Card> tgtCards;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (final Card tgtCard : tgtCards) {
            if ((tgt == null) || tgtCard.canTarget(sa)) {
                final PlayerZone zone = AllZone.getZoneOf(tgtCard);

                if (zone.is(Constant.Zone.Battlefield) || zone.is(Constant.Zone.Exile)) {
                    if (params.containsKey("UpTo") && sa.getActivatingPlayer().isHuman()) {
                        final ArrayList<String> choices = new ArrayList<String>();
                        for (int i = 0; i <= counterAmount; i++) {
                            choices.add("" + i);
                        }
                        final String prompt = "Select the number of " + type + " counters to remove";
                        final Object o = GuiUtils.getChoice(prompt, choices.toArray());
                        counterAmount = Integer.parseInt((String) o);
                    }
                }
                tgtCard.subtractCounter(Counters.valueOf(type), counterAmount);
            }
        }
    }

    // *******************************************
    // ********** Proliferate ********************
    // *******************************************

    /**
     * <p>
     * createAbilityProliferate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityProliferate(final AbilityFactory af) {
        final SpellAbility abProliferate = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -6617234927365102930L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.proliferateShouldPlayAI(this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.proliferateResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.proliferateStackDescription(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.proliferateDoTriggerAI(this, mandatory);
            }
        };

        return abProliferate;
    }

    /**
     * <p>
     * createSpellProliferate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellProliferate(final AbilityFactory af) {
        final SpellAbility spProliferate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 1265466498444897146L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.proliferateShouldPlayAI(this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.proliferateResolve(af, this);
            }

            @Override
            public boolean canPlay() {
                // super takes care of AdditionalCosts
                return super.canPlay();
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.proliferateStackDescription(this);
            }
        };

        return spProliferate;
    }

    /**
     * <p>
     * createDrawbackProliferate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackProliferate(final AbilityFactory af) {
        final SpellAbility dbProliferate = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 1265466498444897146L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.proliferateShouldPlayAI(this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.proliferateResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.proliferateStackDescription(this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounters.proliferateShouldPlayAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.proliferateDoTriggerAI(this, mandatory);
            }
        };

        return dbProliferate;
    }

    /**
     * <p>
     * proliferateStackDescription.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String proliferateStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
        sb.append("Proliferate.");
        sb.append(" (You choose any number of permanents and/or players with ");
        sb.append("counters on them, then give each another counter of a kind already there.)");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * proliferateShouldPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean proliferateShouldPlayAI(final SpellAbility sa) {
        boolean chance = true;
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        // TODO Make sure Human has poison counters or there are some counters
        // we want to proliferate
        return chance;
    }

    /**
     * <p>
     * proliferateDoTriggerAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean proliferateDoTriggerAI(final SpellAbility sa, final boolean mandatory) {
        boolean chance = true;
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        // TODO Make sure Human has poison counters or there are some counters
        // we want to proliferate
        return chance;
    }

    /**
     * <p>
     * proliferateResolve.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void proliferateResolve(final AbilityFactory af, final SpellAbility sa) {
        CardList hperms = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        hperms = hperms.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card crd) {
                return !crd.getName().equals("Mana Pool") /*
                                                           * &&
                                                           * crd.hasCounters()
                                                           */;
            }
        });

        CardList cperms = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        cperms = cperms.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card crd) {
                return !crd.getName().equals("Mana Pool") /*
                                                           * &&
                                                           * crd.hasCounters()
                                                           */;
            }
        });

        if (af.getHostCard().getController().isHuman()) {
            cperms.addAll(hperms);
            final CardList unchosen = cperms;
            AllZone.getInputControl().setInput(new Input() {
                private static final long serialVersionUID = -1779224307654698954L;

                @Override
                public void showMessage() {
                    ButtonUtil.enableOnlyCancel();
                    AllZone.getDisplay().showMessage("Proliferate: Choose permanents and/or players");
                }

                @Override
                public void selectButtonCancel() {
                    // Hacky intermittent solution to triggers that look for
                    // counters being put on. They used
                    // to wait for another priority passing after proliferate
                    // finished.
                    AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();
                    this.stop();
                }

                @Override
                public void selectCard(final Card card, final PlayerZone zone) {
                    if (!unchosen.contains(card)) {
                        return;
                    }
                    unchosen.remove(card);
                    final ArrayList<String> choices = new ArrayList<String>();
                    for (final Counters c1 : Counters.values()) {
                        if (card.getCounters(c1) != 0) {
                            choices.add(c1.getName());
                        }
                    }
                    if (choices.size() > 0) {
                        card.addCounter(
                                Counters.getType((choices.size() == 1 ? choices.get(0) : GuiUtils.getChoice(
                                        "Select counter type", choices.toArray()).toString())), 1);
                    }
                }

                private boolean selComputer = false;
                private boolean selHuman = false;

                @Override
                public void selectPlayer(final Player player) {
                    if (player.isHuman() && (!this.selHuman)) {
                        this.selHuman = true;
                        if (AllZone.getHumanPlayer().getPoisonCounters() > 0) {
                            AllZone.getHumanPlayer().addPoisonCounters(1);
                        }
                    }
                    if (player.isComputer() && (!this.selComputer)) {
                        this.selComputer = true;
                        if (AllZone.getComputerPlayer().getPoisonCounters() > 0) {
                            AllZone.getComputerPlayer().addPoisonCounters(1);
                        }
                    }
                }
            });
        } else { // Compy
            cperms = cperms.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card crd) {
                    int pos = 0;
                    int neg = 0;
                    for (final Counters c1 : Counters.values()) {
                        if (crd.getCounters(c1) != 0) {
                            if (CardFactoryUtil.isNegativeCounter(c1)) {
                                neg++;
                            } else {
                                pos++;
                            }
                        }
                    }
                    return pos > neg;
                }
            });

            hperms = hperms.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card crd) {
                    int pos = 0;
                    int neg = 0;
                    for (final Counters c1 : Counters.values()) {
                        if (crd.getCounters(c1) != 0) {
                            if (CardFactoryUtil.isNegativeCounter(c1)) {
                                neg++;
                            } else {
                                pos++;
                            }
                        }
                    }
                    return pos < neg;
                }
            });

            final StringBuilder sb = new StringBuilder();
            sb.append("<html>Proliferate: <br>Computer selects ");
            if ((cperms.size() == 0) && (hperms.size() == 0) && (AllZone.getHumanPlayer().getPoisonCounters() == 0)) {
                sb.append("<b>nothing</b>.");
            } else {
                if (cperms.size() > 0) {
                    sb.append("<br>From Computer's permanents: <br><b>");
                    for (final Card c : cperms) {
                        sb.append(c);
                        sb.append(" ");
                    }
                    sb.append("</b><br>");
                }
                if (hperms.size() > 0) {
                    sb.append("<br>From Human's permanents: <br><b>");
                    for (final Card c : cperms) {
                        sb.append(c);
                        sb.append(" ");
                    }
                    sb.append("</b><br>");
                }
                if (AllZone.getHumanPlayer().getPoisonCounters() > 0) {
                    sb.append("<b>Human Player</b>.");
                }
            } // else
            sb.append("</html>");

            // add a counter for each counter type, if it would benefit the
            // computer
            for (final Card c : cperms) {
                for (final Counters c1 : Counters.values()) {
                    if (c.getCounters(c1) != 0) {
                        c.addCounter(c1, 1);
                    }
                }
            }

            // add a counter for each counter type, if it would screw over the
            // player
            for (final Card c : hperms) {
                for (final Counters c1 : Counters.values()) {
                    if (c.getCounters(c1) != 0) {
                        c.addCounter(c1, 1);
                    }
                }
            }

            // give human a poison counter, if he has one
            if (AllZone.getHumanPlayer().getPoisonCounters() > 0) {
                AllZone.getHumanPlayer().addPoisonCounters(1);
            }

        } // comp
    }

    // *******************************************
    // ********** PutCounterAll ******************
    // *******************************************

    /**
     * <p>
     * createAbilityPutCounterAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPutCounterAll(final AbilityFactory af) {

        final SpellAbility abPutCounterAll = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -712473347429870385L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.putAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.putAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.putAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.putAllCanPlayAI(af, this);
            }

        };
        return abPutCounterAll;
    }

    /**
     * <p>
     * createSpellPutCounterAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPutCounterAll(final AbilityFactory af) {
        final SpellAbility spPutCounterAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4400684695467183219L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.putAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.putAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.putAllResolve(af, this);
            }

        };
        return spPutCounterAll;
    }

    /**
     * <p>
     * createDrawbackPutCounterAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPutCounterAll(final AbilityFactory af) {
        final SpellAbility dbPutCounterAll = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -3101160929130043022L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.putAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.putAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounters.putAllPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.putAllPlayDrawbackAI(af, this);
            }

        };
        return dbPutCounterAll;
    }

    /**
     * <p>
     * putAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String putAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);

        sb.append("Put ").append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" on each valid permanent.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * putAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean putAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final HashMap<String, String> params = af.getMapParams();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        CardList hList;
        CardList cList;
        final String type = params.get("CounterType");
        final String amountStr = params.get("CounterNum");
        final String valid = params.get("ValidCards");
        final boolean curse = af.isCurse();
        final Target tgt = sa.getTarget();

        hList = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        cList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);

        hList = hList.getValidCards(valid, source.getController(), source);
        cList = cList.getValidCards(valid, source.getController(), source);

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 8)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }
        }

        if (tgt != null) {
            Player pl;
            if (curse) {
                pl = AllZone.getHumanPlayer();
            } else {
                pl = AllZone.getComputerPlayer();
            }

            tgt.addTarget(pl);

            hList = hList.getController(pl);
            cList = cList.getController(pl);
        }

        // TODO improve X value to don't overpay when extra mana won't do
        // anything more useful
        final int amount;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(amount));
        } else {
            amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (curse) {
            if (type.equals("M1M1")) {
                final CardList killable = hList.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.getNetDefense() <= amount;
                    }
                });
                if (!(killable.size() > 2)) {
                    return false;
                }
            } else {
                // make sure compy doesn't harm his stuff more than human's
                // stuff
                if (cList.size() > hList.size()) {
                    return false;
                }
            }
        } else {
            // human has more things that will benefit, don't play
            if (hList.size() >= cList.size()) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    /**
     * <p>
     * putAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean putAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryCounters.putAllCanPlayAI(af, sa);
    }

    /**
     * <p>
     * putAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void putAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final String type = params.get("CounterType");
        final int counterAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);
        final String valid = params.get("ValidCards");

        CardList cards = AllZoneUtil.getCardsIn(Zone.Battlefield);
        cards = cards.getValidCards(valid, sa.getSourceCard().getController(), sa.getSourceCard());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final Player pl = sa.getTargetPlayer();
            cards = cards.getController(pl);
        }

        for (final Card tgtCard : cards) {
            if (AllZone.getZoneOf(tgtCard).is(Constant.Zone.Battlefield)) {
                tgtCard.addCounter(Counters.valueOf(type), counterAmount);
            } else {
                // adding counters to something like re-suspend cards
                tgtCard.addCounterFromNonEffect(Counters.valueOf(type), counterAmount);
            }
        }
    }

    // *******************************************
    // ********** RemoveCounterAll ***************
    // *******************************************

    /**
     * <p>
     * createAbilityRemoveCounterAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityRemoveCounterAll(final AbilityFactory af) {

        final SpellAbility abRemoveCounterAll = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 1189198508841846311L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.removeCounterAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.removeCounterAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.removeCounterAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

        };
        return abRemoveCounterAll;
    }

    /**
     * <p>
     * createSpellRemoveCounterAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellRemoveCounterAll(final AbilityFactory af) {
        final SpellAbility spRemoveCounterAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4173468877313664704L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.removeCounterAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.removeCounterAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.removeCounterAllResolve(af, this);
            }

        };
        return spRemoveCounterAll;
    }

    /**
     * <p>
     * createDrawbackRemoveCounterAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRemoveCounterAll(final AbilityFactory af) {
        final SpellAbility dbRemoveCounterAll = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 9210702927696563686L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.removeCounterAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.removeCounterAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounters.removeCounterAllPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.removeCounterAllPlayDrawbackAI(af, this);
            }

        };
        return dbRemoveCounterAll;
    }

    /**
     * <p>
     * removeCounterAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String removeCounterAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);
        String amountString = Integer.toString(amount);

        if (params.containsKey("AllCounters")) {
            amountString = "all";
        }

        sb.append("Remove ").append(amount).append(" ").append(cType.getName()).append(" counter");
        if (!amountString.equals("1")) {
            sb.append("s");
        }
        sb.append(" from each valid permanent.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * removeCounterAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean removeCounterAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // Heartmender is the only card using this, and it's from a trigger.
        // If at some point, other cards use this as a spell or ability, this
        // will need to be implemented.
        return false;
    }

    /**
     * <p>
     * removeCounterAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean removeCounterAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryCounters.removeCounterAllCanPlayAI(af, sa);
    }

    /**
     * <p>
     * removeCounterAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void removeCounterAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final String type = params.get("CounterType");
        int counterAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);
        final String valid = params.get("ValidCards");

        CardList cards = AllZoneUtil.getCardsIn(Zone.Battlefield);
        cards = cards.getValidCards(valid, sa.getSourceCard().getController(), sa.getSourceCard());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final Player pl = sa.getTargetPlayer();
            cards = cards.getController(pl);
        }

        for (final Card tgtCard : cards) {
            if (params.containsKey("AllCounters")) {
                counterAmount = tgtCard.getCounters(Counters.valueOf(type));
            }

            tgtCard.subtractCounter(Counters.valueOf(type), counterAmount);
        }
    }

    // *******************************************
    // ************ MoveCounters *****************
    // *******************************************

    /**
     * <p>
     * createAbilityMoveCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityMoveCounters(final AbilityFactory af) {
        final SpellAbility abMoveCounter = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4602375375570571305L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.moveCounterStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.moveCounterCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.moveCounterResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.moveCounterDoTriggerAI(af, this, mandatory);
            }

        };
        return abMoveCounter;
    }

    /**
     * <p>
     * createSpellMoveCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellMoveCounters(final AbilityFactory af) {
        final SpellAbility spMoveCounter = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 7987458386444373863L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.moveCounterStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounters.moveCounterCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.moveCounterResolve(af, this);
            }

        };
        return spMoveCounter;
    }

    /**
     * <p>
     * createDrawbackMoveCounters.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackMoveCounters(final AbilityFactory af) {
        final SpellAbility dbMoveCounter = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -9185934729634278014L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounters.moveCounterStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounters.moveCounterResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounters.moveCounterPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounters.moveCounterDoTriggerAI(af, this, mandatory);
            }

        };
        return dbMoveCounter;
    }

    /**
     * <p>
     * moveCounterStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String moveCounterStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }

        final ArrayList<Card> srcCards = AbilityFactory.getDefinedCards(host, params.get("Source"), sa);
        Card source = null;
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }

        ArrayList<Card> tgtCards;
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);

        sb.append("Move ").append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from ");
        sb.append(source).append(" to ").append(tgtCards.get(0));

        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * moveCounterCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean moveCounterCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final HashMap<String, String> params = af.getMapParams();
        final Random r = MyRandom.getRandom();
        final String amountStr = params.get("CounterNum");

        // TODO handle proper calculation of X values based on Cost
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        if (AbilityFactory.playReusable(sa)) {
            return chance;
        }

        return ((r.nextFloat() < .6667) && chance);
    } // moveCounterCanPlayAI

    /**
     * <p>
     * moveCounterPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean moveCounterPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        boolean chance = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    } // moveCounterPlayDrawbackAI

    /**
     * <p>
     * moveCounterDoTriggerAI.
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
    private static boolean moveCounterDoTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();
        boolean chance = false;

        // if there is a cost, it's gotta be optional
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final ArrayList<Card> srcCards = AbilityFactory.getDefinedCards(host, params.get("Source"), sa);
        final ArrayList<Card> destCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        if ((srcCards.size() > 0)
                && cType.equals(Counters.P1P1) // move +1/+1 counters away from
                                               // permanents that cannot use
                                               // them
                && (destCards.size() > 0) && destCards.get(0).getController().isComputer()
                && (!srcCards.get(0).isCreature() || srcCards.get(0).hasStartOfKeyword("CARDNAME can't attack"))) {
            chance = true;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * moveCounterResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void moveCounterResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);

        final ArrayList<Card> srcCards = AbilityFactory.getDefinedCards(host, params.get("Source"), sa);
        Card source = null;
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }

        ArrayList<Card> tgtCards;
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        for (final Card dest : tgtCards) {
            if ((null != source) && (null != dest)) {
                if (source.getCounters(cType) >= amount) {
                    if (!dest.hasKeyword("CARDNAME can't have counters placed on it.")
                            && !(dest.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && cType
                                    .equals("M1M1"))) {
                        dest.addCounter(cType, amount);
                        source.subtractCounter(cType, amount);
                    }
                }
            }
        }
    } // moveCounterResolve

} // end class AbilityFactory_Counters
