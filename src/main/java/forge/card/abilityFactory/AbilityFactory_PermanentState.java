package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.MyRandom;
import forge.Phase;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

/**
 * <p>
 * AbilityFactory_PermanentState class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_PermanentState {

    // ****************************************
    // ************** Untap *******************
    // ****************************************

    /**
     * <p>
     * createAbilityUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityUntap(final AbilityFactory af) {
        final SpellAbility abUntap = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return untapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return untapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                untapResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return untapTrigger(af, this, mandatory);
            }

        };
        return abUntap;
    }

    /**
     * <p>
     * createSpellUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellUntap(final AbilityFactory af) {
        final SpellAbility spUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return untapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return untapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                untapResolve(af, this);
            }

        };
        return spUntap;
    }

    /**
     * <p>
     * createDrawbackUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackUntap(final AbilityFactory af) {
        final SpellAbility dbUntap = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return untapStackDescription(af, this);
            }

            @Override
            public void resolve() {
                untapResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return untapPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return untapTrigger(af, this, mandatory);
            }

        };
        return dbUntap;
    }

    /**
     * <p>
     * untapStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String untapStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        Card hostCard = sa.getSourceCard();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append("Untap ");

        if (params.containsKey("UntapUpTo")) {
            sb.append("up to ").append(params.get("Amount")).append(" ");
            sb.append(params.get("UntapType")).append("s");
        } else {
            ArrayList<Card> tgtCards;
            Target tgt = af.getAbTgt();
            if (tgt != null) {
                tgtCards = tgt.getTargetCards();
            } else {
                tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
            }

            Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        sb.append(".");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * untapCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Target tgt = sa.getTarget();
        Card source = sa.getSourceCard();
        Cost cost = sa.getPayCosts();

        if (!CostUtil.checkAddM1M1CounterCost(cost, source)) {
            return false;
        }

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn() + 1);

        if (tgt == null) {
            if (sa.getSourceCard().isUntapped()) {
                return false;
            }
        } else {
            if (!untapPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        HashMap<String, String> params = af.getMapParams();
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        Target tgt = sa.getTarget();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result
            ArrayList<Card> pDefined = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
            if (pDefined != null && pDefined.get(0).isUntapped() && pDefined.get(0).getController().isComputer()) {
                return false;
            }

            return true;
        } else {
            if (untapPrefTargeting(tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return untapUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * untapPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Target tgt = af.getAbTgt();

        boolean randomReturn = true;

        if (tgt == null) {
            // who cares if its already untapped, it's only a subability?
        } else {
            if (!untapPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapPrefTargeting(final Target tgt, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        Card source = sa.getSourceCard();

        Player targetController = AllZone.getComputerPlayer();

        if (af.isCurse()) {
            targetController = AllZone.getHumanPlayer();
        }

        CardList untapList = targetController.getCardsIn(Zone.Battlefield);
        untapList = untapList.getTargetableCards(source);
        untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController(), source);

        untapList = untapList.filter(CardListFilter.tapped);
        // filter out enchantments and planeswalkers, their tapped state doesn't
        // matter.
        String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        untapList = untapList.getValidCards(tappablePermanents, source.getController(), source);

        if (untapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card choice = null;

            if (untapList.size() == 0) {
                if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (untapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.AI_getBestCreature(untapList); // if
                                                                        // only
                                                                        // creatures
                                                                        // take
                                                                        // the
                                                                        // best
            } else {
                choice = CardFactoryUtil.AI_getMostExpensivePermanent(untapList, af.getHostCard(), false);
            }

            if (choice == null) { // can't find anything left
                if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            untapList.remove(choice);
            tgt.addTarget(choice);
        }
        return true;
    }

    /**
     * <p>
     * untapUnpreferredTargeting.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapUnpreferredTargeting(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        Card source = sa.getSourceCard();
        Target tgt = sa.getTarget();

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

        list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);
        list = list.getTargetableCards(source);

        // filter by enchantments and planeswalkers, their tapped state doesn't
        // matter.
        String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        CardList tapList = list.getValidCards(tappablePermanents, source.getController(), source);

        if (untapTargetList(source, tgt, af, sa, mandatory, tapList)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = list.filter(CardListFilter.untapped);

        if (untapTargetList(source, tgt, af, sa, mandatory, tapList)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (untapTargetList(source, tgt, af, sa, mandatory, tapList)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * untapTargetList.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    private static boolean untapTargetList(final Card source, final Target tgt, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory, final CardList tapList) {
        for (Card c : tgt.getTargetCards())
            tapList.remove(c);

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if (tgt.getNumTargeted() < tgt.getMinTargets(source, sa) || tgt.getNumTargeted() == 0) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (tapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.AI_getBestCreature(tapList); // if only
                                                                      // creatures
                                                                      // take
                                                                      // the
                                                                      // best
            } else {
                choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
            }

            if (choice == null) { // can't find anything left
                if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * untapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void untapResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();
        Target tgt = af.getAbTgt();
        ArrayList<Card> tgtCards = null;

        if (params.containsKey("UntapUpTo")) {
            untapChooseUpTo(af, sa, params);
        } else {
            if (tgt != null) {
                tgtCards = tgt.getTargetCards();
            } else {
                tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
            }

            for (Card tgtC : tgtCards) {
                if (AllZoneUtil.isCardInPlay(tgtC)
                        && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC))) {
                    tgtC.untap();
                }
            }
        }
    }

    /**
     * <p>
     * untapChooseUpTo.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param params
     *            a {@link java.util.HashMap} object.
     */
    private static void untapChooseUpTo(final AbilityFactory af, final SpellAbility sa, final HashMap<String, String> params) {
        int num = Integer.parseInt(params.get("Amount"));
        String valid = params.get("UntapType");

        ArrayList<Player> definedPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"),
                sa);

        for (Player p : definedPlayers) {
            if (p.isHuman()) {
                AllZone.getInputControl().setInput(CardFactoryUtil.input_UntapUpToNType(num, valid));
            } else {
                CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                list = list.getType(valid);
                list = list.filter(CardListFilter.tapped);

                int count = 0;
                while (list.size() != 0 && count < num) {
                    for (int i = 0; i < list.size() && count < num; i++) {

                        Card c = CardFactoryUtil.AI_getBestLand(list);
                        c.untap();
                        list.remove(c);
                        count++;
                    }
                }
            }
        }
    }

    // ****************************************
    // ************** Tap *********************
    // ****************************************

    /**
     * <p>
     * createAbilityTap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityTap(final AbilityFactory af) {
        final SpellAbility abTap = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return tapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return tapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                tapResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return tapTrigger(af, this, mandatory);
            }

        };
        return abTap;
    }

    /**
     * <p>
     * createSpellTap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellTap(final AbilityFactory af) {
        final SpellAbility spTap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return tapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return tapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                tapResolve(af, this);
            }

        };
        return spTap;
    }

    /**
     * <p>
     * createDrawbackTap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackTap(final AbilityFactory af) {
        final SpellAbility dbTap = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return tapStackDescription(af, this);
            }

            @Override
            public void resolve() {
                tapResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return tapPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return tapTrigger(af, this, mandatory);
            }

        };
        return dbTap;
    }

    /**
     * <p>
     * tapStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String tapStackDescription(final AbilityFactory af, final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        Card hostCard = sa.getSourceCard();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append("Tap ");

        ArrayList<Card> tgtCards;
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
        }

        Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(".");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * tapCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        HashMap<String, String> params = af.getMapParams();
        Target tgt = af.getAbTgt();
        Card source = sa.getSourceCard();

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        Phase phase = AllZone.getPhase();
        Player turn = phase.getPlayerTurn();

        if (turn.isHuman()) {
            // Tap things down if it's Human's turn
        } else if (phase.inCombat() && phase.isBefore(Constant.Phase.Combat_Declare_Blockers)) {
            // TODO Tap creatures down if in combat
        } else {
            // Generally don't want to tap things during AI turn outside of
            // combat
            return false;
        }

        if (tgt == null) {
            ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);

            boolean bFlag = false;
            for (Card c : defined)
                bFlag |= c.isUntapped();

            if (!bFlag) {
                return false;
            }
        } else {
            tgt.resetTargets();
            if (!tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        Target tgt = sa.getTarget();
        Card source = sa.getSourceCard();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result

            return true;
        } else {
            if (tapPrefTargeting(source, tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return tapUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * tapPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Target tgt = af.getAbTgt();
        Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            // either self or defined, either way should be fine
        } else {
            // target section, maybe pull this out?
            tgt.resetTargets();
            if (!tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapPrefTargeting.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapPrefTargeting(final Card source, final Target tgt, final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        CardList tapList = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        tapList = tapList.filter(CardListFilter.untapped);
        tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
        // filter out enchantments and planeswalkers, their tapped state doesn't
        // matter.
        String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        tapList = tapList.getValidCards(tappablePermanents, source.getController(), source);
        tapList = tapList.getTargetableCards(source);

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if (tgt.getNumTargeted() < tgt.getMinTargets(source, sa) || tgt.getNumTargeted() == 0) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (tapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.AI_getBestCreature(tapList); // if only
                                                                      // creatures
                                                                      // take
                                                                      // the
                                                                      // best
            } else {
                choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
            }

            if (choice == null) { // can't find anything left
                if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapUnpreferredTargeting.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapUnpreferredTargeting(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        Card source = sa.getSourceCard();
        Target tgt = sa.getTarget();

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);
        list = list.getTargetableCards(source);

        // filter by enchantments and planeswalkers, their tapped state doesn't
        // matter.
        String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        CardList tapList = list.getValidCards(tappablePermanents, source.getController(), source);

        if (tapTargetList(af, sa, tapList, mandatory)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = list.filter(CardListFilter.tapped);

        if (tapTargetList(af, sa, tapList, mandatory)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (tapTargetList(af, sa, tapList, mandatory)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * tapTargetList.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapTargetList(final AbilityFactory af, final SpellAbility sa, final CardList tapList, final boolean mandatory) {
        Card source = sa.getSourceCard();
        Target tgt = sa.getTarget();

        for (Card c : tgt.getTargetCards())
            tapList.remove(c);

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if (tgt.getNumTargeted() < tgt.getMinTargets(source, sa) || tgt.getNumTargeted() == 0) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (tapList.getNotType("Creature").size() == 0) {
                choice = CardFactoryUtil.AI_getBestCreature(tapList); // if only
                                                                      // creatures
                                                                      // take
                                                                      // the
                                                                      // best
            } else {
                choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
            }

            if (choice == null) { // can't find anything left
                if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void tapResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();

        ArrayList<Card> tgtCards;
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (Card tgtC : tgtCards) {
            if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC))) {
                tgtC.tap();
            }
        }
    }

    // ****************************************
    // ************** UntapAll *****************
    // ****************************************
    /**
     * <p>
     * createAbilityUntapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityUntapAll(final AbilityFactory af) {
        final SpellAbility abUntap = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 8914852730903389831L;

            @Override
            public String getStackDescription() {
                return untapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return untapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                untapAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return untapAllTrigger(af, this, mandatory);
            }

        };
        return abUntap;
    }

    /**
     * <p>
     * createSpellUntapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellUntapAll(final AbilityFactory af) {
        final SpellAbility spUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5713174052551899363L;

            @Override
            public String getStackDescription() {
                return untapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return untapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                untapAllResolve(af, this);
            }

        };
        return spUntap;
    }

    /**
     * <p>
     * createDrawbackUntapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackUntapAll(final AbilityFactory af) {
        final SpellAbility dbUntapAll = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -5187900994680626766L;

            @Override
            public String getStackDescription() {
                return untapAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                untapAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return untapAllPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return untapAllPlayDrawbackAI(af, this);
            }

        };
        return dbUntapAll;
    }

    /**
     * <p>
     * untapAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return true;
    }

    /**
     * <p>
     * untapAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void untapAllResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();

        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(valid.split(","), card.getController(), card);

        for (int i = 0; i < list.size(); i++) {
            list.get(i).untap();
        }
    }

    /**
     * <p>
     * untapAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean untapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        /*
         * All cards using this currently have SVar:RemAIDeck:True
         */
        return false;
    }

    /**
     * <p>
     * untapAllTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapAllTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (mandatory) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * untapAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String untapAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
            sb.append("Untap all valid cards.");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
            sb.append(params.get("SpellDescription"));
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    // ****************************************
    // ************** TapAll *****************
    // ****************************************
    /**
     * <p>
     * createAbilityTapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityTapAll(final AbilityFactory af) {
        final SpellAbility abUntap = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -2095140656782946737L;

            @Override
            public String getStackDescription() {
                return tapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return tapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                tapAllResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return tapAllTrigger(af, this, mandatory);
            }

        };
        return abUntap;
    }

    /**
     * <p>
     * createSpellTapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellTapAll(final AbilityFactory af) {
        final SpellAbility spUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -62401571838950166L;

            @Override
            public String getStackDescription() {
                return tapAllStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return tapAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                tapAllResolve(af, this);
            }

        };
        return spUntap;
    }

    /**
     * <p>
     * createDrawbackTapAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackTapAll(final AbilityFactory af) {
        final SpellAbility dbTap = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return tapAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                tapAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return tapAllPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return tapAllPlayDrawbackAI(af, this);
            }

        };
        return dbTap;
    }

    /**
     * <p>
     * tapAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void tapAllResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        CardList cards = null;

        ArrayList<Player> tgtPlayers = null;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers == null || tgtPlayers.isEmpty()) {
            cards = AllZoneUtil.getCardsIn(Zone.Battlefield);
        } else {
            cards = tgtPlayers.get(0).getCardsIn(Zone.Battlefield);
        }

        cards = AbilityFactory.filterListByType(cards, params.get("ValidCards"), sa);

        for (Card c : cards)
            c.tap();
    }

    /**
     * <p>
     * tapAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // If tapping all creatures do it either during declare attackers of AIs
        // turn
        // or during upkeep/begin combat?

        Card source = sa.getSourceCard();
        HashMap<String, String> params = af.getMapParams();

        if (AllZone.getPhase().isAfter(Constant.Phase.Combat_Begin)) {
            return false;
        }

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList validTappables = AllZoneUtil.getCardsIn(Zone.Battlefield);

        Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
            validTappables = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        }

        validTappables = validTappables.getValidCards(valid, source.getController(), source);
        validTappables = validTappables.filter(CardListFilter.untapped);

        Random r = MyRandom.random;
        boolean rr = false;
        if (r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn())) {
            rr = true;
        }

        if (validTappables.size() > 0) {
            CardList human = validTappables.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getController().isHuman();
                }
            });
            CardList compy = validTappables.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getController().isComputer();
                }
            });
            if (human.size() > compy.size()) {
                return rr;
            }
        }
        return false;
    }

    /**
     * <p>
     * getTapAllTargets.
     * </p>
     * 
     * @param valid
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList getTapAllTargets(final String valid, final Card source) {
        CardList tmpList = AllZoneUtil.getCardsIn(Zone.Battlefield);
        tmpList = tmpList.getValidCards(valid, source.getController(), source);
        tmpList = tmpList.filter(CardListFilter.untapped);
        return tmpList;
    }

    /**
     * <p>
     * tapAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String tapAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
            sb.append("Tap all valid cards.");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
            sb.append(params.get("SpellDescription"));
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * tapAllTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapAllTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (mandatory) {
            return true;
        }

        Card source = sa.getSourceCard();
        HashMap<String, String> params = af.getMapParams();

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList validTappables = getTapAllTargets(valid, source);

        Random r = MyRandom.random;
        boolean rr = false;
        if (r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn())) {
            rr = true;
        }

        if (validTappables.size() > 0) {
            CardList human = validTappables.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getController().isHuman();
                }
            });
            CardList compy = validTappables.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getController().isHuman();
                }
            });
            if (human.size() > compy.size()) {
                return rr;
            }
        }

        return false;
    }

    /**
     * <p>
     * tapAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return true;
    }

    // ****************************************
    // ************** Tap or Untap ************
    // ****************************************

    /**
     * <p>
     * createAbilityTapOrUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityTapOrUntap(final AbilityFactory af) {
        final SpellAbility abTapOrUntap = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4713183763302932079L;

            @Override
            public String getStackDescription() {
                return tapOrUntapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return tapOrUntapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                tapOrUntapResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return tapOrUntapTrigger(af, this, mandatory);
            }

        };
        return abTapOrUntap;
    }

    /**
     * <p>
     * createSpellTapOrUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellTapOrUntap(final AbilityFactory af) {
        final SpellAbility spTapOrUntap = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -8870476840484788521L;

            @Override
            public String getStackDescription() {
                return tapOrUntapStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return tapOrUntapCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                tapOrUntapResolve(af, this);
            }

        };
        return spTapOrUntap;
    }

    /**
     * <p>
     * createDrawbackTapOrUntap.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackTapOrUntap(final AbilityFactory af) {
        final SpellAbility dbTapOrUntap = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -8282868583712773337L;

            @Override
            public String getStackDescription() {
                return tapOrUntapStackDescription(af, this);
            }

            @Override
            public void resolve() {
                tapOrUntapResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return tapOrUntapPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return tapOrUntapTrigger(af, this, mandatory);
            }

        };
        return dbTapOrUntap;
    }

    /**
     * <p>
     * tapOrUntapStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String tapOrUntapStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        StringBuilder sb = new StringBuilder();

        HashMap<String, String> params = af.getMapParams();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append("Tap or untap ");

        ArrayList<Card> tgtCards;
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(".");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * tapOrUntapCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapOrUntapCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        HashMap<String, String> params = af.getMapParams();
        Target tgt = af.getAbTgt();
        Card source = sa.getSourceCard();

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (tgt == null) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap
            // those.
            ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);

            boolean bFlag = false;
            for (Card c : defined)
                bFlag |= c.isUntapped();

            if (!bFlag) {
                return false;
            }
        } else {
            tgt.resetTargets();
            if (!tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapOrUntapTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean tapOrUntapTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        Target tgt = sa.getTarget();
        Card source = sa.getSourceCard();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine if this is an unfavorable result

            return true;
        } else {
            if (tapPrefTargeting(source, tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return tapUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * tapOrUntapPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean tapOrUntapPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Target tgt = af.getAbTgt();
        Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            // either self or defined, either way should be fine
        } else {
            // target section, maybe pull this out?
            tgt.resetTargets();
            if (!tapPrefTargeting(source, tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * tapOrUntapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void tapOrUntapResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();

        ArrayList<Card> tgtCards;
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (Card tgtC : tgtCards) {
            if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC))) {
                if (sa.getActivatingPlayer().isHuman()) {
                    String[] tapOrUntap = new String[] { "Tap", "Untap" };
                    Object z = GuiUtils.getChoiceOptional("Tap or Untap " + tgtC + "?", tapOrUntap);
                    if (null == z) {
                        continue;
                    }
                    boolean tap = (z.equals("Tap")) ? true : false;

                    if (tap) {
                        tgtC.tap();
                    } else {
                        tgtC.untap();
                    }
                } else {
                    // computer
                    tgtC.tap();
                }
            }
        }
    }

    // ******************************************
    // ************** Phases ********************
    // ******************************************
    // Phases generally Phase Out. Time and Tide is the only card that can force
    // Phased Out cards in.

    /**
     * <p>
     * createAbilityPhases.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityPhases(final AbilityFactory af) {
        final SpellAbility abPhases = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5445572699000471299L;

            @Override
            public String getStackDescription() {
                return phasesStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return phasesCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                phasesResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return phasesTrigger(af, this, mandatory);
            }

        };
        return abPhases;
    }

    /**
     * <p>
     * createSpellPhases.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellPhases(final AbilityFactory af) {
        final SpellAbility spPhases = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return phasesStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return phasesCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                phasesResolve(af, this);
            }

        };
        return spPhases;
    }

    /**
     * <p>
     * createDrawbackPhases.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPhases(final AbilityFactory af) {
        final SpellAbility dbPhases = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -4990932993654533449L;

            @Override
            public String getStackDescription() {
                return phasesStackDescription(af, this);
            }

            @Override
            public void resolve() {
                phasesResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return phasesPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return phasesTrigger(af, this, mandatory);
            }

        };
        return dbPhases;
    }

    /**
     * <p>
     * phasesStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String phasesStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        Card source = sa.getSourceCard();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Card> tgtCards;
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
        }

        Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(" Phases Out.");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            sb.append(subAb.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * phasesCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean phasesCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // This still needs to be fleshed out
        Target tgt = sa.getTarget();
        Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn() + 1);

        ArrayList<Card> tgtCards;
        if (tgt == null) {
            tgtCards = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
            if (tgtCards.contains(source)) {
                // Protect it from something
            } else {
                // Card def = tgtCards.get(0);
                // Phase this out if it might attack me, or before it can be
                // declared as a blocker
            }

            return false;
        } else {
            if (!phasesPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * phasesTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean phasesTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        Target tgt = sa.getTarget();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            return false;
        } else {
            if (phasesPrefTargeting(tgt, af, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return phasesUnpreferredTargeting(af, sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * phasesPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean phasesPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Target tgt = af.getAbTgt();

        boolean randomReturn = true;

        if (tgt == null) {

        } else {
            if (!phasesPrefTargeting(tgt, af, sa, false)) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * phasesPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean phasesPrefTargeting(final Target tgt, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // Card source = sa.getSourceCard();

        // CardList phaseList =
        // AllZoneUtil.getCardsIn(Zone.Battlefield).getTargetableCards(source)
        // .getValidCards(tgt.getValidTgts(), source.getController(), source);

        // CardList aiPhaseList =
        // phaseList.getController(AllZone.getComputerPlayer());

        // If Something in the Phase List might die from a bad combat, or a
        // spell on the stack save it

        // CardList humanPhaseList =
        // phaseList.getController(AllZone.getHumanPlayer());

        // If something in the Human List is causing issues, phase it out

        return false;
    }

    /**
     * <p>
     * phasesUnpreferredTargeting.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean phasesUnpreferredTargeting(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        Card source = sa.getSourceCard();
        Target tgt = sa.getTarget();

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(tgt.getValidTgts(), source.getController(), source).getTargetableCards(source);

        return false;
    }

    /**
     * <p>
     * phasesResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void phasesResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();
        Target tgt = sa.getTarget();
        ArrayList<Card> tgtCards = null;

        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (Card tgtC : tgtCards) {
            if (!tgtC.isPhasedOut()) {
                tgtC.phase();
            }
        }
    }
}// end of AbilityFactory_PermanentState class
