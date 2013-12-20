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
package forge.gui.input;

import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

//if cost is paid, Command.execute() is called

/**
 * <p>
 * Input_PayManaCost_Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputPayManaExecuteCommands extends InputPayMana {
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
     */
    public InputPayManaExecuteCommands(final Card sourceCard, final Player p, final String prompt, final ManaCost manaCost0) {
        super(new SpellAbility.EmptySa(sourceCard, p));
        this.originalManaCost = manaCost0;
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
            onStateChanged();
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
        player.getManaPool().refundManaPaid(this.saPaidFor);
        bPaid = false;
        this.stop();
    }

    /** {@inheritDoc} */
    @Override
    protected String getMessage() {
        final StringBuilder msg = new StringBuilder(this.message + "\nPay Mana Cost: " + this.manaCost);
        if (this.phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(this.phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (this.manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }
        return msg.toString();
    }
}
