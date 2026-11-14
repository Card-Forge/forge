package forge.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboAssembly;
import forge.ai.simulation.ComboCatalog;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * The two card combos of commander-bracket-combos.txt and ai-commander-combos.txt, put together by ComboAssembly like the
 * lines of ai-combos.txt: the AI holds both permanents and the mana, and has to cast them both in one turn. There are a
 * couple of thousand pairs and a game each takes seconds, so a run takes every {@code flatmc.pairs.stride}th pair (50 by
 * default, 1 for all of them: {@code -Dflatmc.pairs.stride=1}). A pair with a spell, a land, an Aura or a cost above 12
 * is not a scenario for this test (the assembly leaves spells to the AI's own play) and is skipped.
 */
public class FlatMonteCarloCommanderPairTest extends AITest {
    private static final int MAX_COST = 12;

    /**
     * Cards the AI is right not to cast on their own, so a pair with one of them is not a scenario: a clone or a card
     * that copies has nothing to copy, a creature that returns a creature of its owner (or itself) when it enters, an
     * enchantment or an artifact that does nothing alone or hurts its owner (Transcendence, Mind Over Matter, the
     * Liches, Humility, Realm Razer, Day of the Dragons ...), a card with a cast restriction (Rakdos, Lord of Riots) and
     * an aura-like spell that needs a target in a graveyard (Necromancy).
     */
    private static final Set<String> HELD_BY_DESIGN = new HashSet<>(Arrays.asList(
            "Auton Soldier", "Machine God's Effigy", "Vesuvan Shapeshifter", "Progenitor Mimic", "Spark Double",
            "Flesh Duplicate", "Clever Impersonator", "Omni-Changeling", "Unstable Shapeshifter", "Vesuvan Doppelganger",
            "Mirrorhall Mimic", "Sakashima's Protege", "Vizier of Many Faces", "Mercurial Pretender", "Moritte of the Frost",
            "Mirror-Mad Phantasm", "Astral Dragon", "Minion Reflector", "Flameshadow Conjuring", "Day of the Dragons",
            "Shrieking Drake", "Cavern Harpy", "Fleetfoot Panther", "Horned Kavu", "Lava Zombie", "Silver Drake",
            "Ancestral Statue", "Acererak the Archlich", "Transcendence", "Mind Over Matter", "Lich", "Nefarious Lich",
            "Lich's Mastery", "Humility", "Forsaken Wastes", "Sulfuric Vortex", "Ethereal Absolution", "Rain of Gore",
            "Intruder Alarm", "Marina Vendrell's Grimoire", "Mindslaver", "Whip of Erebos", "Necromancy", "Elesh Norn, Grand Cenobite",
            "Massacre Wurm", "Rakdos, Lord of Riots", "Worldgorger Dragon", "Realm Razer", "Volcano Hellion", "Zedruu the Greathearted",
            "Aluren", "Stormfront Riders",
            // the AI's own logic says CantPlayAi for these even with the partner in play (a landfall or constellation
            // creature waits for what it feeds on, a card marked AI:RemoveDeck:All is not played by the heuristics)
            "Sporemound", "Doomwake Giant", "Ethereal Champion"));

    private Game game;
    private Player ai;

    @DataProvider(name = "pairs")
    public Object[][] pairs() {
        final int stride = Math.max(1, Integer.getInteger("flatmc.pairs.stride", 50));
        final List<Object[]> rows = new ArrayList<>();
        int i = 0;
        for (final ComboCatalog.Entry e : ComboCatalog.commanderEntries()) {
            if (!ComboAssembly.isCovered(e) && i++ % stride == 0) {
                rows.add(new Object[] { e.name });
            }
        }
        return rows.toArray(new Object[0][]);
    }

    private ComboCatalog.Entry entry(final String name) {
        for (final ComboCatalog.Entry e : ComboCatalog.commanderEntries()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        throw new IllegalStateException("no pair named " + name);
    }

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
        }
        for (final String basic : new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest" }) {
            addCards(basic, 5, ai);
        }
    }

    private void runTurn() {
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 800 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
    }

    /** Whether the pair is a scenario: two different permanents of the card database that a turn's mana can pay. */
    private boolean isScenario(final ComboCatalog.Entry e) {
        int cost = 0;
        for (final String spec : e.slotSpecs()) {
            final forge.item.IPaperCard paper = FModel.getMagicDb().getCommonCards().getCard(spec);
            if (paper == null) {
                return false;
            }
            final forge.card.CardRules rules = paper.getRules();
            final forge.card.CardType type = rules.getType();
            if (!type.isPermanent() || type.isLand() || type.isAura()) {
                return false;
            }
            cost += rules.getManaCost().getCMC();
        }
        return cost <= MAX_COST;
    }

    @Test(dataProvider = "pairs", timeOut = 180000)
    public void thePairIsPutTogetherWhenBothCardsAreInHand(final String name) {
        setUp();
        final ComboCatalog.Entry e = entry(name);
        if (!isScenario(e)) {
            return;
        }
        final List<String> pieces = new ArrayList<>(e.slotSpecs());
        for (final String piece : pieces) {
            if (HELD_BY_DESIGN.contains(piece)) {
                return;
            }
        }
        for (final String piece : pieces) {
            addCardToZone(piece, ai, ZoneType.Hand);
        }
        // the AI's view of each piece before the turn is played (after it the phase is over and every verdict is a timing one)
        final java.util.Map<String, String> verdictOf = new java.util.HashMap<>();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            final SpellAbility sa = c.getFirstSpellAbility();
            if (sa != null && !c.isLand()) {
                sa.setActivatingPlayer(ai);
                verdictOf.put(c.getName(), "AI says " + ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa)
                        + ", guard: " + forge.ai.simulation.ComboGuard.problem(ai, sa, null,
                                forge.ai.simulation.DeckPlan.forSetting("auto", ai).combos));
            }
        }
        runTurn();
        // a piece that is no longer in the hand was cast (or played, or used: a Room and a transforming card change their
        // names on the table, a Timestream Navigator goes to the library), so the hand is what counts
        final List<String> left = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (pieces.contains(c.getName())) {
                left.add(c.getName());
            }
        }
        final StringBuilder verdicts = new StringBuilder();
        for (final String name2 : left) {
            verdicts.append("\n  ").append(name2).append(": ").append(verdictOf.getOrDefault(name2, "no spell ability"));
        }
        AssertJUnit.assertTrue(e + ": " + left + " not cast, with both cards in hand and all the mana (the first one not"
                + " cast is the one to look at: its AI logic, or ComboGuard)" + verdicts + "\n" + gameStateToString(game),
                left.isEmpty());
    }

    private List<SpellAbility> inHand(final String... names) {
        final List<SpellAbility> sas = new ArrayList<>();
        for (final String name : names) {
            final Card c = addCardToZone(name, ai, ZoneType.Hand);
            c.getFirstSpellAbility().setActivatingPlayer(ai);
            sas.add(c.getFirstSpellAbility());
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        return sas;
    }

    @Test
    public void aPairOfTheAiCommanderListIsAssembled() {
        setUp();
        final SpellAbility next = ComboAssembly.next(ai, inHand("Devoted Druid", "Melira, Sylvok Outcast"), s -> true);
        AssertJUnit.assertNotNull("Devoted Druid + Melira in hand with the mana: cast one", next);
    }

    @Test
    public void aPairWithASpellIsLeftToTheAisOwnPlay() {
        setUp();
        AssertJUnit.assertNull("Peer into the Abyss is a sorcery with a target and a time of its own",
                ComboAssembly.next(ai, inHand("Peer into the Abyss", "Bloodletter of Aclazotz"), s -> true));
    }

    @Test
    public void thePairsAreRead() {
        AssertJUnit.assertTrue("the Commander lists have pairs: " + ComboCatalog.commanderEntries().size(),
                ComboCatalog.commanderEntries().size() > 1000);
        for (final ComboCatalog.Entry e : ComboCatalog.commanderEntries()) {
            AssertJUnit.assertEquals(e + " has two slots", 2, e.slotCount());
            AssertJUnit.assertTrue(e + " is numbered above the lines of ai-combos.txt", e.number >= ComboCatalog.PAIR_NUMBER_BASE);
        }
    }
}
