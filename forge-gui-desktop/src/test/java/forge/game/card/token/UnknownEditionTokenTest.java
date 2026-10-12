package forge.game.card.token;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.testng.annotations.Test;

import forge.StaticData;
import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.item.PaperToken;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * A printing can name an edition this machine does not have: a custom edition travels with a deck
 * in a network game, and the host has no file for it. The card's rules are found by name, and the
 * edition code stays on the card.
 */
public class UnknownEditionTokenTest extends AITest {
    private static final String MISSING = "ZZQ";
    private static final String CARD = "Najeela, the Blade-Blossom";
    private static final String TOKEN = "w_1_1_warrior";

    private PaperCard printingFromAMissingEdition() {
        assertNull(StaticData.instance().getCardEdition(MISSING), "the edition must not exist here");
        final PaperCard stock = StaticData.instance().getCommonCards().getCard(CARD);
        assertNotNull(stock);
        return new PaperCard(stock.getRules(), MISSING, stock.getRarity());
    }

    private static SpellAbility tokenAbilityOf(final Card card) {
        for (final Trigger t : card.getTriggers()) {
            final SpellAbility sa = t.ensureAbility();
            if (sa != null && sa.hasParam("TokenScript")) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void aPrintingKeepsItsEditionCodeWhenTheEditionIsMissingHere() throws Exception {
        final PaperCard sent = printingFromAMissingEdition();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(sent);
        }
        final PaperCard received;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            received = (PaperCard) in.readObject();
        }
        assertEquals(received.getName(), CARD);
        assertFalse(received.getRules().isUnsupported());
        assertEquals(received.getRules().getOracleText(), sent.getRules().getOracleText());
        assertEquals(received.getEdition(), MISSING);
    }

    @Test
    public void aCardFromAMissingEditionStillMakesItsToken() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card card = Card.fromPaperCard(printingFromAMissingEdition(), p);
        card.setGameTimestamp(game.getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(card);
        assertEquals(card.getSetCode(), MISSING);

        final SpellAbility sa = tokenAbilityOf(card);
        assertNotNull(sa);
        sa.setActivatingPlayer(p);

        assertEquals(sa.getParam("TokenScript"), TOKEN);
        final Card token = TokenInfo.getProtoType(sa.getParam("TokenScript"), sa, p);
        assertNotNull(token);
        assertTrue(token.getType().hasCreatureType("Warrior"));
    }

    @Test
    public void theTokenDatabaseAnswersForAMissingEdition() {
        assertNull(StaticData.instance().getCardEdition(MISSING), "the edition must not exist here");
        final PaperToken token = StaticData.instance().getAllTokens().getToken(TOKEN, MISSING);
        assertNotNull(token);
        assertTrue(token.getRules().getType().hasCreatureType("Warrior"));
    }
}
