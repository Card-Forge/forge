/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2019  Forge Team
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
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

/**
 * <p>
 * InputLondonMulligan class.
 * </p>
 *
 * @author Forge
 * @version $Id: InputLondonMulligan.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputLondonMulligan extends InputSyncronizedBase {
    private static final long serialVersionUID = -8112954113001155622L;

    private final CardCollection selected = new CardCollection();
    private final Player player;
    private final int toReturn;

    public InputLondonMulligan(final PlayerControllerHuman controller, final Player humanPlayer, int returning) {
        super(controller);
        player = humanPlayer;
        toReturn = returning;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        final Localizer localizer = Localizer.getInstance();
        final Game game = player.getGame();
        game.getView().updateIsMulligan(true);
        int cardsLeft = toReturn - selected.size();

        StringBuilder sb = new StringBuilder();

        getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOk"), "", cardsLeft == 0, false, true);

        sb.append(String.format(localizer.getMessage("lblReturnForLondon"), cardsLeft));

        showMessage(sb.toString());
    }

    @Override
    protected final boolean allowAwaitNextInput() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        cardSelectLocked = true;
        done();
    }

    private void done() {
        resetCardHighlights();
        getController().getGame().getView().updateIsMulligan(false);
        stop();
    }

    volatile boolean cardSelectLocked = false;

    @Override
    protected boolean onCardSelected(final Card c0, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) { // the only place that would cause troubles - input is supposed only to confirm, not to fire abilities
        final boolean fromHand = player.getZone(ZoneType.Hand).contains(c0);
        if (!fromHand || cardSelectLocked) {
            return false;
        }

        if (selected.contains(c0)) {
            selected.remove(c0);
        } else if (selected.size() < toReturn) {
            selected.add(c0);
        }
        setCardHighlight(c0, selected.contains(c0));
        showMessage();
        return true;
    }

    public CardCollectionView getSelectedCards() {
        return selected;
    }

    @Override
    public String getActivateAction(final Card card) {
        return null;
    }

    private void setCardHighlight(final Card card, final boolean state) {
        getController().getGui().setUsedToPay(card.getView(), state);
    }

    private void resetCardHighlights() {
        for (final Card c : selected) {
            getController().getGui().setUsedToPay(c.getView(), false);
        }
    }
}

