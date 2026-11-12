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
package forge.gamemodes.match.input;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.gamemodes.match.PromptContext;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

import java.util.List;

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

    /** Whether the prompt explains itself (decided once, when the question is asked). */
    private final boolean explained;

    public InputLondonMulligan(final PlayerControllerHuman controller, final Player humanPlayer, int returning) {
        super(controller);
        player = humanPlayer;
        toReturn = returning;
        explained = controller.getGui().explainsPrompts();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        final Localizer localizer = Localizer.getInstance();
        final Game game = player.getGame();
        game.getView().updateIsMulligan(true);
        int cardsLeft = toReturn - selected.size();

        StringBuilder sb = new StringBuilder();

        //explained prompts: a title with how many are picked, how to answer (click the cards in your hand), the main button
        //saying what it does ("Put 2 on bottom", shown but dim until enough are picked) and what the other one does
        if (explained) {
            getController().getGui().setPromptContext(PromptContext.because(
                    localizer.getMessage("lblArenaLondonTitle"), PromptContext.Verb.PUT_ON_BOTTOM));
            getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblArenaPutOnBottom", toReturn),
                    localizer.getMessage("lblArenaChooseForMe"), cardsLeft == 0, cardsLeft != 0, true);
            sb.append(localizer.getMessage("lblArenaLondonCounter", selected.size(), toReturn)).append("\n");
            sb.append(toReturn == 1 ? localizer.getMessage("lblArenaLondonBottomOne") : localizer.getMessage("lblArenaLondonBottom", toReturn));
        } else {
            getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOK"), localizer.getMessage("lblAuto"),
                    cardsLeft == 0, cardsLeft != 0, true);
            sb.append(String.format(localizer.getMessage("lblReturnForLondon"), cardsLeft));
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
        getController().getGame().getView().updateIsMulligan(false);
        if (explained) {
            getController().getGui().setPromptContext(PromptContext.NONE);
        }
        stop();
    }

    @Override
    protected final void onCancel() {
        int cardsLeft = toReturn - selected.size();
        int count = 0;
        for (Card c : player.getZone(ZoneType.Hand).getCards()) {
            if (selected.contains(c)) { continue; }

            selected.add(c);
            count++;

            if (cardsLeft == count) {
                break;
            }
        }

        onOk();
    }

    @Override
    protected boolean onCardSelected(final Card c0, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) { // the only place that would cause troubles - input is supposed only to confirm, not to fire abilities
        final boolean fromHand = player.getZone(ZoneType.Hand).contains(c0);
        if (!fromHand) {
            return false;
        }

        if (selected.contains(c0)) {
            selected.remove(c0);
        } else if (selected.size() < toReturn) {
            selected.add(c0);
        }
        showSelection();
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

    @Override
    protected Selection getSelection() {
        return Selection.ofChosen(CardView.getCollection(selected));
    }
}

