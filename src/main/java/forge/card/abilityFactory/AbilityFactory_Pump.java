package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
import forge.MyRandom;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_Restriction;
import forge.card.spellability.Target;

/**
 * <p>
 * AbilityFactory_Pump class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Pump {

    private final ArrayList<String> keywords = new ArrayList<String>();

    private String numAttack;
    private String numDefense;

    private AbilityFactory abilityFactory = null;
    private HashMap<String, String> params = null;
    private Card hostCard = null;

    /**
     * <p>
     * Constructor for AbilityFactory_Pump.
     * </p>
     * 
     * @param newAF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public AbilityFactory_Pump(final AbilityFactory newAF) {
        this.abilityFactory = newAF;

        this.params = this.abilityFactory.getMapParams();

        this.hostCard = this.abilityFactory.getHostCard();

        this.numAttack = (this.params.containsKey("NumAtt")) ? this.params.get("NumAtt") : "0";
        this.numDefense = (this.params.containsKey("NumDef")) ? this.params.get("NumDef") : "0";

        // Start with + sign now optional
        if (this.numAttack.startsWith("+")) {
            this.numAttack = this.numAttack.substring(1);
        }
        if (this.numDefense.startsWith("+")) {
            this.numDefense = this.numDefense.substring(1);
        }

        if (this.params.containsKey("KW")) {
            final String tmp = this.params.get("KW");
            final String[] kk = tmp.split(" & ");

            this.keywords.clear();
            for (final String element : kk) {
                this.keywords.add(element);
            }
        } else {
            this.keywords.add("none");
        }
    }

    /**
     * <p>
     * getSpellPump.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellPump() {
        final SpellAbility spPump = new Spell(this.hostCard, this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 42244224L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Pump.this.pumpPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Pump.this.pumpStackDescription(AbilityFactory_Pump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Pump.this.pumpResolve(this);
            } // resolve
        }; // SpellAbility

        return spPump;
    }

    /**
     * <p>
     * getAbilityPump.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityPump() {
        final SpellAbility abPump = new Ability_Activated(this.hostCard, this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -1118592153328758083L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Pump.this.pumpPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Pump.this.pumpStackDescription(AbilityFactory_Pump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Pump.this.pumpResolve(this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Pump.this.pumpTriggerAI(AbilityFactory_Pump.this.abilityFactory, this, mandatory);
            }

        }; // SpellAbility

        return abPump;
    }

    /**
     * <p>
     * getDrawbackPump.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackPump() {
        final SpellAbility dbPump = new Ability_Sub(this.hostCard, this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 42244224L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Pump.this.pumpPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Pump.this.pumpStackDescription(AbilityFactory_Pump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Pump.this.pumpResolve(this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactory_Pump.this.pumpDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Pump.this.pumpTriggerAI(AbilityFactory_Pump.this.abilityFactory, this, mandatory);
            }
        }; // SpellAbility

        return dbPump;
    }

    /**
     * <p>
     * Getter for the field <code>numAttack</code>.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    private int getNumAttack(final SpellAbility sa) {
        return AbilityFactory.calculateAmount(this.hostCard, this.numAttack, sa);
    }

    /**
     * <p>
     * Getter for the field <code>numDefense</code>.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    private int getNumDefense(final SpellAbility sa) {
        return AbilityFactory.calculateAmount(this.hostCard, this.numDefense, sa);
    }

    /**
     * <p>
     * getPumpCreatures.
     * </p>
     * 
     * @param defense
     *            a int.
     * @param attack
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private CardList getPumpCreatures(final int defense, final int attack) {

        final boolean kHaste = this.keywords.contains("Haste");
        final boolean evasive = (this.keywords.contains("Flying") || this.keywords.contains("Horsemanship")
                || this.keywords.contains("HIDDEN Unblockable") || this.keywords.contains("Fear") || this.keywords
                .contains("Intimidate"));
        final boolean kSize = !this.keywords.get(0).equals("none");
        String[] kwPump = { "none" };
        if (!this.keywords.get(0).equals("none")) {
            kwPump = this.keywords.toArray(new String[this.keywords.size()]);
        }
        final String[] keywords = kwPump;

        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (!CardFactoryUtil.canTarget(AbilityFactory_Pump.this.hostCard, c)) {
                    return false;
                }

                if ((c.getNetDefense() + defense) <= 0) {
                    return false;
                }

                // Don't add duplicate keywords
                final boolean hKW = c.hasAnyKeyword(keywords);
                if (kSize && hKW) {
                    return false;
                }

                // give haste to creatures that could attack with it
                if (c.hasSickness() && kHaste && AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer())
                        && CombatUtil.canAttackNextTurn(c)
                        && AllZone.getPhase().isBefore(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
                    return true;
                }

                // give evasive keywords to creatures that can attack
                if (evasive && AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer()) && CombatUtil.canAttack(c)
                        && AllZone.getPhase().isBefore(Constant.Phase.COMBAT_DECLARE_ATTACKERS)
                        && (c.getNetCombatDamage() > 0)) {
                    return true;
                }

                // will the creature attack (only relevant for sorcery speed)?
                if (CardFactoryUtil.AI_doesCreatureAttack(c)
                        && AllZone.getPhase().isBefore(Constant.Phase.COMBAT_DECLARE_ATTACKERS)
                        && AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer())) {
                    return true;
                }

                // is the creature blocking and unable to destroy the attacker
                // or would be destroyed itself?
                if (c.isBlocking()
                        && (CombatUtil.blockerWouldBeDestroyed(c) || !CombatUtil.attackerWouldBeDestroyed(AllZone
                                .getCombat().getAttackerBlockedBy(c)))) {
                    return true;
                }

                // is the creature unblocked and the spell will pump its power?
                if (AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_BLOCKERS)
                        && AllZone.getCombat().isAttacking(c) && AllZone.getCombat().isUnblocked(c) && (attack > 0)) {
                    return true;
                }

                // is the creature blocked and the blocker would survive
                if (AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_BLOCKERS)
                        && AllZone.getCombat().isAttacking(c) && AllZone.getCombat().isBlocked(c)
                        && (AllZone.getCombat().getBlockers(c) != null)
                        && !CombatUtil.blockerWouldBeDestroyed(AllZone.getCombat().getBlockers(c).get(0))) {
                    return true;
                }

                // if the life of the computer is in danger, try to pump
                // potential blockers before declaring blocks
                if (CombatUtil.lifeInDanger(AllZone.getCombat())
                        && AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_ATTACKERS)
                        && AllZone.getPhase().isBefore(Constant.Phase.MAIN2)
                        && CombatUtil.canBlock(c, AllZone.getCombat())
                        && AllZone.getPhase().isPlayerTurn(AllZone.getHumanPlayer())) {
                    return true;
                }

                return false;
            }
        });
        return list;
    } // getPumpCreatures()

    /**
     * <p>
     * getCurseCreatures.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param defense
     *            a int.
     * @param attack
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private CardList getCurseCreatures(final SpellAbility sa, final int defense, final int attack) {
        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
        list = list.getTargetableCards(this.hostCard);

        if ((defense < 0) && !list.isEmpty()) { // with spells that give -X/-X,
                                                // compi will try to destroy a
                                                // creature
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    if (c.getNetDefense() <= -defense) {
                        return true; // can kill indestructible creatures
                    }
                    return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                }
            }); // leaves all creatures that will be destroyed
        } // -X/-X end
        else if (!list.isEmpty()) {
            String[] kwPump = { "none" };
            if (!this.keywords.get(0).equals("none")) {
                kwPump = this.keywords.toArray(new String[this.keywords.size()]);
            }
            final String[] keywords = kwPump;
            final boolean addsKeywords = this.keywords.size() > 0;

            if (addsKeywords) {
                if (!this.containsCombatRelevantKeyword(this.keywords)
                        && AllZone.getPhase().isBefore(Constant.Phase.MAIN2)) {
                    list.clear(); // this keyword is not combat relevenat
                }

                list = list.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return !c.hasAnyKeyword(keywords); // don't add duplicate
                                                      // negative keywords
                    }
                });
            }
        }

        return list;
    } // getCurseCreatures()

    private boolean containsCombatRelevantKeyword(final ArrayList<String> keywords) {
        boolean flag = false;
        for (final String keyword : keywords) {
            // since most keywords are combat relevant check for those that are
            // not
            if (!keyword.equals("HIDDEN This card doesn't untap during your next untap step.")) {
                flag = true;
            }
        }
        return flag;
    }

    /**
     * <p>
     * pumpPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpPlayAI(final SpellAbility sa) {
        // if there is no target and host card isn't in play, don't activate
        if ((this.abilityFactory.getAbTgt() == null) && !AllZoneUtil.isCardInPlay(this.hostCard)) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        if (!CostUtil.checkLifeCost(cost, this.hostCard, 4)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(cost, this.hostCard)) {
            return false;
        }

        if (!CostUtil.checkCreatureSacrificeCost(cost, this.hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, this.hostCard)) {
            return false;
        }

        final SpellAbility_Restriction restrict = sa.getRestrictions();

        // Phase Restrictions
        if ((AllZone.getStack().size() == 0) && AllZone.getPhase().isBefore(Constant.Phase.COMBAT_BEGIN)) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty
            if (!this.abilityFactory.isCurse() && !AbilityFactory.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (AllZone.getStack().size() > 0) {
            // TODO: pump something only if the top thing on the stack will kill
            // it via damage
            // or if top thing on stack will pump it/enchant it and I want to
            // kill it
            return false;
        }

        final int activations = restrict.getNumberTurnActivations();
        final int sacActivations = restrict.getActivationNumberSacrifice();
        // don't risk sacrificing a creature just to pump it
        if ((sacActivations != -1) && (activations >= (sacActivations - 1))) {
            return false;
        }

        final Card source = sa.getSourceCard();
        if (source.getSVar("X").equals("Count$xPaid")) {
            source.setSVar("PayX", "");
        }

        int defense;
        if (this.numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
        } else {
            defense = this.getNumDefense(sa);
        }

        int attack;
        if (this.numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final String toPay = source.getSVar("PayX");

            if (toPay.equals("")) {
                final int xPay = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(toPay);
            }
        } else {
            attack = this.getNumAttack(sa);
        }

        if ((this.abilityFactory.getAbTgt() == null) || !this.abilityFactory.getAbTgt().doesTarget()) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                    this.params.get("Defined"), sa);

            if (cards.size() == 0) {
                return false;
            }

            // when this happens we need to expand AI to consider if its ok for
            // everything?
            for (final Card card : cards) {
                // TODO: if AI doesn't control Card and Pump is a Curse, than
                // maybe use?
                if (((card.getNetDefense() + defense) > 0) && (!card.hasAnyKeyword(this.keywords))) {
                    if (card.hasSickness() && this.keywords.contains("Haste")) {
                        return true;
                    } else if (card.hasSickness() ^ this.keywords.contains("Haste")) {
                        return false;
                    } else if (this.hostCard.equals(card)) {
                        final Random r = MyRandom.getRandom();
                        if (r.nextFloat() <= Math.pow(.6667, activations)) {
                            return CardFactoryUtil.AI_doesCreatureAttack(card) && !sa.getPayCosts().getTap();
                        }
                    } else {
                        final Random r = MyRandom.getRandom();
                        return (r.nextFloat() <= Math.pow(.6667, activations));
                    }
                }
            }
        } else {
            return this.pumpTgtAI(sa, defense, attack, false);
        }

        return false;
    } // pumpPlayAI()

    /**
     * <p>
     * pumpTgtAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param defense
     *            a int.
     * @param attack
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean pumpTgtAI(final SpellAbility sa, final int defense, final int attack, final boolean mandatory) {
        if (!mandatory && AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && !(this.abilityFactory.isCurse() && ((defense < 0) || !this.containsCombatRelevantKeyword(this.keywords)))) {
            return false;
        }

        final Target tgt = this.abilityFactory.getAbTgt();
        tgt.resetTargets();
        CardList list;
        if (this.abilityFactory.isCurse()) {
            list = this.getCurseCreatures(sa, defense, attack);
        } else {
            list = this.getPumpCreatures(defense, attack);
        }

        list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

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
            return mandatory && this.pumpMandatoryTarget(this.abilityFactory, sa, mandatory);
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return this.pumpMandatoryTarget(this.abilityFactory, sa, mandatory);
                    }

                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = CardFactoryUtil.AI_getBestCreature(list);
            tgt.addTarget(t);
            list.remove(t);
        }

        return true;
    } // pumpTgtAI()

    /**
     * <p>
     * pumpMandatoryTarget.
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
    private boolean pumpMandatoryTarget(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
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

        CardList pref;
        CardList forced;
        final Card source = sa.getSourceCard();

        if (af.isCurse()) {
            pref = list.getController(AllZone.getHumanPlayer());
            forced = list.getController(AllZone.getComputerPlayer());
        } else {
            pref = list.getController(AllZone.getComputerPlayer());
            forced = list.getController(AllZone.getHumanPlayer());
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (pref.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.AI_getBestCreature(pref);
            } else {
                c = CardFactoryUtil.AI_getMostExpensivePermanent(pref, source, true);
            }

            pref.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (forced.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.AI_getWorstCreature(forced);
            } else {
                c = CardFactoryUtil.AI_getCheapestPermanent(forced, source, true);
            }

            forced.remove(c);

            tgt.addTarget(c);
        }

        if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    /**
     * <p>
     * pumpTriggerAI.
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
    private boolean pumpTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Card source = sa.getSourceCard();

        int defense;
        if (this.numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
        } else {
            defense = this.getNumDefense(sa);
        }

        int attack;
        if (this.numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final String toPay = source.getSVar("PayX");

            if (toPay.equals("")) {
                final int xPay = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(toPay);
            }
        } else {
            attack = this.getNumAttack(sa);
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return this.pumpTgtAI(sa, defense, attack, mandatory);
        }

        return true;
    } // pumpTriggerAI

    /**
     * <p>
     * pumpDrawbackAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpDrawbackAI(final SpellAbility sa) {
        final Card source = sa.getSourceCard();
        int defense;
        if (this.numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            defense = Integer.parseInt(source.getSVar("PayX"));
        } else {
            defense = this.getNumDefense(sa);
        }

        int attack;
        if (this.numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            attack = Integer.parseInt(source.getSVar("PayX"));
        } else {
            attack = this.getNumAttack(sa);
        }

        if ((this.abilityFactory.getAbTgt() == null) || !this.abilityFactory.getAbTgt().doesTarget()) {
            if (this.hostCard.isCreature()) {
                if (!this.hostCard.hasKeyword("Indestructible")
                        && ((this.hostCard.getNetDefense() + defense) <= this.hostCard.getDamage())) {
                    return false;
                }
                if ((this.hostCard.getNetDefense() + defense) <= 0) {
                    return false;
                }
            }
        } else {
            return this.pumpTgtAI(sa, defense, attack, false);
        }

        return true;
    } // pumpDrawbackAI()

    /**
     * <p>
     * pumpStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String pumpStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when damageStackDescription is called, just build exactly what is
        // happening
        final StringBuilder sb = new StringBuilder();
        final String name = af.getHostCard().getName();

        ArrayList<Card> tgtCards;
        final Target tgt = this.abilityFactory.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), this.params.get("Defined"), sa);
        }

        if (tgtCards.size() > 0) {

            if (sa instanceof Ability_Sub) {
                sb.append(" ");
            } else {
                sb.append(name).append(" - ");
            }

            for (final Card c : tgtCards) {
                sb.append(c.getName()).append(" ");
            }

            if (af.getMapParams().containsKey("Radiance")) {
                sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                        .append(" that shares a color with ");
                if (tgtCards.size() > 1) {
                    sb.append("them ");
                } else {
                    sb.append("it ");
                }
            }

            final int atk = this.getNumAttack(sa);
            final int def = this.getNumDefense(sa);

            sb.append("gains ");
            if ((atk != 0) || (def != 0)) {
                if (atk >= 0) {
                    sb.append("+");
                }
                sb.append(atk);
                sb.append("/");
                if (def >= 0) {
                    sb.append("+");
                }
                sb.append(def);
                sb.append(" ");
            }

            for (int i = 0; i < this.keywords.size(); i++) {
                if (!this.keywords.get(i).equals("none")) {
                    sb.append(this.keywords.get(i)).append(" ");
                }
            }

            if (!this.params.containsKey("Permanent")) {
                sb.append("until end of turn.");
            }
        }

        final Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // pumpStackDescription()

    /**
     * <p>
     * pumpResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void pumpResolve(final SpellAbility sa) {
        ArrayList<Card> tgtCards;
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        final Target tgt = this.abilityFactory.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(this.hostCard, this.params.get("Defined"), sa);
        }

        if (this.params.containsKey("Radiance")) {
            for (final Card c : CardUtil.getRadiance(this.hostCard, tgtCards.get(0), this.params.get("ValidTgts")
                    .split(","))) {
                untargetedCards.add(c);
            }
        }

        final Zone pumpZone = this.params.containsKey("PumpZone") ? Zone.smartValueOf(this.params.get("PumpZone"))
                : Zone.Battlefield;

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);

            // only pump things in PumpZone
            if (!AllZoneUtil.getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            // if pump is a target, make sure we can still target now
            if ((tgt != null) && !CardFactoryUtil.canTarget(this.abilityFactory.getHostCard(), tgtC)) {
                continue;
            }

            this.applyPump(sa, tgtC);
        }

        for (int i = 0; i < untargetedCards.size(); i++) {
            final Card tgtC = untargetedCards.get(i);
            // only pump things in PumpZone
            if (!AllZoneUtil.getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            this.applyPump(sa, tgtC);
        }
    } // pumpResolve()

    private void applyPump(final SpellAbility sa, final Card applyTo) {
        final int a = this.getNumAttack(sa);
        final int d = this.getNumDefense(sa);

        applyTo.addTempAttackBoost(a);
        applyTo.addTempDefenseBoost(d);

        for (int i = 0; i < this.keywords.size(); i++) {
            if (!this.keywords.get(i).equals("none")) {
                applyTo.addExtrinsicKeyword(this.keywords.get(i));
            }
        }

        if (!this.params.containsKey("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -42244224L;

                @Override
                public void execute() {
                    if (AllZoneUtil.isCardInPlay(applyTo)) {
                        applyTo.addTempAttackBoost(-1 * a);
                        applyTo.addTempDefenseBoost(-1 * d);

                        if (AbilityFactory_Pump.this.keywords.size() > 0) {
                            for (int i = 0; i < AbilityFactory_Pump.this.keywords.size(); i++) {
                                if (!AbilityFactory_Pump.this.keywords.get(i).equals("none")) {
                                    applyTo.removeExtrinsicKeyword(AbilityFactory_Pump.this.keywords.get(i));
                                }
                            }
                        }

                    }
                }
            };
            if (this.params.containsKey("UntilEndOfCombat")) {
                AllZone.getEndOfCombat().addUntil(untilEOT);
            } else if (this.params.containsKey("UntilYourNextUpkeep")) {
                AllZone.getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else {
                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    // ///////////////////////////////////
    //
    // PumpAll
    //
    // ////////////////////////////////////

    /**
     * <p>
     * getAbilityPumpAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityPumpAll() {
        final SpellAbility abPumpAll = new Ability_Activated(this.hostCard, this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -8299417521903307630L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Pump.this.pumpAllCanPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Pump.this.pumpAllStackDescription(AbilityFactory_Pump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Pump.this.pumpAllResolve(this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Pump.this.pumpAllTriggerAI(AbilityFactory_Pump.this.abilityFactory, this, mandatory);
            }

        }; // SpellAbility

        return abPumpAll;
    }

    /**
     * <p>
     * getSpellPumpAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellPumpAll() {
        final SpellAbility spPumpAll = new Spell(this.hostCard, this.abilityFactory.getAbCost(), this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -4055467978660824703L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Pump.this.pumpAllCanPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Pump.this.pumpAllStackDescription(AbilityFactory_Pump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Pump.this.pumpAllResolve(this);
            } // resolve
        }; // SpellAbility

        return spPumpAll;
    }

    /**
     * <p>
     * getDrawbackPumpAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackPumpAll() {
        final SpellAbility dbPumpAll = new Ability_Sub(this.hostCard, this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 6411531984691660342L;

            @Override
            public String getStackDescription() {
                return AbilityFactory_Pump.this.pumpAllStackDescription(AbilityFactory_Pump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Pump.this.pumpAllResolve(this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactory_Pump.this.pumpAllChkDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Pump.this.pumpAllTriggerAI(AbilityFactory_Pump.this.abilityFactory, this, mandatory);
            }
        }; // SpellAbility

        return dbPumpAll;
    }

    /**
     * <p>
     * pumpAllCanPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpAllCanPlayAI(final SpellAbility sa) {
        String valid = "";
        final Random r = MyRandom.getRandom();
        // final Card source = sa.getSourceCard();
        this.params = this.abilityFactory.getMapParams();
        final int defense = this.getNumDefense(sa);

        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn()); // to
        // prevent
        // runaway
        // activations

        if (this.params.containsKey("ValidCards")) {
            valid = this.params.get("ValidCards");
        }

        CardList comp = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        comp = comp.getValidCards(valid, this.hostCard.getController(), this.hostCard);
        CardList human = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        human = human.getValidCards(valid, this.hostCard.getController(), this.hostCard);

        // only count creatures that can attack
        human = human.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CombatUtil.canAttack(c) && !AbilityFactory_Pump.this.abilityFactory.isCurse();
            }
        });

        if (this.abilityFactory.isCurse()) {
            if (defense < 0) { // try to destroy creatures
                comp = comp.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        if (c.getNetDefense() <= -defense) {
                            return true; // can kill indestructible creatures
                        }
                        return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                    }
                }); // leaves all creatures that will be destroyed
                human = human.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        if (c.getNetDefense() <= -defense) {
                            return true; // can kill indestructible creatures
                        }
                        return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                    }
                }); // leaves all creatures that will be destroyed
            } // -X/-X end

            // evaluate both lists and pass only if human creatures are more
            // valuable
            if ((CardFactoryUtil.evaluateCreatureList(comp) + 200) >= CardFactoryUtil.evaluateCreatureList(human)) {
                return false;
            }

            return chance;
        } // end Curse

        // don't use non curse PumpAll after Combat_Begin until AI is improved
        if (AllZone.getPhase().isAfter(Constant.Phase.COMBAT_BEGIN)) {
            return false;
        }

        if ((comp.size() <= human.size()) || (comp.size() <= 1)) {
            return false;
        }

        return (r.nextFloat() < .6667) && chance;
    } // pumpAllCanPlayAI()

    /**
     * <p>
     * pumpAllResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void pumpAllResolve(final SpellAbility sa) {
        final AbilityFactory af = sa.getAbilityFactory();
        CardList list;
        ArrayList<Player> tgtPlayers = null;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (this.params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), this.params.get("Defined"), sa);
        }

        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        } else {
            list = tgtPlayers.get(0).getCardsIn(Zone.Battlefield);
        }

        String valid = "";
        if (this.params.containsKey("ValidCards")) {
            valid = this.params.get("ValidCards");
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        final int a = this.getNumAttack(sa);
        final int d = this.getNumDefense(sa);

        for (final Card c : list) {
            final Card tgtC = c;

            // only pump things in play
            if (!AllZoneUtil.isCardInPlay(tgtC)) {
                continue;
            }

            tgtC.addTempAttackBoost(a);
            tgtC.addTempDefenseBoost(d);

            for (int i = 0; i < this.keywords.size(); i++) {
                if (!this.keywords.get(i).equals("none")) {
                    tgtC.addExtrinsicKeyword(this.keywords.get(i));
                }
            }

            if (!this.params.containsKey("Permanent")) {
                // If not Permanent, remove Pumped at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 5415795460189457660L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(tgtC)) {
                            tgtC.addTempAttackBoost(-1 * a);
                            tgtC.addTempDefenseBoost(-1 * d);

                            if (AbilityFactory_Pump.this.keywords.size() > 0) {
                                for (int i = 0; i < AbilityFactory_Pump.this.keywords.size(); i++) {
                                    if (!AbilityFactory_Pump.this.keywords.get(i).equals("none")) {
                                        tgtC.removeExtrinsicKeyword(AbilityFactory_Pump.this.keywords.get(i));
                                    }
                                }
                            }
                        }
                    }
                };

                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // pumpAllResolve()

    /**
     * <p>
     * pumpAllTriggerAI.
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
    private boolean pumpAllTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        // TODO: add targeting consideration such as
        // "Creatures target player controls gets"

        return true;
    }

    /**
     * <p>
     * pumpAllChkDrawbackAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpAllChkDrawbackAI(final SpellAbility sa) {
        return true;
    }

    /**
     * <p>
     * pumpAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String pumpAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (this.params.containsKey("SpellDescription")) {
            desc = this.params.get("SpellDescription");
        } else if (this.params.containsKey("PumpAllDescription")) {
            desc = this.params.get("PumpAllDescription");
        }

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
        sb.append(desc);

        final Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // pumpAllStackDescription()

} // end class AbilityFactory_Pump
