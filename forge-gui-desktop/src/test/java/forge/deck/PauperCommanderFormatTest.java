package forge.deck;

import forge.StaticData;
import forge.ai.AITest;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.model.FModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumSet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PauperCommanderFormatTest extends AITest {

    private static final DeckFormat PDH = DeckFormat.PauperCommander;

    /**
     * Mock-based test classes that run earlier in the same JVM leave StaticData.instance() on a
     * card database that FModel never gave the format predicates to, so set them as FModel does.
     */
    @BeforeMethod
    public void loadPauperCommanderFormat() {
        final GameFormat format = FModel.getFormats().get("Pauper Commander");
        assertNotNull(format, "res/formats/Casual/PauperCommander.txt");
        StaticData.instance().setPauperCommanderPredicate(format.getFilterRules());
        StaticData.instance().setPauperCommanderBannedPredicate(PaperCardPredicates.names(format.getBannedCardNames()));
    }

    private static PaperCard card(final String name) {
        final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name);
        assertNotNull(card, name);
        return card;
    }

    /** A deck led by the given commanders, filled to 100 cards with one basic land type per commander. */
    private static Deck deck(final String[] commanders, final String... basics) {
        final Deck deck = new Deck("PDH test");
        for (final String commander : commanders) {
            deck.getOrCreate(DeckSection.Commander).add(card(commander));
        }
        final int perBasic = (100 - commanders.length) / basics.length;
        for (int i = 0; i < basics.length; i++) {
            final int count = i == 0 ? 100 - commanders.length - perBasic * (basics.length - 1) : perBasic;
            deck.getMain().add(card(basics[i]), count);
        }
        return deck;
    }

    @Test
    public void nonLegendaryUncommonCreatureLeadsALegalDeck() {
        // Cephalopod Sentry is a non-legendary uncommon artifact creature
        assertNull(PDH.getDeckConformanceProblem(deck(new String[]{"Cephalopod Sentry"}, "Island", "Plains")));
    }

    @Test
    public void commanderMustBeAnUncommonNonlandCreature() {
        assertFalse(PDH.isLegalCommander(card("Grave Titan").getRules()), "never printed at uncommon");
        assertFalse(PDH.isLegalCommander(card("Dryad Arbor").getRules()), "land creature");
        assertFalse(PDH.isLegalCommander(card("Counterspell").getRules()), "not a creature");
    }

    @Test
    public void bannedCardCannotBeTheCommander() {
        // Pradesh Gypsies is an uncommon creature in Legends, and on the Pauper Commander ban list
        assertFalse(PDH.isLegalCommander(card("Pradesh Gypsies").getRules()));
        assertNotNull(PDH.getDeckConformanceProblem(deck(new String[]{"Pradesh Gypsies"}, "Forest")));
    }

    @Test
    public void mainDeckMustBeCommonsOffTheBanList() {
        assertTrue(PDH.isLegalCard(card("Counterspell")));
        assertFalse(PDH.isLegalCard(card("Mystic Remora")), "banned, though printed at common");
        assertFalse(PDH.isLegalCard(card("Cephalopod Sentry")), "uncommon");
    }

    @Test
    public void nonLegendaryPartnersCanLeadTogether() {
        assertNull(PDH.getDeckConformanceProblem(deck(new String[]{"Ley Weaver", "Lore Weaver"}, "Forest", "Island")));
        assertNotNull(PDH.getDeckConformanceProblem(deck(new String[]{"Ley Weaver", "Proud Mentor"}, "Forest", "Plains")),
                "each has Partner with a different card");
        // Normal Commander still requires legendary partners
        assertFalse(card("Ley Weaver").getRules().canBePartnerCommanders(card("Lore Weaver").getRules()));
    }

    @Test
    public void lifeAndCommanderDamageComeFromTheVariant() {
        final GameRules pdhRules = new GameRules(GameType.PauperCommander);
        pdhRules.setAppliedVariants(EnumSet.of(GameType.PauperCommander));
        assertEquals(pdhRules.getCommanderDamageToLose(), 16);

        final GameRules commanderRules = new GameRules(GameType.Commander);
        commanderRules.setAppliedVariants(EnumSet.of(GameType.Commander));
        assertEquals(commanderRules.getCommanderDamageToLose(), 21);

        final Deck deck = deck(new String[]{"Cephalopod Sentry"}, "Island");
        final RegisteredPlayer rp = RegisteredPlayer.forVariants(4, EnumSet.of(GameType.PauperCommander), deck, null, false, null, null);
        assertEquals(rp.getStartingLife(), 30);
    }
}
