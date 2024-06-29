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
package forge.gamemodes.limited;

import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;

/**
 * <p>
 * BoosterDraft interface.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public interface IBoosterDraft {
    int getRound();
    CardPool nextChoice();
    boolean setChoice(PaperCard c);
    boolean hasNextChoice();
    boolean isRoundOver();
    DraftPack addBooster(CardEdition edition);
    Deck[] getDecks(); // size 7, all the computers decks
    LimitedPlayer[] getOpposingPlayers(); // size 7, all the computers
    LimitedPlayer getHumanPlayer();

    CardEdition[] LAND_SET_CODE = { null };
    String[] CUSTOM_RANKINGS_FILE = { null };
    boolean isPileDraft();

    void setLogEntry(IDraftLog draftingProcess);
    IDraftLog getDraftLog();
    void postDraftActions();
    LimitedPlayer getNeighbor(LimitedPlayer p, boolean left);
    LimitedPlayer getPlayer(int i);
}
