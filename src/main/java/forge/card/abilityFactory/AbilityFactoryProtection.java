package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.CombatUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

/**
 * <p>
 * AbilityFactory_Protection class.
 * </p>
 * 
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 * @version $Id$
 */
public final class AbilityFactoryProtection {

    private AbilityFactoryProtection() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getSpellProtection.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellProtection(final AbilityFactory af) {
        final SpellAbility spProtect = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4678736312735724916L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectResolve(af, this);
            } // resolve
        }; // SpellAbility

        return spProtect;
    }

    /**
     * <p>
     * getAbilityProtection.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityProtection(final AbilityFactory af) {
        final SpellAbility abProtect = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5295298887428747473L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectResolve(af, this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectTriggerAI(af, this, mandatory);
            }

        }; // SpellAbility

        return abProtect;
    }

    /**
     * <p>
     * getDrawbackProtection.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackProtection(final AbilityFactory af) {
        final SpellAbility dbProtect = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 8342800124705819366L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectResolve(af, this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryProtection.protectDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectTriggerAI(af, this, mandatory);
            }
        }; // SpellAbility

        return dbProtect;
    }

    private static boolean hasProtectionFrom(final Card card, final String color) {
        final ArrayList<String> onlyColors = new ArrayList<String>(Arrays.asList(Constant.Color.ONLY_COLORS));

        // make sure we have a valid color
        if (!onlyColors.contains(color)) {
            return false;
        }

        final String protection = "Protection from " + color;

        return card.hasKeyword(protection);
    }

    private static boolean hasProtectionFromAny(final Card card, final ArrayList<String> colors) {
        boolean protect = false;
        for (final String color : colors) {
            protect |= AbilityFactoryProtection.hasProtectionFrom(card, color);
        }
        return protect;
    }

    private static boolean hasProtectionFromAll(final Card card, final ArrayList<String> colors) {
        boolean protect = true;
        if (colors.size() < 1) {
            return false;
        }

        for (final String color : colors) {
            protect &= AbilityFactoryProtection.hasProtectionFrom(card, color);
        }
        return protect;
    }

    /**
     * <p>
     * getProtectCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList getProtectCreatures(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        final ArrayList<String> gains = AbilityFactoryProtection.getProtectionList(hostCard, af.getMapParams());

        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (!CardFactoryUtil.canTarget(hostCard, c)) {
                    return false;
                }

                // Don't add duplicate protections
                if (AbilityFactoryProtection.hasProtectionFromAll(c, gains)) {
                    return false;
                }

                // will the creature attack (only relevant for sorcery speed)?
                if (CardFactoryUtil.doesCreatureAttackAI(c)
                        && AllZone.getPhase().isBefore(Constant.Phase.COMBAT_DECLARE_ATTACKERS)
                        && AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer())) {
                    return true;
                }

                // is the creature blocking and unable to destroy the attacker
                // or would be destroyed itself?
                if (c.isBlocking()
                        && (CombatUtil.blockerWouldBeDestroyed(c) || CombatUtil.attackerWouldBeDestroyed(AllZone
                                .getCombat().getAttackerBlockedBy(c)))) {
                    return true;
                }

                // is the creature in blocked and the blocker would survive
                if (AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_BLOCKERS)
                        && AllZone.getCombat().isAttacking(c) && AllZone.getCombat().isBlocked(c)
                        && CombatUtil.blockerWouldBeDestroyed(AllZone.getCombat().getBlockers(c).get(0))) {
                    return true;
                }

                return false;
            }
        });
        return list;
    } // getProtectCreatures()

    /**
     * <p>
     * protectCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if ((af.getAbTgt() == null) && !AllZoneUtil.isCardInPlay(hostCard)) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkCreatureSacrificeCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        // Phase Restrictions
        if ((AllZone.getStack().size() == 0) && AllZone.getPhase().isBefore(Constant.Phase.COMBAT_FIRST_STRIKE_DAMAGE)) {
            // Instant-speed protections should not be cast outside of combat
            // when the stack is empty
            if (!AbilityFactory.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (AllZone.getStack().size() > 0) {
            // TODO protection something only if the top thing on the stack will
            // kill it via damage or destroy
            return false;
        }

        if ((af.getAbTgt() == null) || !af.getAbTgt().doesTarget()) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

            if (cards.size() == 0) {
                return false;
            }

            /*
             * // when this happens we need to expand AI to consider if its ok
             * for everything? for (Card card : cards) { // TODO if AI doesn't
             * control Card and Pump is a Curse, than maybe use?
             * 
             * }
             */
        } else {
            return AbilityFactoryProtection.protectTgtAI(af, sa, false);
        }

        return false;
    } // protectPlayAI()

    /**
     * <p>
     * protectTgtAI.
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
    private static boolean protectTgtAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!mandatory && AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        final Card source = sa.getSourceCard();

        final Target tgt = af.getAbTgt();
        tgt.resetTargets();
        CardList list = AbilityFactoryProtection.getProtectCreatures(af, sa);

        list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        /*
         * TODO - What this should probably do is if it's time for instants and
         * abilities after Human declares attackers, determine desired
         * protection before assigning blockers.
         * 
         * The other time we want protection is if I'm targeted by a damage or
         * destroy spell on the stack
         * 
         * Or, add protection (to make it unblockable) when Compy is attacking.
         */

        if (AllZone.getStack().size() == 0) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().getTap()) {
                if (AllZone.getPhase().isBefore(Constant.Phase.COMBAT_DECLARE_ATTACKERS)
                        && AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer())) {
                    list.remove(sa.getSourceCard());
                }
                if (AllZone.getPhase().isBefore(Constant.Phase.COMBAT_DECLARE_BLOCKERS)
                        && AllZone.getPhase().isPlayerTurn(AllZone.getHumanPlayer())) {
                    list.remove(sa.getSourceCard());
                }
            }
        }

        if (list.isEmpty()) {
            return mandatory && AbilityFactoryProtection.protectMandatoryTarget(af, sa, mandatory);
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return AbilityFactoryProtection.protectMandatoryTarget(af, sa, mandatory);
                    }

                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = CardFactoryUtil.getBestCreatureAI(list);
            tgt.addTarget(t);
            list.remove(t);
        }

        return true;
    } // protectTgtAI()

    /**
     * <p>
     * protectMandatoryTarget.
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
    private static boolean protectMandatoryTarget(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        final Target tgt = sa.getTarget();
        list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        CardList pref = list.getController(AllZone.getComputerPlayer());
        pref = pref.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !AbilityFactoryProtection.hasProtectionFromAll(c,
                        AbilityFactoryProtection.getProtectionList(host, params));
            }
        });
        final CardList pref2 = list.getController(AllZone.getComputerPlayer());
        pref = pref.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !AbilityFactoryProtection.hasProtectionFromAny(c,
                        AbilityFactoryProtection.getProtectionList(host, params));
            }
        });
        final CardList forced = list.getController(AllZone.getHumanPlayer());
        final Card source = sa.getSourceCard();

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (pref.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref, source, true);
            }

            pref.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref2.isEmpty()) {
                break;
            }

            Card c;
            if (pref2.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref2);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref2, source, true);
            }

            pref2.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (forced.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.getWorstCreatureAI(forced);
            } else {
                c = CardFactoryUtil.getCheapestPermanentAI(forced, source, true);
            }

            forced.remove(c);

            tgt.addTarget(c);
        }

        if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        return true;
    } // protectMandatoryTarget()

    /**
     * <p>
     * protectTriggerAI.
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
    private static boolean protectTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return AbilityFactoryProtection.protectTgtAI(af, sa, mandatory);
        }

        return true;
    } // protectTriggerAI

    /**
     * <p>
     * protectDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        final Card host = af.getHostCard();

        if ((af.getAbTgt() == null) || !af.getAbTgt().doesTarget()) {
            if (host.isCreature()) {
                // TODO
            }
        } else {
            return AbilityFactoryProtection.protectTgtAI(af, sa, false);
        }

        return true;
    } // protectDrawbackAI()

    /**
     * <p>
     * protectStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String protectStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final ArrayList<String> gains = AbilityFactoryProtection.getProtectionList(host, params);
        final boolean choose = (params.containsKey("Choices")) ? true : false;
        final String joiner = choose ? "or" : "and";

        final StringBuilder sb = new StringBuilder();

        ArrayList<Card> tgtCards;
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtCards.size() > 0) {

            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(host).append(" - ");
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

            if (af.getMapParams().containsKey("Radiance") && (sa.getTarget() != null)) {
                sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                        .append(" that shares a color with ");
                if (tgtCards.size() > 1) {
                    sb.append("them");
                } else {
                    sb.append("it");
                }
            }

            sb.append(" gain");
            if (tgtCards.size() == 1) {
                sb.append("s");
            }
            sb.append(" protection from ");

            if (choose) {
                sb.append("your choice of ");
            }

            for (int i = 0; i < gains.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }

                if (i == (gains.size() - 1)) {
                    sb.append(joiner).append(" ");
                }

                sb.append(gains.get(i));
            }

            if (!params.containsKey("Permanent")) {
                sb.append(" until end of turn");
            }

            sb.append(".");
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // protectStackDescription()

    /**
     * <p>
     * protectResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void protectResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final boolean isChoice = params.get("Gains").contains("Choice");
        final ArrayList<String> choices = AbilityFactoryProtection.getProtectionList(host, params);
        final ArrayList<String> gains = new ArrayList<String>();
        if (isChoice) {
            if (sa.getActivatingPlayer().isHuman()) {
                final Object o = GuiUtils.getChoice("Choose a protection", choices.toArray());

                if (null == o) {
                    return;
                }
                final String choice = (String) o;
                gains.add(choice);
            } else {
                // TODO - needs improvement
                final String choice = choices.get(0);
                gains.add(choice);
                JOptionPane.showMessageDialog(null, "Computer chooses " + gains, "" + host, JOptionPane.PLAIN_MESSAGE);
            }
        } else {
            gains.addAll(choices);
        }

        ArrayList<Card> tgtCards;
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        if (params.containsKey("Radiance") && (tgt != null)) {
            for (final Card c : CardUtil.getRadiance(af.getHostCard(), tgtCards.get(0),
                    params.get("ValidTgts").split(","))) {
                untargetedCards.add(c);
            }
        }

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);

            // only pump things in play
            if (!AllZoneUtil.isCardInPlay(tgtC)) {
                continue;
            }

            // if this is a target, make sure we can still target now
            if ((tgt != null) && !CardFactoryUtil.canTarget(host, tgtC)) {
                continue;
            }

            for (final String gain : gains) {
                tgtC.addExtrinsicKeyword("Protection from " + gain);
            }

            if (!params.containsKey("Permanent")) {
                // If not Permanent, remove protection at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(tgtC)) {
                            for (final String gain : gains) {
                                tgtC.removeExtrinsicKeyword("Protection from " + gain);
                            }
                        }
                    }
                };
                if (params.containsKey("UntilEndOfCombat")) {
                    AllZone.getEndOfCombat().addUntil(untilEOT);
                } else {
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }
            }
        }

        for (final Card unTgtC : untargetedCards) {
            // only pump things in play
            if (!AllZoneUtil.isCardInPlay(unTgtC)) {
                continue;
            }

            for (final String gain : gains) {
                unTgtC.addExtrinsicKeyword("Protection from " + gain);
            }

            if (!params.containsKey("Permanent")) {
                // If not Permanent, remove protection at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(unTgtC)) {
                            for (final String gain : gains) {
                                unTgtC.removeExtrinsicKeyword("Protection from " + gain);
                            }
                        }
                    }
                };
                if (params.containsKey("UntilEndOfCombat")) {
                    AllZone.getEndOfCombat().addUntil(untilEOT);
                } else {
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }
            }
        }
    } // protectResolve()

    private static ArrayList<String> getProtectionList(final Card host, final HashMap<String, String> params) {
        final ArrayList<String> gains = new ArrayList<String>();

        final String gainStr = params.get("Gains");
        if (gainStr.equals("Choice")) {
            String choices = params.get("Choices");

            // Replace AnyColor with the 5 colors
            if (choices.contains("AnyColor")) {
                gains.addAll(Arrays.asList(Constant.Color.ONLY_COLORS));
                choices = choices.replaceAll("AnyColor,?", "");
            }
            // Add any remaining choices
            if (choices.length() > 0) {
                gains.addAll(Arrays.asList(choices.split(",")));
            }
        } else {
            gains.addAll(Arrays.asList(gainStr.split(",")));
        }
        return gains;
    }

    // *************************************************************************
    // ************************** ProtectionAll ********************************
    // *************************************************************************
    /**
     * <p>
     * getSpellProtectionAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellProtectionAll(final AbilityFactory af) {
        final SpellAbility spProtectAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 7205636088393235571L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectAllResolve(af, this);
            } // resolve
        }; // SpellAbility

        return spProtectAll;
    }

    /**
     * <p>
     * getAbilityProtectionAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityProtectionAll(final AbilityFactory af) {
        final SpellAbility abProtectAll = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -8491026929105907288L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectAllResolve(af, this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectAllTriggerAI(af, this, mandatory);
            }

        }; // SpellAbility

        return abProtectAll;
    }

    /**
     * <p>
     * getDrawbackProtectionAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackProtectionAll(final AbilityFactory af) {
        final SpellAbility dbProtectAll = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 5096939345199247701L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryProtection.protectAllCanPlayAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryProtection.protectAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryProtection.protectAllResolve(af, this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryProtection.protectAllDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryProtection.protectAllTriggerAI(af, this, mandatory);
            }
        }; // SpellAbility

        return dbProtectAll;
    }

    /**
     * <p>
     * protectAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if ((af.getAbTgt() == null) && !AllZoneUtil.isCardInPlay(hostCard)) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        return false;
    } // protectAllCanPlayAI()

    /**
     * <p>
     * protectAllTriggerAI.
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
    private static boolean protectAllTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        return true;
    } // protectAllTriggerAI

    /**
     * <p>
     * protectAllDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean protectAllDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryProtection.protectAllTriggerAI(af, sa, false);
    } // protectAllDrawbackAI()

    /**
     * <p>
     * protectAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String protectAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final StringBuilder sb = new StringBuilder();

        ArrayList<Card> tgtCards;
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtCards.size() > 0) {

            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(host).append(" - ");
            }

            if (params.containsKey("SpellDescription")) {
                sb.append(params.get("SpellDescription"));
            } else {
                sb.append("Valid card gain protection");
                if (!params.containsKey("Permanent")) {
                    sb.append(" until end of turn");
                }
                sb.append(".");
            }
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // protectStackDescription()

    /**
     * <p>
     * protectAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void protectAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        final boolean isChoice = params.get("Gains").contains("Choice");
        final ArrayList<String> choices = AbilityFactoryProtection.getProtectionList(host, params);
        final ArrayList<String> gains = new ArrayList<String>();
        if (isChoice) {
            if (sa.getActivatingPlayer().isHuman()) {
                final Object o = GuiUtils.getChoice("Choose a protection", choices.toArray());

                if (null == o) {
                    return;
                }
                final String choice = (String) o;
                gains.add(choice);
            } else {
                // TODO - needs improvement
                final String choice = choices.get(0);
                gains.add(choice);
                JOptionPane.showMessageDialog(null, "Computer chooses " + gains, "" + host, JOptionPane.PLAIN_MESSAGE);
            }
        } else {
            if (params.get("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColor()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        final String valid = params.get("ValidCards");
        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(valid, sa.getActivatingPlayer(), host);

        for (final Card tgtC : list) {
            if (AllZoneUtil.isCardInPlay(tgtC)) {
                for (final String gain : gains) {
                    tgtC.addExtrinsicKeyword("Protection from " + gain);
                }

                if (!params.containsKey("Permanent")) {
                    // If not Permanent, remove protection at EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -6573962672873853565L;

                        @Override
                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(tgtC)) {
                                for (final String gain : gains) {
                                    tgtC.removeExtrinsicKeyword("Protection from " + gain);
                                }
                            }
                        }
                    };
                    if (params.containsKey("UntilEndOfCombat")) {
                        AllZone.getEndOfCombat().addUntil(untilEOT);
                    } else {
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }
            }
        }
    } // protectAllResolve()

} // end class AbilityFactory_Protection
