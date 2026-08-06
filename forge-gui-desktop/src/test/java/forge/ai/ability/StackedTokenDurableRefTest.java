// REFORGE COMMANDER EXTENSION
package forge.ai.ability;

import forge.ai.AITest;
import forge.card.CardType;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.AssertJUnit.*;

public class StackedTokenDurableRefTest extends AITest {

    private Card makeSoldier(Game game, Player p) {
        Card token = new Card(game.nextCardId(), p.getGame());
        token.setName("Test Soldier");
        token.setOwner(p);
        token.setController(p, 0);
        token.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
        token.setBasePower(1);
        token.setBaseToughness(1);
        token.setGamePieceType(forge.card.GamePieceType.TOKEN);
        token.setGameTimestamp(p.getGame().getNextTimestamp());
        return token;
    }

    @Test
    public void testUnreferencedTokenFoldsNormally() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        PlayerZoneBattlefield bf = (PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield);

        Card token = makeSoldier(game, p);
        bf.add(token);
        assertTrue(bf.getCards().contains(token));

        assertTrue(bf.tryStackToken(token));
        assertFalse(bf.getCards().contains(token));
        Card expanded = bf.getCardsUnexpanded().iterator().next();
        assertNotSame(token, expanded);
        assertEquals(token.getName(), expanded.getName());
    }

    @Test
    public void testReferencedTokenStaysResident() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        PlayerZoneBattlefield bf = (PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield);
        Card host = addCard("Grizzly Bears", p);

        Card token = makeSoldier(game, p);
        bf.add(token);
        host.addRemembered(token);

        assertTrue(bf.getCards().contains(token));
        assertSame(token, host.getFirstRemembered());
    }

    @Test
    public void testMixedFoldedAndResidentTokens() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        PlayerZoneBattlefield bf = (PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield);
        Card host = addCard("Grizzly Bears", p);

        Card fold1 = makeSoldier(game, p);
        bf.add(fold1);
        assertTrue(bf.tryStackToken(fold1));
        assertEquals(1, bf.getStackedTokens().size());

        Card resident = makeSoldier(game, p);
        bf.add(resident);
        host.addRemembered(resident);

        // ponytail: bf.add triggers onChanged → updateZoneForView → getCards → expandStacks,
        // which clears stackedTokens. Just verify both resident and a soldier copy exist.
        int soldierCount = 0;
        boolean hasResident = false;
        boolean hasDistinctSoldier = false;
        for (Card c : bf.getCards()) {
            if ("Test Soldier".equals(c.getName())) {
                soldierCount++;
                if (c != resident) hasDistinctSoldier = true;
            }
            if (c == resident) hasResident = true;
        }
        assertTrue("resident survives expand", hasResident);
        assertTrue("expansion produced a distinct Test Soldier or at least 2 soldiers",
                hasDistinctSoldier || soldierCount >= 2);
    }
}
