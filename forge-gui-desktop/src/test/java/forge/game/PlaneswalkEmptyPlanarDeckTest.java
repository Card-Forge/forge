package forge.game;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Issue #11800: a Planechase game froze for good when a player whose planar deck was empty
 * planeswalked, because the effect read the top card of that empty deck and threw on the
 * game thread.
 */
public class PlaneswalkEmptyPlanarDeckTest extends AITest {

    private Game planechaseGame() {
        Game game = initAndCreateGame();
        game.getRules().addAppliedVariant(GameType.Planechase);
        return game;
    }

    private Card addPlane(String name, Player owner) {
        PaperCard pc = FModel.getMagicDb().getVariantCards().getCard(name);
        Card c = Card.fromPaperCard(pc, owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(ZoneType.PlanarDeck).add(c);
        return c;
    }

    private void planeswalk(Player activator) {
        SpellAbility sa = AbilityFactory.getAbility("DB$ Planeswalk", addCard("Island", activator));
        sa.setActivatingPlayer(activator);
        sa.resolve();
    }

    @Test
    public void planeswalkingWithAnEmptyPlanarDeckKeepsTheCurrentPlane() {
        Game game = planechaseGame();
        Player owner = game.getPlayers().get(0);
        Player walker = game.getPlayers().get(1);
        Card minamo = addPlane("Minamo", owner);
        owner.initPlane();
        assertTrue(walker.getCardsIn(ZoneType.PlanarDeck).isEmpty());

        planeswalk(walker);

        assertEquals(game.getActivePlanes().size(), 1, "nothing to planeswalk to, so the plane stays");
        assertEquals(game.getActivePlanes().get(0), minamo);
        assertTrue(game.getZoneOf(minamo).is(ZoneType.Command));
    }

    @Test
    public void planeswalkingStillWorksWhenThePlanarDeckHasCards() {
        Game game = planechaseGame();
        Player owner = game.getPlayers().get(0);
        Player walker = game.getPlayers().get(1);
        Card minamo = addPlane("Minamo", owner);
        owner.initPlane();
        Card library = addPlane("The Lux Foundation Library", walker);

        planeswalk(walker);

        assertEquals(game.getActivePlanes().size(), 1);
        assertEquals(game.getActivePlanes().get(0).getName(), library.getName());
        assertTrue(game.getZoneOf(minamo).is(ZoneType.PlanarDeck), "the old plane goes back to its owner's planar deck");
    }

    /** The owner's only plane is the current one: it returns to their deck and comes straight back. */
    @Test
    public void planeswalkingWithOnlyTheCurrentPlaneReturnsToIt() {
        Game game = planechaseGame();
        Player owner = game.getPlayers().get(0);
        Card minamo = addPlane("Minamo", owner);
        owner.initPlane();
        assertTrue(owner.getCardsIn(ZoneType.PlanarDeck).isEmpty());

        planeswalk(owner);

        assertEquals(game.getActivePlanes().size(), 1);
        assertEquals(game.getActivePlanes().get(0).getName(), minamo.getName());
    }

    /** Game.onPlayerLost calls this directly when the owner of the current plane leaves. */
    @Test
    public void playerPlaneswalkIsANoOpOnAnEmptyPlanarDeck() {
        Game game = planechaseGame();
        Player walker = game.getPlayers().get(1);
        game.setActivePlanes(new java.util.ArrayList<>());

        walker.planeswalk(null);

        assertTrue(game.getActivePlanes().isEmpty());
    }
}
