package forge.ai.ability;

import forge.ai.AITest;
import forge.card.CardType;
import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Issue #55 regression: a stacked-token prototype stands for N identical tokens,
 * so a continuous static ability it carries must apply N times (once per token),
 * even on the first static-ability evaluation after stacking.
 */
public class StackedTokenStaticAbilityTest extends AITest {

    private static final String ANTHEM = "Mode$ Continuous | Affected$ Creature.Other | AddPower$ 1 | AddToughness$ 1";

    private Card makeAnthemToken(Game game, Player p) {
        Card token = new Card(game.nextCardId(), p.getGame());
        token.setName("Test Anthem Token");
        token.setOwner(p);
        token.setController(p, 0);
        token.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
        token.setBasePower(1);
        token.setBaseToughness(1);
        token.setGamePieceType(GamePieceType.TOKEN);
        token.setGameTimestamp(p.getGame().getNextTimestamp());
        token.addStaticAbility(ANTHEM);
        return token;
    }

    @Test
    public void testStackedAnthemTokensApplyPerToken() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Victim first: add() expands any pending stack, so no card may be added after stacking.
        Card victim = addCard("Grizzly Bears", p);
        victim.setSickness(false);
        assertEquals(2, victim.getNetPower());

        PlayerZoneBattlefield battlefield = (PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield);

        // Build 3 identical anthem tokens, add them all, then stack them into one stack of 3.
        List<Card> tokens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Card token = makeAnthemToken(game, p);
            battlefield.add(token);
            tokens.add(token);
        }
        for (Card token : tokens) {
            assertTrue(battlefield.tryStackToken(token));
        }
        assertEquals(1, battlefield.getStackedTokens().size());
        assertEquals(3, battlefield.getStackedTokens().get(0).getQuantity());

        game.getAction().checkStateEffects(true);

        // 2/2 + 3x(+1/+1) = 5/5. Without the fix the prototype visits the collection
        // loop once, so the anthem applies once and the victim would only be 3/3.
        assertEquals(5, victim.getNetPower());
        assertEquals(5, victim.getNetToughness());
    }
}
