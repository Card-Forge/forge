package forge.game.extrahands;

import com.google.common.collect.Lists;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

import java.util.List;

public class BackupPlanService {
    private final Player player;
    private boolean multipleHands = false;
    List<PlayerZone> hands = Lists.newArrayList();
    private PlayerZone hand;

    public BackupPlanService(Player p1) {
        this.player = p1;
    }

    public boolean initializeExtraHands() {
        hand = player.getZone(ZoneType.Hand);
        hands.add(hand);

        // If pl has Backup Plan as a Conspiracy draw that many extra hands
        if (player.getExtraZones() == null) {
            return multipleHands;
        }
        for(PlayerZone extraHand : player.getExtraZones()) {
            if (extraHand.getZoneType() == ZoneType.ExtraHand) {
                player.drawCards(7, extraHand);
                multipleHands = true;
                hands.add(extraHand);
                // If we figure out how to render the zone in the UI, do it here
            }
        }

        player.updateZoneForView(hand);
        return multipleHands;
    }

    public void chooseHand() {
        if (!multipleHands) {
            return;
        }

        PlayerZone library = player.getZone(ZoneType.Library);
        // Choose one of the starting hands and recycle the rest
        PlayerZone startingHand = player.getController().chooseStartingHand(hands);
        if (startingHand == hand) {
            for(PlayerZone extraHand : player.getExtraZones()) {
                if (extraHand.getZoneType() == ZoneType.ExtraHand) {
                    for (Card c : Lists.newArrayList(extraHand.getCards().iterator())) {
                        player.getGame().getAction().moveTo(library, c, null);
                    }
                }
            }
        } else {
            for (Card c : Lists.newArrayList(hand.getCards().iterator())) {
                player.getGame().getAction().moveTo(library, c, null);
            }

            for(PlayerZone extraHand : player.getExtraZones()) {
                boolean starting = startingHand.equals(extraHand);
                for (Card c : Lists.newArrayList(extraHand.getCards().iterator())) {
                    if (starting) {
                        player.getGame().getAction().moveTo(hand, c, null);
                    } else {
                        player.getGame().getAction().moveTo(library, c, null);
                    }
                }
            }
        }

        player.resetExtraZones(ZoneType.ExtraHand);
        player.updateZoneForView(player.getZone(ZoneType.Hand));
    }
}
