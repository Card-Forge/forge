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

import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

//if cost is paid, Command.execute() is called

/**
 * <p>
 * Input_PayManaCost_Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputPayManaExecuteCommands extends InputPayManaBase {
    /**
     * Constant <code>serialVersionUID=3836655722696348713L</code>.
     */
    private static final long serialVersionUID = 3836655722696348713L;

    private ManaCost originalManaCost;
    private String message = "";

    private boolean bPaid = false;
    public boolean isPaid() { return bPaid; }


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
    public InputPayManaExecuteCommands(final Player p, final String prompt, final ManaCost manaCost2) {
        super(new SpellAbility(null, Cost.Zero) {
            @Override public void resolve() {}
            @Override public Player getActivatingPlayer() { return p; }
            @Override public boolean canPlay() { return false; }
        });
        this.originalManaCost = manaCost2;
        this.phyLifeToLose = 0;
        this.message = prompt;

        this.manaCost = new ManaCostBeingPaid(this.originalManaCost);

    }


    @Override
    public void selectPlayer(final Player selectedPlayer) {
        if (player == selectedPlayer) {
            if (player.canPayLife(this.phyLifeToLose + 2) && manaCost.payPhyrexian()) {
                this.phyLifeToLose += 2;
            }

            if (this.manaCost.isPaid()) {
                this.done();
                this.stop();
            } else {
                this.showMessage();
            }
        }
    }

    @Override
    protected void done() {
        if (this.phyLifeToLose > 0) {
            player.payLife(this.phyLifeToLose, null);
        }
        player.getManaPool().clearManaPaid(this.saPaidFor, false);
        bPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        player.getManaPool().refundManaPaid(this.saPaidFor, true);
        bPaid = false;
        this.stop();
    }

    /* (non-Javadoc)
     * @see forge.control.input.InputPayManaBase#updateMessage()
     */
    @Override
    protected void updateMessage() {
        final StringBuilder msg = new StringBuilder(this.message + "Pay Mana Cost: " + this.manaCost);
        if (this.phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(this.phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (this.manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }
        showMessage(msg.toString());
    }
}
