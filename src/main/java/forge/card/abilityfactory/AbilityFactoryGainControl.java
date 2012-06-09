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
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
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
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

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
                return AbilityFactoryGainControl.this.gainControlTgtAI(this);
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

        final SpellAbility abControl = new AbilityActivated(this.hostCard, this.af.getAbCost(), this.af.getAbTgt()) {
            private static final long serialVersionUID = -4384705198674678831L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryGainControl.this.gainControlTgtAI(this);
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
                return AbilityFactoryGainControl.this.gainControlTgtAI(this);
            }
        }; // Ability_Activated

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
        final SpellAbility dbControl = new AbilitySub(this.hostCard, this.af.getAbTgt()) {
            private static final long serialVersionUID = -5577742598032345880L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryGainControl.this.gainControlTgtAI(this);
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
                return AbilityFactoryGainControl.this.gainControlDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryGainControl.this.gainControlTriggerAI(this, mandatory);
            }
        }; // SpellAbility

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
            tgtCards = AbilityFactory.getDefinedCards(this.hostCard, this.params.get("Defined"), sa);
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
    private boolean gainControlTgtAI(final SpellAbility sa) {
        boolean hasCreature = false;
        boolean hasArtifact = false;
        boolean hasEnchantment = false;
        boolean hasLand = false;

        final Target tgt = sa.getTarget();

        // if Defined, then don't worry about targeting
        if (tgt == null) {
            return true;
        } else {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                if (!AllZone.getHumanPlayer().canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.addTarget(AllZone.getHumanPlayer());
            }
        }

        CardList list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        list = list.getValidCards(tgt.getValidTgts(), this.hostCard.getController(), this.hostCard);
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
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
                && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
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
        CardList tgtCards = new CardList();

        final Target tgt = sa.getTarget();
        if (this.params.containsKey("AllValid")) {
            tgtCards = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            tgtCards = AbilityFactory.filterListByType(tgtCards, this.params.get("AllValid"), sa);
        } else if ((tgt != null) && !this.params.containsKey("Defined")) {
            tgtCards.addAll(tgt.getTargetCards());
        } else {
            tgtCards.addAll(AbilityFactory.getDefinedCards(this.hostCard, this.params.get("Defined"), sa));
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
                newController = this.hostCard;
            }
        } else {
            newController = controllers.get(0);
        }

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);
            final Player originalController = tgtC.getController();

            if (!tgtC.equals(this.hostCard) && !this.hostCard.getGainControlTargets().contains(tgtC)) {
                this.hostCard.addGainControlTarget(tgtC);
            }

            if (AllZoneUtil.isCardInPlay(tgtC)) {

                tgtC.addController(newController);
                // Singletons.getModel().getGameAction().changeController(new CardList(tgtC),
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
                    this.hostCard.addLeavesPlayCommand(this.getLoseControlCommand(tgtC, originalController, newController));
                }
                if (this.lose.contains("Untap")) {
                    this.hostCard.addUntapCommand(this.getLoseControlCommand(tgtC, originalController, newController));
                }
                if (this.lose.contains("LoseControl")) {
                    this.hostCard.addChangeControllerCommand(this.getLoseControlCommand(tgtC, originalController, newController));
                }
                if (this.lose.contains("EOT")) {
                    AllZone.getEndOfTurn().addAt(this.getLoseControlCommand(tgtC, originalController, newController));
                }
            }

            if (this.destroyOn != null) {
                if (this.destroyOn.contains("LeavesPlay")) {
                    this.hostCard.addLeavesPlayCommand(this.getDestroyCommand(tgtC));
                }
                if (this.destroyOn.contains("Untap")) {
                    this.hostCard.addUntapCommand(this.getDestroyCommand(tgtC));
                }
                if (this.destroyOn.contains("LoseControl")) {
                    this.hostCard.addChangeControllerCommand(this.getDestroyCommand(tgtC));
                }
            }

            this.hostCard.clearGainControlReleaseCommands();
            this.hostCard.addGainControlReleaseCommand(this.getLoseControlCommand(tgtC, originalController, newController));

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
    private boolean gainControlTriggerAI(final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return this.gainControlTgtAI(sa);
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
    private boolean gainControlDrawbackAI(final SpellAbility sa) {
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            if (this.params.containsKey("AllValid")) {
                CardList tgtCards = AllZoneUtil.getCardsIn(ZoneType.Battlefield)
                        .getController(AllZone.getHumanPlayer());
                tgtCards = AbilityFactory.filterListByType(tgtCards, this.params.get("AllValid"), sa);
                if (tgtCards.isEmpty()) {
                    return false;
                }
            }
            if ((this.lose != null) && this.lose.contains("EOT")
                    && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else {
            return this.gainControlTgtAI(sa);
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
                            Singletons.getModel().getGameAction().destroyNoRegeneration(c);
                        } else {
                            Singletons.getModel().getGameAction().destroy(c);
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

                AllZone.getStack().addSimultaneousStackEntry(ability);
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
        if (AllZoneUtil.isCardInPlay(c)) {
            c.removeController(newController);
            // Singletons.getModel().getGameAction().changeController(new CardList(c),
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

} // end class AbilityFactory_GainControl
