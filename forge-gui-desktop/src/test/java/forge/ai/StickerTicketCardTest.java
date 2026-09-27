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
 * The Unfinity cards that hand out tickets and stickers without needing anything the engine does
 * not already do. A card script's abilities are parsed when the card is built, so building each
 * one is itself the check that the script is well formed.
 */
public class StickerTicketCardTest extends AITest {

    private static final List<String> CARDS = List.of(
            "Finishing Move", "Robo-Piñata", "Command Performance", "Costume Shop",
            "Done for the Day", "Park Bleater", "Lineprancers", "Tusk and Whiskers",
            "Wolf in _____ Clothing", "Fight the _____ Fight",
            "Ambassador Blorpityblorpboop", "Roxi, Publicist to the Stars",
            "Pin Collection", "Clandestine Chameleon", "_____ _____ Rocketship",
            "Last Voyage of the _____");

    private Card giveSheet(Player p, String sheetName) {
        PaperCard pc = StaticData.instance().getVariantCards().getCard(sheetName);
        assertNotNull(pc, "no such sticker sheet: " + sheetName);
        Card sheet = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        return sheet;
    }

    /** Costume Shop is an Attraction, which Forge keeps with the other variant cards. */
    private PaperCard paper(String name) {
        PaperCard pc = StaticData.instance().getCommonCards().getCard(name);
        return pc != null ? pc : StaticData.instance().getVariantCards().getCard(name);
    }

    @Test
    public void testEveryCardBuilds() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        for (String name : CARDS) {
            PaperCard pc = paper(name);
            assertNotNull(pc, name + " is not in the card pool");
            Card c = Card.fromPaperCard(pc, p);
            assertTrue(c.getSpellAbilities().size() + c.getTriggers().size()
                    + c.getKeywords().size() > 0, name + " built with nothing on it");
        }
    }

    /**
     * Pin Collection and Clandestine Chameleon both hand an object the abilities printed on
     * ability stickers sitting on a different object.
     */
    @Test
    public void testStickerAbilitiesCarryToAnotherObject() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card sheet = giveSheet(p, "Ancestral Hot Dog Minotaur");
        Sticker flying = StickerSheet.getStickers(sheet).stream()
                .filter(x -> "Flying".equals(x.getKeywords())).findFirst().orElseThrow();

        Card pins = addCard("Pin Collection", p);
        Card bear = addCard("Grizzly Bears", p);
        pins.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        assertEquals(bear.getNetPower(), 3, "equipped creature gets +1/+1");
        assertFalse(bear.hasKeyword("Flying"), "no sticker on the Equipment yet");

        pins.addSticker(new AppliedSticker(flying, game.getNextTimestamp()));
        game.getAction().checkStateEffects(true);
        assertTrue(bear.hasKeyword("Flying"), "the sticker is on the Equipment, the creature has it");

        // The Chameleon reads stickers on everything else you own, including the graveyard.
        Card chameleon = addCard("Clandestine Chameleon", p);
        game.getAction().checkStateEffects(true);
        assertTrue(chameleon.hasKeyword("Flying"), "an ability sticker on another permanent you own");

        Card corpse = addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        Sticker second = StickerSheet.getStickers(sheet).stream()
                .filter(x -> x.getKind() == StickerKind.ABILITY && !x.equals(flying))
                .findFirst().orElseThrow();
        corpse.addSticker(new AppliedSticker(second, game.getNextTimestamp()));
        game.getAction().checkStateEffects(true);
        assertTrue(chameleon.getKeywords().size() > 1
                        || chameleon.getSpellAbilities().size() > 0
                        || chameleon.getTriggers().size() > 0,
                "and one in your graveyard");
    }
}
