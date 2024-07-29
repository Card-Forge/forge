package forge.gamemodes.match.input;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.player.PlayerControllerHuman;

import java.util.Deque;
import java.util.List;

public class InputChooseStartingHand extends InputSyncronizedBase {
    // An input for choosing multiple different starting hands for Backup Plan
    // Ideally all would render at once, but for now we're just going to show the primary, and loop through each hand
    // When you like one, click OK and the rest will be cleared
    private final Deque<PlayerZone> hands;
    PlayerZone primaryHand = null;
    Game game;


    public InputChooseStartingHand(final PlayerControllerHuman controller, final Player humanPlayer) {
        super(controller);

        game = humanPlayer.getGame();
        primaryHand = humanPlayer.getZone(ZoneType.Hand);
        hands = Lists.newLinkedList();
        for(PlayerZone extraHand : humanPlayer.getExtraZones()) {
            if (extraHand.getZoneType() == ZoneType.ExtraHand) {
                hands.add(extraHand);
            }
        }
    }

    public PlayerZone getSelectedHand() {
        // We're going to be manipulating what the primary hand is during this process so always return it
        return primaryHand;
    }

    @Override
    protected void showMessage() {
        if (hands.isEmpty()) {
            stop();
        }

        getController().getGui().updateButtons(getOwner(), "View Next", "Accept Hand", true, true, true);
        showMessage("Select the currently viewed hand to start the game with. The other " + hands.size() + " hand(s) will be shuffled into your library.");
    }

    @Override
    protected final void onCancel() {
        stop();
    }

    @Override
    protected final void onOk() {
        // Rotate hands. Take everything in current hand, add it to the next hand
        // Then take everything in the next hand and add it to the current hand
        PlayerZone nextExtraHand = hands.poll();
        assert nextExtraHand != null;
        List<Card> currentList = Lists.newArrayList(primaryHand.getCards().iterator());
        List<Card> extraList = Lists.newArrayList(nextExtraHand.getCards().iterator());

        primaryHand.setCards(extraList);
        nextExtraHand.setCards(currentList);
        hands.add(nextExtraHand);
    }


    @Override
    public String getActivateAction(Card card) {
        return null;
    }
}
