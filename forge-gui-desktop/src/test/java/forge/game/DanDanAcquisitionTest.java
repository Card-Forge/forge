package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DanDanAcquisitionTest extends AITest {
    @Test
    public void commonLibraryCardAcquiresNamedHandOwnerAndKeepsSourceLki() {
        assertCommonPoolHandAcquisition(ZoneType.Library);
    }

    @Test
    public void commonGraveyardCardAcquiresNamedHandOwnerAndKeepsSourceLki() {
        assertCommonPoolHandAcquisition(ZoneType.Graveyard);
    }

    @Test
    public void ordinaryLibraryToHandMovementDoesNotMatchRecipientOwnershipReplacement() {
        Game game = newGame(false);
        Player originalOwner = game.getRegisteredPlayers().get(0);
        Player recipient = game.getRegisteredPlayers().get(1);

        installRecipientOwnershipReplacement(game, recipient,
                "Event$ Moved | ActiveZones$ Battlefield | Origin$ Library | Destination$ Hand "
                        + "| ValidCard$ Card.YouOwn | Prevent$ True | Description$ Prevent this move.");
        Card card = addCardToZone("Dandân", originalOwner, ZoneType.Library);

        game.getAction().moveTo(recipient.getZone(ZoneType.Hand), card, null);

        Card inHand = game.findById(card.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Hand).contains(inHand),
                "the recipient's YouOwn replacement does not match an ordinarily owned card");
        assertSame(inHand.getOwner(), originalOwner);
    }

    @Test
    public void preventedProspectiveAcquisitionLeavesSourceAndOwnerUnchanged() {
        Game game = newGame(true);
        Player originalOwner = game.getRegisteredPlayers().get(0);
        Player recipient = game.getRegisteredPlayers().get(1);
        installRecipientOwnershipReplacement(game, recipient,
                "Event$ Moved | ActiveZones$ Battlefield | Origin$ Library | Destination$ Hand "
                        + "| ValidCard$ Card.YouOwn | Prevent$ True | Description$ Prevent this move.");
        Card card = addCardToZone("Dandân", originalOwner, ZoneType.Library);

        game.getAction().moveTo(recipient.getZone(ZoneType.Hand), card, null);

        assertTrue(originalOwner.getCardsIn(ZoneType.Library).contains(card),
                "the replacement prevents the proposed move, leaving the card in its source zone");
        assertTrue(recipient.getCardsIn(ZoneType.Hand).isEmpty());
        assertSame(card.getOwner(), originalOwner);
    }

    @Test
    public void prospectiveAcquisitionReplacementRedirectsWithoutChangingProvenance() {
        Game game = newGame(true);
        Player originalOwner = game.getRegisteredPlayers().get(0);
        Player recipient = game.getRegisteredPlayers().get(1);
        Card replacementSource = addCardToZone("Veteran Ghoulcaller", recipient, ZoneType.Hand);
        game.getAction().moveToPlay(replacementSource, recipient, null, AbilityKey.newMap());
        replacementSource.setSVar("RedirectToSharedGraveyard",
                "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Library | Destination$ Graveyard | Hidden$ True");
        ReplacementEffect replacement = ReplacementHandler.parseReplacement(
                "Event$ Moved | ActiveZones$ Battlefield | Origin$ Library | Destination$ Hand "
                        + "| ValidCard$ Card.YouOwn | ReplaceWith$ RedirectToSharedGraveyard "
                        + "| Description$ Put this card into the shared graveyard instead.", replacementSource, false);
        replacementSource.addReplacementEffect(replacement);
        Card card = addCardToZone("Dandân", originalOwner, ZoneType.Library);

        game.getAction().moveTo(recipient.getZone(ZoneType.Hand), card, null);

        Card inGraveyard = game.findById(card.getId());
        assertTrue(originalOwner.getCardsIn(ZoneType.Graveyard).contains(inGraveyard),
                "the replacement's actual destination is the shared graveyard");
        assertTrue(recipient.getCardsIn(ZoneType.Hand).isEmpty());
        assertSame(inGraveyard.getOwner(), originalOwner,
                "redirected movement preserves the source owner rather than acquiring the proposed hand recipient");
        assertTrue(recipient.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Library).stream()
                .anyMatch(lki -> lki.getId() == card.getId() && lki.getOwner() == originalOwner),
                "the redirected zone transition retains source LKI");
    }

    private void assertCommonPoolHandAcquisition(ZoneType sourceType) {
        Game game = newGame(true);
        Player originalOwner = game.getRegisteredPlayers().get(0);
        Player recipient = game.getRegisteredPlayers().get(1);
        Card card = addCardToZone("Dandân", originalOwner, sourceType);

        game.getAction().moveTo(recipient.getZone(ZoneType.Hand), card, null);

        Card inHand = game.findById(card.getId());
        assertTrue(recipient.getCardsIn(ZoneType.Hand).contains(inHand));
        assertSame(inHand.getOwner(), recipient,
                "a successful shared " + sourceType + " to named hand move transfers ownership");
        assertEquals(originalOwner, card.getOwner(), "the source object retains its last-known owner");
        assertTrue(recipient.getZone(ZoneType.Hand).getCardsAddedThisTurn(sourceType).stream()
                .anyMatch(lki -> lki.getId() == card.getId() && lki.getOwner() == originalOwner),
                "the hand's recorded source state keeps its original owner");
    }

    private void installRecipientOwnershipReplacement(Game game, Player recipient, String script) {
        Card replacementSource = addCardToZone("Veteran Ghoulcaller", recipient, ZoneType.Hand);
        game.getAction().moveToPlay(replacementSource, recipient, null, AbilityKey.newMap());
        ReplacementEffect replacement = ReplacementHandler.parseReplacement(script, replacementSource, false);
        replacementSource.addReplacementEffect(replacement);
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan acquisition test");
        return match.createGame();
    }
}
