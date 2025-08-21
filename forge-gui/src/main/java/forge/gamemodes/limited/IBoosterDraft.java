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
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    default boolean setChoice(PaperCard c) {
        return setChoice(c, DeckSection.Sideboard);
    }

    boolean setChoice(PaperCard c, DeckSection section);
    void skipChoice();
    boolean hasNextChoice();
    boolean isRoundOver();
    DraftPack addBooster(CardEdition edition);
    Deck[] getComputerDecks(); // size 7, all the computers decks
    LimitedPlayer[] getOpposingPlayers(); // size 7, all the computers
    LimitedPlayer getHumanPlayer();
    default List<LimitedPlayer> getAllPlayers() {
        List<LimitedPlayer> out = new ArrayList<>();
        out.add(getHumanPlayer());
        out.addAll(Arrays.asList(getOpposingPlayers()));
        return out;
    }

    default DeckGroup getDecksAsGroup() {
        DeckGroup out = new DeckGroup();
        out.setHumanDeck(getHumanPlayer().deck);
        out.addAiDecks(Arrays.stream(getOpposingPlayers()).map(LimitedPlayer::getDeck).toArray(Deck[]::new));
        return out;
    }

    CardEdition[] LAND_SET_CODE = { null };
    String[] CUSTOM_RANKINGS_FILE = { null };
    boolean isPileDraft();

    void setLogEntry(IDraftLog draftingProcess);
    IDraftLog getDraftLog();
    boolean shouldShowDraftLog();
    void addLog(String message);
    void postDraftActions();
    LimitedPlayer getNeighbor(LimitedPlayer p, boolean left);
    LimitedPlayer getPlayer(int i);
}
