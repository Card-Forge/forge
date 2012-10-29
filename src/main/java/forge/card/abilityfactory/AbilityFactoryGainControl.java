/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;

import forge.CardLists;
import forge.Command;
import forge.GameEntity;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;


//AB:GainControl|ValidTgts$Creature|TgtPrompt$Select target legendary creature|LoseControl$Untap,LoseControl|SpellDescription$Gain control of target xxxxxxx

//GainControl specific params:
//  LoseControl - the lose control conditions (as a comma separated list)
//  -Untap - source card becomes untapped
//  -LoseControl - you lose control of source card
//  -LeavesPlay - source card leaves the battlefield
//  -PowerGT - (not implemented yet for Old Man of the Sea)
//  AddKWs - Keywords to add to the controlled card
//            (as a "&"-separated list; like Haste, Sacrifice CARDNAME at EOT, any standard keyword)
//  OppChoice - set to True if opponent chooses creature (for Preacher) - not implemented yet
//  Untap - set to True if target card should untap when control is taken
//  DestroyTgt - actions upon which the tgt should be destroyed.  same list as LoseControl
//  NoRegen - set if destroyed creature can't be regenerated.  used only with DestroyTgt

/**
 * <p>
 * AbilityFactory_GainControl class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryGainControl {

    private AbilityFactory af = null;
    private HashMap<String, String> params = null;
    private Card hostCard = null;
    private ArrayList<String> lose = null;
    private ArrayList<String> destroyOn = null;
    private boolean bNoRegen = false;
    private boolean bUntap = false;
    private boolean bTapOnLose = false;
    private ArrayList<String> kws = null;

    /**
     * <p>
     * Constructor for AbilityFactory_GainControl.
     * </p>
     * 
     * @param newAF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactoryGainControl(final AbilityFactory newAF) {
        this.af = newAF;
        this.params = this.af.getMapParams();
        this.hostCard = this.af.getHostCard();
        if (this.params.containsKey("LoseControl")) {
            this.lose = new ArrayList<String>(Arrays.asList(this.params.get("LoseControl").split(",")));
        }
        if (this.params.containsKey("Untap")) {
            this.bUntap = true;
        }
        if (this.params.containsKey("TapOnLose")) {
            this.bTapOnLose = true;
        }
        if (this.params.containsKey("AddKWs")) {
            this.kws = new ArrayList<String>(Arrays.asList(this.params.get("AddKWs").split(" & ")));
        }
        if (this.params.containsKey("DestroyTgt")) {
            this.destroyOn = new ArrayList<String>(Arrays.asList(this.params.get("DestroyTgt").split(",")));
        }
        if (this.params.containsKey("NoRegen")) {
            this.bNoRegen = true;
        }
    }

    /**
     * <p>
     * getSpellGainControl.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getSpellGainControl() {
        final SpellAbility spControl = new Spell(this.hostCard, this.af.getAbCost(), this.af.getAbTgt()) {
            private static final long serialVersionUID = 3125489644424832311L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryGainControl.this.gainControlTgtAI(getActivatingPlayer(), this);
            }

            @Override
            public void resolve() {
                AbilityFactoryGainControl.this.gainControlResolve(this);
            } // resolve

            @Override
            public String getStackDescription() {
                return AbilityFactoryGainControl.this.gainControlStackDescription(this);
            }
        }; // SpellAbility

        return spControl;
    }

    /**
     * <p>
     * getAbilityGainControl.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getAbilityGainControl() {
        class AbilityGainControl extends AbilityActivated {
            public AbilityGainControl(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityGainControl(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -4384705198674678831L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryGainControl.this.gainControlTgtAI(getActivatingPlayer(), this);
            }

            @Override
            public void resolve() {
                AbilityFactoryGainControl.this.gainControlResolve(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryGainControl.this.gainControlStackDescription(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryGainControl.this.gainControlTgtAI(getActivatingPlayer(), this);
            }
        }
        final SpellAbility abControl = new AbilityGainControl(this.hostCard, this.af.getAbCost(), this.af.getAbTgt());

        return abControl;
    }

    /**
     * <p>
     * getDrawbackGainControl.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getDrawbackGainControl() {
        class DrawbackGainControl extends AbilitySub {
            public DrawbackGainControl(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackGainControl(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -5577742598032345880L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryGainControl.this.gainControlTgtAI(getActivatingPlayer(), this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryGainControl.this.gainControlStackDescription(this);
            }

            @Override
            public void resolve() {
                AbilityFactoryGainControl.this.gainControlResolve(this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryGainControl.this.gainControlDrawbackAI(getActivatingPlayer(), this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryGainControl.this.gainControlTriggerAI(getActivatingPlayer(), this, mandatory);
            }
        }
        final SpellAbility dbControl = new DrawbackGainControl(this.hostCard, this.af.getAbTgt()); // SpellAbility

        return dbControl;
    }

    /**
     * <p>
     * gainControlStackDescription.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String gainControlStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !this.params.containsKey("Defined")) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), this.params.get("Defined"), sa);
        }

        ArrayList<Player> newController = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                this.params.get("NewController"), sa);
        if ((tgt != null) && tgt.getTargetPlayers() != null && !tgt.getTargetPlayers().isEmpty()) {
            newController = tgt.getTargetPlayers();
        }
        if (newController.size() == 0) {
            newController.add(sa.getActivatingPlayer());
        }

        sb.append(newController).append(" gains control of ");

        for (final Card c : tgtCards) {
            sb.append(" ");
            if (c.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(c);
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
     * gainControlTgtAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean gainControlTgtAI(final Player ai, final SpellAbility sa) {
        boolean hasCreature = false;
        boolean hasArtifact = false;
        boolean hasEnchantment = false;
        boolean hasLand = false;

        final Target tgt = sa.getTarget();
        Player opp = ai.getOpponent();

        // if Defined, then don't worry about targeting
        if (tgt == null) {
            return true;
        } else {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                if (!opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.addTarget(opp);
            }
        }

        List<Card> list = 
                CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final Map<String, String> vars = c.getSVars();
                return !vars.containsKey("RemAIDeck") && c.canBeTargetedBy(sa);
            }
        });

        if (list.isEmpty()) {
            return false;
        }

        // Don't steal something if I can't Attack without, or prevent it from
        // blocking at least
        if ((this.lose != null) && this.lose.contains("EOT")
                && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            for (final Card c : list) {
                if (c.isCreature()) {
                    hasCreature = true;
                }
                if (c.isArtifact()) {
                    hasArtifact = true;
                }
                if (c.isLand()) {
                    hasLand = true;
                }
                if (c.isEnchantment()) {
                    hasEnchantment = true;
                }
            }

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (hasCreature) {
                t = CardFactoryUtil.getBestCreatureAI(list);
            } else if (hasArtifact) {
                t = CardFactoryUtil.getBestArtifactAI(list);
            } else if (hasLand) {
                t = CardFactoryUtil.getBestLandAI(list);
            } else if (hasEnchantment) {
                t = CardFactoryUtil.getBestEnchantmentAI(list, sa, true);
            } else {
                t = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, true);
            }

            tgt.addTarget(t);
            list.remove(t);

            hasCreature = false;
            hasArtifact = false;
            hasLand = false;
            hasEnchantment = false;
        }

        return true;

    }

    /**
     * <p>
     * gainControlResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void gainControlResolve(final SpellAbility sa) {
        List<Card> tgtCards = new ArrayList<Card>();
        Card source = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        if (this.params.containsKey("AllValid")) {
            tgtCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            tgtCards = AbilityFactory.filterListByType(tgtCards, this.params.get("AllValid"), sa);
        } else if ((tgt != null) && !this.params.containsKey("Defined")) {
            tgtCards.addAll(tgt.getTargetCards());
        } else {
            tgtCards.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), this.params.get("Defined"), sa));
        }

        ArrayList<Player> controllers = new ArrayList<Player>();

        if (this.params.containsKey("NewController")) {
            controllers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), this.params.get("NewController"), sa);
        } else if ((tgt != null) && (tgt.getTargetPlayers() != null) && tgt.canTgtPlayer()) {
            controllers = tgt.getTargetPlayers();
        }

        GameEntity newController;

        if (controllers.size() == 0) {
            if (sa.isSpell()) {
                newController = sa.getActivatingPlayer();
            } else {
                newController = source;
            }
        } else {
            newController = controllers.get(0);
        }
        // check for lose control criteria right away
        if (this.lose != null && this.lose.contains("LeavesPlay") && !source.isInZone(ZoneType.Battlefield)) {
            return;
        }
        if (this.lose != null && this.lose.contains("Untap") && !source.isTapped()) {
            return;
        }

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);
            final Player originalController = tgtC.getController();

            if (!tgtC.equals(sa.getSourceCard()) && !sa.getSourceCard().getGainControlTargets().contains(tgtC)) {
                sa.getSourceCard().addGainControlTarget(tgtC);
            }

            if (tgtC.isInPlay()) {

                if (!tgtC.equals(newController)) {
                    tgtC.addController(newController);
                }
                // Singletons.getModel().getGameAction().changeController(new ArrayList<Card>(tgtC),
                // tgtC.getController(), newController.get(0));

                if (this.bUntap) {
                    tgtC.untap();
                }

                if (null != this.kws) {
                    for (final String kw : this.kws) {
                        tgtC.addExtrinsicKeyword(kw);
                    }
                }
            }

            // end copied

            if (this.lose != null) {
                if (this.lose.contains("LeavesPlay")) {
                    sa.getSourceCard().addLeavesPlayCommand(this.getLoseControlCommand(tgtC, originalController, newController));
                }
                if (this.lose.contains("Untap")) {
                    sa.getSourceCard().addUntapCommand(this.getLoseControlCommand(tgtC, originalController, newController));
                }
                if (this.lose.contains("LoseControl")) {
                    sa.getSourceCard().addChangeControllerCommand(this.getLoseControlCommand(tgtC, originalController, newController));
                }
                if (this.lose.contains("EOT")) {
                    Singletons.getModel().getGame().getEndOfTurn().addAt(this.getLoseControlCommand(tgtC, originalController, newController));
                }
            }

            if (this.destroyOn != null) {
                if (this.destroyOn.contains("LeavesPlay")) {
                    sa.getSourceCard().addLeavesPlayCommand(this.getDestroyCommand(tgtC));
                }
                if (this.destroyOn.contains("Untap")) {
                    sa.getSourceCard().addUntapCommand(this.getDestroyCommand(tgtC));
                }
                if (this.destroyOn.contains("LoseControl")) {
                    sa.getSourceCard().addChangeControllerCommand(this.getDestroyCommand(tgtC));
                }
            }

            sa.getSourceCard().clearGainControlReleaseCommands();
            sa.getSourceCard().addGainControlReleaseCommand(this.getLoseControlCommand(tgtC, originalController, newController));

        } // end foreach target
    }

    /**
     * <p>
     * gainControlTriggerAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean gainControlTriggerAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return this.gainControlTgtAI(ai, sa);
        }

        return true;
    }

    /**
     * <p>
     * gainControlDrawbackAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean gainControlDrawbackAI(final Player ai, final SpellAbility sa) {
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            if (this.params.containsKey("AllValid")) {
                List<Card> tgtCards = CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), ai.getOpponent());
                tgtCards = AbilityFactory.filterListByType(tgtCards, this.params.get("AllValid"), sa);
                if (tgtCards.isEmpty()) {
                    return false;
                }
            }
            if ((this.lose != null) && this.lose.contains("EOT")
                    && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else {
            return this.gainControlTgtAI(ai, sa);
        }

        return true;
    } // pumpDrawbackAI()

    /**
     * <p>
     * getDestroyCommand.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.Command} object.
     */
    private Command getDestroyCommand(final Card c) {
        final Command destroy = new Command() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void execute() {
                final Ability ability = new Ability(AbilityFactoryGainControl.this.hostCard, "0") {
                    @Override
                    public void resolve() {

                        if (AbilityFactoryGainControl.this.bNoRegen) {
                            Singletons.getModel().getGame().getAction().destroyNoRegeneration(c);
                        } else {
                            Singletons.getModel().getGame().getAction().destroy(c);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(AbilityFactoryGainControl.this.hostCard).append(" - destroy ").append(c.getName())
                        .append(".");
                if (AbilityFactoryGainControl.this.bNoRegen) {
                    sb.append("  It can't be regenerated.");
                }
                ability.setStackDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
            }

        };
        return destroy;
    }

    /**
     * <p>
     * getLoseControlCommand.
     * </p>
     * 
     * @param i
     *            a int.
     * @param originalController
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.Command} object.
     */
    private Command getLoseControlCommand(final Card c, final Player originalController, final GameEntity newController) {
        final Command loseControl = new Command() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void execute() {
                AbilityFactoryGainControl.doLoseControl(c, AbilityFactoryGainControl.this.hostCard,
                        AbilityFactoryGainControl.this.bTapOnLose, AbilityFactoryGainControl.this.kws,
                        newController);
            } // execute()
        };

        return loseControl;
    }

    private static void doLoseControl(final Card c, final Card host, final boolean tapOnLose,
            final ArrayList<String> addedKeywords, final GameEntity newController) {
        if (null == c) {
            return;
        }
        if (c.isInPlay()) {
            c.removeController(newController);
            // Singletons.getModel().getGameAction().changeController(new ArrayList<Card>(c),
            // c.getController(), originalController);

            if (tapOnLose) {
                c.tap();
            }

            if (null != addedKeywords) {
                for (final String kw : addedKeywords) {
                    c.removeExtrinsicKeyword(kw);
                }
            }
        } // if
        host.clearGainControlTargets();
        host.clearGainControlReleaseCommands();
    }

    // *************************************************************************
    // ******************************* ExchangeControl ***********************************
    // *************************************************************************
    /**
     * <p>
     * getAbilityExchangeControl.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityExchangeControl() {
        class AbilityExchangeControl extends AbilityActivated {
            public AbilityExchangeControl(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityExchangeControl(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1831356710492849854L;
            private final AbilityFactory af = AbilityFactoryGainControl.this.af;

            @Override
            public String getStackDescription() {
                return exchangeControlStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                return exchangeControlCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                exchangeControlResolve(this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return exchangeControlDoTriggerAI(getActivatingPlayer(), this.af, this, mandatory);
            }
        }

        final SpellAbility abExchangeControl = new AbilityExchangeControl(this.af.getHostCard(),
                this.af.getAbCost(), this.af.getAbTgt());

        return abExchangeControl;
    }

    /**
     * <p>
     * getSpellExchangeControl.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellExchangeControl() {
        final SpellAbility spExchangeControl = new Spell(this.af.getHostCard(), this.af.getAbCost(),
                this.af.getAbTgt()) {
            private static final long serialVersionUID = 8004957182752960518L;
            private final AbilityFactory af = AbilityFactoryGainControl.this.af;

            @Override
            public String getStackDescription() {
                return exchangeControlStackDescription(this.af, this);
            }

            @Override
            public boolean canPlayAI() {
                return exchangeControlCanPlayAI(getActivatingPlayer(), this.af, this);
            }

            @Override
            public void resolve() {
                exchangeControlResolve(this.af, this);
            }

        };
        return spExchangeControl;
    }

    /**
     * <p>
     * getDrawbackExchangeControl.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackExchangeControl() {
        class DrawbackExchangeControl extends AbilitySub {
            public DrawbackExchangeControl(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackExchangeControl(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -6169562107675964474L;
            private final AbilityFactory af = AbilityFactoryGainControl.this.af;

            @Override
            public String getStackDescription() {
                return exchangeControlStackDescription(this.af, this);
            }

            @Override
            public void resolve() {
                exchangeControlResolve(this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                // check AI life before playing this drawback?
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return exchangeControlDoTriggerAI(getActivatingPlayer(), this.af, this, mandatory);
            }
        }
        final SpellAbility dbExchangeControl = new DrawbackExchangeControl(this.af.getHostCard(), this.af.getAbTgt());

        return dbExchangeControl;
    }

    /**
     * <p>
     * exchangeControlStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String exchangeControlStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<String, String> params = af.getMapParams();
        Card object1 = null;
        Card object2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = tgt.getTargetCards();
        if (tgts.size() > 0) {
            object1 = tgts.get(0);
        }
        if (params.containsKey("Defined")) {
            object2 = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa).get(0);
        } else if (tgts.size() > 1) {
            object2 = tgts.get(1);
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append(object1 + " exchanges controller with " + object2);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private boolean exchangeControlCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        Card object1 = null;
        Card object2 = null;
        final Target tgt = sa.getTarget();
        tgt.resetTargets();

        List<Card> list = 
                CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, sa.getSourceCard());
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final Map<String, String> vars = c.getSVars();
                return !vars.containsKey("RemAIDeck") && c.canBeTargetedBy(sa);
            }
        });
        object1 = CardFactoryUtil.getBestAI(list);
        if (params.containsKey("Defined")) {
            object2 = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa).get(0);
        } else if (tgt.getMinTargets(sa.getSourceCard(), sa) > 1) {
            List<Card> list2 = ai.getCardsIn(ZoneType.Battlefield);
            list2 = CardLists.getValidCards(list2, tgt.getValidTgts(), ai, sa.getSourceCard());
            object2 = CardFactoryUtil.getWorstAI(list2);
            tgt.addTarget(object2);
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        if (CardFactoryUtil.evaluateCreature(object1) > CardFactoryUtil.evaluateCreature(object2) + 40) {
            tgt.addTarget(object1);
            return MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        }
        return false;
    }

    private boolean exchangeControlDoTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            return false;
        }

        return false;
    }

    private void exchangeControlResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        Card object1 = null;
        Card object2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = tgt.getTargetCards();
        if (tgts.size() > 0) {
            object1 = tgts.get(0);
        }
        if (params.containsKey("Defined")) {
            object2 = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa).get(0);
        } else if (tgts.size() > 1) {
            object2 = tgts.get(1);
        }

        if (object1 == null || object2 == null || !object1.isInPlay()
                || !object2.isInPlay()) {
            return;
        }

        Player player2 = object2.getController();
        object2.addController(object1.getController());
        object1.addController(player2);
    }

} // end class AbilityFactory_GainControl
