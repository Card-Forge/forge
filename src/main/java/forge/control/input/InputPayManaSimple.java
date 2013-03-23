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

import java.util.concurrent.CountDownLatch;
import forge.Card;
import forge.Singletons;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

//pays the cost of a card played from the player's hand
//the card is removed from the players hand if the cost is paid
//CANNOT be used for ABILITIES
public class InputPayManaSimple extends InputPayManaBase {
    // anything that uses this should be converted to Ability_Cost
    /** Constant <code>serialVersionUID=3467312982164195091L</code>. */
    private static final long serialVersionUID = 3467312982164195091L;

    private final Card originalCard;
    private final String originalManaCost;
    private final CountDownLatch cdlNotify;

    public InputPayManaSimple(final GameState game, final SpellAbility sa, final ManaCostBeingPaid manaCostToPay, final CountDownLatch callOnDone) {
        super(game, sa);
        this.originalManaCost = manaCostToPay.toString(); // Change
        this.originalCard = sa.getSourceCard();

        if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
            this.manaCost = new ManaCostBeingPaid("0");
            game.getStack().add(this.saPaidFor);
        } else {
            this.manaCost = manaCostToPay;
        }
        
        cdlNotify = callOnDone;
    }

    /**
     * <p>
     * resetManaCost.
     * </p>
     */
    private void resetManaCost() {
        this.manaCost = new ManaCostBeingPaid(this.originalManaCost);
        this.phyLifeToLose = 0;
    }

    protected void onManaAbilityPaid() {
        if (this.manaCost.isPaid()) {
            this.originalCard.setSunburstValue(this.manaCost.getSunburst());
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectPlayer(final Player player) {

        if (player == whoPays) {
            if (player.canPayLife(this.phyLifeToLose + 2) && manaCost.payPhyrexian()) {
                this.phyLifeToLose += 2;
            }

            this.showMessage();
        }

    }

    /**
     * <p>
     * done.
     * </p>
     */
    @Override
    protected void done() {
        if (this.phyLifeToLose > 0) {
            whoPays.payLife(this.phyLifeToLose, this.originalCard);
        }
        if (!this.saPaidFor.getSourceCard().isCopiedSpell()) {
            whoPays.getManaPool().clearManaPaid(this.saPaidFor, false);
            this.resetManaCost();

            if (this.saPaidFor.isSpell()) {
                this.saPaidFor.setSourceCard(game.getAction().moveToStack(this.originalCard));
            }

            // If this is a spell with convoke, re-tap all creatures used for
            // it.
            // This is done to make sure Taps triggers go off at the right time
            // (i.e. AFTER cost payment, they are tapped previously as well so
            // that
            // any mana tapabilities can't be used in payment as well as being
            // tapped for convoke)

            if (this.saPaidFor.getTappedForConvoke() != null) {
                for (final Card c : this.saPaidFor.getTappedForConvoke()) {
                    c.setTapped(false);
                    c.tap();
                }
                this.saPaidFor.clearTappedForConvoke();
            }
        }

        Singletons.getModel().getMatch().getInput().resetInput();
        cdlNotify.countDown();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonCancel() {
        // If this is a spell with convoke, untap all creatures used for it.
        if (this.saPaidFor.getTappedForConvoke() != null) {
            for (final Card c : this.saPaidFor.getTappedForConvoke()) {
                c.setTapped(false);
            }
            this.saPaidFor.clearTappedForConvoke();
        }

        this.resetManaCost();

        whoPays.getManaPool().refundManaPaid(this.saPaidFor, true);
        whoPays.getZone(ZoneType.Battlefield).updateObservers(); // DO

        this.stop();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.enableOnlyCancel();

        final StringBuilder msg = new StringBuilder("Pay Mana Cost: " + this.manaCost.toString());
        if (this.phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(this.phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (this.manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }

        CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
        if (this.manaCost.isPaid() && !new ManaCostBeingPaid(this.originalManaCost).isPaid()) {
            this.originalCard.setSunburstValue(this.manaCost.getSunburst());
            this.done();
        }

    }

    @Override public void isClassUpdated() {
    }
}
