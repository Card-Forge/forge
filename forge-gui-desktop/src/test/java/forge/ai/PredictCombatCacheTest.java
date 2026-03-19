package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Validates that the predictNextCombatsRemainingLife cache and short-circuit
 * produce identical results to the uncached full simulation across 20 diverse
 * board states.
 */
public class PredictCombatCacheTest extends AITest {

    private Game setupGame() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }
        return game;
    }

    private AiController getAiController(Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    /**
     * For a given board state, calls predictNextCombatsRemainingLife without cache (fresh scope),
     * then with cache (same scope, second call), and verifies the results match.
     * Also verifies aiLifeInDanger returns a consistent boolean.
     */
    private void assertCacheMatchesUncached(Player ai, String scenario) {
        AiController aic = getAiController(ai);

        // Call 1: fresh scope, no cache (uncached result)
        aic.beginPredictCombatCacheScope();
        int uncachedNotSerious;
        int uncachedSerious;
        try {
            uncachedNotSerious = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
            uncachedSerious = ComputerUtil.predictNextCombatsRemainingLife(ai, true, false, 0, null);
        } finally {
            aic.endPredictCombatCacheScope();
        }

        // Call 2: new scope, first call populates cache, second call hits cache
        aic.beginPredictCombatCacheScope();
        int cachedNotSerious;
        int cachedSerious;
        int cachedNotSerious2;
        int cachedSerious2;
        try {
            cachedNotSerious = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
            cachedSerious = ComputerUtil.predictNextCombatsRemainingLife(ai, true, false, 0, null);
            // These should come from cache
            cachedNotSerious2 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
            cachedSerious2 = ComputerUtil.predictNextCombatsRemainingLife(ai, true, false, 0, null);
        } finally {
            aic.endPredictCombatCacheScope();
        }

        // Call 3: with excludedBlockers=null but outside scope (no cache)
        int noScopeNotSerious = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
        int noScopeSerious = ComputerUtil.predictNextCombatsRemainingLife(ai, true, false, 0, null);

        // Call 4: with checkDiff=true (bypasses short-circuit, tests full simulation path)
        aic.beginPredictCombatCacheScope();
        int checkDiffResult;
        try {
            checkDiffResult = ComputerUtil.predictNextCombatsRemainingLife(ai, false, true, 0, null);
        } finally {
            aic.endPredictCombatCacheScope();
        }

        // All paths must agree
        AssertJUnit.assertEquals(scenario + " [notSerious: uncached vs cached]", uncachedNotSerious, cachedNotSerious);
        AssertJUnit.assertEquals(scenario + " [serious: uncached vs cached]", uncachedSerious, cachedSerious);
        AssertJUnit.assertEquals(scenario + " [notSerious: cache hit]", cachedNotSerious, cachedNotSerious2);
        AssertJUnit.assertEquals(scenario + " [serious: cache hit]", cachedSerious, cachedSerious2);
        AssertJUnit.assertEquals(scenario + " [notSerious: no scope vs scoped]", uncachedNotSerious, noScopeNotSerious);
        AssertJUnit.assertEquals(scenario + " [serious: no scope vs scoped]", uncachedSerious, noScopeSerious);

        // aiLifeInDanger must match
        boolean dangerNotSerious = (uncachedNotSerious == Integer.MIN_VALUE);
        boolean dangerSerious = (uncachedSerious == Integer.MIN_VALUE);
        AssertJUnit.assertEquals(scenario + " [aiLifeInDanger notSerious]",
                dangerNotSerious, ComputerUtil.aiLifeInDanger(ai, false, 0));
        AssertJUnit.assertEquals(scenario + " [aiLifeInDanger serious]",
                dangerSerious, ComputerUtil.aiLifeInDanger(ai, true, 0));
    }

    // --- 20 Scenarios ---

    @Test
    public void test01_EmptyBoard() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "01: Empty board");
    }

    @Test
    public void test02_OpponentOnlyLands() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Forest", opp);
        for (int i = 0; i < 5; i++) addCard("Plains", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "02: Opponent only lands");
    }

    @Test
    public void test03_SmallCreaturesLowThreat() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 5; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "03: Small creatures low threat");
    }

    @Test
    public void test04_OverwhelmingAttackers() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) addCard("Serra Angel", opp);
        for (int i = 0; i < 3; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "04: Overwhelming attackers");
    }

    @Test
    public void test05_FlyersVsGround() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Serra Angel", opp);
        for (int i = 0; i < 10; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "05: Flyers vs ground");
    }

    @Test
    public void test06_DoubleStrikeAttackers() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) addCard("Fencing Ace", opp);
        for (int i = 0; i < 4; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "06: Double strike attackers");
    }

    @Test
    public void test07_FirstStrikeMix() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) addCard("White Knight", opp);
        for (int i = 0; i < 4; i++) addCard("Black Knight", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "07: First strike mix");
    }

    @Test
    public void test08_LargeTokenSwarm() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) addCard("Savannah Lions", opp);
        for (int i = 0; i < 15; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "08: Large token swarm");
    }

    @Test
    public void test09_AnthemBoostedCreatures() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Glorious Anthem", opp);
        for (int i = 0; i < 8; i++) addCard("Savannah Lions", opp);
        for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStaticAbilities();
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "09: Anthem boosted creatures");
    }

    @Test
    public void test10_HighLifeTotal() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(40, null);
        for (int i = 0; i < 6; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 3; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "10: High life total (short-circuit path)");
    }

    @Test
    public void test11_LowLifeTotal() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(3, null);
        for (int i = 0; i < 4; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 2; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "11: Low life total");
    }

    @Test
    public void test12_InfectCreatures() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) addCard("Glistener Elf", opp);
        for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "12: Infect creatures (no short-circuit)");
    }

    @Test
    public void test13_DeathtouchCreatures() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) addCard("Typhoid Rats", opp);
        for (int i = 0; i < 6; i++) addCard("Serra Angel", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "13: Deathtouch creatures");
    }

    @Test
    public void test14_TrampleCreatures() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) addCard("Colossal Dreadmaw", opp);
        for (int i = 0; i < 6; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "14: Trample creatures");
    }

    @Test
    public void test15_VigilanceCreatures() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Serra Angel", opp);
        for (int i = 0; i < 5; i++) addCard("Serra Angel", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "15: Vigilance creatures both sides");
    }

    @Test
    public void test16_WithLifePayment() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 5; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AiController aic = getAiController(ai);

        // Test with payment=5 and payment=10
        aic.beginPredictCombatCacheScope();
        int r1, r2;
        try {
            r1 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 5, null);
            r2 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 5, null);
        } finally {
            aic.endPredictCombatCacheScope();
        }
        AssertJUnit.assertEquals("16: payment=5 cache hit", r1, r2);

        aic.beginPredictCombatCacheScope();
        int r3, r4;
        try {
            r3 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 10, null);
            r4 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 10, null);
        } finally {
            aic.endPredictCombatCacheScope();
        }
        AssertJUnit.assertEquals("16: payment=10 cache hit", r3, r4);
    }

    @Test
    public void test17_ExcludedBlockersNotCached() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", opp);
        Card lion1 = addCard("Savannah Lions", ai);
        Card lion2 = addCard("Savannah Lions", ai);
        Card lion3 = addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AiController aic = getAiController(ai);

        // excludedBlockers != null should NOT be cached
        CardCollection excluded = new CardCollection();
        excluded.add(lion1);

        aic.beginPredictCombatCacheScope();
        int withExcluded, withoutExcluded;
        try {
            withExcluded = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, excluded);
            withoutExcluded = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
        } finally {
            aic.endPredictCombatCacheScope();
        }
        // These should potentially differ (fewer blockers = more danger)
        // Key assertion: the null call should NOT return the excluded-blockers result
        int fresh = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
        AssertJUnit.assertEquals("17: null excludedBlockers consistent", withoutExcluded, fresh);
    }

    @Test
    public void test18_NestedScopes() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 5; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AiController aic = getAiController(ai);

        // Outer scope
        aic.beginPredictCombatCacheScope();
        try {
            int outer1 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);

            // Inner scope (simulates doTrigger inside chooseSpellAbilityToPlay)
            aic.beginPredictCombatCacheScope();
            try {
                int inner1 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
                AssertJUnit.assertEquals("18: inner matches outer (cache survives nesting)", outer1, inner1);
            } finally {
                aic.endPredictCombatCacheScope();
            }

            // After inner scope ends, outer cache should still work
            int outer2 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null);
            AssertJUnit.assertEquals("18: outer cache survives inner scope exit", outer1, outer2);
        } finally {
            aic.endPredictCombatCacheScope();
        }

        // After all scopes end, cache should be cleared
        AssertJUnit.assertFalse("18: cache cleared after all scopes",
                aic.hasCachedPredictCombat(false, false, 0));
    }

    @Test
    public void test19_MixedKeywords() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // Diverse opponent board: flyers, first strikers, tramplers
        for (int i = 0; i < 3; i++) addCard("Serra Angel", opp);       // 4/4 flying vigilance
        for (int i = 0; i < 3; i++) addCard("White Knight", opp);      // 2/2 first strike
        for (int i = 0; i < 2; i++) addCard("Colossal Dreadmaw", opp); // 6/6 trample
        // AI board: ground blockers
        for (int i = 0; i < 10; i++) addCard("Runeclaw Bear", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "19: Mixed keywords");
    }

    @Test
    public void test20_LargeAsymmetricBoard() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // Opponent: 25 small creatures
        for (int i = 0; i < 25; i++) addCard("Savannah Lions", opp);
        // AI: 5 big creatures
        for (int i = 0; i < 5; i++) addCard("Serra Angel", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "20: Large asymmetric board");
    }

    @Test
    public void test21_AiNoCreaturesOppHasAttackers() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 8; i++) addCard("Runeclaw Bear", opp);
        // AI has lands only, no blockers at all
        for (int i = 0; i < 5; i++) addCard("Plains", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "21: AI no creatures, opp has attackers");
    }

    @Test
    public void test22_ExactLethalDamage() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(8, null);
        // 4 bears = 8 power = exactly lethal with no blocks
        for (int i = 0; i < 4; i++) addCard("Runeclaw Bear", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "22: Exact lethal damage (boundary)");
    }

    @Test
    public void test23_OneDamageUnderLethal() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(9, null);
        // 4 bears = 8 power < 9 life, short-circuit should fire
        for (int i = 0; i < 4; i++) addCard("Runeclaw Bear", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "23: One damage under lethal (short-circuit boundary)");
    }

    @Test
    public void test24_ProtectionFromColor() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // Black Knight has protection from white
        for (int i = 0; i < 5; i++) addCard("Black Knight", opp);
        // AI has only white creatures that can't block them
        for (int i = 0; i < 5; i++) addCard("White Knight", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "24: Protection from color");
    }

    @Test
    public void test25_ShadowCreatures() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) addCard("Soltari Priest", opp);  // shadow, pro red
        for (int i = 0; i < 6; i++) addCard("Runeclaw Bear", ai);    // can't block shadow
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "25: Shadow creatures (evasion)");
    }

    @Test
    public void test26_MixedInfectAndRegular() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // Mix of infect and regular damage
        for (int i = 0; i < 3; i++) addCard("Glistener Elf", opp);   // 1/1 infect
        for (int i = 0; i < 3; i++) addCard("Runeclaw Bear", opp);   // 2/2 regular
        for (int i = 0; i < 5; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "26: Mixed infect and regular attackers");
    }

    @Test
    public void test27_MultipleAnthems() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Glorious Anthem", opp);
        addCard("Crusade", opp);
        for (int i = 0; i < 10; i++) addCard("Savannah Lions", opp);  // 2/1 base, +2/+2 = 4/3
        for (int i = 0; i < 8; i++) addCard("Serra Angel", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStaticAbilities();
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "27: Multiple anthems boosting opponent");
    }

    @Test
    public void test28_LifePaymentBoundary() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(15, null);
        for (int i = 0; i < 3; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 4; i++) addCard("Savannah Lions", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AiController aic = getAiController(ai);

        // Test that within a single scope, cached result matches first computation
        // for various payment values (including short-circuit boundary at payment=9)
        for (int pay : new int[]{0, 5, 9, 14}) {
            aic.beginPredictCombatCacheScope();
            try {
                int r1 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, pay, null);
                int r2 = ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, pay, null);
                AssertJUnit.assertEquals("28: payment=" + pay + " cache hit", r1, r2);
            } finally {
                aic.endPredictCombatCacheScope();
            }
        }
    }

    @Test
    public void test29_OpponentTurnCombat() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) addCard("Runeclaw Bear", opp);
        for (int i = 0; i < 4; i++) addCard("Savannah Lions", ai);
        // Set to opponent's turn, before combat
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "29: Opponent's turn (thisCombat path)");
    }

    @Test
    public void test30_ReachVsFlying() {
        Game game = setupGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) addCard("Serra Angel", opp);       // 4/4 flying
        for (int i = 0; i < 5; i++) addCard("Silklash Spider", ai);    // 2/7 reach
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertCacheMatchesUncached(ai, "30: Reach blockers vs flying attackers");
    }
}
