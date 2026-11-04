package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.AiCardLists;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Vintage metagame: the main decks (written out here) are read as the combo they are built around or, for the decks that
 * are not combo decks, are not read as a wrong one; the combo decks (written out here) are read as the combo they are built around, and the cards the
 * heuristics drop (AI:RemoveDeck:All) are still plays for the rollouts.
 */
public class FlatMonteCarloVintageMetaTest extends AITest {
    private static final String OATH = "4 Oath of Druids\n4 Show and Tell\n4 Atraxa, Grand Unifier\n4 Force of Will\n"
            + "4 Brainstorm\n4 Ponder\n4 Misty Rainforest\n4 Polluted Delta\n4 Island\n4 Forest\n4 Swamp\n4 Counterspell\n"
            + "4 Mana Drain\n4 Gitaxian Probe\n";

    private static final String BESEECH_STORM = "3 Beseech the Mirror\n1 Tendrils of Agony\n1 Yawgmoth's Will\n"
            + "1 Bolas's Citadel\n4 Dark Ritual\n4 Lotus Petal\n4 Mox Opal\n4 Vampiric Tutor\n4 Demonic Tutor\n"
            + "4 Gitaxian Probe\n4 Brainstorm\n4 Ponder\n4 Swamp\n4 Island\n4 Underground Sea\n4 Mana Crypt\n";

    private static final String VAULT_KEY = "4 Goblin Engineer\n4 Goblin Welder\n1 Time Vault\n1 Manifold Key\n"
            + "4 Stock Up\n4 Void Mirror\n4 Sewer-veillance Cam\n1 Sol Ring\n1 Mana Crypt\n1 Black Lotus\n"
            + "4 Scalding Tarn\n2 Tundra\n1 Volcanic Island\n1 Tolarian Academy\n1 Time Walk\n1 Ancestral Recall\n";

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

    @Test
    public void oathDeckIsReadAsTheCheatDeck() {
        final ComboPlan combos = plan(deal(OATH));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.CHEAT_FATTY));
    }

    @Test
    public void oathOfDruidsIsAnEngineAndTimeVaultAPayoff() {
        AssertJUnit.assertTrue(AiCardLists.has(AiCardLists.Role.COMBO_ENGINE, "Oath of Druids"));
        AssertJUnit.assertTrue(AiCardLists.has(AiCardLists.Role.ARTIFACT_PAYOFF, "Time Vault"));
    }

    /**
     * The AI:RemoveDeck:All cards of the metagame stay marked (a card that loses the mark is no longer a play of the
     * rollouts only), and the cantrips and tutors among them are offered to the rollouts.
     */
    @Test
    public void theRemoveDeckCardsAreMarkedAndTheCantripsAreOffered() {
        final Player p = deal(BESEECH_STORM);
        for (final String name : new String[] { "Gitaxian Probe", "Vampiric Tutor", "Mishra's Bauble", "Urza's Bauble",
                "Demonic Consultation", "Merchant Scroll", "Prismatic Ending", "Mindbreak Trap", "Manifold Key",
                "Lion's Eye Diamond", "Doomsday", "Goblin Welder", "Commandeer", "Flash", "Nethergoyf" }) {
            AssertJUnit.assertTrue(name, ComputerUtilCard.isCardRemAIDeck(addCardToZone(name, p, ZoneType.Hand)));
        }
        for (final String name : new String[] { "Gitaxian Probe", "Vampiric Tutor", "Mishra's Bauble", "Urza's Bauble",
                "Demonic Consultation" }) {
            final Card c = addCardToZone(name, p, ZoneType.Hand);
            boolean offered = false;
            for (final SpellAbility sa : c.getSpellAbilities()) {
                sa.setActivatingPlayer(p);
                offered |= FlatMonteCarloPicker.isDiscoverable(sa);
            }
            AssertJUnit.assertTrue(name, offered);
        }
    }

    /** Time Vault, Tinker, the free mana of the shops and the like are AI:RemoveDeck:Random: dropped from AI built decks only. */
    @Test
    public void theRandomRemoveDeckCardsAreOnlyDroppedFromBuiltDecks() {
        final Player p = deal(OATH);
        for (final String name : new String[] { "Time Vault", "Tinker", "Mana Crypt", "Mana Vault", "Demonic Tutor",
                "Tolarian Academy", "Mishra's Workshop", "Karn, the Great Creator" }) {
            final Card c = addCardToZone(name, p, ZoneType.Hand);
            AssertJUnit.assertTrue(name, ComputerUtilCard.isCardRemRandomDeck(c));
            AssertJUnit.assertFalse(name, ComputerUtilCard.isCardRemAIDeck(c));
        }
    }

    @Test
    public void lurrusVaultKeyIsReadAsAPair() {
        final ComboPlan combos = plan(deal(VAULT_KEY));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.PAIR));
    }

    @Test
    public void vaultDecksWithoutTheGoblinsAreNotAPair() {
        final ComboPlan combos = plan(deal(BESEECH_STORM + "1 Time Vault\n1 Manifold Key\n"));
        AssertJUnit.assertFalse(combos.summary(), combos.has(ComboPlan.Kind.PAIR));
    }

    /**
     * Decks built for the AI keep the AI:RemoveDeck:All cards Flat MC plays (free cantrips, the combo pieces) and still
     * leave out the others; with Flat MC off nothing is kept.
     */
    @Test
    public void aiDecksKeepTheRemoveDeckCardsFlatMcPlays() {
        final Player p = deal(OATH);
        final String[] kept = { "Gitaxian Probe", "Mishra's Bauble", "Urza's Bauble", "Vampiric Tutor",
                "Demonic Consultation", "Merchant Scroll", "Manifold Key", "Goblin Welder", "Doomsday" };
        final String[] left = { "Prismatic Ending", "Mindbreak Trap", "Commandeer", "Flash", "Nethergoyf" };
        try {
            forge.card.CardAiHints.setKeptForFlatMc(AiCardLists::isFlatMcPlayable);
            for (final String name : kept) {
                AssertJUnit.assertTrue(name + " should be kept in AI decks while Flat MC is on",
                        forge.card.CardRulesPredicates.IS_KEPT_IN_AI_DECKS.test(addCardToZone(name, p, ZoneType.Hand).getRules()));
            }
            for (final String name : left) {
                AssertJUnit.assertFalse(name + " is not played by Flat MC and stays out of AI decks",
                        forge.card.CardRulesPredicates.IS_KEPT_IN_AI_DECKS.test(addCardToZone(name, p, ZoneType.Hand).getRules()));
            }
            // AI:RemoveDeck:Random: the fast mana, tutors and combo pieces Flat MC plays are kept, the prison pieces are not
            for (final String name : new String[] { "Mana Crypt", "Mox Opal", "Time Vault", "Demonic Tutor", "Tinker" }) {
                AssertJUnit.assertTrue(name + " should be kept in random decks while Flat MC is on",
                        forge.card.CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS.test(addCardToZone(name, p, ZoneType.Hand).getRules()));
            }
            for (final String name : new String[] { "Chalice of the Void", "Null Rod", "Sphere of Resistance" }) {
                AssertJUnit.assertFalse(name + " is not played by Flat MC and stays out of random decks",
                        forge.card.CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS.test(addCardToZone(name, p, ZoneType.Hand).getRules()));
            }
            forge.card.CardAiHints.setKeptForFlatMc(null); // Flat MC off
            AssertJUnit.assertFalse("with Flat MC off nothing is kept", forge.card.CardRulesPredicates.IS_KEPT_IN_AI_DECKS
                    .test(addCardToZone("Gitaxian Probe", p, ZoneType.Hand).getRules()));
        } finally {
            forge.card.CardAiHints.setKeptForFlatMc(null);
        }
    }


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


    /** What the scan must not find in these decks. */
    private static final ComboPlan.Kind[] NOT_COMBOS = { ComboPlan.Kind.DOOMSDAY, ComboPlan.Kind.LIBRARY_OUT,
        ComboPlan.Kind.LIBRARY_CUT, ComboPlan.Kind.MASS_REANIMATION, ComboPlan.Kind.PAIR, ComboPlan.Kind.TUTOR_STORM,
        ComboPlan.Kind.STORM, ComboPlan.Kind.CHEAT_FATTY, ComboPlan.Kind.DREDGE };

    /** Kinds a deck may legitimately be read as, although it is on the list: Scam really is a graveyard deck. */
    private static final java.util.Map<String, ComboPlan.Kind> ALLOWED = java.util.Collections.singletonMap("Scam", ComboPlan.Kind.DREDGE);

    private static final String[][] NOT_COMBO_DECKS = {
        { "Dimir Lurrus Control", "1 Aether Spellbomb\n" + "1 Ancestral Recall\n" + "4 Bilbo, Thief in the Night\n" + "1 Black Lotus\n" + "1 Boomerang Basics\n" + "1 Brainstorm\n" + "1 Daze\n" + "1 Demonic Tutor\n" + "1 Dig Through Time\n" + "1 Dismember\n" + "3 Fatal Push\n" + "1 Flooded Strand\n" + "2 Flow State\n" + "4 Force of Negation\n" + "4 Force of Will\n" + "1 Gitaxian Probe\n" + "1 Gush\n" + "1 Island\n" + "1 Lórien Revealed\n" + "1 Mental Misstep\n" + "1 Mox Jet\n" + "1 Mox Sapphire\n" + "4 Polluted Delta\n" + "4 Psychic Frog\n" + "2 Spell Pierce\n" + "1 Strip Mine\n" + "1 Swamp\n" + "1 Thoughtseize\n" + "1 Time Walk\n" + "1 Treasure Cruise\n" + "1 Undercity Sewers\n" + "4 Underground Sea\n" + "1 Urza's Saga\n" + "1 Vexing Bauble\n" + "4 Wasteland\n" },
        { "Esper Lurrus Control", "4 Wasteland\n" + "1 Urza's Saga\n" + "3 Underground Sea\n" + "1 Meticulous Archive\n" + "1 Nihil Spellbomb\n" + "2 Tundra\n" + "1 Treasure Cruise\n" + "1 Time Walk\n" + "1 Thoughtseize\n" + "3 Swords to Plowshares\n" + "1 Strip Mine\n" + "2 Spell Pierce\n" + "1 Scalding Tarn\n" + "4 Psychic Frog\n" + "1 Polluted Delta\n" + "2 Orcish Bowmasters\n" + "1 Stony Silence\n" + "1 Mox Sapphire\n" + "1 Mox Pearl\n" + "1 Mox Jet\n" + "1 Misty Rainforest\n" + "1 Mental Misstep\n" + "3 Lórien Revealed\n" + "1 Karakas\n" + "1 Island\n" + "1 Gitaxian Probe\n" + "4 Force of Will\n" + "3 Force of Negation\n" + "1 Flusterstorm\n" + "4 Flow State\n" + "1 Demonic Tutor\n" + "1 Black Lotus\n" + "2 Bilbo, Thief in the Night\n" + "1 Ancestral Recall\n" + "1 Flooded Strand\n" + "1 Prismatic Ending\n" },
        { "Grixis Lurrus Control", "1 Ancestral Recall\n" + "1 Black Lotus\n" + "1 Demonic Consultation\n" + "1 Demonic Tutor\n" + "2 Flusterstorm\n" + "1 Sol Ring\n" + "4 Flow State\n" + "2 Force of Negation\n" + "1 Scalding Tarn\n" + "1 Mental Misstep\n" + "1 Mana Crypt\n" + "4 Lórien Revealed\n" + "2 Island\n" + "1 Gitaxian Probe\n" + "1 Misty Rainforest\n" + "1 Momentum Breaker\n" + "1 Mox Emerald\n" + "1 Mox Jet\n" + "1 Mox Pearl\n" + "1 Mox Ruby\n" + "1 Mox Sapphire\n" + "1 Polluted Delta\n" + "4 Force of Will\n" + "1 Snapcaster Mage\n" + "1 Flooded Strand\n" + "1 Step Through\n" + "4 Stock Up\n" + "1 Thassa's Oracle\n" + "3 Volcanic Island\n" + "1 Time Walk\n" + "1 Tolarian Academy\n" + "1 Undercity Sewers\n" + "2 Underground Sea\n" + "1 Urza's Saga\n" + "1 Vexing Bauble\n" + "3 Flame of Anor\n" + "4 Thundertrap Trainer\n" },
        { "Jeskai Lurrus", "3 Volcanic Island\n" + "1 Black Lotus\n" + "3 Tundra\n" + "3 Flame of Anor\n" + "1 Time Walk\n" + "3 Flow State\n" + "2 Swords to Plowshares\n" + "4 Stock Up\n" + "1 Soul-Guide Lantern\n" + "1 Sol Ring\n" + "1 Snapcaster Mage\n" + "1 Scalding Tarn\n" + "1 Pyroblast\n" + "1 Prismatic Ending\n" + "1 Polluted Delta\n" + "1 Pithing Needle\n" + "1 Mox Sapphire\n" + "1 Mox Ruby\n" + "1 Mox Pearl\n" + "1 Mox Jet\n" + "1 Mox Emerald\n" + "1 Misty Rainforest\n" + "1 Meticulous Archive\n" + "1 Mental Misstep\n" + "1 Mana Crypt\n" + "3 Lórien Revealed\n" + "1 Island\n" + "1 Gitaxian Probe\n" + "1 Forth Eorlingas!\n" + "4 Force of Will\n" + "1 Suplex\n" + "3 Force of Negation\n" + "4 Thundertrap Trainer\n" + "1 Flooded Strand\n" + "1 Tolarian Academy\n" + "1 Dress Down\n" + "1 Urza's Saga\n" + "1 Ancestral Recall\n" },
        { "Lurrus DRS", "2 Spell Pierce\n" + "1 Ancestral Recall\n" + "4 Avatar of Burgeoning Echoes\n" + "3 Polluted Delta\n" + "1 Boseiju, Who Endures\n" + "1 Carnivorous Cultivator\n" + "3 Collector Ouphe\n" + "3 Misty Rainforest\n" + "1 Mental Misstep\n" + "2 Fatal Push\n" + "3 Force of Negation\n" + "1 Gitaxian Probe\n" + "4 Force of Will\n" + "1 Hedge Maze\n" + "1 Marwyn, the Preserver\n" + "1 Demonic Tutor\n" + "1 Mindbreak Trap\n" + "1 Mox Emerald\n" + "1 Mox Jet\n" + "1 Mox Sapphire\n" + "4 Psychic Frog\n" + "4 Deathrite Shaman\n" + "1 Black Lotus\n" + "1 Time Walk\n" + "1 Treasure Cruise\n" + "1 Bayou\n" + "3 Underground Sea\n" + "1 Abrupt Decay\n" + "1 Strip Mine\n" + "2 Tropical Island\n" + "4 Wasteland\n" + "1 Daze\n" },
        { "Blue Tempo", "1 Magus of the Moon\n" + "1 Thundering Falls\n" + "1 Force of Will\n" + "1 Otawara, Soaring City\n" + "1 Prismatic Vista\n" + "1 Detective's Phoenix\n" + "1 Chain Lightning\n" + "1 Murktide Regent\n" + "1 Mockingbird\n" + "1 Reckless Charge\n" + "1 Arena of Glory\n" + "1 Scalding Tarn\n" + "1 Mental Misstep\n" + "1 Mountain\n" + "1 Force of Negation\n" + "2 Island\n" + "1 Gitaxian Probe\n" + "1 Polluted Delta\n" + "1 Wooded Foothills\n" + "1 Fear of Missing Out\n" + "1 Stock Up\n" + "1 Lórien Revealed\n" + "1 Vapor Snag\n" + "1 Violent Urge\n" + "1 Fireblast\n" + "1 Steam Vents\n" + "1 Fiery Islet\n" + "1 Dreadhorde Arcanist\n" + "1 Bloodstained Mire\n" + "1 Flooded Strand\n" + "1 Expressive Iteration\n" + "1 Dragon's Rage Channeler\n" + "1 Volcanic Island\n" + "1 Arid Mesa\n" + "1 Stormchaser's Talent\n" + "1 Flashback\n" + "1 Preordain\n" + "1 Misty Rainforest\n" + "1 Lightning Bolt\n" + "1 Ponder\n" + "1 Mutagenic Growth\n" + "1 Pompous Battlemage\n" + "1 Flow State\n" + "1 Delver of Secrets\n" + "1 Slickshot Show-Off\n" + "1 Thought Scour\n" + "1 Triton Wavebreaker\n" + "1 Ledger Shredder\n" + "1 Gush\n" + "1 Mishra's Bauble\n" + "1 Price of Progress\n" + "1 Sprite Dragon\n" + "1 Brainstorm\n" + "1 Cori-Steel Cutter\n" + "1 Daze\n" + "1 Sink into Stupor\n" + "1 Unholy Heat\n" + "1 Lava Dart\n" + "1 Urza's Bauble\n" },
        { "RUG Control", "4 Wasteland\n" + "3 Volcanic Island\n" + "3 Tropical Island\n" + "1 Time Walk\n" + "4 Avatar of Burgeoning Echoes\n" + "1 Boseiju, Who Endures\n" + "3 Force of Negation\n" + "1 Strip Mine\n" + "2 Null Rod\n" + "1 Gitaxian Probe\n" + "1 Mental Misstep\n" + "2 Mindbreak Trap\n" + "1 Minsc & Boo, Timeless Heroes\n" + "4 Misty Rainforest\n" + "1 Mox Sapphire\n" + "1 Mox Ruby\n" + "1 Mox Emerald\n" + "1 Ragavan, Nimble Pilferer\n" + "2 Spell Pierce\n" + "3 Scalding Tarn\n" + "4 Force of Will\n" + "1 Hedge Maze\n" + "2 Dack Fayden\n" + "1 Brainstorm\n" + "1 Black Lotus\n" + "1 Ancestral Recall\n" + "3 Wrenn and Six\n" + "3 Questing Druid\n" + "1 Uro, Titan of Nature's Wrath\n" + "1 Dig Through Time\n" + "1 Treasure Cruise\n" + "1 Lightning Bolt\n" },
        { "4c Psychic Frog", "1 Ancestral Recall\n" + "2 Bilbo, Thief in the Night\n" + "1 Black Lotus\n" + "1 Brainstorm\n" + "3 Flow State\n" + "3 Underground Sea\n" + "3 Force of Negation\n" + "3 Lórien Revealed\n" + "1 Gitaxian Probe\n" + "1 Island\n" + "2 Wasteland\n" + "2 Polluted Delta\n" + "1 Mental Misstep\n" + "4 Psychic Frog\n" + "1 Mox Jet\n" + "1 Mox Pearl\n" + "1 Mox Sapphire\n" + "1 Mox Ruby\n" + "1 Null Rod\n" + "2 Flooded Strand\n" + "1 Prismatic Ending\n" + "1 Soul-Guide Lantern\n" + "3 Swords to Plowshares\n" + "1 Spell Pierce\n" + "1 Strip Mine\n" + "2 Volcanic Island\n" + "2 Hexing Squelcher\n" + "1 Time Walk\n" + "1 Treasure Cruise\n" + "3 Tundra\n" + "1 Demonic Tutor\n" + "1 Urza's Saga\n" + "1 Vexing Bauble\n" + "2 Scalding Tarn\n" + "4 Force of Will\n" },
        { "Dimir Psychic Frog", "4 Wasteland\n" + "1 Urza's Saga\n" + "4 Underground Sea\n" + "1 Undercity Sewers\n" + "1 Time Walk\n" + "1 Flusterstorm\n" + "2 Nihil Spellbomb\n" + "1 Strip Mine\n" + "2 Spell Pierce\n" + "4 Psychic Frog\n" + "4 Polluted Delta\n" + "1 Mox Sapphire\n" + "1 Mox Pearl\n" + "1 Mox Jet\n" + "1 Mental Misstep\n" + "4 Lórien Revealed\n" + "1 Swamp\n" + "2 Island\n" + "4 Force of Will\n" + "3 Force of Negation\n" + "4 Flow State\n" + "3 Fatal Push\n" + "1 Demonic Tutor\n" + "1 Black Lotus\n" + "2 Bilbo, Thief in the Night\n" + "1 Ancestral Recall\n" + "1 Gitaxian Probe\n" + "1 Mishra's Bauble\n" + "3 Murktide Regent\n" },
        { "Sultai Psychic Frog", "1 Ancestral Recall\n" + "1 Bilbo, Thief in the Night\n" + "1 Black Lotus\n" + "1 Brainstorm\n" + "1 Demonic Tutor\n" + "3 Polluted Delta\n" + "3 Flow State\n" + "3 Force of Negation\n" + "3 Lórien Revealed\n" + "1 Gitaxian Probe\n" + "2 Misty Rainforest\n" + "3 Collector Ouphe\n" + "1 Mental Misstep\n" + "1 Mox Jet\n" + "1 Mox Emerald\n" + "1 Mox Sapphire\n" + "1 Nihil Spellbomb\n" + "1 Island\n" + "4 Force of Will\n" + "2 Fatal Push\n" + "3 Tropical Island\n" + "2 Abrupt Decay\n" + "1 Strip Mine\n" + "1 Time Walk\n" + "3 Underground Sea\n" + "1 Urza's Saga\n" + "4 Wasteland\n" + "4 Avatar of Burgeoning Echoes\n" + "1 Life from the Loam\n" + "1 Bayou\n" + "4 Psychic Frog\n" },
        { "Abzan Initiative", "1 Adeline, Resplendent Cathar\n" + "1 Ajani, Nacatl Pariah\n" + "1 Arid Mesa\n" + "1 Black Widow, Super Spy\n" + "1 Bleachbone Verge\n" + "1 Bloodstained Mire\n" + "1 Cecil, Dark Knight\n" + "1 Chrome Mox\n" + "1 Clarion Conqueror\n" + "1 Concealed Courtyard\n" + "1 Dauthi Voidwalker\n" + "1 Deathrite Shaman\n" + "1 Eiganjo, Seat of the Empire\n" + "1 Fatal Push\n" + "1 Flooded Strand\n" + "1 Giver of Runes\n" + "1 Godless Shrine\n" + "1 Gollum, Riddle Master\n" + "1 Grief\n" + "1 Guide of Souls\n" + "1 Inquisition of Kozilek\n" + "1 Jennifer Walters\n" + "1 Karakas\n" + "1 Lotus Petal\n" + "1 Mai, Scornful Striker\n" + "1 Marsh Flats\n" + "1 Mishra's Bauble\n" + "1 Moonshadow\n" + "1 Mother of Runes\n" + "1 Ocelot Pride\n" + "1 Orcish Bowmasters\n" + "1 Phelia, Exuberant Shepherd\n" + "1 Plains\n" + "1 Polluted Delta\n" + "1 Prismatic Ending\n" + "1 Sage of the Skies\n" + "1 Scrubland\n" + "1 Seasoned Dungeoneer\n" + "1 Shadowy Backstreet\n" + "1 Silent Clearing\n" + "1 Solitude\n" + "1 Spectacular Spider-Man\n" + "1 Super Shredder\n" + "2 Swamp\n" + "1 Swords to Plowshares\n" + "1 Syr Vondam, Sunstar Exemplar\n" + "1 Takenuma, Abandoned Mire\n" + "1 Temple Garden\n" + "1 Thalia, Guardian of Thraben\n" + "1 Thalia, Heretic Cathar\n" + "1 The Queen of Dale\n" + "1 Thoughtseize\n" + "1 Tokka & Rahzar, Terrible Twos\n" + "1 Unearth\n" + "1 Vanishing Verse\n" + "1 Verdant Catacombs\n" + "1 Voice of Victory\n" + "1 White Plume Adventurer\n" + "1 Windswept Heath\n" },
        { "Mono-White Initiative", "2 Plains\n" + "4 Anointed Peacekeeper\n" + "4 Archon of Emeria\n" + "1 Black Lotus\n" + "1 Mana Crypt\n" + "4 Chancellor of the Annex\n" + "1 Karakas\n" + "4 Clarion Conqueror\n" + "4 Chrome Mox\n" + "4 Cavern of Souls\n" + "1 Mox Emerald\n" + "1 Mox Jet\n" + "1 Mox Pearl\n" + "1 Mox Ruby\n" + "1 Mox Sapphire\n" + "4 Ancient Tomb\n" + "4 Seasoned Dungeoneer\n" + "1 Strip Mine\n" + "1 Vexing Bauble\n" + "4 Void Mirror\n" + "4 Wasteland\n" + "4 White Plume Adventurer\n" + "4 Witch Enchanter\n" },
        { "Red Prison", "1 Magus of the Moon\n" + "1 Sundering Eruption\n" + "1 Broadside Bombardiers\n" + "1 Thoughtseize\n" + "1 Prismatic Vista\n" + "1 Chain Lightning\n" + "1 Dismember\n" + "1 Unearth\n" + "1 Arena of Glory\n" + "1 Blood Moon\n" + "1 Bayou\n" + "1 Gut, True Soul Zealot\n" + "1 Molten Collapse\n" + "1 Black Widow, Super Spy\n" + "1 Chrome Mox\n" + "1 Polluted Delta\n" + "1 Verdant Catacombs\n" + "1 Moonshadow\n" + "1 Wooded Foothills\n" + "1 Super Shredder\n" + "1 Nethergoyf\n" + "1 Emperor of Bones\n" + "1 Fear of Missing Out\n" + "1 Sunset Saboteur\n" + "1 Screaming Nemesis\n" + "1 Deathrite Shaman\n" + "1 Marsh Flats\n" + "1 Bloodstained Mire\n" + "1 Inti, Seneschal of the Sun\n" + "1 Pyrogoyf\n" + "4 Swamp\n" + "1 Dragon's Rage Channeler\n" + "1 Dark Confidant\n" + "1 Taiga\n" + "1 Badlands\n" + "1 Dauthi Voidwalker\n" + "1 Cecil, Dark Knight\n" + "1 Deadpool, Trading Card\n" + "1 Fatal Push\n" + "1 Lotus Petal\n" + "1 Inquisition of Kozilek\n" + "1 Lightning Bolt\n" + "1 Zhao, the Moon Slayer\n" + "1 Orcish Bowmasters\n" + "2 Mountain\n" + "1 Barrowgoyf\n" + "1 Mishra's Bauble\n" + "1 Price of Progress\n" + "1 Mai, Scornful Striker\n" + "1 Blazemire Verge\n" + "1 Raucous Theater\n" + "1 Unholy Heat\n" + "1 Boggart Trawler\n" + "1 Blood Crypt\n" + "1 Urza's Bauble\n" + "1 Stalactite Stalker\n" },
        { "Eldrazi", "1 Vexing Bauble\n" + "1 Black Lotus\n" + "2 Eye of Ugin\n" + "1 Chalice of the Void\n" + "4 Kozilek's Command\n" + "4 Glaring Fleshraker\n" + "1 Karn, the Great Creator\n" + "1 Mox Emerald\n" + "1 Mox Sapphire\n" + "4 Eldrazi Temple\n" + "4 Wasteland\n" + "1 Thorn of Amethyst\n" + "4 Sowing Mycospawn\n" + "4 Wastescape Battlemage\n" + "1 Yavimaya, Cradle of Growth\n" + "4 Cavern of Souls\n" + "4 Ancient Tomb\n" + "1 Strip Mine\n" + "1 Sol Ring\n" + "4 Eldrazi Linebreaker\n" + "1 Mox Ruby\n" + "3 Thought-Knot Seer\n" + "4 Null Rod\n" + "1 Urza's Saga\n" + "3 Secluded Courtyard\n" },
        { "Other Shops", "1 Ancestral Recall\n" + "4 Ancient Tomb\n" + "4 Arcbound Ravager\n" + "4 Big Mother Mouser\n" + "1 Black Lotus\n" + "4 Force of Negation\n" + "4 Force of Will\n" + "2 Island\n" + "1 Lodestone Golem\n" + "1 Mana Vault\n" + "1 Mechanized Production\n" + "4 Mishra's Workshop\n" + "1 Misty Rainforest\n" + "1 Mox Emerald\n" + "1 Mox Jet\n" + "1 Mox Pearl\n" + "1 Mox Ruby\n" + "1 Mox Sapphire\n" + "1 Nettlecyst\n" + "4 Phyrexian Metamorph\n" + "1 Polluted Delta\n" + "2 Sink into Stupor\n" + "1 Sol Ring\n" + "4 Sphere of Resistance\n" + "1 Spine of Ish Sah\n" + "1 Strip Mine\n" + "1 Thorn of Amethyst\n" + "1 Time Walk\n" + "1 Tolarian Academy\n" + "3 Ultron, Artificial Malevolence\n" + "1 Urza's Saga\n" + "1 Walking Ballista\n" + "1 Wasteland\n" },
        { "Fastbond Lands", "1 Abrupt Decay\n" + "1 Badgermole Cub\n" + "1 Baloth Prime\n" + "1 Bayou\n" + "1 Carnivorous Cultivator\n" + "1 Crop Rotation\n" + "1 Dark Depths\n" + "1 Deathrite Shaman\n" + "1 Dismember\n" + "1 Dryad Arbor\n" + "1 Elvish Reclaimer\n" + "1 Eumidian Hatchery\n" + "1 Expedition Map\n" + "1 Exploration\n" + "1 Fastbond\n" + "1 Forest\n" + "1 Gaea's Cradle\n" + "1 Glacial Chasm\n" + "1 Green Sun's Zenith\n" + "1 Grist, the Hunger Tide\n" + "1 Icetill Explorer\n" + "1 Karakas\n" + "1 Life from the Loam\n" + "1 Malevolent Rumble\n" + "1 Maze of Ith\n" + "1 Misty Rainforest\n" + "1 Mole Man, Moloid Master\n" + "1 Once Upon a Time\n" + "1 Opposition Agent\n" + "1 Overgrown Tomb\n" + "1 Polluted Delta\n" + "1 Sakura-Tribe Scout\n" + "1 Scythecat Cub\n" + "1 Shifting Woodland\n" + "1 Six\n" + "1 Snow-Covered Forest\n" + "1 Snow-Covered Swamp\n" + "1 Sowing Mycospawn\n" + "1 Springheart Nantuko\n" + "1 Strip Mine\n" + "1 Summer Bloom\n" + "1 Swamp\n" + "1 Sylvan Safekeeper\n" + "1 Sylvan Scrying\n" + "1 Talon Gates of Madara\n" + "1 The Tabernacle at Pendrell Vale\n" + "1 Thespian's Stage\n" + "1 Thoughtseize\n" + "1 Titania, Protector of Argoth\n" + "1 Underground Mortuary\n" + "1 Urborg, Tomb of Yawgmoth\n" + "1 Urza's Saga\n" + "1 Verdant Catacombs\n" + "1 Vibrance\n" + "1 Walk-In Closet\n" + "1 Wasteland\n" + "1 Wight of the Reliquary\n" + "1 Windswept Heath\n" + "1 Wooded Foothills\n" + "1 Yavimaya, Cradle of Growth\n" + "1 Zuran Orb\n" },
        { "Counter Vine", "4 Basking Rootwalla\n" + "4 Bazaar of Baghdad\n" + "4 Blazing Rootwalla\n" + "1 Chalice of the Void\n" + "4 Commandeer\n" + "4 Force of Negation\n" + "4 Force of Will\n" + "1 Ghost Quarter\n" + "1 Gitaxian Probe\n" + "4 Hollow One\n" + "4 Master of Death\n" + "1 Mental Misstep\n" + "3 Mindbreak Trap\n" + "4 Serum Powder\n" + "4 Squee, Goblin Nabob\n" + "1 Strip Mine\n" + "4 Subtlety\n" + "4 Vengevine\n" + "4 Wasteland\n" },
        { "Goblins", "1 Ancient Tomb\n" + "1 Arena of Glory\n" + "1 Arid Mesa\n" + "1 Badlands\n" + "1 Battle Cry Goblin\n" + "1 Bayou\n" + "1 Birds of Paradise\n" + "1 Blood Crypt\n" + "1 Bloodstained Mire\n" + "1 Boggart Cursecrafter\n" + "1 Boggart Harbinger\n" + "1 Boggart Mischief\n" + "1 Boggart Trawler\n" + "1 Breeches, Eager Pillager\n" + "1 Broadside Bombardiers\n" + "1 Cavern of Souls\n" + "1 Celestial Reunion\n" + "1 Commercial District\n" + "1 Conspicuous Snoop\n" + "1 Crystal Vein\n" + "1 Deathrite Shaman\n" + "1 General Kreat, the Boltbringer\n" + "1 Goblin Chirurgeon\n" + "1 Goblin Lackey\n" + "1 Goblin Matron\n" + "1 Goblin Recruiter\n" + "1 Goblin Ringleader\n" + "1 Grumgully, the Generous\n" + "1 Howlsquad Heavy\n" + "1 Ignoble Hierarch\n" + "1 Kiki-Jiki, Mirror Breaker\n" + "1 Marsh Flats\n" + "1 Metallic Mimic\n" + "1 Minsc & Boo, Timeless Heroes\n" + "2 Mountain\n" + "1 Mudbutton Cursetosser\n" + "1 Munitions Expert\n" + "1 Murderous Redcap\n" + "1 Muxus, Goblin Grandee\n" + "1 Once Upon a Time\n" + "1 Orcish Bowmasters\n" + "1 Pashalik Mons\n" + "1 Polluted Delta\n" + "1 Putrid Goblin\n" + "1 Raucous Theater\n" + "1 Realmwalker\n" + "1 Rundvelt Hordemaster\n" + "1 Scalding Tarn\n" + "1 Scuzzback Scrounger\n" + "1 Skirk Prospector\n" + "1 Sling-Gang Lieutenant\n" + "1 Stomping Ground\n" + "1 Taiga\n" + "1 Taster of Wares\n" + "1 The Great Goblin\n" + "1 Verdant Catacombs\n" + "1 Warren Instigator\n" + "1 Windswept Heath\n" + "1 Wooded Foothills\n" },
        { "Stiflenought", "1 Sol Ring\n" + "1 Mana Crypt\n" + "4 Ancient Tomb\n" + "4 Crucible of Worlds\n" + "4 Stonecoil Serpent\n" + "4 Wasteland\n" + "4 Eater of Days\n" + "4 Phyrexian Dreadnought\n" + "1 Urza's Saga\n" + "4 Patchwork Automaton\n" + "4 Mishra's Workshop\n" + "4 Phyrexian Revoker\n" + "1 Strip Mine\n" + "4 Torpor Orb\n" + "4 Scrawling Crawler\n" + "4 City of Traitors\n" + "4 Karn, Argent Defender\n" + "4 Null Rod\n" },
        { "Scam", "4 Bazaar of Baghdad\n" + "1 Mox Jet\n" + "4 Prized Amalgam\n" + "1 Strip Mine\n" + "4 Grief\n" + "4 Life from the Loam\n" + "4 Chrome Mox\n" + "1 Mox Emerald\n" + "4 Witherbloom Command\n" + "4 Wasteland\n" + "4 Cabal Therapy\n" + "4 Stinkweed Imp\n" + "4 Hollow One\n" + "1 Golgari Grave-Troll\n" + "4 Ichorid\n" + "4 Force of Vigor\n" + "1 Black Lotus\n" + "1 Demonic Consultation\n" + "1 Bayou\n" + "1 Urborg, Tomb of Yawgmoth\n" + "1 Yavimaya, Cradle of Growth\n" + "1 Demonic Tutor\n" + "2 Reanimate\n" }
    };


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

    @Test
    public void theNonComboDecksAreNotReadAsACombo() {
        final StringBuilder wrong = new StringBuilder();
        for (final String[] deck : NOT_COMBO_DECKS) {
            final ComboPlan combos = plan(deal(deck[1]));
            for (final ComboPlan.Kind kind : NOT_COMBOS) {
                if (combos.has(kind) && ALLOWED.get(deck[0]) != kind) {
                    wrong.append(deck[0]).append(" is read as ").append(kind).append(": ").append(combos.summary()).append('\n');
                }
            }
        }
        AssertJUnit.assertEquals("decks read as a combo they are not built around (see the detector of each kind and its roles):\n"
                + wrong, 0, wrong.length());
    }
}
