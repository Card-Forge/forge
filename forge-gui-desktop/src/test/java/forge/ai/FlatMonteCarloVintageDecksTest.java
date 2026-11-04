package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Vintage metagame decks (main decks, written out here): each one a deck the Flat MC AI is meant to play as a combo
 * deck must be read as one. A failure prints the deck, what the scan found instead, and what to look at.
 */
public class FlatMonteCarloVintageDecksTest extends AITest {
    private static final String JEWEL_SHOPS = "1 Ancestral Recall\n"
            + "4 Transmute Artifact\n"
            + "1 Black Lotus\n"
            + "1 Chaos Defiler\n"
            + "1 Tinker\n"
            + "1 Sol Ring\n"
            + "3 Grim Monolith\n"
            + "4 The One Ring\n"
            + "1 Paradox Engine\n"
            + "4 Ancient Tomb\n"
            + "3 Relic of Sauron\n"
            + "4 Mishra's Workshop\n"
            + "1 Mox Emerald\n"
            + "1 Mox Jet\n"
            + "1 Mox Pearl\n"
            + "1 Mox Ruby\n"
            + "1 Mox Sapphire\n"
            + "4 Coveted Jewel\n"
            + "4 Phyrexian Metamorph\n"
            + "1 Mana Vault\n"
            + "2 Sink into Stupor\n"
            + "1 Mana Crypt\n"
            + "2 Talon Gates of Madara\n"
            + "1 Karn, the Great Creator\n"
            + "4 Force of Will\n"
            + "1 Time Walk\n"
            + "1 Trinisphere\n"
            + "1 Tolarian Academy\n"
            + "1 Urza's Saga\n"
            + "1 Vexing Bauble\n"
            + "2 Stock Up\n"
            + "1 Otawara, Soaring City\n";

    private static final String RAKER_SHOPS = "4 The One Ring\n"
            + "1 Black Lotus\n"
            + "4 Sensei's Divining Top\n"
            + "1 Sol Ring\n"
            + "4 Glaring Fleshraker\n"
            + "4 Scrawling Crawler\n"
            + "1 Inventors' Fair\n"
            + "1 Karn, the Great Creator\n"
            + "1 Mana Vault\n"
            + "1 Mana Crypt\n"
            + "3 Manifold Key\n"
            + "4 Mishra's Workshop\n"
            + "1 Mox Emerald\n"
            + "1 Mox Jet\n"
            + "2 Mox Opal\n"
            + "1 Mox Pearl\n"
            + "1 Mox Ruby\n"
            + "1 Mox Sapphire\n"
            + "1 Mystic Forge\n"
            + "1 Pithing Needle\n"
            + "4 Grim Monolith\n"
            + "1 Chalice of the Void\n"
            + "1 City of Traitors\n"
            + "3 Talon Gates of Madara\n"
            + "4 Tezzeret, Cruel Captain\n"
            + "1 The Fantasticar\n"
            + "4 Ancient Tomb\n"
            + "1 Time Vault\n"
            + "1 Tolarian Academy\n"
            + "1 Urza's Saga\n"
            + "1 Vexing Bauble\n";

    private static final String SPHERE_SHOPS = "2 Sculpting Steel\n"
            + "1 Thorn of Amethyst\n"
            + "4 Interdimensional Web Watch\n"
            + "1 Mox Emerald\n"
            + "1 Mox Jet\n"
            + "1 Mox Pearl\n"
            + "1 Mox Ruby\n"
            + "1 Mox Sapphire\n"
            + "1 Mox Opal\n"
            + "1 Urza's Saga\n"
            + "1 Tolarian Academy\n"
            + "1 Black Lotus\n"
            + "1 Mana Crypt\n"
            + "4 Ancient Tomb\n"
            + "1 Sol Ring\n"
            + "1 Mana Vault\n"
            + "1 Vexing Bauble\n"
            + "1 Plains\n"
            + "4 The One Ring\n"
            + "2 Paradox Engine\n"
            + "1 Mystic Forge\n"
            + "1 Karn, the Great Creator\n"
            + "4 Smelting Vat\n"
            + "1 Lodestone Golem\n"
            + "1 Trinisphere\n"
            + "3 Dusk Rose Reliquary\n"
            + "4 Sphere of Resistance\n"
            + "4 Mishra's Workshop\n"
            + "1 Strip Mine\n"
            + "1 Crucible of Worlds\n"
            + "4 Wasteland\n"
            + "4 Castle Doom\n";

    private static final String DREDGE = "4 Prized Amalgam\n"
            + "4 Creeping Chill\n"
            + "4 Narcomoeba\n"
            + "4 Stinkweed Imp\n"
            + "1 Golgari Grave-Troll\n"
            + "4 Shambling Shell\n"
            + "4 Ichorid\n"
            + "1 Mental Misstep\n"
            + "4 Golgari Thug\n"
            + "1 Chalice of the Void\n"
            + "4 Hollow One\n"
            + "4 Serum Powder\n"
            + "4 Silversmote Ghoul\n"
            + "4 Bazaar of Baghdad\n"
            + "4 Grief\n"
            + "4 Force of Will\n"
            + "1 Force of Negation\n"
            + "4 Noxious Revival\n";

    static final String BESEECH_STORM_DECK = "1 Ancestral Recall\n"
            + "3 Beseech the Mirror\n"
            + "1 Black Lotus\n"
            + "1 Bolas's Citadel\n"
            + "1 Brainstorm\n"
            + "4 Dark Ritual\n"
            + "1 Demonic Tutor\n"
            + "3 Extrapolate the Impossible\n"
            + "1 Flooded Strand\n"
            + "1 Flusterstorm\n"
            + "4 Force of Will\n"
            + "1 Gitaxian Probe\n"
            + "1 Island\n"
            + "1 Lion's Eye Diamond\n"
            + "1 Lorien Revealed\n"
            + "1 Lotus Petal\n"
            + "1 Mana Crypt\n"
            + "1 Mana Vault\n"
            + "1 Mental Misstep\n"
            + "1 Misty Rainforest\n"
            + "1 Mox Emerald\n"
            + "1 Mox Jet\n"
            + "1 Mox Pearl\n"
            + "1 Mox Ruby\n"
            + "1 Mox Sapphire\n"
            + "2 Polluted Delta\n"
            + "4 Stock Up\n"
            + "1 Sensei's Divining Top\n"
            + "1 Sol Ring\n"
            + "1 Swamp\n"
            + "1 Tendrils of Agony\n"
            + "1 The Fantasticar\n"
            + "1 The One Ring\n"
            + "1 Time Walk\n"
            + "1 Tinker\n"
            + "1 Tolarian Academy\n"
            + "4 Underground Sea\n"
            + "1 Urza's Saga\n"
            + "1 Yawgmoth's Will\n"
            + "4 Ponder\n";

    private static final String SCAM = "4 Bazaar of Baghdad\n"
            + "1 Mox Jet\n"
            + "4 Prized Amalgam\n"
            + "1 Strip Mine\n"
            + "4 Grief\n"
            + "4 Life from the Loam\n"
            + "4 Chrome Mox\n"
            + "1 Mox Emerald\n"
            + "4 Witherbloom Command\n"
            + "4 Wasteland\n"
            + "4 Cabal Therapy\n"
            + "4 Stinkweed Imp\n"
            + "4 Hollow One\n"
            + "1 Golgari Grave-Troll\n"
            + "4 Ichorid\n"
            + "4 Force of Vigor\n"
            + "1 Black Lotus\n"
            + "1 Demonic Consultation\n"
            + "1 Bayou\n"
            + "1 Urborg, Tomb of Yawgmoth\n"
            + "1 Yavimaya, Cradle of Growth\n"
            + "1 Demonic Tutor\n"
            + "2 Reanimate\n";

    static final String DOOMSDAY = "2 Flooded Strand\n"
            + "3 Misty Rainforest\n"
            + "2 Scalding Tarn\n"
            + "4 Underground Sea\n"
            + "2 Polluted Delta\n"
            + "4 Street Wraith\n"
            + "2 Thassa's Oracle\n"
            + "1 Ancestral Recall\n"
            + "1 Black Lotus\n"
            + "1 Brainstorm\n"
            + "4 Dark Ritual\n"
            + "2 Daze\n"
            + "1 Demonic Consultation\n"
            + "1 Demonic Tutor\n"
            + "4 Doomsday\n"
            + "3 Flusterstorm\n"
            + "4 Force of Will\n"
            + "1 Gitaxian Probe\n"
            + "1 Gush\n"
            + "1 Mental Misstep\n"
            + "1 Mox Jet\n"
            + "1 Mox Sapphire\n"
            + "1 Mystical Tutor\n"
            + "4 Ponder\n"
            + "1 Preordain\n"
            + "1 Time Walk\n"
            + "1 Vampiric Tutor\n"
            + "1 Dig Through Time\n"
            + "1 Dress Down\n"
            + "2 Force of Negation\n"
            + "1 Undercity Sewers\n"
            + "1 Necropotence\n";

    private Player deal(final String list) {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        for (final String line : list.split("\n")) {
            final int space = line.indexOf(' ');
            if (space < 0) {
                continue;
            }
            for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                addCardToZone(line.substring(space + 1).trim(), p, ZoneType.Library);
            }
        }
        return p;
    }

    private static ComboPlan plan(final Player p) {
        return DeckPlan.forSetting("auto", p).combos;
    }

    /** The deck is read as the combo; the message says which deck, what the scan found and which detector to look at. */
    private void expect(final String deck, final String list, final ComboPlan.Kind kind, final String detector) {
        final ComboPlan combos = plan(deal(list));
        AssertJUnit.assertTrue(deck + " should be read as " + kind + " but the scan found: " + combos.summary()
                + " (look at " + detector + " in ComboPlan: its thresholds, and the roles in ai-card-roles.txt)",
                combos.has(kind));
    }

    private void expectKeyCards(final String deck, final String list, final String... names) {
        final Player p = deal(list);
        final ComboPlan combos = plan(p);
        for (final String name : names) {
            final Card c = addCardToZone(name, p, ZoneType.Hand);
            AssertJUnit.assertTrue(deck + ": " + name + " is not a piece of any combo found: " + combos.summary(),
                    combos.isKeyCard(c));
        }
    }

    @Test
    public void jewelShopsIsAnArtifactEngineDeck() {
        expect("Jewel Shops", JEWEL_SHOPS, ComboPlan.Kind.ARTIFACT_ENGINE, "artifactEngine");
    }

    @Test
    public void rakerShopsIsAnArtifactEngineDeck() {
        expect("Raker Shops", RAKER_SHOPS, ComboPlan.Kind.ARTIFACT_ENGINE, "artifactEngine");
    }

    @Test
    public void sphereShopsIsAnArtifactEngineDeck() {
        expect("Sphere Shops", SPHERE_SHOPS, ComboPlan.Kind.ARTIFACT_ENGINE, "artifactEngine");
    }

    @Test
    public void rakerShopsKeepsTheVaultAndItsKeys() {
        expectKeyCards("Raker Shops", RAKER_SHOPS, "Time Vault", "Manifold Key", "Mystic Forge");
    }

    @Test
    public void dredgeIsADredgeDeck() {
        expect("Dredge", DREDGE, ComboPlan.Kind.DREDGE, "dredge");
    }

    @Test
    public void beseechStormIsATutorStormDeck() {
        expect("Beseech Storm", BESEECH_STORM_DECK, ComboPlan.Kind.TUTOR_STORM, "tutorStorm");
    }

    @Test
    public void beseechStormKeepsItsFinishers() {
        expectKeyCards("Beseech Storm", BESEECH_STORM_DECK, "Tendrils of Agony", "Yawgmoth's Will");
    }

    /** The decks the scan does not read as a combo yet must at least not be read as a wrong one. */
    @Test
    public void doomsdayAndScamAreNotMisreadAsALibraryCutOrAMassReanimation() {
        for (final String[] deck : new String[][] { { "Doomsday", DOOMSDAY }, { "Scam", SCAM } }) {
            final ComboPlan combos = plan(deal(deck[1]));
            AssertJUnit.assertFalse(deck[0] + " is read as a library cut: " + combos.summary(),
                    combos.has(ComboPlan.Kind.LIBRARY_CUT));
            AssertJUnit.assertFalse(deck[0] + " is read as a mass reanimation: " + combos.summary(),
                    combos.has(ComboPlan.Kind.MASS_REANIMATION));
        }
    }
}
