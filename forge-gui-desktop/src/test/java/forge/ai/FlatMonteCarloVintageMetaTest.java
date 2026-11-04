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
 * Vintage metagame: the combo decks (written out here) are read as the combo they are built around, and the cards the
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
                "Demonic Consultation", "Merchant Scroll", "Manifold Key", "Goblin Welder" };
        final String[] left = { "Doomsday", "Prismatic Ending", "Mindbreak Trap", "Commandeer", "Flash", "Nethergoyf" };
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
}
