package forge.screens.home;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.model.FModel;

/** #11800: picking a new main deck in the lobby dropped the planar deck picked for that player. */
public class VLobbyVariantSectionTest extends AITest {

    @Test
    public void aPickedPlanarDeckSurvivesAMainDeckChange() {
        CardPool planes = new CardPool();
        planes.add(FModel.getMagicDb().getVariantCards().getCard("Minamo"));
        Deck previous = new Deck();
        previous.putSection(DeckSection.Planes, planes);

        assertSame(VLobby.variantSectionToKeep("Random", previous, DeckSection.Planes), planes);
        assertNull(VLobby.variantSectionToKeep("Use deck's planes section", previous, DeckSection.Planes));
    }
}
