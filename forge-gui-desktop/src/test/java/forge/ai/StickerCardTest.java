package forge.ai;

import java.util.List;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerKind;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * The stickers a player may choose from, and the first card that places one.
 */
public class StickerCardTest extends AITest {

    private Card giveSheet(Player p, String sheetName) {
        PaperCard pc = StaticData.instance().getVariantCards().getCard(sheetName);
        assertNotNull(pc, "no such sticker sheet: " + sheetName);
        Card sheet = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        return sheet;
    }

    /** CR 123.3 - the pool is what is on the revealed sheets and not already on something. */
    @Test
    public void testAvailableStickersExcludePlacedOnes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        giveSheet(p, "Eldrazi Guacamole Tightrope");

        List<Sticker> before = StickerSheet.getAvailableStickers(p);
        // Ability stickers carry no Forge ability yet, so they are not offered.
        assertEquals(before.size(), 6, "three name and three art stickers, no ability or P/T yet");

        Card bear = addCard("Grizzly Bears", p);
        Sticker taken = before.get(0);
        bear.addSticker(new forge.game.card.sticker.AppliedSticker(taken, game.getNextTimestamp(), 0));

        List<Sticker> after = StickerSheet.getAvailableStickers(p);
        assertEquals(after.size(), 5, "the placed sticker is no longer available");
        assertFalse(after.stream().anyMatch(s -> s.getSlot().equals(taken.getSlot())
                && s.getSheet().equals(taken.getSheet())));
    }

    /** CR 123.3c - a sticker whose ticket cost the player cannot pay is not a legal choice. */
    @Test
    public void testTicketCostGatesAvailability() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card sheet = giveSheet(p, "Eldrazi Guacamole Tightrope");

        int cheapestPT = StickerSheet.getStickers(sheet).stream()
                .filter(s -> s.getKind() == StickerKind.PT)
                .mapToInt(Sticker::getTickets).min().orElseThrow();
        assertTrue(cheapestPT > 0);

        assertFalse(StickerSheet.getAvailableStickers(p).stream().anyMatch(s -> s.getKind() == StickerKind.PT),
                "with no tickets, no P/T sticker is affordable");

        p.setCounters(CounterEnumType.TICKET, cheapestPT, p, false);
        assertTrue(StickerSheet.getAvailableStickers(p).stream().anyMatch(s -> s.getKind() == StickerKind.PT),
                "with tickets, a P/T sticker becomes a legal choice");
    }

    /** CR 123.5 - a sticker on a card that goes to a hidden zone is on nothing, so it is free again. */
    @Test
    public void testStickerReturnsToThePoolFromAHiddenZone() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        giveSheet(p, "Eldrazi Guacamole Tightrope");

        Card bear = addCard("Grizzly Bears", p);
        Sticker taken = StickerSheet.getAvailableStickers(p).get(0);
        bear.addSticker(new forge.game.card.sticker.AppliedSticker(taken, game.getNextTimestamp(), 0));
        assertEquals(StickerSheet.getAvailableStickers(p).size(), 5);

        game.getAction().moveTo(ZoneType.Hand, bear, null, null);
        assertEquals(StickerSheet.getAvailableStickers(p).size(), 6,
                "the sticker is on no object now, so it can be chosen again");
    }
}
