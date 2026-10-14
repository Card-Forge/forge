package forge.game;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** #11800: planeswalking with an empty planar deck threw and froze the game. */
public class PlaneswalkEmptyPlanarDeckTest extends AITest {

    @Test
    public void planeswalkingWithAnEmptyPlanarDeckKeepsTheCurrentPlane() {
        Game game = initAndCreateGame();
        game.getRules().addAppliedVariant(GameType.Planechase);
        Player owner = game.getPlayers().get(0);
        Player walker = game.getPlayers().get(1);
        Card minamo = Card.fromPaperCard(FModel.getMagicDb().getVariantCards().getCard("Minamo"), owner);
        owner.getZone(ZoneType.PlanarDeck).add(minamo);
        owner.initPlane();

        SpellAbility sa = AbilityFactory.getAbility("DB$ Planeswalk", addCard("Island", walker));
        sa.setActivatingPlayer(walker);
        sa.resolve();

        assertEquals(game.getActivePlanes(), List.of(minamo));
    }
}
