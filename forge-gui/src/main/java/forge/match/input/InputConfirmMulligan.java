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
package forge.match.input;

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Lang;
import forge.util.ThreadUtil;

/**
 * <p>
 * InputConfirmMulligan class.
 * </p>
 *
 * @author Forge
 * @version $Id: InputConfirmMulligan.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputConfirmMulligan extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code>. */
    private static final long serialVersionUID = -8112954303001155622L;

    boolean keepHand = false;
    final boolean isCommander;

    private final CardCollection selected = new CardCollection();
    private final Player player;
    private final Player startingPlayer;

    public InputConfirmMulligan(final PlayerControllerHuman controller, final Player humanPlayer, final Player startsGame, final boolean commander) {
        super(controller);
        player = humanPlayer;
        isCommander = commander;
        startingPlayer = startsGame;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        final Game game = player.getGame();

        final StringBuilder sb = new StringBuilder();
        if (startingPlayer == player) {
            sb.append(player).append(", you are going first!\n\n");
        }
        else {
            sb.append(startingPlayer.getName()).append(" is going first.\n");
            sb.append(player).append(", you are going ").append(Lang.getOrdinal(game.getPosition(player, startingPlayer))).append(".\n\n");
        }

        if (isCommander) {
            getController().getGui().updateButtons(getOwner(), "Keep", "Exile", true, false, true);
            sb.append("Will you keep your hand or choose some cards to exile those and draw one less card?");
        }
        else {
            getController().getGui().updateButtons(getOwner(), "Keep", "Mulligan", true, true, true);
            sb.append("Do you want to keep your hand?");
        }

        showMessage(sb.toString());
    }

    @Override
    protected final boolean allowAwaitNextInput() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        keepHand = true;
        done();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        keepHand = false;
        done();
    }

    private void done() {
        if (isCommander) {
            // Clear the "selected" icon after clicking the done button
            for (final Card c : this.selected) {
                getController().getGui().setUsedToPay(c.getView(), false);
            }
        }
        stop();
    }

    volatile boolean cardSelectLocked = false;

    @Override
    protected boolean onCardSelected(final Card c0, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) { // the only place that would cause troubles - input is supposed only to confirm, not to fire abilities
        final boolean fromHand = player.getZone(ZoneType.Hand).contains(c0);
        final boolean isSerumPowder = c0.getName().equals("Serum Powder");
        final boolean isLegalChoice = fromHand && (isCommander || isSerumPowder);
        if (!isLegalChoice || cardSelectLocked) {
            return false;
        }

        final CardView cView = c0.getView();
        if (isSerumPowder && getController().getGui().confirm(cView, "Use " + cView + "'s ability?")) {
            cardSelectLocked = true;
            ThreadUtil.invokeInGameThread(new Runnable() {
                @Override public void run() {
                    final CardCollection hand = new CardCollection(c0.getController().getCardsIn(ZoneType.Hand));
                    for (final Card c : hand) {
                        player.getGame().getAction().exile(c);
                    }
                    c0.getController().drawCards(hand.size());
                    cardSelectLocked = false;
                }
            });
            return true;
        }

        if (isCommander) { // allow to choose cards for partial paris
            if (selected.contains(c0)) {
                getController().getGui().setUsedToPay(c0.getView(), false);
                selected.remove(c0);
            }
            else {
                getController().getGui().setUsedToPay(c0.getView(), true);
                selected.add(c0);
            }
            getController().getGui().updateButtons(getOwner(), "Keep", "Exile", true, !selected.isEmpty(), true);
        }
        return true;
    }

    public final boolean isKeepHand() {
        return keepHand;
    }

    public CardCollectionView getSelectedCards() {
        return selected;
    }

    @Override
    public String getActivateAction(final Card card) {
        return null;
    }
}
