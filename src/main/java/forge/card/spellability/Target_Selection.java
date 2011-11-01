package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * <p>
 * Target_Selection class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Target_Selection {
    private Target target = null;
    private SpellAbility ability = null;
    private Card card = null;
    private Target_Selection subSelection = null;

    /**
     * <p>
     * getTgt.
     * </p>
     * 
     * @return a {@link forge.card.spellability.Target} object.
     */
    public final Target getTgt() {
        return this.target;
    }

    /**
     * <p>
     * Getter for the field <code>ability</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbility() {
        return this.ability;
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        return this.card;
    }

    private SpellAbility_Requirements req = null;

    /**
     * <p>
     * setRequirements.
     * </p>
     * 
     * @param reqs
     *            a {@link forge.card.spellability.SpellAbility_Requirements}
     *            object.
     */
    public final void setRequirements(final SpellAbility_Requirements reqs) {
        this.req = reqs;
    }

    private boolean bCancel = false;

    /**
     * <p>
     * setCancel.
     * </p>
     * 
     * @param done
     *            a boolean.
     */
    public final void setCancel(final boolean done) {
        this.bCancel = done;
    }

    /**
     * <p>
     * isCanceled.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCanceled() {
        if (this.bCancel) {
            return this.bCancel;
        }

        if (this.subSelection == null) {
            return false;
        }

        return this.subSelection.isCanceled();
    }

    private boolean bDoneTarget = false;

    /**
     * <p>
     * setDoneTarget.
     * </p>
     * 
     * @param done
     *            a boolean.
     */
    public final void setDoneTarget(final boolean done) {
        this.bDoneTarget = done;
    }

    /**
     * <p>
     * Constructor for Target_Selection.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public Target_Selection(final Target tgt, final SpellAbility sa) {
        this.target = tgt;
        this.ability = sa;
        this.card = sa.getSourceCard();
    }

    /**
     * <p>
     * doesTarget.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean doesTarget() {
        if (this.target == null) {
            return false;
        }
        return this.target.doesTarget();
    }

    /**
     * <p>
     * resetTargets.
     * </p>
     */
    public final void resetTargets() {
        if (this.target != null) {
            this.target.resetTargets();
        }
    }

    /**
     * <p>
     * chooseTargets.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean chooseTargets() {
        // if not enough targets chosen, reset and cancel Ability
        if (this.bCancel || (this.bDoneTarget && !this.target.isMinTargetsChosen(this.card, this.ability))) {
            this.bCancel = true;
            this.req.finishedTargeting();
            return false;
        } else if (!this.doesTarget() || (this.bDoneTarget && this.target.isMinTargetsChosen(this.card, this.ability))
                || this.target.isMaxTargetsChosen(this.card, this.ability)) {
            final Ability_Sub abSub = this.ability.getSubAbility();

            if (abSub == null) {
                // if no more SubAbilities finish targeting
                this.req.finishedTargeting();
                return true;
            } else {
                // Has Sub Ability
                this.subSelection = new Target_Selection(abSub.getTarget(), abSub);
                this.subSelection.setRequirements(this.req);
                this.subSelection.resetTargets();
                return this.subSelection.chooseTargets();
            }
        }

        this.chooseValidInput();

        return false;
    }

    /**
     * Gets the unique targets.
     * 
     * @param ability
     *            the ability
     * @return the unique targets
     */
    public final ArrayList<Object> getUniqueTargets(final SpellAbility ability) {
        final ArrayList<Object> targets = new ArrayList<Object>();
        SpellAbility child = ability;
        while (child instanceof Ability_Sub) {
            child = ((Ability_Sub) child).getParent();
            targets.addAll(child.getTarget().getTargets());
        }

        return targets;
    }

    // these have been copied over from CardFactoryUtil as they need two extra
    // parameters for target selection.
    // however, due to the changes necessary for SA_Requirements this is much
    // different than the original

    /**
     * <p>
     * chooseValidInput.
     * </p>
     */
    public final void chooseValidInput() {
        final Target tgt = this.getTgt();
        final List<Zone> zone = tgt.getZone();
        final boolean mandatory = this.target.getMandatory() ? this.target.hasCandidates(true) : false;

        if (zone.contains(Constant.Zone.Stack) && (zone.size() == 1)) {
            // If Zone is Stack, the choices are handled slightly differently
            this.chooseCardFromStack(mandatory);
            return;
        }

        final CardList choices = AllZoneUtil.getCardsIn(zone).getValidCards(this.target.getValidTgts(),
                this.ability.getActivatingPlayer(), this.ability.getSourceCard());

        ArrayList<Object> objects = new ArrayList<Object>();
        if (tgt.isUniqueTargets()) {
            objects = this.getUniqueTargets(this.ability);
            for (final Object o : objects) {
                if ((o instanceof Card) && objects.contains(o)) {
                    choices.remove((Card) o);
                }
            }
        }

        // Remove cards already targeted
        final ArrayList<Card> targeted = tgt.getTargetCards();
        for (final Card c : targeted) {
            if (choices.contains(c)) {
                choices.remove(c);
            }
        }

        if (zone.contains(Constant.Zone.Battlefield)) {
            AllZone.getInputControl().setInput(this.inputTargetSpecific(choices, true, mandatory, objects));
        } else {
            this.chooseCardFromList(choices, true, mandatory);
        }
    } // input_targetValid

    // CardList choices are the only cards the user can successful select
    /**
     * <p>
     * input_targetSpecific.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param targeted
     *            a boolean.
     * @param mandatory
     *            a boolean.
     * @param alreadyTargeted
     *            the already targeted
     * @return a {@link forge.gui.input.Input} object.
     */
    public final Input inputTargetSpecific(final CardList choices, final boolean targeted, final boolean mandatory,
            final ArrayList<Object> alreadyTargeted) {
        final SpellAbility sa = this.ability;
        final Target_Selection select = this;
        final Target tgt = this.target;
        final SpellAbility_Requirements req = this.req;

        final Input target = new Input() {
            private static final long serialVersionUID = -1091595663541356356L;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Targeted: ");
                for (final Object o : alreadyTargeted) {
                    sb.append(o).append(" ");
                }
                sb.append(tgt.getTargetedString());
                sb.append("\n");
                sb.append(tgt.getVTSelection());

                AllZone.getDisplay().showMessage(sb.toString());

                // If reached Minimum targets, enable OK button
                if (!tgt.isMinTargetsChosen(sa.getSourceCard(), sa)) {
                    ButtonUtil.enableOnlyCancel();
                } else {
                    ButtonUtil.enableAll();
                }

                if (mandatory) {
                    ButtonUtil.disableCancel();
                }
            }

            @Override
            public void selectButtonCancel() {
                select.setCancel(true);
                this.stop();
                req.finishedTargeting();
            }

            @Override
            public void selectButtonOK() {
                select.setDoneTarget(true);
                this.done();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                // leave this in temporarily, there some seriously wrong things
                // going on here
                if (targeted && !CardFactoryUtil.canTarget(sa, card)) {
                    AllZone.getDisplay().showMessage("Cannot target this card (Shroud? Protection? Restrictions?).");
                } else if (choices.contains(card)) {
                    tgt.addTarget(card);
                    this.done();
                }
            } // selectCard()

            @Override
            public void selectPlayer(final Player player) {
                if (alreadyTargeted.contains(player)) {
                    return;
                }

                if (((tgt.canTgtPlayer() && !tgt.canOnlyTgtOpponent())
                        || (tgt.canOnlyTgtOpponent() && player.equals(sa.getActivatingPlayer()
                        .getOpponent()))) && player.canTarget(sa)) {
                    tgt.addTarget(player);
                    this.done();
                }
            }

            void done() {
                this.stop();

                select.chooseTargets();
            }
        };

        return target;
    } // input_targetSpecific()

    /**
     * <p>
     * chooseCardFromList.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param targeted
     *            a boolean.
     * @param mandatory
     *            a boolean.
     */
    public final void chooseCardFromList(final CardList choices, final boolean targeted, final boolean mandatory) {
        // Send in a list of valid cards, and popup a choice box to target
        final Card dummy = new Card();
        dummy.setName("[FINISH TARGETING]");
        final SpellAbility sa = this.ability;
        final String message = this.target.getVTSelection();

        final Target tgt = this.getTgt();

        final CardList choicesWithDone = choices;
        if (tgt.isMinTargetsChosen(sa.getSourceCard(), sa)) {
            // is there a more elegant way of doing this?
            choicesWithDone.add(dummy);
        }
        final Object check = GuiUtils.getChoiceOptional(message, choicesWithDone.toArray());
        if (check != null) {
            final Card c = (Card) check;
            if (c.equals(dummy)) {
                this.setDoneTarget(true);
            } else {
                tgt.addTarget(c);
            }
        } else {
            this.setCancel(true);
        }

        this.chooseTargets();
    }

    /**
     * <p>
     * chooseCardFromStack.
     * </p>
     * 
     * @param mandatory
     *            a boolean.
     */
    public final void chooseCardFromStack(final boolean mandatory) {
        final Target tgt = this.target;
        final String message = tgt.getVTSelection();
        final Target_Selection select = this;

        // Find what's targetable, then allow human to choose
        final ArrayList<SpellAbility> choosables = Target_Selection.getTargetableOnStack(this.ability, select.getTgt());

        final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();

        for (final SpellAbility sa : choosables) {
            map.put(sa.getStackDescription(), sa);
        }

        String[] choices = new String[map.keySet().size()];
        choices = map.keySet().toArray(choices);

        if (choices.length == 0) {
            select.setCancel(true);
        } else {
            final String madeChoice = GuiUtils.getChoiceOptional(message, choices);

            if (madeChoice != null) {
                tgt.addTarget(map.get(madeChoice));
            } else {
                select.setCancel(true);
            }
        }

        select.chooseTargets();
    }

    // TODO The following three functions are Utility functions for
    // TargetOnStack, probably should be moved
    // The following should be select.getTargetableOnStack()
    /**
     * <p>
     * getTargetableOnStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<SpellAbility> getTargetableOnStack(final SpellAbility sa, final Target tgt) {
        final ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

        for (int i = 0; i < AllZone.getStack().size(); i++) {
            choosables.add(AllZone.getStack().peekAbility(i));
        }

        for (int i = 0; i < choosables.size(); i++) {
            if (!Target_Selection.matchSpellAbility(sa, choosables.get(i), tgt)) {
                choosables.remove(i);
            }
        }
        return choosables;
    }

    /**
     * <p>
     * matchSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param topSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @return a boolean.
     */
    public static boolean matchSpellAbility(final SpellAbility sa, final SpellAbility topSA, final Target tgt) {
        final String saType = tgt.getTargetSpellAbilityType();

        if (null == saType) {
            // just take this to mean no restrictions - carry on.
        } else if (topSA.isSpell()) {
            if (!saType.contains("Spell")) {
                return false;
            }
        } else if (topSA.isTrigger()) {
            if (!saType.contains("Triggered")) {
                return false;
            }
        } else if (topSA.isAbility()) {
            if (!saType.contains("Activated")) {
                return false;
            }
        }

        final String splitTargetRestrictions = tgt.getSAValidTargeting();
        if (splitTargetRestrictions != null) {
            // TODO What about spells with SubAbilities with Targets?

            final Target matchTgt = topSA.getTarget();

            if (matchTgt == null) {
                return false;
            }

            boolean result = false;

            for (final Object o : matchTgt.getTargets()) {
                if (Target_Selection.matchesValid(o, splitTargetRestrictions.split(","), sa)) {
                    result = true;
                    break;
                }
            }

            if (!result) {
                return false;
            }
        }

        if (!Target_Selection.matchesValid(topSA, tgt.getValidTgts(), sa)) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * matchesValid.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @param valids
     *            an array of {@link java.lang.String} objects.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean matchesValid(final Object o, final String[] valids, final SpellAbility sa) {
        final Card srcCard = sa.getSourceCard();
        final Player activatingPlayer = sa.getActivatingPlayer();
        if (o instanceof Card) {
            final Card c = (Card) o;
            return c.isValid(valids, activatingPlayer, srcCard);
        }

        if (o instanceof Player) {
            for (final String v : valids) {
                if (v.equalsIgnoreCase("Player")) {
                    return true;
                }

                if (v.equalsIgnoreCase("Opponent")) {
                    if (o.equals(activatingPlayer.getOpponent())) {
                        return true;
                    }
                }
                if (v.equalsIgnoreCase("You")) {
                    return o.equals(activatingPlayer);
                }
            }
        }

        if (o instanceof SpellAbility) {
            final Card c = ((SpellAbility) o).getSourceCard();
            return c.isValid(valids, activatingPlayer, srcCard);
        }

        return false;
    }
}
