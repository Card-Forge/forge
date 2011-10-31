package forge.card.abilityFactory;

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
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

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
public class AbilityFactory_GainControl {

    private final Card[] movedCards = new Card[1];

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
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public AbilityFactory_GainControl(final AbilityFactory newAF) {
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
                return AbilityFactory_GainControl.this.gainControlTgtAI(this);
            }

            @Override
            public void resolve() {
                AbilityFactory_GainControl.this.gainControlResolve(this);
            } // resolve

            @Override
            public String getStackDescription() {
                return AbilityFactory_GainControl.this.gainControlStackDescription(this);
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

        final SpellAbility abControl = new Ability_Activated(this.hostCard, this.af.getAbCost(), this.af.getAbTgt()) {
            private static final long serialVersionUID = -4384705198674678831L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_GainControl.this.gainControlTgtAI(this);
            }

            @Override
            public void resolve() {
                AbilityFactory_GainControl.this.gainControlResolve(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_GainControl.this.gainControlStackDescription(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_GainControl.this.gainControlTgtAI(this);
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
        final SpellAbility dbControl = new Ability_Sub(this.hostCard, this.af.getAbTgt()) {
            private static final long serialVersionUID = -5577742598032345880L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_GainControl.this.gainControlTgtAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_GainControl.this.gainControlStackDescription(this);
            }

            @Override
            public void resolve() {
                AbilityFactory_GainControl.this.gainControlResolve(this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactory_GainControl.this.gainControlDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_GainControl.this.gainControlTriggerAI(this, mandatory);
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

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Card> tgtCards;

        final Target tgt = this.af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(this.hostCard, this.params.get("Defined"), sa);
        }

        final ArrayList<Player> newController = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                this.params.get("NewController"), sa);
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

        final Ability_Sub abSub = sa.getSubAbility();
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

        final Target tgt = this.af.getAbTgt();

        // if Defined, then don't worry about targeting
        if (tgt == null) {
            return true;
        }

        CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        list = list.getValidCards(tgt.getValidTgts(), this.hostCard.getController(), this.hostCard);
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                final Map<String, String> vars = c.getSVars();
                return !vars.containsKey("RemAIDeck")
                        && CardFactoryUtil.canTarget(AbilityFactory_GainControl.this.hostCard, c);
            }
        });

        if (list.isEmpty()) {
            return false;
        }

        // Don't steal something if I can't Attack without, or prevent it from
        // blocking at least
        if ((this.lose != null) && this.lose.contains("EOT")
                && AllZone.getPhase().isAfter(Constant.Phase.COMBAT_DECLARE_BLOCKERS)) {
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
                t = CardFactoryUtil.AI_getBestCreature(list);
            } else if (hasArtifact) {
                t = CardFactoryUtil.AI_getBestArtifact(list);
            } else if (hasLand) {
                t = CardFactoryUtil.AI_getBestLand(list);
            } else if (hasEnchantment) {
                t = CardFactoryUtil.AI_getBestEnchantment(list, sa.getSourceCard(), true);
            } else {
                t = CardFactoryUtil.AI_getMostExpensivePermanent(list, sa.getSourceCard(), true);
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
        ArrayList<Card> tgtCards;
        final boolean self = this.params.containsKey("Defined") && this.params.get("Defined").equals("Self");

        final Target tgt = this.af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(this.hostCard, this.params.get("Defined"), sa);
        }
        // tgtCards.add(hostCard);

        final ArrayList<Player> newController = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                this.params.get("NewController"), sa);
        if (newController.size() == 0) {
            newController.add(sa.getActivatingPlayer());
        }

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);
            final Player originalController = tgtC.getController();

            this.movedCards[j] = tgtC;
            if (!self) {
                this.hostCard.addGainControlTarget(tgtC);
            }

            if (AllZoneUtil.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(this.hostCard, tgtC)) {

                if (this.params.containsKey("NewController")) {
                    tgtC.addController(newController.get(0));
                } else {
                    tgtC.addController(this.hostCard);
                }
                // AllZone.getGameAction().changeController(new CardList(tgtC),
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
                    this.hostCard.addLeavesPlayCommand(this.getLoseControlCommand(j, originalController));
                }
                if (this.lose.contains("Untap")) {
                    this.hostCard.addUntapCommand(this.getLoseControlCommand(j, originalController));
                }
                if (this.lose.contains("LoseControl")) {
                    this.hostCard.addChangeControllerCommand(this.getLoseControlCommand(j, originalController));
                }
                if (this.lose.contains("EOT")) {
                    AllZone.getEndOfTurn().addAt(this.getLoseControlCommand(j, originalController));
                }
            }

            if (this.destroyOn != null) {
                if (this.destroyOn.contains("LeavesPlay")) {
                    this.hostCard.addLeavesPlayCommand(this.getDestroyCommand(j));
                }
                if (this.destroyOn.contains("Untap")) {
                    this.hostCard.addUntapCommand(this.getDestroyCommand(j));
                }
                if (this.destroyOn.contains("LoseControl")) {
                    this.hostCard.addChangeControllerCommand(this.getDestroyCommand(j));
                }
            }

            // for Old Man of the Sea - 0 is hardcoded since it only allows 1
            // target
            this.hostCard.clearGainControlReleaseCommands();
            this.hostCard.addGainControlReleaseCommand(this.getLoseControlCommand(0, originalController));

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
        if ((this.af.getAbTgt() == null) || !this.af.getAbTgt().doesTarget()) {
            // all is good
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
    private Command getDestroyCommand(final int i) {
        final Command destroy = new Command() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void execute() {
                final Card c = AbilityFactory_GainControl.this.movedCards[i];
                final Ability ability = new Ability(AbilityFactory_GainControl.this.hostCard, "0") {
                    @Override
                    public void resolve() {

                        if (AbilityFactory_GainControl.this.bNoRegen) {
                            AllZone.getGameAction().destroyNoRegeneration(c);
                        } else {
                            AllZone.getGameAction().destroy(c);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(AbilityFactory_GainControl.this.hostCard).append(" - destroy ").append(c.getName())
                        .append(".");
                if (AbilityFactory_GainControl.this.bNoRegen) {
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
     *            a {@link forge.Player} object.
     * @return a {@link forge.Command} object.
     */
    private Command getLoseControlCommand(final int i, final Player originalController) {
        final Card c = this.movedCards[i];
        final Command loseControl = new Command() {
            private static final long serialVersionUID = 878543373519872418L;

            @Override
            public void execute() {
                AbilityFactory_GainControl.doLoseControl(c, AbilityFactory_GainControl.this.hostCard,
                        AbilityFactory_GainControl.this.bTapOnLose, AbilityFactory_GainControl.this.kws);
            } // execute()
        };

        return loseControl;
    }

    private static void doLoseControl(final Card c, final Card host, final boolean tapOnLose,
            final ArrayList<String> addedKeywords) {
        if (null == c) {
            return;
        }
        if (AllZoneUtil.isCardInPlay(c)) {
            c.removeController(host);
            // AllZone.getGameAction().changeController(new CardList(c),
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
