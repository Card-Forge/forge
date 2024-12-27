package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class LandDropTests extends AITest {
    @Test
    public void testPlayTapland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card te = addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(te));
    }

    @Test
    public void testPlayTypedTapland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Radiant Grove", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Test
    public void testPlayUntappedLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Test
    public void testPlayDual() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Savannah", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Test
    public void testPlayCheckland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Sunpetal Grove", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Ignore
    @Test
    public void testPlayShockland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        Card gs = addCardToZone("Godless Shrine", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);
        addCardToZone("Swamp", p, ZoneType.Hand);
        Card ts = addCardToZone("Thoughtseize", p, ZoneType.Hand);
        addCardToZone("Bitterblossom", p, ZoneType.Hand);
        addCardToZone("Lingering Souls", p, ZoneType.Hand);
        addCardToZone("Sorin, Solemn Visitor", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCardToZone("Godless Shrine", opponent, ZoneType.Hand);
        addCardToZone("Plains", opponent, ZoneType.Hand);
        addCardToZone("Swamp", opponent, ZoneType.Hand);
        addCardToZone("Thoughtseize", opponent, ZoneType.Hand);
        Card bb = addCardToZone("Bitterblossom", opponent, ZoneType.Hand);
        addCardToZone("Lingering Souls", opponent, ZoneType.Hand);
        addCardToZone("Sorin, Solemn Visitor", opponent, ZoneType.Hand);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        // test that we shock in Godless Shrine and cast thoughtseize, and then take bitterblossom
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(gs));
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(ts));
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(bb));
    }

    // TODO test bounceland on empty board
    // test bounceland with amulet of vigor
}
