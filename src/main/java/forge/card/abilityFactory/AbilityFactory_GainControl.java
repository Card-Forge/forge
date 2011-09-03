package forge.card.abilityFactory;

import forge.*;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//AB:GainControl|ValidTgts$Creature|TgtPrompt$Select target legendary creature|LoseControl$Untap,LoseControl|SpellDescription$Gain control of target xxxxxxx

//GainControl specific params:
//  LoseControl - the lose control conditions (as a comma separated list)
//			-Untap - source card becomes untapped
//			-LoseControl - you lose control of source card
//			-LeavesPlay - source card leaves the battlefield
//			-PowerGT - (not implemented yet for Old Man of the Sea)
//	AddKWs	- Keywords to add to the controlled card
//              (as a "&"-separated list; like Haste, Sacrifice CARDNAME at EOT, any standard keyword)
//  OppChoice - set to True if opponent chooses creature (for Preacher) - not implemented yet
//	Untap	- set to True if target card should untap when control is taken
//	DestroyTgt - actions upon which the tgt should be destroyed.  same list as LoseControl
//	NoRegen - set if destroyed creature can't be regenerated.  used only with DestroyTgt

/**
 * <p>AbilityFactory_GainControl class.</p>
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
     * <p>Constructor for AbilityFactory_GainControl.</p>
     *
     * @param newAF a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public AbilityFactory_GainControl(final AbilityFactory newAF) {
        af = newAF;
        params = af.getMapParams();
        hostCard = af.getHostCard();
        if (params.containsKey("LoseControl")) {
            lose = new ArrayList<String>(Arrays.asList(params.get("LoseControl").split(",")));
        }
        if (params.containsKey("Untap")) {
            bUntap = true;
        }
        if (params.containsKey("TapOnLose")) {
            bTapOnLose = true;
        }
        if (params.containsKey("AddKWs")) {
            kws = new ArrayList<String>(Arrays.asList(params.get("AddKWs").split(" & ")));
        }
        if (params.containsKey("DestroyTgt")) {
            destroyOn = new ArrayList<String>(Arrays.asList(params.get("DestroyTgt").split(",")));
        }
        if (params.containsKey("NoRegen")) {
            bNoRegen = true;
        }
    }

    /**
     * <p>getSpellGainControl.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getSpellGainControl() {
        SpellAbility spControl = new Spell(hostCard, af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3125489644424832311L;

            @Override
            public boolean canPlayAI() {
                return gainControlTgtAI(this);
            }

            @Override
            public void resolve() {
                gainControlResolve(this);
            } //resolve

            @Override
            public String getStackDescription() {
                return gainControlStackDescription(this);
            }
        }; //SpellAbility

        return spControl;
    }

    /**
     * <p>getAbilityGainControl.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getAbilityGainControl() {

        final SpellAbility abControl = new Ability_Activated(hostCard, af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4384705198674678831L;

            @Override
            public boolean canPlayAI() {
                return gainControlTgtAI(this);
            }

            @Override
            public void resolve() {
                gainControlResolve(this);
            }

            @Override
            public String getStackDescription() {
                return gainControlStackDescription(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return gainControlTgtAI(this);
            }
        }; //Ability_Activated

        return abControl;
    }

    /**
     * <p>getDrawbackGainControl.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getDrawbackGainControl() {
        SpellAbility dbControl = new Ability_Sub(hostCard, af.getAbTgt()) {
            private static final long serialVersionUID = -5577742598032345880L;

            @Override
            public boolean canPlayAI() {
                return gainControlTgtAI(this);
            }

            @Override
            public String getStackDescription() {
                return gainControlStackDescription(this);
            }

            @Override
            public void resolve() {
                gainControlResolve(this);
            } //resolve

            @Override
            public boolean chkAI_Drawback() {
                return gainControlDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return gainControlTriggerAI(this, mandatory);
            }
        }; //SpellAbility

        return dbControl;
    }

    /**
     * <p>gainControlStackDescription.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String gainControlStackDescription(final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Card> tgtCards;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
        }

        ArrayList<Player> newController = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                params.get("NewController"), sa);
        if (newController.size() == 0) {
            newController.add(sa.getActivatingPlayer());
        }

        sb.append(newController).append(" gains control of ");

        for (Card c : tgtCards) {
            sb.append(" ");
            if (c.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(c);
            }
        }
        sb.append(".");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>gainControlTgtAI.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean gainControlTgtAI(final SpellAbility sa) {
        boolean hasCreature = false;
        boolean hasArtifact = false;
        boolean hasEnchantment = false;
        boolean hasLand = false;

        Target tgt = af.getAbTgt();

        //if Defined, then don't worry about targeting
        if (tgt == null) {
            return true;
        }

        CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.getHumanPlayer());
        list = list.getValidCards(tgt.getValidTgts(), hostCard.getController(), hostCard);
        //AI won't try to grab cards that are filtered out of AI decks on purpose
        list = list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                Map<String, String> vars = c.getSVars();
                return !vars.containsKey("RemAIDeck") && CardFactoryUtil.canTarget(hostCard, c);
            }
        });

        if (list.isEmpty()) {
            return false;
        }

        // Don't steal something if I can't Attack without, or prevent it from blocking at least
        if (lose != null && lose.contains("EOT")
                && AllZone.getPhase().isAfter(Constant.Phase.Combat_Declare_Blockers))
        {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            for (Card c : list) {
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
                if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0) {
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
     * <p>gainControlResolve.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private void gainControlResolve(final SpellAbility sa) {
        ArrayList<Card> tgtCards;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
        }
        //tgtCards.add(hostCard);

        ArrayList<Player> newController = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                params.get("NewController"), sa);
        if (newController.size() == 0) {
            newController.add(sa.getActivatingPlayer());
        }

        int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);
            final Player originalController = tgtC.getController();

            movedCards[j] = tgtC;
            hostCard.addGainControlTarget(tgtC);

            if (AllZoneUtil.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(hostCard, tgtC)) {

                if (params.containsKey("NewController")) {
                    tgtC.addController(newController.get(0));
                }
                else {
                    tgtC.addController(hostCard);
                }
                //AllZone.getGameAction().changeController(new CardList(tgtC),
                //      tgtC.getController(), newController.get(0));

                if (bUntap) {
                    tgtC.untap();
                }

                if (null != kws) {
                    for (String kw : kws) {
                        tgtC.addExtrinsicKeyword(kw);
                    }
                }
            }

            //end copied

            if (lose != null) {
                if (lose.contains("LeavesPlay")) {
                    hostCard.addLeavesPlayCommand(getLoseControlCommand(j, originalController));
                }
                if (lose.contains("Untap")) {
                    hostCard.addUntapCommand(getLoseControlCommand(j, originalController));
                }
                if (lose.contains("LoseControl")) {
                    hostCard.addChangeControllerCommand(getLoseControlCommand(j, originalController));
                }
                if (lose.contains("EOT")) {
                    AllZone.getEndOfTurn().addAt(getLoseControlCommand(j, originalController));
                }
            }

            if (destroyOn != null) {
                if (destroyOn.contains("LeavesPlay")) {
                    hostCard.addLeavesPlayCommand(getDestroyCommand(j));
                }
                if (destroyOn.contains("Untap")) {
                    hostCard.addUntapCommand(getDestroyCommand(j));
                }
                if (destroyOn.contains("LoseControl")) {
                    hostCard.addChangeControllerCommand(getDestroyCommand(j));
                }
            }

            //for Old Man of the Sea - 0 is hardcoded since it only allows 1 target
            hostCard.clearGainControlReleaseCommands();
            hostCard.addGainControlReleaseCommand(getLoseControlCommand(0, originalController));

        } //end foreach target
    }

    /**
     * <p>gainControlTriggerAI.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
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
            return gainControlTgtAI(sa);
        }

        return true;
    }

    /**
     * <p>gainControlDrawbackAI.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean gainControlDrawbackAI(final SpellAbility sa) {
        if (af.getAbTgt() == null || !af.getAbTgt().doesTarget()) {
            //all is good
        } else {
            return gainControlTgtAI(sa);
        }

        return true;
    } //pumpDrawbackAI()

    /**
     * <p>getDestroyCommand.</p>
     *
     * @param i a int.
     * @return a {@link forge.Command} object.
     */
    private Command getDestroyCommand(final int i) {
        final Command destroy = new Command() {
            private static final long serialVersionUID = 878543373519872418L;

            public void execute() {
                final Card c = movedCards[i];
                Ability ability = new Ability(hostCard, "0") {
                    public void resolve() {

                        if (bNoRegen) {
                            AllZone.getGameAction().destroyNoRegeneration(c);
                        } else {
                            AllZone.getGameAction().destroy(c);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(hostCard).append(" - destroy ").append(c.getName()).append(".");
                if (bNoRegen) {
                    sb.append("  It can't be regenerated.");
                }
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);
            }

        };
        return destroy;
    }

    /**
     * <p>getLoseControlCommand.</p>
     *
     * @param i a int.
     * @param originalController a {@link forge.Player} object.
     * @return a {@link forge.Command} object.
     */
    private Command getLoseControlCommand(final int i, final Player originalController) {
        final Command loseControl = new Command() {
            private static final long serialVersionUID = 878543373519872418L;

            public void execute() {
                Card c = movedCards[i];
                //ArrayList<Card> c = hostCard.getGainControlTargets();
                if (null == c) {
                    return;
                }

                if (AllZoneUtil.isCardInPlay(c)) {
                    c.removeController(hostCard);
                    //AllZone.getGameAction().changeController(new CardList(c), c.getController(), originalController);

                    if (bTapOnLose) {
                        c.tap();
                    }

                    if (null != kws) {
                        for (String kw : kws) {
                            c.removeExtrinsicKeyword(kw);
                        }
                    }
                } //if
                hostCard.clearGainControlTargets();
                hostCard.clearGainControlReleaseCommands();
                movedCards[i] = null;
            } //execute()
        };

        return loseControl;
    }

} //end class AbilityFactory_GainControl
