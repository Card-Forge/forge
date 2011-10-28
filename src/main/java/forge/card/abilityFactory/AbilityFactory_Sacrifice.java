package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.MyRandom;
import forge.Player;
import forge.PlayerUtil;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

/**
 * <p>
 * AbilityFactory_Sacrifice class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Sacrifice {
    // **************************************************************
    // *************************** Sacrifice ***********************
    // **************************************************************

    /**
     * <p>
     * createAbilitySacrifice.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilitySacrifice(final AbilityFactory af) {
        final SpellAbility abSacrifice = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -1933592438783630254L;

            @Override
            public boolean canPlayAI() {
                return sacrificeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                sacrificeResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return sacrificeDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return sacrificeTriggerAI(af, this, mandatory);
            }
        };
        return abSacrifice;
    }

    /**
     * <p>
     * createSpellSacrifice.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellSacrifice(final AbilityFactory af) {
        final SpellAbility spSacrifice = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public boolean canPlayAI() {
                return sacrificeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                sacrificeResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return sacrificeDescription(af, this);
            }
        };
        return spSacrifice;
    }

    /**
     * <p>
     * createDrawbackSacrifice.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackSacrifice(final AbilityFactory af) {
        final SpellAbility dbSacrifice = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public void resolve() {
                sacrificeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return sacrificePlayDrawbackAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return sacrificeDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return sacrificeTriggerAI(af, this, mandatory);
            }
        };
        return dbSacrifice;
    }

    /**
     * <p>
     * sacrificeDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String sacrificeDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }

        String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        Target tgt = af.getAbTgt();
        ArrayList<Player> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetPlayers();
        } else {
            tgts = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String valid = params.get("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String num = params.get("Amount");
        num = (num == null) ? "1" : num;
        int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);

        if (valid.equals("Self")) {
            sb.append("Sacrifice ").append(sa.getSourceCard().toString());
        } else if (valid.equals("Card.AttachedBy")) {
            Card toSac = sa.getSourceCard().getEnchantingCard();
            sb.append(toSac.getController()).append(" sacrifices ").append(toSac).append(".");
        } else {
            for (Player p : tgts)
                sb.append(p.getName()).append(" ");

            String msg = params.get("SacMessage");
            if (msg == null) {
                msg = valid;
            }

            sb.append("Sacrifices ").append(amount).append(" ").append(msg).append(".");
        }

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * sacrificeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean sacrificeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        HashMap<String, String> params = af.getMapParams();
        boolean chance = sacrificeTgtAI(af, sa);

        // Some additional checks based on what is being sacrificed, and who is
        // sacrificing
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            String valid = params.get("SacValid");
            String num = params.get("Amount");
            num = (num == null) ? "1" : num;
            int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);

            CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
            list = list.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            if (list.size() == 0) {
                return false;
            }

            Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                int xPay = Math.min(ComputerUtil.determineLeftoverMana(sa), amount);
                source.setSVar("PayX", Integer.toString(xPay));
            }

            int half = amount / 2 + amount % 2; // Half of amount rounded up

            // If the Human has at least half rounded up of the amount to be
            // sacrificed, cast the spell
            if (list.size() < half) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * sacrificePlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean sacrificePlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance = sacrificeTgtAI(af, sa);

        // TODO: restrict the subAbility a bit

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * sacrificeTriggerAI.
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
    public static boolean sacrificeTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        // AI should only activate this during Human's turn
        boolean chance = sacrificeTgtAI(af, sa);

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with not
        // mandatory as part of the checks to cast something

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance || mandatory;
    }

    /**
     * <p>
     * sacrificeTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean sacrificeTgtAI(final AbilityFactory af, final SpellAbility sa) {

        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();
        Target tgt = af.getAbTgt();

        if (tgt != null) {
            tgt.resetTargets();
            if (AllZone.getHumanPlayer().canTarget(sa)) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                return false;
            }
        } else {
            String defined = params.get("Defined");
            if (defined == null) {
                // Self Sacrifice.
            } else if (defined.equals("Each")) {
                // If Sacrifice hits both players:
                // Only cast it if Human has the full amount of valid
                // Only cast it if AI doesn't have the full amount of Valid
                // TODO: Cast if the type is favorable: my "worst" valid is
                // worse than his "worst" valid
                String valid = params.get("SacValid");
                String num = params.containsKey("Amount") ? params.get("Amount") : "1";
                int amount = AbilityFactory.calculateAmount(card, num, sa);

                Card source = sa.getSourceCard();
                if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                    // Set PayX here to maximum value.
                    amount = Math.min(ComputerUtil.determineLeftoverMana(sa), amount);
                }

                CardList humanList = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                humanList = humanList.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
                CardList computerList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                computerList = computerList.getValidCards(valid.split(","), sa.getActivatingPlayer(),
                        sa.getSourceCard());

                // Since all of the cards have remAIDeck:True, I enabled 1 for 1
                // (or X for X) trades for special decks
                if (humanList.size() < amount /*
                                               * || computerList.size() >=
                                               * amount
                                               */) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * sacrificeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void sacrificeResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();

        // Expand Sacrifice keyword here depending on what we need out of it.
        String num = params.containsKey("Amount") ? params.get("Amount") : "1";
        int amount = AbilityFactory.calculateAmount(card, num, sa);

        Target tgt = af.getAbTgt();
        ArrayList<Player> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetPlayers();
        } else {
            tgts = AbilityFactory.getDefinedPlayers(card, params.get("Defined"), sa);
        }

        String valid = params.get("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String msg = params.get("SacMessage");
        if (msg == null) {
            msg = valid;
        }

        msg = "Sacrifice a " + msg;

        boolean remSacrificed = params.containsKey("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        if (valid.equals("Self")) {
            if (AllZone.getZoneOf(card).is(Constant.Zone.Battlefield)) {
                AllZone.getGameAction().sacrifice(card);
            }
            if (remSacrificed) {
                card.addRemembered(card);
            }
        }
        // TODO - maybe this can be done smarter...
        else if (valid.equals("Card.AttachedBy")) {
            Card toSac = card.getEnchantingCard();
            if (AllZone.getZoneOf(card).is(Constant.Zone.Battlefield) && AllZoneUtil.isCardInPlay(toSac)) {
                AllZone.getGameAction().sacrifice(toSac);
                if (remSacrificed) {
                    card.addRemembered(toSac);
                }
            }
        } else if (valid.equals("TriggeredCard")) {
            Card equipee = (Card) sa.getTriggeringObject("Card");
            if (tgts.contains(card.getController()) && AllZoneUtil.isCardInPlay(equipee)) {
                AllZone.getGameAction().sacrifice(equipee);
                if (remSacrificed) {
                    card.addRemembered(equipee);
                }
            }
        } else {
            CardList sacList = null;
            for (Player p : tgts) {

                // TODO - Can only add cards computer sacrificed to remembered
                // list because
                // human sacrifice choices are buried in a Input. Why is this
                // hardcoded
                // into the GUI!?! A better approach would be to have two
                // different methods
                // that return the sacrifice choices for the AI and the human
                // respectively,
                // then actually sacrifice the cards in this resolve method.
                // (ArsenalNut 09/20/2011)
                if (p.isComputer()) {
                    sacList = sacrificeAI(p, amount, valid, sa);
                    if (remSacrificed) {
                        for (int i = 0; i < sacList.size(); i++) {
                            card.addRemembered(sacList.get(i));
                        }
                    }
                } else {
                    sacrificeHuman(p, amount, valid, sa, msg);
                }
            }

        }

    }

    /**
     * <p>
     * sacrificeAI.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static CardList sacrificeAI(final Player p, final int amount, final String valid, final SpellAbility sa) {
        CardList list = p.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

        CardList sacList = ComputerUtil.sacrificePermanents(amount, list);

        return sacList;
    }

    /**
     * <p>
     * sacrificeHuman.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param message
     *            a {@link java.lang.String} object.
     */
    private static void sacrificeHuman(final Player p, final int amount, final String valid, final SpellAbility sa, final String message) {
        CardList list = p.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

        // TODO: Wait for Input to finish before moving on with the rest of
        // Resolution
        AllZone.getInputControl().setInput(PlayerUtil.input_sacrificePermanentsFromList(amount, list, message), true);
    }

    // **************************************************************
    // *********************** SacrificeAll *************************
    // **************************************************************

    /**
     * <p>
     * createAbilitySacrificeAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilitySacrificeAll(final AbilityFactory af) {
        final SpellAbility abSacrifice = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -1933592438783630254L;

            @Override
            public boolean canPlayAI() {
                return sacrificeAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                sacrificeAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return sacrificeAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return sacrificeAllCanPlayAI(af, this);
            }
        };
        return abSacrifice;
    }

    /**
     * <p>
     * createSpellSacrificeAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellSacrificeAll(final AbilityFactory af) {
        final SpellAbility spSacrifice = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public boolean canPlayAI() {
                return sacrificeAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                sacrificeAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return sacrificeAllStackDescription(af, this);
            }
        };
        return spSacrifice;
    }

    /**
     * <p>
     * createDrawbackSacrificeAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackSacrificeAll(final AbilityFactory af) {
        final SpellAbility dbSacrifice = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public void resolve() {
                sacrificeAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public String getStackDescription() {
                return sacrificeAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return sacrificeAllCanPlayAI(af, this);
            }
        };
        return dbSacrifice;
    }

    /**
     * <p>
     * sacrificeAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     * @since 1.0.15
     */
    public static String sacrificeAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening

        StringBuilder sb = new StringBuilder();
        Card host = af.getHostCard();
        HashMap<String, String> params = af.getMapParams();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }

        /*
         * This is not currently targeted ArrayList<Player> tgtPlayers;
         * 
         * Target tgt = af.getAbTgt(); if (tgt != null) tgtPlayers =
         * tgt.getTargetPlayers(); else tgtPlayers =
         * AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
         * params.get("Defined"), sa);
         */

        sb.append("Sacrifice permanents.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * sacrificeAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean sacrificeAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        Random r = MyRandom.random;
        Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final HashMap<String, String> params = af.getMapParams();
        String Valid = "";

        if (params.containsKey("ValidCards")) {
            Valid = params.get("ValidCards");
        }

        if (Valid.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            Valid = Valid.replace("X", Integer.toString(xPay));
        }

        CardList humanlist = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        CardList computerlist = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);

        humanlist = humanlist.getValidCards(Valid.split(","), source.getController(), source);
        computerlist = computerlist.getValidCards(Valid.split(","), source.getController(), source);

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
        if (humanlist.getNotType("Creature").size() == 0 && computerlist.getNotType("Creature").size() == 0) {
            if (CardFactoryUtil.evaluateCreatureList(computerlist) + 200 >= CardFactoryUtil
                    .evaluateCreatureList(humanlist)) {
                return false;
            }
        }// only lands involved
        else if (humanlist.getNotType("Land").size() == 0 && computerlist.getNotType("Land").size() == 0) {
            if (CardFactoryUtil.evaluatePermanentList(computerlist) + 1 >= CardFactoryUtil
                    .evaluatePermanentList(humanlist)) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human
          // permanents are more valuable
        else if (CardFactoryUtil.evaluatePermanentList(computerlist) + 3 >= CardFactoryUtil
                .evaluatePermanentList(humanlist)) {
            return false;
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return ((r.nextFloat() < .9667) && chance);
    }

    /**
     * <p>
     * sacrificeAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static void sacrificeAllResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        Card card = sa.getSourceCard();

        String Valid = "";

        if (params.containsKey("ValidCards")) {
            Valid = params.get("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (Valid.contains("X")) {
            Valid = Valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
        }

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

        boolean remSacrificed = params.containsKey("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        list = list.getValidCards(Valid.split(","), card.getController(), card);

        for (int i = 0; i < list.size(); i++) {
            if (AllZone.getGameAction().sacrifice(list.get(i)) && remSacrificed)
                card.addRemembered(list.get(i));
        }
    }

}// end class AbilityFactory_Sacrifice
