package forge.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardType;
import forge.card.GamePieceType;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unfinity sticker sheets: the data in cardsfolder and the CR 123.2a pregame choice.
 */
public class StickerSheetTest extends AITest {

    private static final int SHEET_COUNT = 48;

    private List<PaperCard> allSheets() {
        List<PaperCard> sheets = new ArrayList<>();
        for (PaperCard pc : StaticData.instance().getVariantCards().getAllCards()) {
            if (pc.getRules().getType().isStickers()) {
                sheets.add(pc);
            }
        }
        return sheets;
    }

    @Test
    public void testAllSheetsLoad() {
        List<PaperCard> sheets = allSheets();
        assertEquals(sheets.size(), SHEET_COUNT, "expected every Unfinity sticker sheet in the variant card pool");
        for (PaperCard pc : sheets) {
            assertEquals(DeckSection.matchingSection(pc), DeckSection.Stickers,
                    pc.getName() + " should sort into the sticker sheet deck section");
            assertTrue(pc.getRules().isVariant(), pc.getName() + " should be a variant card");
        }
        assertEquals(CardType.CoreType.Stickers.toGamePieceType(), GamePieceType.STICKER_SHEET,
                "a sticker sheet should be a sticker sheet game piece");
    }

    /** CR 123.2a - three of the player's sheets are chosen at random and revealed. */
    @Test
    public void testThreeSheetsAreRevealed() {
        List<PaperCard> pool = allSheets().subList(0, 10);
        Player p = playerWithSheets(pool);
        assertEquals(p.getZone(ZoneType.StickerSheets).size(), 3,
                "three of the ten registered sheets should be revealed");
        for (Card c : p.getZone(ZoneType.StickerSheets)) {
            assertTrue(pool.stream().anyMatch(pc -> pc.getName().equals(c.getName())),
                    c.getName() + " was revealed but is not one of the registered sheets");
            assertTrue(c.getView().getText().contains(StickerSheet.getStickers(c).get(0).getDescription()),
                    c.getName() + " should list its stickers from the start");
        }
    }

    /**
     * Builds a game whose first player registered the given sheets, and runs the pregame
     * variant setup that {@link Match} would normally run.
     */
    private Player playerWithSheets(List<PaperCard> sheets) {
        Deck deck = new Deck();
        for (PaperCard pc : sheets) {
            deck.getOrCreate(DeckSection.Stickers).add(pc);
        }
        List<RegisteredPlayer> players = Lists.newArrayList();
        RegisteredPlayer rp = new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p1", null));
        players.add(rp);
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("p2", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "StickerSheetTest");
        Game game = new Game(players, rules, match);

        Player p = game.getPlayers().get(0);
        rp.restoreDeck();
        p.initVariantsZones(rp);
        return p;
    }
}
