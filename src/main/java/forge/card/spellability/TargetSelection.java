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
package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.card.abilityfactory.AbilityFactory;
import forge.control.input.Input;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * <p>
 * Target_Selection class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TargetSelection {
    private Target target = null;
    private SpellAbility ability = null;
    private Card card = null;
    private TargetSelection subSelection = null;

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

    private SpellAbilityRequirements req = null;

    /**
     * <p>
     * setRequirements.
     * </p>
     * 
     * @param reqs
     *            a {@link forge.card.spellability.SpellAbilityRequirements}
     *            object.
     */
    public final void setRequirements(final SpellAbilityRequirements reqs) {
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
    public TargetSelection(final Target tgt, final SpellAbility sa) {
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
            final AbilitySub abSub = this.ability.getSubAbility();

            if (abSub == null) {
                // if no more SubAbilities finish targeting
                this.req.finishedTargeting();
                return true;
            } else {
                // Has Sub Ability
                this.subSelection = new TargetSelection(abSub.getTarget(), abSub);
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
    public static final ArrayList<Object> getUniqueTargets(final SpellAbility ability) {
        final ArrayList<Object> targets = new ArrayList<Object>();
        SpellAbility child = ability;
        while (child instanceof AbilitySub) {
            child = ((AbilitySub) child).getParent();
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
        final List<ZoneType> zone = tgt.getZone();
        final boolean mandatory = this.target.getMandatory() ? this.target.hasCandidates(this.ability, true) : false;

        if (zone.contains(ZoneType.Stack) && (zone.size() == 1)) {
            // If Zone is Stack, the choices are handled slightly differently
            this.chooseCardFromStack(mandatory);
            return;
        }

        CardList choices = AllZoneUtil
                .getCardsIn(zone)
                .getValidCards(this.target.getValidTgts(), this.ability.getActivatingPlayer(),
                        this.ability.getSourceCard()).getTargetableCards(this.ability);

        ArrayList<Object> objects = new ArrayList<Object>();
        if (tgt.isUniqueTargets()) {
            objects = getUniqueTargets(this.ability);
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

        // If all cards must be from the same zone
        if (tgt.isSingleZone() && !targeted.isEmpty()) {
            choices = choices.getController(targeted.get(0).getController());
        }
        // If all cards must be from different zones
        if (tgt.isDifferentZone() && !targeted.isEmpty()) {
            choices = choices.getController(targeted.get(0).getController().getOpponent());
        }
        // If the cards must have a specific controller
        if (tgt.getDefinedController() != null) {
            ArrayList<Player> pl = AbilityFactory.getDefinedPlayers(card, tgt.getDefinedController(), this.ability);
            if (pl != null && !pl.isEmpty()) {
                Player controller = pl.get(0);
                choices = choices.getController(controller);
            } else {
                choices.clear();
            }
        }

        if (zone.contains(ZoneType.Battlefield)) {
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
     * @return a {@link forge.control.input.Input} object.
     */
    public final Input inputTargetSpecific(final CardList choices, final boolean targeted, final boolean mandatory,
            final ArrayList<Object> alreadyTargeted) {
        final SpellAbility sa = this.ability;
        final TargetSelection select = this;
        final Target tgt = this.target;
        final SpellAbilityRequirements req = this.req;

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

                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());

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
                if (targeted && !card.canBeTargetedBy(sa)) {
                    CMatchUI.SINGLETON_INSTANCE.showMessage("Cannot target this card (Shroud? Protection? Restrictions?).");
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

                if (sa.canTarget(player)) {
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
        final Object check = GuiUtils.chooseOneOrNone(message, choicesWithDone.toArray());
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
        final TargetSelection select = this;

        // Find what's targetable, then allow human to choose
        final ArrayList<SpellAbility> choosables = TargetSelection.getTargetableOnStack(this.ability, select.getTgt());

        final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();

        for (final SpellAbility sa : choosables) {
            map.put(sa.getStackDescription(), sa);
        }

        String[] choices = new String[map.keySet().size()];
        choices = map.keySet().toArray(choices);

        if (choices.length == 0) {
            select.setCancel(true);
        } else {
            final String madeChoice = GuiUtils.chooseOneOrNone(message, choices);

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
            if (!TargetSelection.matchSpellAbility(sa, choosables.get(i), tgt)) {
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
                if (TargetSelection.matchesValid(o, splitTargetRestrictions.split(","), sa)) {
                    result = true;
                    break;
                }
            }

            if (!result) {
                return false;
            }
        }

        if (!TargetSelection.matchesValid(topSA, tgt.getValidTgts(), sa)) {
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
