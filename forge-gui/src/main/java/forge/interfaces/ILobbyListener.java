package forge.interfaces;

import java.util.List;

import forge.deck.Deck;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.item.PaperCard;

public interface ILobbyListener {
    void message(String source, String message);
    void update(GameLobbyData state, int slot);
    void close();
    ClientGameLobby getLobby();

    default void draftPackArrived(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds) { }
    default void draftSeatPicked(int seatIndex, int[] seatQueueDepths) { }
    default void draftAutoPicked(int seatIndex, PaperCard card, int packNumber, int pickInPack) { }
    default void receiveEventPool(String eventId, Deck pool) { }
}
