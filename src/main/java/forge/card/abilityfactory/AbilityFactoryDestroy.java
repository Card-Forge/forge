package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.ComputerUtil;
import forge.Constant.Zone;
import forge.MyRandom;
import forge.Player;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

/**
 * <p>
 * AbilityFactory_Destroy class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryDestroy {
    // An AbilityFactory subclass for destroying permanents
    // *********************************************************************************
    // ************************** DESTROY
    // **********************************************
    // *********************************************************************************
    /**
     * <p>
     * createAbilityDestroy.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDestroy(final AbilityFactory af) {
        final SpellAbility abDestroy = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4153613567150919283L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDestroy.destroyStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDestroy.destroyCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDestroy.destroyResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDestroy.destroyDoTriggerAI(af, this, mandatory);
            }

        };
        return abDestroy;
    }

    /**
     * <p>
     * createSpellDestroy.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDestroy(final AbilityFactory af) {
        final SpellAbility spDestroy = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -317810567632846523L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDestroy.destroyStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDestroy.destroyCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryDestroy.destroyResolve(af, this);
            }

        };
        return spDestroy;
    }

    /**
     * <p>
     * createDrawbackDestroy.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    public static AbilitySub createDrawbackDestroy(final AbilityFactory af) {
        final AbilitySub dbDestroy = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4153613567150919283L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryDestroy.destroyStackDescription(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return false;
            }

            @Override
            public void resolve() {
                AbilityFactoryDestroy.destroyResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDestroy.destroyDoTriggerAI(af, this, mandatory);
            }
        };
        return dbDestroy;
    }

    /**
     * <p>
     * destroyCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean destroyCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        final boolean noRegen = params.containsKey("NoRegen");

        CardList list;
        list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        list = list.getTargetableCards(sa);

        if (abTgt != null) {
            list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
            list = list.getNotKeyword("Indestructible");

            // If NoRegen is not set, filter out creatures that have a
            // regeneration shield
            if (!noRegen) {
                // TODO filter out things that could regenerate in response?
                // might be tougher?
                list = list.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return ((c.getShield() == 0) && !ComputerUtil.canRegenerate(c));
                    }
                });
            }

            if (list.size() == 0) {
                return false;
            }
        }

        if (abCost != null) {
            // AI currently disabled for some costs
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }
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

                Card choice = null;
                if (list.getNotType("Creature").size() == 0) {
                    choice = CardFactoryUtil.getBestCreatureAI(list); // if the
                                                                       // targets
                                                                       // are
                                                                       // only
                                                                       // creatures,
                                                                       // take
                                                                       // the
                                                                       // best
                } else {
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, true);
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
     * destroyDoTriggerAI.
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
    private static boolean destroyDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        final boolean noRegen = params.containsKey("NoRegen");

        if (tgt != null) {
            CardList list;
            list = AllZoneUtil.getCardsIn(Zone.Battlefield);
            list = list.getTargetableCards(sa);
            list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);

            if ((list.size() == 0) || (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                return false;
            }

            tgt.resetTargets();

            CardList preferred = list.getNotKeyword("Indestructible");
            preferred = list.getController(AllZone.getHumanPlayer());

            // If NoRegen is not set, filter out creatures that have a
            // regeneration shield
            if (!noRegen) {
                // TODO filter out things that could regenerate in response?
                // might be tougher?
                preferred = preferred.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.getShield() == 0;
                    }
                });
            }

            for (final Card c : preferred) {
                list.remove(c);
            }

            while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (preferred.size() == 0) {
                    if ((tgt.getNumTargeted() == 0)
                            || (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                        if (!mandatory) {
                            tgt.resetTargets();
                            return false;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    Card c;
                    if (preferred.getNotType("Creature").size() == 0) {
                        c = CardFactoryUtil.getBestCreatureAI(preferred);
                    } else if (preferred.getNotType("Land").size() == 0) {
                        c = CardFactoryUtil.getBestLandAI(preferred);
                    } else {
                        c = CardFactoryUtil.getMostExpensivePermanentAI(preferred, sa, false);
                    }
                    tgt.addTarget(c);
                    preferred.remove(c);
                }
            }

            while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    break;
                } else {
                    Card c;
                    if (list.getNotType("Creature").size() == 0) {
                        c = CardFactoryUtil.getWorstCreatureAI(list);
                    } else {
                        c = CardFactoryUtil.getCheapestPermanentAI(list, sa, false);
                    }
                    tgt.addTarget(c);
                    list.remove(c);
                }
            }

            if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
                return false;
            }
        } else {
            if (!mandatory) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            return subAb.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * destroyStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String destroyStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final boolean noRegen = params.containsKey("NoRegen");
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Card> tgtCards;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }
        
        if(params.containsKey("Sacrifice")) {
            sb.append("Sacrifice ");
        }
        else { 
            sb.append("Destroy ");
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph ").append("(").append(tgtC.getUniqueNumber()).append(")");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        if (af.getMapParams().containsKey("Radiance")) {
            sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                    .append(" that shares a color with ");
            if (tgtCards.size() > 1) {
                sb.append("them");
            } else {
                sb.append("it");
            }
        }

        if (noRegen) {
            sb.append(". ");
            if (tgtCards.size() == 1) {
                sb.append("It");
            } else {
                sb.append("They");
            }
            sb.append(" can't be regenerated");
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
     * destroyResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void destroyResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        final boolean noRegen = params.containsKey("NoRegen");
        final boolean sac = params.containsKey("Sacrifice");
        final Card card = sa.getSourceCard();

        ArrayList<Card> tgtCards;
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (params.containsKey("Radiance")) {
            for (final Card c : CardUtil.getRadiance(af.getHostCard(), tgtCards.get(0),
                    params.get("ValidTgts").split(","))) {
                untargetedCards.add(c);
            }
        }

        for (final Card tgtC : tgtCards) {
            if (AllZoneUtil.isCardInPlay(tgtC) && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                if (sac) {
                    AllZone.getGameAction().sacrifice(tgtC);
                } else if (noRegen) {
                    AllZone.getGameAction().destroyNoRegeneration(tgtC);
                } else {
                    AllZone.getGameAction().destroy(tgtC);
                }
            }
        }

        for (final Card unTgtC : untargetedCards) {
            if (AllZoneUtil.isCardInPlay(unTgtC)) {
                if (sac) {
                    AllZone.getGameAction().sacrifice(unTgtC);
                } else  if (noRegen) {
                    AllZone.getGameAction().destroyNoRegeneration(unTgtC);
                } else {
                    AllZone.getGameAction().destroy(unTgtC);
                }
            }
        }
    }

    // *********************************************************************************
    // ************************ DESTROY ALL
    // ********************************************
    // *********************************************************************************
    /**
     * <p>
     * createAbilityDestroyAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDestroyAll(final AbilityFactory af) {

        final SpellAbility abDestroyAll = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -1376444173137861437L;

            private final HashMap<String, String> params = af.getMapParams();
            private final boolean noRegen = this.params.containsKey("NoRegen");

            @Override
            public String getStackDescription() {
                return AbilityFactoryDestroy.destroyAllStackDescription(af, this, this.noRegen);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDestroy.destroyAllCanPlayAI(af, this, this.noRegen);
            }

            @Override
            public void resolve() {
                AbilityFactoryDestroy.destroyAllResolve(af, this, this.noRegen);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryDestroy.destroyAllCanPlayAI(af, this, this.noRegen);
            }

        };
        return abDestroyAll;
    }

    /**
     * <p>
     * createSpellDestroyAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDestroyAll(final AbilityFactory af) {
        final SpellAbility spDestroyAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3712659336576469102L;

            private final HashMap<String, String> params = af.getMapParams();
            private final boolean noRegen = this.params.containsKey("NoRegen");

            @Override
            public String getStackDescription() {
                if (this.params.containsKey("SpellDescription")) {
                    return af.getHostCard().getName() + " - " + this.params.get("SpellDescription");
                } else {
                    return AbilityFactoryDestroy.destroyAllStackDescription(af, this, this.noRegen);
                }
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryDestroy.destroyAllCanPlayAI(af, this, this.noRegen);
            }

            @Override
            public void resolve() {
                AbilityFactoryDestroy.destroyAllResolve(af, this, this.noRegen);
            }

        };
        return spDestroyAll;
    }

    /**
     * <p>
     * createDrawbackDestroyAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDestroyAll(final AbilityFactory af) {
        final SpellAbility dbDestroyAll = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -242160421677518351L;

            private final HashMap<String, String> params = af.getMapParams();
            private final boolean noRegen = this.params.containsKey("NoRegen");

            @Override
            public String getStackDescription() {
                if (this.params.containsKey("SpellDescription")) {
                    return af.getHostCard().getName() + " - " + this.params.get("SpellDescription");
                } else {
                    return AbilityFactoryDestroy.destroyAllStackDescription(af, this, this.noRegen);
                }
            }

            @Override
            public void resolve() {
                AbilityFactoryDestroy.destroyAllResolve(af, this, this.noRegen);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                // TODO Auto-generated method stub
                return false;
            }

        };
        return dbDestroyAll;
    }

    /**
     * <p>
     * destroyAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param noRegen
     *            a boolean.
     * @return a {@link java.lang.String} object.
     */
    private static String destroyAllStackDescription(final AbilityFactory af, final SpellAbility sa,
            final boolean noRegen) {

        final StringBuilder sb = new StringBuilder();
        final String name = af.getHostCard().getName();
        final HashMap<String, String> params = af.getMapParams();

        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        ArrayList<Card> tgtCards;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = new ArrayList<Card>();
            tgtCards.add(sa.getSourceCard());
        }

        sb.append(name).append(" - Destroy permanents.");

        if (noRegen) {
            sb.append(" They can't be regenerated");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * destroyAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param noRegen
     *            a boolean.
     * @return a boolean.
     */
    private static boolean destroyAllCanPlayAI(final AbilityFactory af, final SpellAbility sa, final boolean noRegen) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        if (valid.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            valid = valid.replace("X", Integer.toString(xPay));
        }

        CardList humanlist = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        CardList computerlist = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
            computerlist.clear();
        }

        humanlist = humanlist.getValidCards(valid.split(","), source.getController(), source);
        computerlist = computerlist.getValidCards(valid.split(","), source.getController(), source);

        humanlist = humanlist.getNotKeyword("Indestructible");
        computerlist = computerlist.getNotKeyword("Indestructible");

        if (abCost != null) {
            // AI currently disabled for some costs

            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // if only creatures are affected evaluate both lists and pass only if
        // human creatures are more valuable
        if ((humanlist.getNotType("Creature").size() == 0) && (computerlist.getNotType("Creature").size() == 0)) {
            if ((CardFactoryUtil.evaluateCreatureList(computerlist) + 200) >= CardFactoryUtil
                    .evaluateCreatureList(humanlist)) {
                return false;
            }
        } // only lands involved
        else if ((humanlist.getNotType("Land").size() == 0) && (computerlist.getNotType("Land").size() == 0)) {
            if ((CardFactoryUtil.evaluatePermanentList(computerlist) + 1) >= CardFactoryUtil
                    .evaluatePermanentList(humanlist)) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human
          // permanents are more valuable
        else if ((CardFactoryUtil.evaluatePermanentList(computerlist) + 3) >= CardFactoryUtil
                .evaluatePermanentList(humanlist)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return ((r.nextFloat() < .9667) && chance);
    }

    /**
     * <p>
     * destroyAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param noRegen
     *            a boolean.
     */
    private static void destroyAllResolve(final AbilityFactory af, final SpellAbility sa, final boolean noRegen) {
        final HashMap<String, String> params = af.getMapParams();

        final Card card = sa.getSourceCard();

        final Target tgt = af.getAbTgt();
        Player targetPlayer = null;
        if (tgt != null) {
            targetPlayer = tgt.getTargetPlayers().get(0);
        }

        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
        }

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

        if (targetPlayer != null) {
            list = list.getController(targetPlayer);
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        final boolean remDestroyed = params.containsKey("RememberDestroyed");
        if (remDestroyed) {
            card.clearRemembered();
        }

        if (noRegen) {
            for (int i = 0; i < list.size(); i++) {
                if (AllZone.getGameAction().destroyNoRegeneration(list.get(i)) && remDestroyed) {
                    card.addRemembered(list.get(i));
                }
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (AllZone.getGameAction().destroy(list.get(i)) && remDestroyed) {
                    card.addRemembered(list.get(i));
                }
            }
        }
    }

} // end class AbilityFactory_Destroy
