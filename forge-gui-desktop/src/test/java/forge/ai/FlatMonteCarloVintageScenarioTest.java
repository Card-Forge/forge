package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ParadoxOutcome;
import forge.ai.simulation.VaultLoop;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** The infinite turn loop of the vintage artifact decks: Time Vault with Manifold Key, and the goblins that fetch them. */
public class FlatMonteCarloVintageScenarioTest extends AITest {

    private Game game;
    private Player ai;

    private void setUp(final PhaseType phase) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        addCards("Mountain", 3, ai);
        game.getPlayers().get(0).setLife(10, null); // the Forge loop needs a library that outlasts the life total
        game.getPhaseHandler().devModeSet(phase, ai);
        game.getAction().checkStateEffects(true);
    }

    private static List<SpellAbility> abilitiesOf(final Card... cards) {
        final List<SpellAbility> list = new ArrayList<>();
        for (final Card c : cards) {
            list.addAll(c.getSpellAbilities());
        }
        return list;
    }

    @Test
    public void anUntappedVaultIsTappedForAnExtraTurnInTheSecondMainPhase() {
        setUp(PhaseType.MAIN2);
        final Card vault = addCard("Time Vault", ai);
        vault.setTapped(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(vault));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(ApiType.AddTurn, sa.getApi());
    }

    @Test
    public void theVaultWaitsForTheSecondMainPhase() {
        setUp(PhaseType.MAIN1);
        final Card vault = addCard("Time Vault", ai);
        vault.setTapped(false);
        AssertJUnit.assertNull(VaultLoop.sureCast(ai, abilitiesOf(vault)));
    }

    @Test
    public void aNearlyEmptyLibraryStopsTheLoop() {
        setUp(PhaseType.MAIN2);
        while (ai.getCardsIn(ZoneType.Library).size() > 2) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        final Card vault = addCard("Time Vault", ai);
        vault.setTapped(false);
        AssertJUnit.assertNull(VaultLoop.sureCast(ai, abilitiesOf(vault)));
    }

    @Test
    public void aTappedVaultIsUntappedWithTheKey() {
        setUp(PhaseType.MAIN1);
        final Card vault = addCard("Time Vault", ai);
        vault.setTapped(true);
        final Card key = addCard("Manifold Key", ai);
        key.setTapped(false);
        key.setSickness(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(vault, key));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(ApiType.Untap, sa.getApi());
        AssertJUnit.assertTrue(sa.getTargets().contains(vault));
    }

    /** Every step the Welder choice goes through, so that a failure says which one it was. */
    private String whyNotWelding(final Card welder, final Card spare, final Card vault, final List<SpellAbility> candidates) {
        final StringBuilder sb = new StringBuilder();
        final PhaseHandler ph = game.getPhaseHandler();
        sb.append("phase=").append(ph.getPhase()).append(" aiTurn=").append(ph.isPlayerTurn(ai))
                .append(" stackEmpty=").append(game.getStack().isEmpty()).append('\n');
        sb.append("vaultInYard=").append(vault.isInZone(ZoneType.Graveyard)).append(" isVault=").append(VaultLoop.isVault(vault))
                .append(" welderOnBoard=").append(welder.isInZone(ZoneType.Battlefield))
                .append(" sick=").append(welder.isSick()).append(" tapped=").append(welder.isTapped()).append('\n');
        sb.append("candidates=").append(candidates.size()).append('\n');
        for (final SpellAbility sa : candidates) {
            sa.setActivatingPlayer(ai);
            sb.append("  ").append(sa.getHostCard()).append(" api=").append(sa.getApi()).append(" sub=")
                    .append(sa.getSubAbility() == null ? null : sa.getSubAbility().getApi()).append(" targets=")
                    .append(sa.usesTargeting()).append(" isAbility=").append(sa.isAbility()).append(" canPlay=")
                    .append(sa.canPlay()).append(" canPay=").append(ComputerUtilCost.canPayCost(sa, ai, false));
            if (sa.usesTargeting()) {
                sb.append(" canTargetSpare=").append(sa.canTarget(spare));
                if (sa.getSubAbility() != null && sa.getSubAbility().usesTargeting()) {
                    sa.resetTargets();
                    sa.getTargets().add(spare);
                    sb.append(" canTargetVaultAfterSpare=").append(sa.getSubAbility().canTarget(vault));
                    sa.resetTargets();
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @Test
    public void theWelderSwapsASpareArtifactForTheVaultInTheGraveyard() {
        setUp(PhaseType.MAIN1);
        final Card vault = addCardToZone("Time Vault", ai, ZoneType.Graveyard);
        final Card spare = addCard("Mishra's Bauble", ai);
        final Card welder = addCard("Goblin Welder", ai);
        welder.setSickness(false);
        final List<SpellAbility> candidates = abilitiesOf(welder, spare);
        final SpellAbility sa = VaultLoop.sureCast(ai, candidates);
        if (sa == null) { // the explanation resets the targets of the abilities it looks at: only built when it is needed
            AssertJUnit.fail(whyNotWelding(welder, spare, vault, candidates));
        }
        AssertJUnit.assertTrue("the spare artifact is the first target", sa.getTargets().contains(spare));
        AssertJUnit.assertEquals("Time Vault", sa.getSubAbility().getTargets().getFirstTargetedCard().getName());
    }

    @Test
    public void theEngineerSearchesForTheVaultFirstAndTheKeyNext() {
        setUp(PhaseType.MAIN1);
        final Card vault = addCardToZone("Time Vault", ai, ZoneType.Library);
        final Card key = addCardToZone("Manifold Key", ai, ZoneType.Library);
        final forge.game.card.CardCollection options = new forge.game.card.CardCollection();
        options.add(vault);
        options.add(key);
        AssertJUnit.assertEquals(vault, VaultLoop.pick(ai, options));
        addCardToZone("Time Vault", ai, ZoneType.Graveyard);
        AssertJUnit.assertEquals(key, VaultLoop.pick(ai, options));
    }

    @Test
    public void topIsDrawnWithTheForgeAndTheFleshrakerInPlay() {
        setUp(PhaseType.MAIN1);
        addCard("Mystic Forge", ai);
        addCard("Glaring Fleshraker", ai);
        final Card top = addCard("Sensei's Divining Top", ai);
        top.setTapped(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(top));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(top, sa.getHostCard());
        AssertJUnit.assertEquals(ApiType.Draw, sa.getApi());
    }

    @Test
    public void noForgeLoopWithoutTheFleshraker() {
        setUp(PhaseType.MAIN1);
        addCard("Mystic Forge", ai);
        final Card top = addCard("Sensei's Divining Top", ai);
        top.setTapped(false);
        AssertJUnit.assertNull(VaultLoop.sureCast(ai, abilitiesOf(top)));
    }

    @Test
    public void theCauldronExilesAGoblinFromTheGraveyard() {
        setUp(PhaseType.MAIN1);
        final Card cauldron = addCard("Agatha's Soul Cauldron", ai);
        cauldron.setTapped(false);
        addCard("Goblin Engineer", ai);
        final Card welder = addCardToZone("Goblin Welder", ai, ZoneType.Graveyard);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(cauldron));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertTrue(sa.getTargets().contains(welder));
    }

    // ------------------------------------------------------------------------------ the weak spots

    /**
     * The Forge half of the Raker loop: with Top on top of the library and Mystic Forge in play, the game must offer the
     * cast from the library as one of the AI's plays at all (checked through the same list the AI chooses from).
     */
    @Test
    public void theGameOffersTopFromTheLibraryWithTheForge() {
        setUp(PhaseType.MAIN1);
        addCard("Mystic Forge", ai);
        addCard("Glaring Fleshraker", ai);
        final Card top = addCardToZone("Sensei's Divining Top", ai, ZoneType.Library);
        ai.getZone(ZoneType.Library).remove(top);
        ai.getZone(ZoneType.Library).add(top, 0);
        game.getAction().checkStateEffects(true); // the Forge's permission follows the top card of the library
        boolean offered = false;
        final StringBuilder seen = new StringBuilder();
        for (final SpellAbility sa : ComputerUtilAbility.getSpellAbilities(ComputerUtilAbility.getAvailableCards(game, ai), ai)) {
            seen.append(sa.getHostCard()).append('[').append(sa.getApi()).append("] ");
            offered |= top.getName().equals(sa.getHostCard().getName()) && sa.isSpell()
                    && sa.getHostCard().isInZone(ZoneType.Library);
        }
        AssertJUnit.assertTrue("Top on top of the library is not among the plays: " + seen, offered);
        final SpellAbility sa = VaultLoop.sureCast(ai,
                ComputerUtilAbility.getSpellAbilities(ComputerUtilAbility.getAvailableCards(game, ai), ai));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertTrue(sa.isSpell());
    }

    /** A vault that is not tapped yet must not hold up the rest of the loop in the first main phase. */
    @Test
    public void anUntappedVaultDoesNotBlockTheOtherStepsInTheFirstMainPhase() {
        setUp(PhaseType.MAIN1);
        final Card vault = addCard("Time Vault", ai);
        vault.setTapped(false);
        addCard("Mystic Forge", ai);
        addCard("Glaring Fleshraker", ai);
        final Card top = addCard("Sensei's Divining Top", ai);
        top.setTapped(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(vault, top));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(top, sa.getHostCard());
    }

    /** The loop deals one damage per card drawn: with more life to take than the library holds it is not started. */
    @Test
    public void theForgeLoopIsOnlyStartedWhenTheLibraryOutlastsTheOpponent() {
        setUp(PhaseType.MAIN1);
        addCard("Mystic Forge", ai);
        addCard("Glaring Fleshraker", ai);
        final Card top = addCard("Sensei's Divining Top", ai);
        top.setTapped(false);
        AssertJUnit.assertNotNull("20 cards against 10 life", VaultLoop.sureCast(ai, abilitiesOf(top)));
        game.getPlayers().get(0).setLife(40, null);
        AssertJUnit.assertNull("20 cards against 40 life", VaultLoop.sureCast(ai, abilitiesOf(top)));
    }

    // ------------------------------------------------------------------------------ the One Ring, and the loop pieces

    @Test
    public void aTappedRingIsUntappedWithTheKey() {
        setUp(PhaseType.MAIN1);
        final Card ring = addCard("The One Ring", ai);
        ring.setTapped(true);
        final Card key = addCard("Manifold Key", ai);
        key.setTapped(false);
        key.setSickness(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(ring, key));
        AssertJUnit.assertNotNull("a tapped One Ring with an untapper should be untapped\n" + gameStateToString(game), sa);
        AssertJUnit.assertEquals(ApiType.Untap, sa.getApi());
        AssertJUnit.assertTrue("the ring is the target", sa.getTargets().contains(ring));
    }

    @Test
    public void theRingIsNotUntappedWithAFewCardsLeftOrALowLifeTotal() {
        setUp(PhaseType.MAIN1);
        final Card ring = addCard("The One Ring", ai);
        ring.setTapped(true);
        final Card key = addCard("Manifold Key", ai);
        key.setTapped(false);
        key.setSickness(false);
        ai.setLife(4, null);
        AssertJUnit.assertNull("4 life is too little for another burden counter", VaultLoop.sureCast(ai, abilitiesOf(ring, key)));
    }

    @Test
    public void theVaultComesBeforeTheRing() {
        setUp(PhaseType.MAIN1);
        final Card vault = addCard("Time Vault", ai);
        vault.setTapped(true);
        final Card ring = addCard("The One Ring", ai);
        ring.setTapped(true);
        final Card key = addCard("Manifold Key", ai);
        key.setTapped(false);
        key.setSickness(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(vault, ring, key));
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertTrue("the key untaps the vault first", sa.getTargets().contains(vault));
    }

    /** The Engineer's sacrifice is chosen by the game: while a piece of the loop is in play, the Engineer is not used. */
    @Test
    public void theEngineerIsNotUsedWhileAPieceCouldBeSacrificed() {
        setUp(PhaseType.MAIN1);
        addCardToZone("Time Vault", ai, ZoneType.Graveyard);
        final Card key = addCard("Manifold Key", ai);
        key.setTapped(true);
        final Card spare = addCard("Mishra's Bauble", ai);
        final Card engineer = addCard("Goblin Engineer", ai);
        engineer.setSickness(false);
        final SpellAbility sa = VaultLoop.sureCast(ai, abilitiesOf(engineer, spare));
        AssertJUnit.assertTrue("the Engineer sacrifices what the game picks, possibly the key: "
                + (sa == null ? null : sa.getHostCard()), sa == null || !"Goblin Engineer".equals(sa.getHostCard().getName()));
    }

    // ------------------------------------------------------------------------------ Beseech Storm

    /** The storm finisher is cast when it is lethal: Tendrils of Agony with enough spells before it. */
    @Test(timeOut = 60000)
    public void tendrilsIsCastWhenTheStormIsLethal() {
        setUp(PhaseType.MAIN1);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(8, null);
        // the real Beseech Storm list is the library: without it the deck is not read as a storm deck and no plan casts the finisher
        for (final Card c : new ArrayList<>(ai.getCardsIn(ZoneType.Library))) {
            ai.getZone(ZoneType.Library).remove(c);
        }
        for (final String line : FlatMonteCarloVintageMetaTest.BESEECH_STORM_DECK.split("\n")) {
            final int space = line.indexOf(' ');
            if (space > 0) {
                for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                    addCardToZone(line.substring(space + 1).trim(), ai, ZoneType.Library);
                }
            }
        }
        addCards("Swamp", 4, ai);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        }
        addCardToZone("Tendrils of Agony", ai, ZoneType.Hand);
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 600 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertTrue("Tendrils with three rituals before it is lethal against 8 life, the opponent has "
                + opp.getLife() + "\n" + gameStateToString(game), game.isGameOver() || opp.getLife() <= 0);
    }

    // ------------------------------------------------------------------------------ Paradoxical Outcome

    private SpellAbility paradoxWith(final String... permanents) {
        setUp(PhaseType.MAIN1);
        addCards("Island", 2, ai);
        for (final String name : permanents) {
            addCard(name, ai);
        }
        final Card outcome = addCardToZone("Paradoxical Outcome", ai, ZoneType.Hand);
        return ParadoxOutcome.sureCast(ai, abilitiesOf(outcome));
    }

    @Test
    public void paradoxicalOutcomeReturnsAllTheCheapArtifacts() {
        final SpellAbility sa = paradoxWith("Sol Ring", "Mox Pearl", "Mox Jet", "Mox Ruby", "Mox Emerald");
        AssertJUnit.assertNotNull("five artifacts are five cards for one spell\n" + gameStateToString(game), sa);
        AssertJUnit.assertEquals("every artifact is a target", 5, sa.getTargets().getTargetCards().size());
        for (final Card c : sa.getTargets().getTargetCards()) {
            AssertJUnit.assertFalse("no land is returned: " + c, c.isLand());
        }
    }

    @Test
    public void paradoxicalOutcomeKeepsTheVaultTheRingAndTezzeret() {
        final SpellAbility sa = paradoxWith("Time Vault", "The One Ring", "Tezzeret, Cruel Captain", "Sol Ring", "Mox Pearl",
                "Mox Jet", "Mox Ruby", "Mox Emerald");
        AssertJUnit.assertNotNull(gameStateToString(game), sa);
        for (final Card c : sa.getTargets().getTargetCards()) {
            AssertJUnit.assertTrue(c + " should stay in play", ParadoxOutcome.returnable(c));
        }
        AssertJUnit.assertEquals(5, sa.getTargets().getTargetCards().size());
    }

    @Test
    public void paradoxicalOutcomeWaitsForEnoughPermanents() {
        AssertJUnit.assertNull("two artifacts are not worth the spell", paradoxWith("Sol Ring", "Mox Pearl"));
    }

    @Test
    public void paradoxicalOutcomeDoesNotDeckTheAi() {
        setUp(PhaseType.MAIN1);
        addCards("Island", 2, ai);
        for (final String name : new String[] { "Sol Ring", "Mox Pearl", "Mox Jet", "Mox Ruby", "Mox Emerald" }) {
            addCard(name, ai);
        }
        while (ai.getCardsIn(ZoneType.Library).size() > 8) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        final Card outcome = addCardToZone("Paradoxical Outcome", ai, ZoneType.Hand);
        AssertJUnit.assertNull("8 cards left against a draw of 5", ParadoxOutcome.sureCast(ai, abilitiesOf(outcome)));
    }
}
