package forge.web.deck;

import forge.StaticData;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;
import forge.web.WebTestSupport;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LegalityTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
        FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, true);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, false);
    }

    private static PaperCard card(final String name) {
        return StaticData.instance().getCommonCards().getCard(name);
    }

    private static Deck commanderDeck(final String commander) {
        final Deck d = new Deck("t");
        d.getOrCreate(DeckSection.Commander).add(card(commander), 1);
        return d;
    }

    // Fails if a card outside the commander's colours is not marked, or the verdict is not the engine's
    @Test
    public void cardOutsideTheCommandersColoursIsFlagged() {
        final Deck d = commanderDeck("Meren of Clan Nel Toth");
        d.getMain().add(card("Lightning Bolt"), 1);
        final Legality.Result r = Legality.check(d, LegalityScope.of(GameType.Commander, null));
        Assert.assertEquals(r.flags().get("Lightning Bolt"), "outside B G");
        Assert.assertEquals(r.verdict(), GameType.Commander.getDeckFormat().getDeckConformanceProblem(d));
    }

    // Fails if a Constructed deck's fifth copy of a card is not flagged, or a card that allows any number is
    @Test
    public void constructedFlagsCopiesOverTheLimit() {
        final Deck d = new Deck("t");
        d.getMain().add(card("Lightning Bolt"), 5);
        d.getMain().add(card("Relentless Rats"), 20);
        final Legality.Result r = Legality.check(d, LegalityScope.of(GameType.Constructed, null));
        Assert.assertEquals(r.flags().get("Lightning Bolt"), Localizer.getInstance().getMessage("lblWebNOfM", 5, 4));
        Assert.assertFalse(r.flags().containsKey("Relentless Rats"), r.flags().toString());
    }

    // Fails if a card pool's own message, such as singleton, no longer reaches the verdict as one line
    @Test
    public void cardPoolReportsDeckRules() {
        final Deck d = new Deck("t");
        d.getMain().add(card("Sol Ring"), 2);
        d.getMain().add(card("Forest"), 98);
        final String problem = DeckFacts.problem(d, GameType.Constructed, FModel.getFormats().get("Canadian Highlander"));
        Assert.assertNotNull(problem, "a second Sol Ring in a singleton pool was not reported");
        Assert.assertFalse(problem.contains("\n"), problem);
        Assert.assertTrue(problem.contains("Sol Ring"), problem);
    }
}
