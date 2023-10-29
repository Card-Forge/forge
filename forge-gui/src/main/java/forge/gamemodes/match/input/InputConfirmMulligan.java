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
package forge.gamemodes.match.input;

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
import forge.util.Localizer;
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

    private final CardCollection selected = new CardCollection();
    private final Player player;
    private final Player startingPlayer;

    public InputConfirmMulligan(final PlayerControllerHuman controller, final Player humanPlayer, final Player startsGame) {
        super(controller);
        player = humanPlayer;
        startingPlayer = startsGame;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        final Localizer localizer = Localizer.getInstance();
        final Game game = player.getGame();

        final StringBuilder sb = new StringBuilder();
        if (startingPlayer == player) {
            sb.append(player).append(", ").append(localizer.getMessage("lblYouAreGoingFirst")).append("\n\n");
        }
        else {
            sb.append(startingPlayer.getName()).append(" ").append(localizer.getMessage("lblIsGoingFirst")).append(".\n");
            sb.append(player).append(", ").append(localizer.getMessage("lblYouAreGoing")).append(" ").append(Lang.getInstance().getOrdinal(game.getPosition(player, startingPlayer))).append(".\n\n");
        }

        getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblKeep"), localizer.getMessage("lblMulligan"), true, true, true);
        sb.append(localizer.getMessage("lblDoYouWantToKeepYourHand"));

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
        stop();
    }

    volatile boolean cardSelectLocked = false;

    /**
     * When a card selected at the time of mulligan (currently affects just Serum Powder).
     */
    @Override
    protected boolean onCardSelected(final Card c0, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) { // the only place that would cause troubles - input is supposed only to confirm, not to fire abilities
        if (cardSelectLocked) { return false; }
        final boolean fromHand = player.getZone(ZoneType.Hand).contains(c0);
        final boolean isSerumPowder = c0.getName().equals("Serum Powder");
        if (!isSerumPowder || !fromHand) {
            return false;
        }

        final CardView cView = c0.getView();
        //pfps leave this as is for now - it is confirming during another confirm so it might need the popup
        if (getController().getGui().confirm(cView, "Use " + cView + "'s ability?")) {
            cardSelectLocked = true;
            ThreadUtil.invokeInGameThread(new Runnable() {
                @Override public void run() {
                    final CardCollectionView hand = c0.getController().getCardsIn(ZoneType.Hand);
                    final int handSize = hand.size();
                    for (final Card c : hand.threadSafeIterable()) {
                        player.getGame().getAction().exile(c, null, null);
                    }
                    c0.getController().drawCards(handSize);
                    cardSelectLocked = false;
                }
            });
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
