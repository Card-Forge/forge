package forge.ai;

import java.util.List;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.sticker.AppliedSticker;
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
 * Putting a sticker on a card: what it changes, and what happens when the card changes zones.
 */
public class StickerApplyTest extends AITest {

    /** The stickers of one sheet, owned by the given player. */
    private List<Sticker> sheet(Player p, String sheetName) {
        PaperCard pc = StaticData.instance().getVariantCards().getCard(sheetName);
        assertNotNull(pc, "no such sticker sheet: " + sheetName);
        Card sheet = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        return StickerSheet.getStickers(sheet);
    }

    private Sticker first(List<Sticker> stickers, StickerKind kind) {
        for (Sticker s : stickers) {
            if (s.getKind() == kind) {
                return s;
            }
        }
        throw new AssertionError("sheet has no " + kind + " sticker");
    }

    private Sticker nth(List<Sticker> stickers, StickerKind kind, int index) {
        int seen = 0;
        for (Sticker s : stickers) {
            if (s.getKind() == kind && seen++ == index) {
                return s;
            }
        }
        throw new AssertionError("sheet has no " + kind + " sticker at " + index);
    }

    /**
     * CR 123.6a - a blank line is not a word, so a sticker put on a card printed with one fills
     * it rather than being placed among the words, and blanks nobody stickered stay put.
     */
    @Test
    public void testNameStickerFillsABlank() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        List<Sticker> stickers = sheet(p, "Eldrazi Guacamole Tightrope");

        Card ship = addCard("_____ _____ Rocketship", p);
        ship.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 0), game.getNextTimestamp(), 0));
        assertEquals(ship.getName(), "Eldrazi _____ Rocketship",
                "the first blank is filled, the second is still waiting");

        ship.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 1), game.getNextTimestamp(), 0));
        assertEquals(ship.getName(), "Eldrazi Guacamole Rocketship", "and then there are none left");

        // A third word has no blank to fill, so it is placed among the words as usual.
        ship.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 2), game.getNextTimestamp(), 3));
        assertEquals(ship.getName(), "Eldrazi Guacamole Rocketship Tightrope");
    }

    /** CR 123.6b - a later name sticker builds on the name the earlier one produced. */
    @Test
    public void testTwoNameStickersCompose() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        List<Sticker> stickers = sheet(p, "Eldrazi Guacamole Tightrope");
        Card bear = addCard("Grizzly Bears", p);

        bear.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 0), game.getNextTimestamp(), 0));
        assertEquals(bear.getName(), "Eldrazi Grizzly Bears");
        bear.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 1), game.getNextTimestamp(), 3));
        assertEquals(bear.getName(), "Eldrazi Grizzly Bears Guacamole");
    }

    /**
     * CR 123.5 - both halves of the zone rule, on the same board so that neither assertion can
     * pass merely because nothing was ever stickered.
     */
    @Test
    public void testStickersSurvivePublicZonesAndNotHiddenOnes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        List<Sticker> stickers = sheet(p, "Eldrazi Guacamole Tightrope");

        Card dies = addCard("Grizzly Bears", p);
        dies.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 0), game.getNextTimestamp(), 0));
        Card bounced = addCard("Grizzly Bears", p);
        bounced.addSticker(new AppliedSticker(nth(stickers, StickerKind.NAME, 1), game.getNextTimestamp(), 0));

        assertEquals(dies.getName(), "Eldrazi Grizzly Bears");
        assertEquals(bounced.getName(), "Guacamole Grizzly Bears");

        Card inGraveyard = game.getAction().moveTo(ZoneType.Graveyard, dies, null, null);
        Card inHand = game.getAction().moveTo(ZoneType.Hand, bounced, null, null);

        assertTrue(inGraveyard.isStickered(), "a public zone keeps the sticker");
        assertEquals(inGraveyard.getName(), "Eldrazi Grizzly Bears");

        assertFalse(inHand.isStickered(), "a hidden zone does not keep the sticker");
        assertEquals(inHand.getName(), "Grizzly Bears");
    }

    /**
     * An art sticker changes nothing else a player can see (CR 123.9), so the details pane has
     * to say it is there - otherwise two identical creatures cannot be told apart. The sheet
     * says which of its stickers have been taken, for the same reason.
     */
    @Test
    public void testStickersShowInTheCardDetails() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        List<Sticker> stickers = sheet(p, "Eldrazi Guacamole Tightrope");
        Card bear = addCard("Grizzly Bears", p);
        assertFalse(bear.getView().getText().contains("Stickers:"), "nothing on it yet");

        Sticker art = first(stickers, StickerKind.ART);
        bear.addSticker(new AppliedSticker(art, game.getNextTimestamp()));
        assertTrue(bear.getView().getText().contains(art.getDescription()),
                "the details should name the art sticker that is on it");

        Card sheetCard = p.getZone(ZoneType.StickerSheets).get(0);
        String sheetText = sheetCard.getView().getText();
        assertTrue(sheetText.contains("used"), "the sheet should mark the sticker as taken");
        assertTrue(sheetText.contains(first(stickers, StickerKind.NAME).getWord()),
                "and still list the ones that are not");
    }
}
