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
package forge.control.input;

import forge.AllZone;
import forge.Card;
import forge.Command;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.views.VMessage;
import forge.view.ButtonUtil;

//if cost is paid, Command.execute() is called

/**
 * <p>
 * Input_PayManaCost_Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputPayManaCostAbility extends InputMana {
    /**
     * Constant <code>serialVersionUID=3836655722696348713L</code>.
     */
    private static final long serialVersionUID = 3836655722696348713L;

    private String originalManaCost;
    private String message = "";
    private ManaCost manaCost;
    private SpellAbility fakeAbility;

    private Command paidCommand;
    private Command unpaidCommand;

    // only used for X costs:
    private boolean showOnlyOKButton = false;

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param paid
     *            a {@link forge.Command} object.
     */
    public InputPayManaCostAbility(final String manaCost, final Command paid) {
        this(manaCost, paid, Command.BLANK);
    }

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param manaCost2
     *            a {@link java.lang.String} object.
     * @param paidCommand2
     *            a {@link forge.Command} object.
     * @param unpaidCommand2
     *            a {@link forge.Command} object.
     */
    public InputPayManaCostAbility(final String manaCost2, final Command paidCommand2, final Command unpaidCommand2) {
        this("", manaCost2, paidCommand2, unpaidCommand2);
    }

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     * @param manaCost2
     *            a {@link java.lang.String} object.
     * @param paidCommand2
     *            a {@link forge.Command} object.
     * @param unpaidCommand2
     *            a {@link forge.Command} object.
     */
    public InputPayManaCostAbility(final String m, final String manaCost2, final Command paidCommand2,
            final Command unpaidCommand2) {
        this(m, manaCost2, paidCommand2, unpaidCommand2, false);
    }

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     * @param manaCost2
     *            a {@link java.lang.String} object.
     * @param paidCommand2
     *            a {@link forge.Command} object.
     * @param unpaidCommand2
     *            a {@link forge.Command} object.
     * @param showOKButton
     *            a boolean.
     */
    public InputPayManaCostAbility(final String m, final String manaCost2, final Command paidCommand2,
            final Command unpaidCommand2, final boolean showOKButton) {
        this.fakeAbility = new SpellAbility(SpellAbility.getAbility(), null) {
            @Override
            public void resolve() {
            }

            @Override
            public boolean canPlay() {
                return false;
            }
        };
        this.originalManaCost = manaCost2;
        this.message = m;

        this.manaCost = new ManaCost(this.originalManaCost);
        this.paidCommand = paidCommand2;
        this.unpaidCommand = unpaidCommand2;
        this.showOnlyOKButton = showOKButton;
    }

    /**
     * <p>
     * resetManaCost.
     * </p>
     */
    public final void resetManaCost() {
        this.manaCost = new ManaCost(this.originalManaCost);
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        // only tap card if the mana is needed
        this.manaCost = InputPayManaCostUtil.activateManaAbility(this.fakeAbility, card, this.manaCost);

        if (card.getManaAbility().isEmpty() || card.isInZone(ZoneType.Hand)) {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }

        if (this.manaCost.isPaid()) {
            this.paidCommand.execute();
            this.resetManaCost();
            AllZone.getHumanPlayer().getManaPool().clearManaPaid(this.fakeAbility, false);
            this.stop();
        } else {
            this.showMessage();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonCancel() {
        this.unpaidCommand.execute();
        this.resetManaCost();
        AllZone.getHumanPlayer().getManaPool().refundManaPaid(this.fakeAbility, true);
        this.stop();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (this.showOnlyOKButton) {
            this.stop();
            this.unpaidCommand.execute();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.enableOnlyCancel();
        if (this.showOnlyOKButton) {
            ButtonUtil.enableOnlyOK();
        }
        CMatchUI.SINGLETON_INSTANCE.showMessage(this.message + "Pay Mana Cost: \r\n" + this.manaCost.toString());
    }

    /* (non-Javadoc)
     * @see forge.control.input.InputMana#selectManaPool()
     */
    @Override
    public void selectManaPool(String color) {
        this.manaCost = InputPayManaCostUtil.activateManaAbility(color, this.fakeAbility, this.manaCost);

        if (this.manaCost.isPaid()) {
            this.resetManaCost();
            AllZone.getHumanPlayer().getManaPool().clearManaPaid(this.fakeAbility, false);
            this.stop();
            this.paidCommand.execute();
        } else {
            this.showMessage();
        }
    }

}
